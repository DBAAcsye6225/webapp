package com.csye6225.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.csye6225.webapp.dto.UserCreateRequest;
import com.csye6225.webapp.dto.UserUpdateRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class WebappApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "";
    private static String testEmail = "jane.doe" + System.currentTimeMillis() + "@example.com";
    private static final String testPassword = "secureP@ssw0rd";
    private static final String newPassword = "newP@ssw0rd123";

    // ==================== Health Check API Tests ====================

    @Test
    @Order(1)
    @DisplayName("1.1 GET /healthz - Service is healthy and database connection is successful")
    void testHealthCheckSuccess() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(content().string(""));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 GET /healthz - Bad Request - request contains query parameters")
    void testHealthCheckWithQueryParams() throws Exception {
        mockMvc.perform(get("/healthz?test=123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("1.3 POST /healthz - Method Not Allowed")
    void testHealthCheckPostNotAllowed() throws Exception {
        mockMvc.perform(post("/healthz"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 PUT /healthz - Method Not Allowed")
    void testHealthCheckPutNotAllowed() throws Exception {
        mockMvc.perform(put("/healthz"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(5)
    @DisplayName("1.5 DELETE /healthz - Method Not Allowed")
    void testHealthCheckDeleteNotAllowed() throws Exception {
        mockMvc.perform(delete("/healthz"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(6)
    @DisplayName("1.6 HEAD /healthz - Method Not Allowed")
    void testHealthCheckHeadNotAllowed() throws Exception {
        mockMvc.perform(head("/healthz"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(7)
    @DisplayName("1.7 OPTIONS /healthz - Method Not Allowed")
    void testHealthCheckOptionsNotAllowed() throws Exception {
        mockMvc.perform(options("/healthz"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ==================== User Creation API Tests ====================

    @Test
    @Order(8)
    @DisplayName("2.1 User created successfully")
    void testCreateUserSuccess() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(testEmail);
        request.setPassword(testPassword);
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(testEmail))
                .andExpect(jsonPath("$.first_name").value("Jane"))
                .andExpect(jsonPath("$.last_name").value("Doe"))
                .andExpect(jsonPath("$.account_created").exists())
                .andExpect(jsonPath("$.account_updated").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(9)
    @DisplayName("2.2 Create User - Invalid email format")
    void testCreateUserInvalidEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("invalid-email");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("2.3 Create User - Missing required fields")
    void testCreateUserMissingFields() throws Exception {
        String jsonWithMissingField = "{\"last_name\":\"Doe\",\"username\":\"" + testEmail + "\",\"password\":\"" + testPassword + "\"}";

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithMissingField))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("2.4 Create User - Password Too Weak")
    void testCreateUserWeakPassword() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("weakpass@example.com");
        request.setPassword("weak");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("2.5 Conflict - User with this email already exists")
    void testCreateUserDuplicateEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(testEmail); // Same as test 6
        request.setPassword(testPassword);
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @Order(13)
    @DisplayName("2.6 Create User - Content-Type must be application/json")
    void testCreateUserWrongContentType() throws Exception {
        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"first_name\":\"Test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ==================== Get User Info API Tests ====================

    @Test
    @Order(14)
    @DisplayName("3.1 Get User - User information retrieved successfully")
    void testGetUserSuccess() throws Exception {
        mockMvc.perform(get("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testEmail))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(15)
    @DisplayName("3.2 Get User - Missing or invalid authentication credentials")
    void testGetUserNoAuth() throws Exception {
        mockMvc.perform(get("/v1/user/self"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(16)
    @DisplayName("3.4 Get User - User account not found")
    void testGetUserNotFound() throws Exception {
        mockMvc.perform(get("/v1/user/self")
                .header("Authorization", getBasicAuthHeader("nonexistent@example.com", "password123")))
                .andExpect(status().isNotFound());
    }

    // ==================== Update User Info API Tests ====================

    @Test
    @Order(17)
    @DisplayName("4.1 User updated successfully - no content returned")
    void testUpdateUserSuccess() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Janet");
        request.setLastName("Smith");

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(18)
    @DisplayName("4.2 Verify Update - First name and Last name")
    void testVerifyUserUpdate() throws Exception {
        mockMvc.perform(get("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first_name").value("Janet"))
                .andExpect(jsonPath("$.last_name").value("Smith"));
    }

    @Test
    @Order(19)
    @DisplayName("4.3 Update User - Only First Name")
    void testUpdateUserPartial() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Jane");

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(20)
    @DisplayName("4.4 Update Password")
    void testUpdatePassword() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword(newPassword);

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(21)
    @DisplayName("4.5 Get User - New Password")
    void testGetUserWithNewPassword() throws Exception {
        mockMvc.perform(get("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    @DisplayName("4.6 Get User - Old Password (Should Fail)")
    void testGetUserWithOldPassword() throws Exception {
        mockMvc.perform(get("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, testPassword)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(23)
    @DisplayName("4.7 Update User - Try to update username (read-only field)")
    void testUpdateUserReadOnlyUsername() throws Exception {
        String jsonWithUsername = "{\"username\":\"newemail@example.com\"}";

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithUsername))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be updated")));
    }

    @Test
    @Order(24)
    @DisplayName("4.8 Update User - Try ID (Disallowed)")
    void testUpdateUserReadOnlyId() throws Exception {
        String jsonWithId = "{\"id\":\"00000000-0000-0000-0000-000000000000\"}";

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(25)
    @DisplayName("4.9 Update User - Try account_created")
    void testUpdateUserReadOnlyAccountCreated() throws Exception {
        String jsonWithAccountCreated = "{\"account_created\":\"2020-01-01T00:00:00.000Z\"}";

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithAccountCreated))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(26)
    @DisplayName("4.10 Update User - Try account_updated")
    void testUpdateUserReadOnlyAccountUpdated() throws Exception {
        String jsonWithAccountUpdated = "{\"account_updated\":\"2020-01-01T00:00:00.000Z\"}";

        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithAccountUpdated))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(27)
    @DisplayName("4.11 Update User - Missing or invalid authentication credentials")
    void testUpdateUserNoAuth() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Test");

        mockMvc.perform(put("/v1/user/self")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(28)
    @DisplayName("4.12 Update User - Wrong Content-Type")
    void testUpdateUserWrongContentType() throws Exception {
        mockMvc.perform(put("/v1/user/self")
                .header("Authorization", getBasicAuthHeader(testEmail, newPassword))
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"first_name\":\"Test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

     // ==================== Demo: Intentional Failure ====================
    // @Test
    // @Order(27)
    // @DisplayName("Demo: Intentional test failure to demonstrate branch protection")
    // void testIntentionalFailureForDemo() {
    //     fail("This test intentionally fails to demonstrate that CI/CD prevents merging failed tests");
    // }

    // Helper method for Basic Auth
    private String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
