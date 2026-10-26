package com.banking.backend.controller;

import com.banking.backend.dto.CustomerRegistrationRequest;
import com.banking.backend.dto.CustomerResponse;
import com.banking.backend.entity.Customer;
import com.banking.backend.service.CustomerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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