package com.csye6225.webapp.service;

import com.csye6225.webapp.exception.MetadataUnavailableException;
import com.csye6225.webapp.model.MetadataResponse;
import com.csye6225.webapp.model.NetworkInterface;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class AwsMetadataService implements MetadataService {
    
    private static final String METADATA_BASE = "http://169.254.169.254/latest/meta-data/";
    private static final String TOKEN_URL = "http://169.254.169.254/latest/api/token";
    private static final int TIMEOUT_MS = 2000;
    private static final int TOKEN_TTL_SECONDS = 21600; // 6 hours (AWS max)
    
    private String cachedToken = null;
    private long tokenExpiryTime = 0;
    
    @Override
    public MetadataResponse getMetadata() {
        try {
            String token = getIMDSv2Token();
            
            String instanceId = fetchMetadata("instance-id", token);
            String availabilityZone = fetchMetadata("placement/availability-zone", token);
            String instanceType = fetchMetadata("instance-type", token);
            
            // Extract region from AZ (e.g., us-east-1a -> us-east-1)
            String region = availabilityZone.substring(0, availabilityZone.length() - 1);
            
            // Get network interfaces
            List<NetworkInterface> interfaces = getNetworkInterfaces(token);
            
            return new MetadataResponse("aws", instanceId, region, instanceType, interfaces);
            
        } catch (Exception e) {
            // Invalidate cached token on failure so next call retries
            cachedToken = null;
            tokenExpiryTime = 0;
            throw new MetadataUnavailableException("Failed to retrieve AWS metadata", e);
        }
    }
    
    private String getIMDSv2Token() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryTime) {
            return cachedToken;
        }
        
        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", String.valueOf(TOKEN_TTL_SECONDS));
        conn.setDoOutput(true);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to get IMDSv2 token: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String token = reader.readLine();
        reader.close();
        conn.disconnect();
        
        if (token == null) {
            throw new Exception("Token response was empty");
        }
        
        cachedToken = token.trim();
        // Expire 5 minutes early to avoid edge cases
        tokenExpiryTime = System.currentTimeMillis() + ((TOKEN_TTL_SECONDS - 300) * 1000L);
        return cachedToken;
    }
    
    private List<NetworkInterface> getNetworkInterfaces(String token) throws Exception {
        List<NetworkInterface> interfaces = new ArrayList<>();
        
        // Get MAC addresses
        String macsData = fetchMetadata("network/interfaces/macs/", token);
        String[] macs = macsData.trim().split("\n");
        
        for (String mac : macs) {
            mac = mac.trim();
            if (mac.isEmpty()) continue;
            
            String privateIp = fetchMetadata("network/interfaces/macs/" + mac + "local-ipv4s", token);
            String publicIp = null;
            String vpcId = null;
            
            try {
                publicIp = fetchMetadata("network/interfaces/macs/" + mac + "public-ipv4s", token);
            } catch (Exception e) {
                // Public IP may not exist
            }
            
            try {
                vpcId = fetchMetadata("network/interfaces/macs/" + mac + "vpc-id", token);
            } catch (Exception e) {
                // VPC ID may not exist
            }
            
            interfaces.add(new NetworkInterface(privateIp, publicIp, vpcId));
        }
        
        return interfaces;
    }
    
    private String fetchMetadata(String path, String token) throws Exception {
        URL url = new URL(METADATA_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        
        // Add IMDSv2 token header
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("X-aws-ec2-metadata-token", token);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to fetch metadata: " + path + " (HTTP " + responseCode + ")");
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
        
        reader.close();
        conn.disconnect();
        
        return response.toString().trim();
    }
}
