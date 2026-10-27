import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import {
  Customer,
  CustomerService
} from '../../services/customer';

@Component({
  selector: 'app-customer-details',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './customer-details.html',
  styleUrl: './customer-details.css'
})
export class CustomerDetails implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  customer: Customer | null = null;
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    const idValue = this.route.snapshot.paramMap.get('id');
    const customerId = Number(idValue);

    if (
      !idValue ||
      !Number.isInteger(customerId) ||
      customerId <= 0
    ) {
      this.errorMessage = 'Invalid customer ID.';
      this.cdr.detectChanges();
      return;
    }

    this.loadCustomer(customerId);
  }

  private loadCustomer(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.customerService.getCustomerById(id).subscribe({
      next: (customer) => {
        this.customer = customer;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.loading = false;

        if (
          error.message ===
          'Please sign in with your development credentials first.'
        ) {
          this.cdr.detectChanges();
          this.router.navigate(['/customers']);
          return;
        }

        this.errorMessage =
          error.message || 'Unable to load customer details.';

        this.cdr.detectChanges();
      }
    });
  }

  backToCustomers(): void {
    this.router.navigate(['/customers']);
  }
}