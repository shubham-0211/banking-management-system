import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
  HttpParams
} from '@angular/common/http';
import {
  Observable,
  catchError,
  throwError
} from 'rxjs';

export interface Customer {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  createdAt: string;
  status: string;
}

export interface CustomerRegistrationRequest {
  fullName: string;
  email: string;
  phone: string;
}

export interface CustomerPage {
  customers: Customer[];
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
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/customers';

  private authorizationHeader: string | null = null;

  /**
   * Stores development credentials in service memory.
   * Call this after the user enters their credentials.
   */
  setCredentials(username: string, password: string): void {
    const token = btoa(`${username}:${password}`);
    this.authorizationHeader = `Basic ${token}`;
  }

  /**
   * Clears the current in-memory authentication header.
   */
  clearCredentials(): void {
    this.authorizationHeader = null;
  }

  /**
   * Returns headers without throwing when credentials
   * have not been entered yet.
   */
  private getHeaders(): HttpHeaders {
    if (this.authorizationHeader) {
      return new HttpHeaders({
        Authorization: this.authorizationHeader
      });
    }

    return new HttpHeaders();
  }

  /**
   * Prevents API calls when credentials are missing.
   */
  private requireCredentials<T>(
    request: () => Observable<T>
  ): Observable<T> {
    return new Observable<T>((subscriber) => {
      if (!this.authorizationHeader) {
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

  getCustomers(
    page = 0,
    size = 10
  ): Observable<CustomerPage> {
    return this.requireCredentials(() => {
      const params = new HttpParams()
        .set('page', page)
        .set('size', size);

      return this.http.get<CustomerPage>(this.apiUrl, {
        params,
        headers: this.getHeaders()
      }).pipe(
        catchError(
          (error: HttpErrorResponse) => this.handleError(error)
        )
      );
    });
  }

  getCustomerById(id: number): Observable<Customer> {
    return this.requireCredentials(() =>
      this.http.get<Customer>(`${this.apiUrl}/${id}`, {
        headers: this.getHeaders()
      }).pipe(
        catchError(
          (error: HttpErrorResponse) => this.handleError(error)
        )
      )
    );
  }

  registerCustomer(
    request: CustomerRegistrationRequest
  ): Observable<Customer> {
    return this.requireCredentials(() =>
      this.http.post<Customer>(this.apiUrl, request, {
        headers: this.getHeaders()
      }).pipe(
        catchError(
          (error: HttpErrorResponse) => this.handleError(error)
        )
      )
    );
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
    } else if (error.status === 409) {
      message =
        'A customer with these details may already exist.';
    } else if (error.status === 400) {
      message =
        'Invalid customer details. Please check the form.';
    } else if (error.status >= 500) {
      message = 'The backend encountered an error.';
    }

    return throwError(() => new Error(message));
  }
}