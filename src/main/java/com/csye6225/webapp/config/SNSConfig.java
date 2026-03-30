package com.csye6225.webapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Configuration
public class SNSConfig {

    private static final Logger logger = LoggerFactory.getLogger(SNSConfig.class);

    @Value("${aws.sns.topic-arn:}")
    private String topicArn;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder().build();
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void publishMessage(SnsClient snsClient, String topicArn, String message) {
        if (topicArn == null || topicArn.isBlank()) {
            logger.warn("SNS_TOPIC_ARN is not set. Skipping SNS publish for local/dev mode.");
            return;
        }

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();

        snsClient.publish(publishRequest);
    }
}
