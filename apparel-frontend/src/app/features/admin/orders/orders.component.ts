import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { AdminOrderService } from '../../../core/services/admin-order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

const STATUS_OPTIONS: OrderStatus[] = [
  'PLACED', 'CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED',
];

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './orders.component.html',
})
export class AdminOrdersComponent implements OnInit {
  orders = signal<Order[]>([]);
  loading = signal(true);
  page = signal(0);
  totalPages = signal(0);
  statusOptions = STATUS_OPTIONS;
  expandedOrder = signal<string | null>(null);

  constructor(private orderService: AdminOrderService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.orderService.list(this.page(), 20).subscribe({
      next: (res) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleExpand(orderNumber: string): void {
    this.expandedOrder.set(this.expandedOrder() === orderNumber ? null : orderNumber);
  }

  updateStatus(orderNumber: string, status: string): void {
    this.orderService.updateStatus(orderNumber, status as OrderStatus).subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((o) => (o.orderNumber === orderNumber ? updated : o)));
        this.toastr.success('Order status updated');
      },
    });
  }

  refund(orderNumber: string): void {
    if (!confirm('Issue a refund for this order via Razorpay?')) return;
    this.orderService.refund(orderNumber).subscribe(() => {
      this.toastr.success('Refund initiated');
      this.fetch();
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
  }
}
