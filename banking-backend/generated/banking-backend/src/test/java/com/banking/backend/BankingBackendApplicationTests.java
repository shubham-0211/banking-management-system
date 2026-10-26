
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

    // Helper: register a customer and return the generated customer ID.
    private long createTestCustomer() throws Exception {
        String email =
            "test." + java.util.UUID.randomUUID() + "@example.com";

        String requestBody = """
            {
                "fullName": "Automated Test Customer",
                "email": "%s",
                "phone": "9876543210"
            }
            """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        java.util.regex.Matcher matcher =
            java.util.regex.Pattern
                .compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(responseBody);

        if (!matcher.find()) {
            throw new AssertionError("Customer ID not found");
        }

        return Long.parseLong(matcher.group(1));
    }

    // Helper: create a bank account for an existing customer.
    private MvcResult createTestAccount(
            long customerId, String accountType) throws Exception {

        String requestBody = """
            {
                "customerId": %d,
                "accountType": "%s"
            }
            """.formatted(customerId, accountType);

        return mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andReturn();
    }

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
        long customerId = createTestCustomer();

        mockMvc.perform(get("/api/customers/{id}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customerId))
            .andExpect(jsonPath("$.fullName")
                .value("Automated Test Customer"))
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
        createTestCustomer();
        createTestCustomer();

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

    // Test 11: Create a SAVINGS account with zero initial balance
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void createSavingsAccountReturnsCreatedWithZeroBalance()
            throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": %d,
                        "accountType": "SAVINGS"
                    }
                    """.formatted(customerId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.accountNumber").isString())
            .andExpect(jsonPath("$.accountNumber").value(
                org.hamcrest.Matchers.matchesPattern("\\d{12}")))
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.accountType").value("SAVINGS"))
            .andExpect(jsonPath("$.balance").value(0))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    // Test 12: Create a CURRENT account
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void createCurrentAccountReturnsCreated() throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": %d,
                        "accountType": "CURRENT"
                    }
                    """.formatted(customerId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountType").value("CURRENT"))
            .andExpect(jsonPath("$.balance").value(0))
            .andExpect(jsonPath("$.customerId").value(customerId));
    }

    // Test 13: Nonexistent customer must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void createAccountForMissingCustomerReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": 999999999,
                        "accountType": "SAVINGS"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    // Test 14: Retrieve a bank account by ID
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void getBankAccountByIdReturnsAccount() throws Exception {
        long customerId = createTestCustomer();

        MvcResult result = createTestAccount(customerId, "SAVINGS");
        org.junit.jupiter.api.Assertions.assertEquals(
            201, result.getResponse().getStatus());

        String body = result.getResponse().getContentAsString();
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern
                .compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(body);

        if (!matcher.find()) {
            throw new AssertionError("Bank account ID not found");
        }

        long accountId = Long.parseLong(matcher.group(1));

        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.accountType").value("SAVINGS"))
            .andExpect(jsonPath("$.balance").value(0));
    }

    // Test 15: Bank account not found
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void bankAccountNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Bank account not found"));
    }

    // Test 16: Missing customer ID must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void missingCustomerIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "accountType": "SAVINGS"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // Test 17: Invalid account type must be rejected
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void invalidAccountTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": 1,
                        "accountType": "INVALID"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
