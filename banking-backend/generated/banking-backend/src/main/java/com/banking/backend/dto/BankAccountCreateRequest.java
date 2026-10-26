
package com.banking.backend.dto;

import com.banking.backend.entity.BankAccount;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BankAccountCreateRequest(

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    Long customerId,

    @NotNull(message = "Account type is required")
    BankAccount.AccountType accountType

) {}
