package com.banking.backend.controller;

import com.banking.backend.dto.CustomerRegistrationRequest;
import com.banking.backend.dto.CustomerResponse;
import com.banking.backend.entity.Customer;
import com.banking.backend.service.CustomerService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {

        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        try {
            Customer savedCustomer =
                customerService.registerCustomer(customer);

            return toResponse(savedCustomer);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, ex.getMessage()
            );
        }
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return toResponse(customer);
    }

    @GetMapping
    public Map<String, Object> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Customer> customerPage =
            customerService.getCustomers(page, size);

        return Map.of(
            "customers", customerPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList(),
            "currentPage", customerPage.getNumber(),
            "pageSize", customerPage.getSize(),
            "totalElements", customerPage.getTotalElements(),
            "totalPages", customerPage.getTotalPages(),
            "first", customerPage.isFirst(),
            "last", customerPage.isLast()
        );
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getCreatedAt(),
            customer.getStatus()
        );
    }
}