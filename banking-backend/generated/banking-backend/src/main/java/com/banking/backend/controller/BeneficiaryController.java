
package com.banking.backend.controller;

import com.banking.backend.dto.BeneficiaryCreateRequest;
import com.banking.backend.dto.BeneficiaryResponse;
import com.banking.backend.entity.Beneficiary;
import com.banking.backend.service.BeneficiaryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse addBeneficiary(
            @Valid @RequestBody BeneficiaryCreateRequest request) {

        Beneficiary beneficiary = beneficiaryService.addBeneficiary(
            request.customerId(),
            request.beneficiaryName(),
            request.accountNumber(),
            request.bankName(),
            request.ifscCode()
        );

        return toResponse(beneficiary);
    }

    @GetMapping("/customer/{customerId}")
    public List<BeneficiaryResponse> getBeneficiaries(
            @PathVariable Long customerId) {

        return beneficiaryService
            .getBeneficiariesByCustomerId(customerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
            beneficiary.getId(),
            beneficiary.getCustomer().getId(),
            beneficiary.getBeneficiaryName(),
            beneficiary.getAccountNumber(),
            beneficiary.getBankName(),
            beneficiary.getIfscCode(),
            beneficiary.getCreatedAt(),
            beneficiary.getStatus()
        );
    }
}
