import { Routes } from '@angular/router';

import { Dashboard } from './pages/dashboard/dashboard';
import { Customers } from './pages/customers/customers';
import { CustomerDetails } from './pages/customer-details/customer-details';

export const routes: Routes = [
  { path: 'dashboard', component: Dashboard },
  { path: 'customers', component: Customers },
  { path: 'customers/:id', component: CustomerDetails },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];