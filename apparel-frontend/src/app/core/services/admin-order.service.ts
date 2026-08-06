import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Order, OrderStatus } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class AdminOrderService {
  private readonly baseUrl = `${environment.apiUrl}/admin/orders`;

  constructor(private http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<PagedResponse<Order>> {
    return this.http
      .get<ApiResponse<PagedResponse<Order>>>(this.baseUrl, { params: { page, pageSize } })
      .pipe(map((r) => r.data));
  }

  updateStatus(orderNumber: string, status: OrderStatus): Observable<Order> {
    return this.http
      .patch<ApiResponse<Order>>(`${this.baseUrl}/${orderNumber}/status`, { status })
      .pipe(map((r) => r.data));
  }

  refund(orderNumber: string): Observable<void> {
    return this.http
      .post<ApiResponse<null>>(`${environment.apiUrl}/admin/payments/refund/${orderNumber}`, {})
      .pipe(map(() => undefined));
  }
}
