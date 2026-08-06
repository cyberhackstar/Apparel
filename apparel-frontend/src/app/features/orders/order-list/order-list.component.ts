import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
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
  orders = signal<Order[]>([]);
  loading = signal(true);
  page = signal(0);
  totalPages = signal(0);

  constructor(private orderService: OrderService) {}

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
  }

  statusColor(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'text-green-700 bg-green-50';
      case 'CANCELLED': return 'text-wine bg-blush';
      case 'PLACED': case 'CONFIRMED': return 'text-ink/70 bg-ink/5';
      default: return 'text-wine bg-blush';
    }
  }
}
