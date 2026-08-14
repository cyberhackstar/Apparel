import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-list.component.html',
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.orderService.getMyOrders(this.page()).subscribe({
      next: (res) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  statusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'DELIVERED':
        return 'text-green-700 bg-green-50 border border-green-200/60';
      case 'SHIPPED':
      case 'OUT_FOR_DELIVERY':
        return 'text-amber-800 bg-amber-50 border border-amber-200/60';
      case 'PLACED':
      case 'CONFIRMED':
        return 'text-ink/80 bg-ink/5 border border-ink/10';
      case 'CANCELLED':
      case 'RETURNED':
        return 'text-wine bg-blush border border-wine/15';
      default:
        return 'text-wine bg-blush border border-wine/15';
    }
  }
}
