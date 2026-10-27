import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CustomerDetails } from './customer-details';

describe('CustomerDetails', () => {
  let component: CustomerDetails;
  let fixture: ComponentFixture<CustomerDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerDetails],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});