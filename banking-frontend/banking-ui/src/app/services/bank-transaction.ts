
import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';
import {
  Observable,
  catchError,
  throwError
} from 'rxjs';

import { CustomerService } from './customer';

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
export type TransactionStatus = 'SUCCESS' | 'FAILED';

export interface TransactionRequest {
  accountId: number;
  amount: number;
  destinationAccountId?: number;
}

export interface BankTransaction {
  id: number;
  accountId: number;
  transactionType: TransactionType;
  amount: number;
  status: TransactionStatus;
  description: string;
  createdAt: string;
}

export interface TransactionHistoryPage {
  transactions: BankTransaction[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BankTransactionService {
  private readonly http = inject(HttpClient);
  private readonly customerService = inject(CustomerService);

  private readonly apiUrl = 'http://localhost:8080/api/transactions';

  deposit(request: TransactionRequest): Observable<BankTransaction> {
    return this.postTransaction('/deposit', request);
  }

  withdraw(request: TransactionRequest): Observable<BankTransaction> {
    return this.postTransaction('/withdraw', request);
  }

  transfer(request: TransactionRequest): Observable<BankTransaction> {
    return this.postTransaction('/transfer', request);
  }

  getTransactionHistory(
    accountId: number,
    page = 0,
    size = 10
  ): Observable<TransactionHistoryPage> {
    return this.withCredentials(() =>
      this.http.get<TransactionHistoryPage>(
        `${this.apiUrl}/account/${accountId}`,
        {
          headers: this.customerService.getAuthHeaders(),
          params: {
            page: page.toString(),
            size: size.toString()
          }
        }
      ).pipe(
        catchError((error: HttpErrorResponse) => this.handleError(error))
      )
    );
  }

  private postTransaction(
    endpoint: string,
    request: TransactionRequest
  ): Observable<BankTransaction> {
    return this.withCredentials(() =>
      this.http.post<BankTransaction>(
        `${this.apiUrl}${endpoint}`,
        request,
        {
          headers: this.customerService.getAuthHeaders()
        }
      ).pipe(
        catchError((error: HttpErrorResponse) => this.handleError(error))
      )
    );
  }

  private withCredentials<T>(
    request: () => Observable<T>
  ): Observable<T> {
    return new Observable<T>((subscriber) => {
      if (!this.customerService.hasCredentials()) {
        subscriber.error(
          new Error(
            'Please sign in with your development credentials first.'
          )
        );
        return;
      }

      const subscription = request().subscribe(subscriber);

      return () => subscription.unsubscribe();
    });
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let message = 'The transaction could not be completed. Please try again.';

    if (error.status === 0) {
      message =
        'Cannot connect to the backend. Check that Spring Boot is running.';
    } else if (error.status === 401) {
      message = 'Authentication failed. Check your username and password.';
    } else if (error.status === 403) {
      message = 'Access denied.';
    } else if (error.status === 400) {
      message =
        error.error?.message ||
        'The transaction was rejected. Check the amount, account status, and available balance.';
    } else if (error.status === 404) {
      message = 'The selected account was not found.';
    } else if (error.status >= 500) {
      message = 'The backend encountered an error. Check the server logs.';
    }

    return throwError(() => new Error(message));
  }
}
