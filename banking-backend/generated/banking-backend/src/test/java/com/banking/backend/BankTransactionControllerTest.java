
package com.banking.backend;

import com.banking.backend.entity.BankAccount;
import com.banking.backend.entity.Customer;
import com.banking.backend.repository.BankAccountRepository;
import com.banking.backend.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private long createAccount() {
        Customer customer = new Customer();
        customer.setFullName("Transaction Test Customer");
        customer.setEmail("transaction." + UUID.randomUUID() + "@example.com");

        Customer savedCustomer = customerRepository.save(customer);

        BankAccount account = new BankAccount();
        account.setCustomer(savedCustomer);
        account.setAccountType(BankAccount.AccountType.SAVINGS);
        account.setAccountNumber(
            String.format("%012d", Math.abs(UUID.randomUUID().getMostSignificantBits())
                % 1_000_000_000_000L)
        );
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(BankAccount.AccountStatus.ACTIVE);

        return bankAccountRepository.save(account).getId();
    }

    private String requestBody(long accountId, String amount) {
        return """
            {
                "accountId": %d,
                "amount": %s
            }
            """.formatted(accountId, amount);
    }

    private long createFundedAccount() throws Exception {
        long accountId = createAccount();

        mockMvc.perform(post("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(accountId, "1000.00")))
            .andExpect(status().isCreated());

        return accountId;
    }

    private BigDecimal getBalance(long accountId) throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andReturn();

        Matcher matcher = Pattern
            .compile("\"balance\"\\s*:\\s*([0-9.]+)")
            .matcher(result.getResponse().getContentAsString());

        if (!matcher.find()) {
            throw new AssertionError("Balance not found in response");
        }

        return new BigDecimal(matcher.group(1));
    }

    @Test
    @WithMockUser
    void depositIncreasesBalance() throws Exception {
        long accountId = createAccount();

        mockMvc.perform(post("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(accountId, "500.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(new BigDecimal("500.00"), getBalance(accountId));
    }

    @Test
    @WithMockUser
    void withdrawalDecreasesBalance() throws Exception {
        long accountId = createFundedAccount();

        mockMvc.perform(post("/api/transactions/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(accountId, "250.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(new BigDecimal("750.00"), getBalance(accountId));
    }

    @Test
    @WithMockUser
    void insufficientFundsDoesNotChangeBalance() throws Exception {
        long accountId = createFundedAccount();
        BigDecimal balanceBefore = getBalance(accountId);

        mockMvc.perform(post("/api/transactions/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(accountId, "1500.00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Insufficient account balance"));

        assertEquals(balanceBefore, getBalance(accountId));
    }

    @Test
    @WithMockUser
    void transferUpdatesBothBalancesAndRecordsHistory() throws Exception {
        long sourceId = createFundedAccount();
        long destinationId = createAccount();

        String body = """
            {
                "accountId": %d,
                "destinationAccountId": %d,
                "amount": 300.00
            }
            """.formatted(sourceId, destinationId);

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(new BigDecimal("700.00"), getBalance(sourceId));
        assertEquals(new BigDecimal("300.00"), getBalance(destinationId));

        mockMvc.perform(get("/api/transactions/account/{id}", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/transactions/account/{id}", destinationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void selfTransferIsRejectedWithoutChangingBalance() throws Exception {
        long accountId = createFundedAccount();
        BigDecimal balanceBefore = getBalance(accountId);

        String body = """
            {
                "accountId": %d,
                "destinationAccountId": %d,
                "amount": 100.00
            }
            """.formatted(accountId, accountId);

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Source and destination accounts must be different"));

        assertEquals(balanceBefore, getBalance(accountId));
    }
}
