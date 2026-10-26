
package com.banking.backend.dto;

import com.banking.backend.entity.BankAccount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponse(
    Long id,
    String accountNumber,
    Long customerId,
    BankAccount.AccountType accountType,
    BigDecimal balance,
    BankAccount.AccountStatus status,
    LocalDateTime createdAt
) {}
