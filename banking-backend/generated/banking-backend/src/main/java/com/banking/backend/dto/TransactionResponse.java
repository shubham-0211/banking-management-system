package com.banking.backend.dto;

import com.banking.backend.entity.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    Long accountId,
    BankTransaction.TransactionType transactionType,
    BigDecimal amount,
    BankTransaction.TransactionStatus status,
    String description,
    LocalDateTime createdAt
) {}
