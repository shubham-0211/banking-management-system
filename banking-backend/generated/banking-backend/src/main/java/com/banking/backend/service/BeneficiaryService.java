
package com.banking.backend.service;

import com.banking.backend.entity.Beneficiary;
import com.banking.backend.entity.Customer;
import com.banking.backend.repository.BeneficiaryRepository;
import com.banking.backend.repository.CustomerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            CustomerRepository customerRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Beneficiary addBeneficiary(
            Long customerId,
            String beneficiaryName,
            String accountNumber,
            String bankName,
            String ifscCode) {

        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Customer ID must be positive");
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer not found"));

        if (beneficiaryName == null || beneficiaryName.isBlank()
                || beneficiaryName.trim().length() > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Enter a valid beneficiary name");
        }

        if (accountNumber == null || accountNumber.isBlank()
                || accountNumber.trim().length() > 34) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Enter a valid account number");
        }

        if (bankName == null || bankName.isBlank()
                || bankName.trim().length() > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Enter a valid bank name");
        }

        String normalizedIfsc = ifscCode == null
                ? ""
                : ifscCode.trim().toUpperCase(Locale.ROOT);

        if (!normalizedIfsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Enter a valid 11-character IFSC code");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setCustomer(customer);
        beneficiary.setBeneficiaryName(beneficiaryName.trim());
        beneficiary.setAccountNumber(accountNumber.trim());
        beneficiary.setBankName(bankName.trim());
        beneficiary.setIfscCode(normalizedIfsc);
        beneficiary.setStatus("ACTIVE");

        return beneficiaryRepository.save(beneficiary);
    }

    @Transactional(readOnly = true)
    public List<Beneficiary> getBeneficiariesByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Customer ID must be positive");
        }

        if (!customerRepository.existsById(customerId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer not found");
        }

        return beneficiaryRepository
            .findByCustomerIdOrderByIdAsc(customerId);
    }
}
