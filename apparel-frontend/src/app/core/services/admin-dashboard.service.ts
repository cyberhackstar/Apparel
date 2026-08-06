import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { DailySales, DashboardSummary, LowStockVariant, TopProduct } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  private readonly baseUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<ApiResponse<DashboardSummary>>(`${this.baseUrl}/dashboard/summary`).pipe(map((r) => r.data));
  }

  getTopProducts(limit = 10): Observable<TopProduct[]> {
    return this.http
      .get<ApiResponse<TopProduct[]>>(`${this.baseUrl}/dashboard/top-products`, { params: { limit } })
      .pipe(map((r) => r.data));
  }

  getLowStock(threshold = 5): Observable<LowStockVariant[]> {
    return this.http
      .get<ApiResponse<LowStockVariant[]>>(`${this.baseUrl}/dashboard/low-stock`, { params: { threshold } })
      .pipe(map((r) => r.data));
  }

  getSalesReport(from: string, to: string): Observable<DailySales[]> {
    return this.http
      .get<ApiResponse<DailySales[]>>(`${this.baseUrl}/reports/sales`, { params: { from, to } })
      .pipe(map((r) => r.data));
  }

  /** Downloads the orders CSV as an authenticated blob (a plain <a href> can't carry the JWT header). */
  exportOrdersCsv(from: string, to: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/reports/orders/export`, {
      params: { from, to },
      responseType: 'blob',
    });
  }
}
