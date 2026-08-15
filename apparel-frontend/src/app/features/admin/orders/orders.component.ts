import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminOrderService } from '../../../core/services/admin-order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

const STATUS_OPTIONS: OrderStatus[] = [
  'PLACED',
  'CONFIRMED',
  'PACKED',
  'SHIPPED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
  'RETURNED',
];

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './orders.component.html',
})
export class AdminOrdersComponent implements OnInit {
  private readonly orderService = inject(AdminOrderService);
  private readonly toastr = inject(ToastrService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly updatingStatus = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly statusOptions = STATUS_OPTIONS;
  readonly expandedOrder = signal<string | null>(null);

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
    const newStatus = status as OrderStatus;
    this.updatingStatus.set(orderNumber);

    this.orderService.updateStatus(orderNumber, newStatus).subscribe({
      next: (updated) => {
        // Explicitly update the status on the matching order
        this.orders.update((list) =>
          list.map((o) =>
            o.orderNumber === orderNumber
              ? { ...o, ...updated, status: updated.status || newStatus }
              : o,
          ),
        );
        this.updatingStatus.set(null);
        this.toastr.success(`Order #${orderNumber} updated to ${newStatus}`);
      },
      error: () => {
        this.updatingStatus.set(null);
        this.toastr.error('Failed to update status');
        this.fetch(); // Re-fetch to reset select box to actual DB state
      },
    });
  }

  refund(orderNumber: string): void {
    if (!confirm(`Issue a refund for order #${orderNumber} via Razorpay?`)) return;
    this.orderService.refund(orderNumber).subscribe({
      next: () => {
        this.toastr.success('Refund initiated');
        this.fetch();
      },
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
  }
}
