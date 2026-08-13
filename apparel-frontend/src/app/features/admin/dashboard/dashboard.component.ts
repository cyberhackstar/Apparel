import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin } from 'rxjs';
import {
  DashboardSummary,
  LowStockVariant,
  TopProduct,
} from '../../../core/models/dashboard.model';
import { AdminDashboardService } from '../../../core/services/admin-dashboard.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  // Dependency Injection using inject()
  private readonly dashboardService = inject(AdminDashboardService);

  // State Signals
  readonly summary = signal<DashboardSummary | null>(null);
  readonly topProducts = signal<TopProduct[]>([]);
  readonly lowStock = signal<LowStockVariant[]>([]);
  readonly loading = signal(true);
  readonly exporting = signal(false);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);

    // Load parallel initial dashboard requests simultaneously
    forkJoin({
      summary: this.dashboardService.getSummary().pipe(catchError(() => EMPTY)),
      topProducts: this.dashboardService.getTopProducts(5).pipe(catchError(() => EMPTY)),
      lowStock: this.dashboardService.getLowStock(5).pipe(catchError(() => EMPTY)),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ summary, topProducts, lowStock }) => {
        if (summary) this.summary.set(summary);
        if (topProducts) this.topProducts.set(topProducts);
        if (lowStock) this.lowStock.set(lowStock);
      });
  }

  exportOrders(): void {
    this.exporting.set(true);
    const today = new Date();
    const monthAgo = new Date();
    monthAgo.setDate(today.getDate() - 30);

    this.dashboardService
      .exportOrdersCsv(monthAgo.toISOString(), today.toISOString())
      .pipe(
        finalize(() => this.exporting.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `orders-${today.toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      });
  }
}
