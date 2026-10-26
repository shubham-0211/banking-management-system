
package com.banking.backend.dto;

import java.time.LocalDateTime;

public record BeneficiaryResponse(
    Long id,
    Long customerId,
    String beneficiaryName,
    String accountNumber,
    String bankName,
    String ifscCode,
    LocalDateTime createdAt,
    String status
) {}
