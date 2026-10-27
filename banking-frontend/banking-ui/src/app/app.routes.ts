import { Routes } from '@angular/router';

import { Dashboard } from './pages/dashboard/dashboard';
import { Customers } from './pages/customers/customers';
import { CustomerDetails } from './pages/customer-details/customer-details';
import { Accounts } from './pages/accounts/accounts';
import { Transactions } from './pages/transactions/transactions';

export const routes: Routes = [
  { path: 'dashboard', component: Dashboard },
  { path: 'customers', component: Customers },
  { path: 'customers/:id', component: CustomerDetails },
  { path: 'accounts', component: Accounts },
  { path: 'transactions', component: Transactions },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];