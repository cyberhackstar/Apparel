import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { AdminCustomerService } from '../../../core/services/admin-customer.service';
import { Customer } from '../../../core/models/admin-misc.model';

@Component({
  selector: 'app-admin-customers',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customers.component.html',
})
export class AdminCustomersComponent implements OnInit {
  customers = signal<Customer[]>([]);
  loading = signal(true);
  page = signal(0);
  totalPages = signal(0);

  constructor(private customerService: AdminCustomerService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.customerService.list(this.page(), 20).subscribe({
      next: (res) => {
        this.customers.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  block(customer: Customer): void {
    if (!confirm(`Block ${customer.fullName}? They won't be able to log in.`)) return;
    this.customerService.block(customer.id).subscribe(() => {
      this.toastr.success('Customer blocked');
      this.fetch();
    });
  }

  unblock(customer: Customer): void {
    this.customerService.unblock(customer.id).subscribe(() => {
      this.toastr.success('Customer unblocked');
      this.fetch();
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
  }
}
