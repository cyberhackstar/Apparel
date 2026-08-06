import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Coupon, CouponRequest } from '../models/admin-misc.model';

@Injectable({ providedIn: 'root' })
export class AdminCouponService {
  private readonly baseUrl = `${environment.apiUrl}/admin/coupons`;

  constructor(private http: HttpClient) {}

  list(): Observable<Coupon[]> {
    return this.http.get<ApiResponse<Coupon[]>>(this.baseUrl).pipe(map((r) => r.data));
  }

  create(request: CouponRequest): Observable<Coupon> {
    return this.http.post<ApiResponse<Coupon>>(this.baseUrl, request).pipe(map((r) => r.data));
  }

  update(id: number, request: CouponRequest): Observable<Coupon> {
    return this.http.put<ApiResponse<Coupon>>(`${this.baseUrl}/${id}`, request).pipe(map((r) => r.data));
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }
}
