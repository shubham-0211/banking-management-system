import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  BankTransactionService,
  BankTransaction,
  TransactionHistoryPage
} from '../../services/bank-transaction';

import {
  BankAccountService,
  BankAccount
} from '../../services/bank-account';

import {
  CustomerService,
  Customer
} from '../../services/customer';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class Transactions implements OnInit {
  private readonly transactionService = inject(BankTransactionService);
  private readonly accountService = inject(BankAccountService);
  private readonly customerService = inject(CustomerService);
  private readonly cdr = inject(ChangeDetectorRef);

  customers: Customer[] = [];
  accounts: BankAccount[] = [];
  transactions: BankTransaction[] = [];

  selectedCustomerId: number | null = null;
  selectedAccountId: number | null = null;

  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' = 'DEPOSIT';
  amount: number | null = null;
  destinationAccountId: number | null = null;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  loadingCustomers = false;
  loadingAccounts = false;
  loadingHistory = false;
  submitting = false;

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loadingCustomers = true;
    this.errorMessage = '';

    this.customerService.getCustomers(0, 100).subscribe({
      next: (page) => {
        this.customers = page.customers ?? [];
        this.loadingCustomers = false;

        if (this.customers.length > 0) {
          this.selectedCustomerId = this.customers[0].id;
          this.loadAccounts();
        } else {
          this.accounts = [];
          this.transactions = [];
          this.selectedCustomerId = null;
          this.selectedAccountId = null;
        }

        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.loadingCustomers = false;
        this.errorMessage =
          error.message ||
          'Unable to load customers. You may need to sign in first.';
        this.cdr.detectChanges();
      }
    });
  }

  onCustomerChange(): void {
    this.selectedAccountId = null;
    this.accounts = [];
    this.transactions = [];
    this.currentPage = 0;
    this.totalPages = 0;
    this.totalElements = 0;
    this.loadAccounts();
  }

  loadAccounts(): void {
    if (this.selectedCustomerId === null) {
      this.accounts = [];
      this.transactions = [];
      this.selectedAccountId = null;
      return;
    }

    this.loadingAccounts = true;
    this.errorMessage = '';

    this.accountService
      .getAccountsByCustomerId(this.selectedCustomerId)
      .subscribe({
        next: (accounts: BankAccount[]) => {
          this.accounts = accounts ?? [];
          this.loadingAccounts = false;

          if (this.accounts.length > 0) {
            this.selectedAccountId = this.accounts[0].id;
            this.currentPage = 0;
            this.loadHistory();
          } else {
            this.selectedAccountId = null;
            this.transactions = [];
            this.totalPages = 0;
            this.totalElements = 0;
          }

          this.cdr.detectChanges();
        },
        error: (error: Error) => {
          this.loadingAccounts = false;
          this.errorMessage =
            error.message || 'Unable to load bank accounts.';
          this.cdr.detectChanges();
        }
      });
  }

  onAccountChange(): void {
    this.currentPage = 0;
    this.transactions = [];
    this.loadHistory();
  }

  loadHistory(): void {
    if (this.selectedAccountId === null) {
      this.transactions = [];
      this.totalPages = 0;
      this.totalElements = 0;
      return;
    }

    this.loadingHistory = true;
    this.errorMessage = '';

    this.transactionService
      .getTransactionHistory(
        this.selectedAccountId,
        this.currentPage,
        this.pageSize
      )
      .subscribe({
        next: (page: TransactionHistoryPage) => {
          this.transactions = page.transactions ?? [];
          this.currentPage = page.currentPage;
          this.totalPages = page.totalPages;
          this.totalElements = page.totalElements;
          this.loadingHistory = false;
          this.cdr.detectChanges();
        },
        error: (error: Error) => {
          this.loadingHistory = false;
          this.errorMessage =
            error.message || 'Unable to load transaction history.';
          this.cdr.detectChanges();
        }
      });
  }

  submitTransaction(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.selectedAccountId === null) {
      this.errorMessage = 'Please select an account.';
      return;
    }

    if (
      this.amount === null ||
      !Number.isFinite(this.amount) ||
      this.amount <= 0
    ) {
      this.errorMessage = 'Enter an amount greater than zero.';
      return;
    }

    if (
      this.transactionType === 'TRANSFER' &&
      (this.destinationAccountId === null ||
        this.destinationAccountId === this.selectedAccountId)
    ) {
      this.errorMessage =
        'Select a destination account different from the source account.';
      return;
    }

    const request = {
      accountId: this.selectedAccountId,
      amount: this.amount,
      ...(this.transactionType === 'TRANSFER'
        ? { destinationAccountId: this.destinationAccountId! }
        : {})
    };

    this.submitting = true;

    const operation =
      this.transactionType === 'DEPOSIT'
        ? this.transactionService.deposit(request)
        : this.transactionType === 'WITHDRAWAL'
          ? this.transactionService.withdraw(request)
          : this.transactionService.transfer(request);

    operation.subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage =
          `${this.transactionType} completed successfully.`;
        this.amount = null;
        this.destinationAccountId = null;
        this.currentPage = 0;
        this.loadHistory();
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.submitting = false;
        this.errorMessage = error.message || 'Transaction failed.';
        this.cdr.detectChanges();
      }
    });
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadHistory();
    }
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.currentPage++;
      this.loadHistory();
    }
  }

  trackTransaction(
    _index: number,
    transaction: BankTransaction
  ): number {
    return transaction.id;
  }
}