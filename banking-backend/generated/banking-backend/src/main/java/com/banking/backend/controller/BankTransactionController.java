
package com.banking.backend.controller;

import com.banking.backend.dto.TransactionRequest;
import com.banking.backend.dto.TransactionResponse;
import com.banking.backend.entity.BankTransaction;
import com.banking.backend.service.BankTransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    public BankTransactionController(
            BankTransactionService bankTransactionService) {
        this.bankTransactionService = bankTransactionService;
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @Valid @RequestBody TransactionRequest request) {

        BankTransaction transaction = bankTransactionService.deposit(
            request.accountId(),
            request.amount()
        );

        return toResponse(transaction);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(
            @Valid @RequestBody TransactionRequest request) {

        BankTransaction transaction = bankTransactionService.withdraw(
            request.accountId(),
            request.amount()
        );

        return toResponse(transaction);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @Valid @RequestBody TransactionRequest request) {

        if (request.destinationAccountId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Destination account ID is required for a transfer"
            );
        }

        BankTransaction transaction = bankTransactionService.transfer(
            request.accountId(),
            request.destinationAccountId(),
            request.amount()
        );

        return toResponse(transaction);
    }

    @GetMapping("/account/{accountId}")
    public Map<String, Object> getTransactionHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<BankTransaction> transactionPage =
            bankTransactionService.getTransactionHistory(
                accountId, page, size
            );

        return Map.of(
            "transactions", transactionPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList(),
            "currentPage", transactionPage.getNumber(),
            "pageSize", transactionPage.getSize(),
            "totalElements", transactionPage.getTotalElements(),
            "totalPages", transactionPage.getTotalPages(),
            "first", transactionPage.isFirst(),
            "last", transactionPage.isLast()
        );
    }

    private TransactionResponse toResponse(
            BankTransaction transaction) {

        return new TransactionResponse(
            transaction.getId(),
            transaction.getAccount().getId(),
            transaction.getTransactionType(),
            transaction.getAmount(),
            transaction.getStatus(),
            transaction.getDescription(),
            transaction.getCreatedAt()
        );
    }
}
