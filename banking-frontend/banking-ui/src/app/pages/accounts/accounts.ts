
import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';

import {
  BankAccount,
  BankAccountService
} from '../../services/bank-account';
import { CustomerService } from '../../services/customer';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './accounts.html',
  styleUrl: './accounts.css'
})
export class Accounts {
  private readonly accountService = inject(BankAccountService);
  private readonly customerService = inject(CustomerService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  customerId: number | null = null;
  accountType: 'SAVINGS' | 'CURRENT' = 'SAVINGS';

  accounts: BankAccount[] = [];
  loading = false;
  creating = false;
  errorMessage = '';
  successMessage = '';

  get isAuthenticated(): boolean {
    return this.customerService.hasCredentials();
  }

  goToSignIn(): void {
    this.router.navigate(['/customers']);
  }

  loadAccounts(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.accounts = [];

    if (!this.isAuthenticated) {
      this.errorMessage =
        'Please sign in with your development credentials first.';
      return;
    }

    if (
      this.customerId === null ||
      !Number.isInteger(Number(this.customerId)) ||
      Number(this.customerId) <= 0
    ) {
      this.errorMessage = 'Enter a valid positive customer ID.';
      return;
    }

    this.loading = true;

    this.accountService
      .getAccountsByCustomerId(Number(this.customerId))
      .subscribe({
        next: (accounts) => {
          this.accounts = accounts;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (error: Error) => {
          this.loading = false;
          this.errorMessage =
            error.message || 'Unable to load bank accounts.';
          this.cdr.detectChanges();
        }
      });
  }

  createAccount(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.isAuthenticated) {
      this.errorMessage =
        'Please sign in with your development credentials first.';
      return;
    }

    if (
      this.customerId === null ||
      !Number.isInteger(Number(this.customerId)) ||
      Number(this.customerId) <= 0
    ) {
      this.errorMessage = 'Enter a valid positive customer ID.';
      return;
    }

    this.creating = true;

    this.accountService.createAccount({
      customerId: Number(this.customerId),
      accountType: this.accountType
    }).subscribe({
      next: (account) => {
        this.creating = false;
        this.successMessage =
          `Account created successfully. Account ID: ${account.id}`;

        this.loadAccounts();
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.creating = false;
        this.errorMessage =
          error.message || 'Unable to create the bank account.';
        this.cdr.detectChanges();
      }
    });
  }
}
