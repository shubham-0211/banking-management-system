package com.banking.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long createTestCustomer() throws Exception {
        String email =
            "beneficiary." + java.util.UUID.randomUUID() + "@example.com";

        String requestBody = """
            {
                "fullName": "Beneficiary Test Customer",
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

    private String beneficiaryRequest(
            long customerId,
            String beneficiaryName,
            String accountNumber,
            String bankName,
            String ifscCode) {

        return """
            {
                "customerId": %d,
                "beneficiaryName": "%s",
                "accountNumber": "%s",
                "bankName": "%s",
                "ifscCode": "%s"
            }
            """.formatted(
                customerId,
                beneficiaryName,
                accountNumber,
                bankName,
                ifscCode
            );
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void createBeneficiaryReturnsCreated() throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beneficiaryRequest(
                    customerId,
                    "Rahul Sharma",
                    "123456789012",
                    "Demo Bank",
                    "DEMO0123456"
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.beneficiaryName")
                .value("Rahul Sharma"))
            .andExpect(jsonPath("$.accountNumber")
                .value("123456789012"))
            .andExpect(jsonPath("$.bankName").value("Demo Bank"))
            .andExpect(jsonPath("$.ifscCode").value("DEMO0123456"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void invalidIfscReturnsBadRequest() throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beneficiaryRequest(
                    customerId,
                    "Invalid IFSC Test",
                    "123456789012",
                    "Demo Bank",
                    "BADIFSC"
                )))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void missingCustomerReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beneficiaryRequest(
                    999999999L,
                    "Missing Customer Test",
                    "112233445566",
                    "Demo Bank",
                    "DEMO0123456"
                )))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void getBeneficiariesReturnsCustomerBeneficiaries()
            throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beneficiaryRequest(
                    customerId,
                    "Rahul Sharma",
                    "123456789012",
                    "Demo Bank",
                    "DEMO0123456"
                )))
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/api/beneficiaries/customer/{customerId}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].customerId").value(customerId))
            .andExpect(jsonPath("$[0].beneficiaryName")
                .value("Rahul Sharma"))
            .andExpect(jsonPath("$[0].ifscCode").value("DEMO0123456"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void getBeneficiariesReturnsEmptyListWhenNoneExist()
            throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(
                get("/api/beneficiaries/customer/{customerId}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void getBeneficiariesForMissingCustomerReturnsNotFound()
            throws Exception {
        mockMvc.perform(get("/api/beneficiaries/customer/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void missingBeneficiaryNameReturnsBadRequest() throws Exception {
        long customerId = createTestCustomer();

        mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "customerId": %d,
                        "beneficiaryName": "",
                        "accountNumber": "123456789012",
                        "bankName": "Demo Bank",
                        "ifscCode": "DEMO0123456"
                    }
                    """.formatted(customerId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void invalidCustomerIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/beneficiaries/customer/0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Customer ID must be positive"));
    }
}