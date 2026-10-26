
package com.banking.backend.service;

import com.banking.backend.entity.BankAccount;
import com.banking.backend.entity.BankTransaction;
import com.banking.backend.repository.BankAccountRepository;
import com.banking.backend.repository.BankTransactionRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankTransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public BankTransactionService(
            BankAccountRepository bankAccountRepository,
            BankTransactionRepository bankTransactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @Transactional
    public BankTransaction deposit(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        BankAccount account = getActiveAccountForUpdate(accountId);

        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);

        return saveTransaction(
            account,
            BankTransaction.TransactionType.DEPOSIT,
            amount,
            "Deposit successful"
        );
    }

    @Transactional
    public BankTransaction withdraw(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        BankAccount account = getActiveAccountForUpdate(accountId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Insufficient account balance"
            );
        }

        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);

        return saveTransaction(
            account,
            BankTransaction.TransactionType.WITHDRAWAL,
            amount,
            "Withdrawal successful"
        );
    }

    @Transactional
    public BankTransaction transfer(
            Long sourceAccountId,
            Long destinationAccountId,
            BigDecimal amount) {

        validateAmount(amount);

        if (sourceAccountId == null || sourceAccountId <= 0
                || destinationAccountId == null
                || destinationAccountId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Both account IDs must be positive"
            );
        }

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Source and destination accounts must be different"
            );
        }

        // Lock accounts in a consistent ID order to reduce deadlocks.
        Long firstId = Math.min(sourceAccountId, destinationAccountId);
        Long secondId = Math.max(sourceAccountId, destinationAccountId);

        BankAccount firstAccount = getActiveAccountForUpdate(firstId);
        BankAccount secondAccount = getActiveAccountForUpdate(secondId);

        BankAccount sourceAccount =
            sourceAccountId.equals(firstId) ? firstAccount : secondAccount;

        BankAccount destinationAccount =
            destinationAccountId.equals(firstId) ? firstAccount : secondAccount;

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Insufficient account balance"
            );
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(
            destinationAccount.getBalance().add(amount)
        );

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);

        saveTransaction(
            sourceAccount,
            BankTransaction.TransactionType.TRANSFER,
            amount,
            "Transfer to account " + destinationAccount.getAccountNumber()
        );

        return saveTransaction(
            destinationAccount,
            BankTransaction.TransactionType.TRANSFER,
            amount,
            "Transfer from account " + sourceAccount.getAccountNumber()
        );
    }

    @Transactional(readOnly = true)
    public Page<BankTransaction> getTransactionHistory(
            Long accountId, int page, int size) {

        if (accountId == null || accountId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Account ID must be positive"
            );
        }

        if (!bankAccountRepository.existsById(accountId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bank account not found"
            );
        }

        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return bankTransactionRepository.findByAccountId(accountId, pageable);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transaction amount must be greater than zero"
            );
        }

        if (amount.scale() > 2) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transaction amount cannot have more than two decimal places"
            );
        }

        if (amount.precision() > 19) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transaction amount is too large"
            );
        }
    }

    private BankAccount getActiveAccountForUpdate(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Account ID must be positive"
            );
        }

        BankAccount account = bankAccountRepository.findByIdForUpdate(accountId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bank account not found"
            ));

        if (account.getStatus() != BankAccount.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transactions are not allowed on a closed account"
            );
        }

        return account;
    }

    private BankTransaction saveTransaction(
            BankAccount account,
            BankTransaction.TransactionType type,
            BigDecimal amount,
            String description) {

        BankTransaction transaction = new BankTransaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setStatus(BankTransaction.TransactionStatus.SUCCESS);
        transaction.setDescription(description);

        return bankTransactionRepository.save(transaction);
    }
}
