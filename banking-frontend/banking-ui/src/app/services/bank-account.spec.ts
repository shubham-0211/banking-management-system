import { TestBed } from '@angular/core/testing';
import { BankAccount } from './bank-account';

describe('BankAccount', () => {
  let service: BankAccount;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BankAccount);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
