
package com.banking.backend.controller;

import com.banking.backend.dto.BankAccountCreateRequest;
import com.banking.backend.dto.BankAccountResponse;
import com.banking.backend.entity.BankAccount;
import com.banking.backend.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse createAccount(
            @Valid @RequestBody BankAccountCreateRequest request) {

        BankAccount account = bankAccountService.createAccount(
            request.customerId(),
            request.accountType()
        );

        return toResponse(account);
    }

    @GetMapping("/{id}")
    public BankAccountResponse getAccountById(@PathVariable Long id) {
        BankAccount account = bankAccountService.getAccountById(id);
        return toResponse(account);
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCustomer().getId(),
            account.getAccountType(),
            account.getBalance(),
            account.getStatus(),
            account.getCreatedAt()
        );
    }
}
