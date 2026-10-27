import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { BankAccountService } from './bank-account';

describe('BankAccountService', () => {
  let service: BankAccountService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient()]
    });

    service = TestBed.inject(BankAccountService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});