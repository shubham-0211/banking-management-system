
package com.banking.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BeneficiaryCreateRequest(

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    Long customerId,

    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 100, message = "Beneficiary name cannot exceed 100 characters")
    String beneficiaryName,

    @NotBlank(message = "Account number is required")
    @Size(max = 34, message = "Account number cannot exceed 34 characters")
    String accountNumber,

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    String bankName,

    @NotBlank(message = "IFSC code is required")
    @Size(min = 11, max = 11, message = "IFSC code must be 11 characters")
    String ifscCode
) {}
