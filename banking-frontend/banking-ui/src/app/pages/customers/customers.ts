import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';

import {
  Customer,
  CustomerRegistrationRequest,
  CustomerService
} from '../../services/customer';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './customers.html',
  styleUrl: './customers.css'
})
export class Customers {
  private readonly customerService = inject(CustomerService);
  private readonly cdr = inject(ChangeDetectorRef);

  customers: Customer[] = [];
  currentPage = 0;
  readonly pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  loading = false;
  submitting = false;
  loggingIn = false;
  isAuthenticated = false;

  errorMessage = '';
  successMessage = '';

  username = 'banking-dev';
  password = '';

  form: CustomerRegistrationRequest = {
    fullName: '',
    email: '',
    phone: ''
  };

  login(): void {
    if (this.loggingIn || !this.username.trim() || !this.password) {
      this.errorMessage = 'Enter your username and password.';
      return;
    }

    this.loggingIn = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.cdr.detectChanges();

    this.customerService.setCredentials(
      this.username.trim(),
      this.password
    );

    this.customerService.getCustomers(0, this.pageSize).subscribe({
      next: (result) => {
        this.customers = result.customers;
        this.currentPage = result.currentPage;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;

        this.isAuthenticated = true;
        this.password = '';
        this.loggingIn = false;
        this.errorMessage = '';

        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.customerService.clearCredentials();
        this.isAuthenticated = false;
        this.errorMessage =
          error.message || 'Login failed. Check your credentials.';
        this.loggingIn = false;

        this.cdr.detectChanges();
      }
    });
  }

  logout(): void {
    this.customerService.clearCredentials();

    this.isAuthenticated = false;
    this.customers = [];
    this.currentPage = 0;
    this.totalPages = 0;
    this.totalElements = 0;
    this.password = '';
    this.successMessage = '';
    this.errorMessage = '';

    this.cdr.detectChanges();
  }

  loadCustomers(page = 0): void {
    if (!this.isAuthenticated || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.customerService.getCustomers(page, this.pageSize).subscribe({
      next: (result) => {
        this.customers = result.customers;
        this.currentPage = result.currentPage;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;

        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.errorMessage = error.message || 'Unable to load customers.';
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  registerCustomer(customerForm: NgForm): void {
    if (!this.isAuthenticated) {
      this.errorMessage = 'Please log in before registering a customer.';
      return;
    }

    if (customerForm.invalid || this.submitting) {
      customerForm.control.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: CustomerRegistrationRequest = {
      fullName: this.form.fullName.trim(),
      email: this.form.email.trim(),
      phone: this.form.phone.trim()
    };

    this.customerService.registerCustomer(request).subscribe({
      next: (createdCustomer) => {
        this.successMessage =
          `Customer ${createdCustomer.fullName} registered successfully.`;

        this.form = { fullName: '', email: '', phone: '' };
        customerForm.resetForm(this.form);
        this.submitting = false;

        this.cdr.detectChanges();
        this.loadCustomers(0);
      },
      error: (error: Error) => {
        this.errorMessage =
          error.message || 'Customer registration failed.';
        this.submitting = false;

        this.cdr.detectChanges();
      }
    });
  }

  previousPage(): void {
    if (!this.loading && this.currentPage > 0) {
      this.loadCustomers(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (!this.loading && this.currentPage + 1 < this.totalPages) {
      this.loadCustomers(this.currentPage + 1);
    }
  }
}