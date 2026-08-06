import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Customer } from '../models/admin-misc.model';

@Injectable({ providedIn: 'root' })
export class AdminCustomerService {
  private readonly baseUrl = `${environment.apiUrl}/admin/customers`;

  constructor(private http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<PagedResponse<Customer>> {
    return this.http
      .get<ApiResponse<PagedResponse<Customer>>>(this.baseUrl, { params: { page, pageSize } })
      .pipe(map((r) => r.data));
  }

  block(id: number): Observable<void> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/block`, {}).pipe(map(() => undefined));
  }

  unblock(id: number): Observable<void> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/unblock`, {}).pipe(map(() => undefined));
  }
}
