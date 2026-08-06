import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Order, PlaceOrderRequest } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly baseUrl = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  placeOrder(request: PlaceOrderRequest): Observable<Order> {
    return this.http.post<ApiResponse<Order>>(this.baseUrl, request).pipe(map((res) => res.data));
  }

  getMyOrders(page = 0): Observable<PagedResponse<Order>> {
    return this.http
      .get<ApiResponse<PagedResponse<Order>>>(this.baseUrl, { params: { page, pageSize: 10 } })
      .pipe(map((res) => res.data));
  }

  getOrder(orderNumber: string): Observable<Order> {
    return this.http.get<ApiResponse<Order>>(`${this.baseUrl}/${orderNumber}`).pipe(map((res) => res.data));
  }

  cancelOrder(orderNumber: string): Observable<Order> {
    return this.http.post<ApiResponse<Order>>(`${this.baseUrl}/${orderNumber}/cancel`, {}).pipe(map((res) => res.data));
  }

  /** Authenticated blob download — a plain <a href> can't carry the JWT header. */
  downloadInvoice(orderNumber: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${orderNumber}/invoice`, { responseType: 'blob' });
  }
}
