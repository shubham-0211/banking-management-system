
package com.banking.backend.service;

import com.banking.backend.entity.BankAccount;
import com.banking.backend.entity.Customer;
import com.banking.backend.repository.BankAccountRepository;
import com.banking.backend.repository.CustomerRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BankAccount createAccount(
            Long customerId,
            BankAccount.AccountType accountType) {

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer not found"
            ));

        if (accountType == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Account type is required"
            );
        }

        BankAccount account = new BankAccount();
        account.setCustomer(customer);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setAccountNumber(generateUniqueAccountNumber());

        return bankAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public BankAccount getAccountById(Long id) {
        return bankAccountRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Bank account not found"
            ));
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String accountNumber =
                String.format("%012d", secureRandom.nextLong(1_000_000_000_000L));

            if (!bankAccountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unable to generate a unique account number. Please try again."
        );
    }
}
