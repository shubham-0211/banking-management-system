
package com.banking.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransactionRequest(

    @NotNull(message = "Account ID is required")
    @Positive(message = "Account ID must be positive")
    Long accountId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @Positive(message = "Destination account ID must be positive")
    Long destinationAccountId

) {}
