import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { ResolveReturnRequest, ReturnRequest } from '../models/return-request.model';

@Injectable({ providedIn: 'root' })
export class AdminReturnService {
  private readonly baseUrl = `${environment.apiUrl}/admin/returns`;

  constructor(private http: HttpClient) {}

  getPending(page = 0): Observable<PagedResponse<ReturnRequest>> {
    return this.http
      .get<ApiResponse<PagedResponse<ReturnRequest>>>(this.baseUrl, { params: { page, pageSize: 20 } })
      .pipe(map((r) => r.data));
  }

  approve(id: number, payload: ResolveReturnRequest): Observable<ReturnRequest> {
    return this.http.patch<ApiResponse<ReturnRequest>>(`${this.baseUrl}/${id}/approve`, payload).pipe(map((r) => r.data));
  }

  reject(id: number, payload: ResolveReturnRequest): Observable<ReturnRequest> {
    return this.http.patch<ApiResponse<ReturnRequest>>(`${this.baseUrl}/${id}/reject`, payload).pipe(map((r) => r.data));
  }

  complete(id: number, payload: ResolveReturnRequest): Observable<ReturnRequest> {
    return this.http.patch<ApiResponse<ReturnRequest>>(`${this.baseUrl}/${id}/complete`, payload).pipe(map((r) => r.data));
  }
}
