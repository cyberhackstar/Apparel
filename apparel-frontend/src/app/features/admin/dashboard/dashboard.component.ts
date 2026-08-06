import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminDashboardService } from '../../../core/services/admin-dashboard.service';
import { DashboardSummary, LowStockVariant, TopProduct } from '../../../core/models/dashboard.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  summary = signal<DashboardSummary | null>(null);
  topProducts = signal<TopProduct[]>([]);
  lowStock = signal<LowStockVariant[]>([]);
  loading = signal(true);
  exporting = signal(false);

  constructor(private dashboardService: AdminDashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getSummary().subscribe((s) => this.summary.set(s));
    this.dashboardService.getTopProducts(5).subscribe((p) => this.topProducts.set(p));
    this.dashboardService.getLowStock(5).subscribe((v) => {
      this.lowStock.set(v);
      this.loading.set(false);
    });
  }

  exportOrders(): void {
    this.exporting.set(true);
    const today = new Date();
    const monthAgo = new Date();
    monthAgo.setDate(today.getDate() - 30);

    this.dashboardService.exportOrdersCsv(monthAgo.toISOString(), today.toISOString()).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `orders-${today.toISOString().slice(0, 10)}.csv`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false),
    });
  }
}
