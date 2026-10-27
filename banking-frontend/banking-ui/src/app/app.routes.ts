
import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Customers } from './pages/customers/customers';

export const routes: Routes = [
  {
    path: 'dashboard',
    component: Dashboard,
  },
  {
    path: 'customers',
    component: Customers,
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
