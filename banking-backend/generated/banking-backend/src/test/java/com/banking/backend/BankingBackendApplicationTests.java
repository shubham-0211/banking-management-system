
package com.banking.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Test 1: Health endpoint
    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("banking-backend"));
    }

    // Test 2: Customer registration
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void customerRegistrationReturnsCreated() throws Exception {
        String uniqueEmail =
            "test." + java.util.UUID.randomUUID() + "@example.com";

        String requestBody = """
            {
                "fullName": "Automated Test Customer",
                "email": "%s",
                "phone": "9876543210"
            }
            """.formatted(uniqueEmail);

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.fullName")
                .value("Automated Test Customer"))
            .andExpect(jsonPath("$.email").value(uniqueEmail))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // Test 3: Duplicate email must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void duplicateEmailReturnsConflict() throws Exception {
        String email =
            "duplicate." + java.util.UUID.randomUUID() + "@example.com";

        String requestBody = """
            {
                "fullName": "Duplicate Test Customer",
                "email": "%s",
                "phone": "9876543210"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value("A customer with this email already exists."));
    }

    // Test 4: Invalid email must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void invalidEmailReturnsBadRequest() throws Exception {
        String requestBody = """
            {
                "fullName": "Invalid Email Customer",
                "email": "customerexample.com",
                "phone": "9876543210"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // Test 5: Missing full name must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void missingFullNameReturnsBadRequest() throws Exception {
        String requestBody = """
            {
                "fullName": "",
                "email": "missing.name@example.com",
                "phone": "9876543210"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message")
                .value("fullName: Full name is required"));
    }

    // Test 6: Retrieve a customer by ID
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void getCustomerByIdReturnsCustomer() throws Exception {
        String email =
            "retrieve." + java.util.UUID.randomUUID() + "@example.com";

        String requestBody = """
            {
                "fullName": "Customer Retrieval Test",
                "email": "%s",
                "phone": "9876543210"
            }
            """.formatted(email);

        MvcResult registrationResult = mockMvc.perform(
                post("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody =
            registrationResult.getResponse().getContentAsString();

        java.util.regex.Matcher matcher =
            java.util.regex.Pattern
                .compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(responseBody);

        if (!matcher.find()) {
            throw new AssertionError(
                "Customer ID was not found in the registration response"
            );
        }

        long customerId = Long.parseLong(matcher.group(1));

        mockMvc.perform(get("/api/customers/{id}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customerId))
            .andExpect(jsonPath("$.fullName")
                .value("Customer Retrieval Test"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.phone").value("9876543210"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // Test 7: Customer not found
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void customerNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/customers/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message")
                .value("Customer not found"));
    }

    // Test 8: Customer list pagination
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void customerListPaginationReturnsCorrectMetadata() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customers.length()").value(2))
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.pageSize").value(2))
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.totalPages").isNumber())
            .andExpect(jsonPath("$.first").value(true));
    }

    // Test 9: Negative page number must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void negativePageNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "-1")
                .param("size", "2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Page number cannot be negative"));
    }

    // Test 10: Page size above the allowed limit must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void pageSizeAboveLimitReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "0")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Page size must be between 1 and 100"));
    }
}
