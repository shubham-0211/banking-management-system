package com.banking.backend.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    LocalDateTime createdAt,
    String status
) {
}