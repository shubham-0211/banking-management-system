
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

export interface BankAccount {
  id: number;
  accountNumber: string;
  customerId: number;
  accountType: 'SAVINGS' | 'CURRENT';
  balance: number;
  status: 'ACTIVE' | 'CLOSED';
  createdAt: string;
}

export interface BankAccountCreateRequest {
  customerId: number;
  accountType: 'SAVINGS' | 'CURRENT';
}

@Injectable({
  providedIn: 'root'
})
export class BankAccountService {
  private readonly http = inject(HttpClient);
  private readonly customerService = inject(CustomerService);

  private readonly apiUrl = 'http://localhost:8080/api/accounts';

  getAccountsByCustomerId(
    customerId: number
  ): Observable<BankAccount[]> {
    return this.requireCredentials(() =>
      this.http.get<BankAccount[]>(
        `${this.apiUrl}/customer/${customerId}`,
        {
          headers: this.customerService.getAuthHeaders()
        }
      ).pipe(
        catchError((error: HttpErrorResponse) =>
          this.handleError(error)
        )
      )
    );
  }

  getAccountById(id: number): Observable<BankAccount> {
    return this.requireCredentials(() =>
      this.http.get<BankAccount>(
        `${this.apiUrl}/${id}`,
        {
          headers: this.customerService.getAuthHeaders()
        }
      ).pipe(
        catchError((error: HttpErrorResponse) =>
          this.handleError(error)
        )
      )
    );
  }

  createAccount(
    request: BankAccountCreateRequest
  ): Observable<BankAccount> {
    return this.requireCredentials(() =>
      this.http.post<BankAccount>(
        this.apiUrl,
        request,
        {
          headers: this.customerService.getAuthHeaders()
        }
      ).pipe(
        catchError((error: HttpErrorResponse) =>
          this.handleError(error)
        )
      )
    );
  }

  private requireCredentials<T>(
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

  private handleError(
    error: HttpErrorResponse
  ): Observable<never> {
    let message = 'Something went wrong. Please try again.';

    if (error.status === 0) {
      message =
        'Cannot connect to the backend. Check that Spring Boot is running.';
    } else if (error.status === 401) {
      message =
        'Authentication failed. Check your username and password.';
    } else if (error.status === 403) {
      message = 'Access denied.';
    } else if (error.status === 400) {
      message =
        'Invalid account details. Check the customer ID and account type.';
    } else if (error.status === 404) {
      message =
        'Customer or bank account not found. Check the supplied ID.';
    } else if (error.status >= 500) {
      message = 'The backend encountered an error.';
    }

    return throwError(() => new Error(message));
  }
}
