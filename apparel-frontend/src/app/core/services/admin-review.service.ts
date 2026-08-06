import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Review } from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class AdminReviewService {
  private readonly baseUrl = `${environment.apiUrl}/admin/reviews`;

  constructor(private http: HttpClient) {}

  getPending(page = 0): Observable<PagedResponse<Review>> {
    return this.http
      .get<ApiResponse<PagedResponse<Review>>>(`${this.baseUrl}/pending`, { params: { page, pageSize: 20 } })
      .pipe(map((r) => r.data));
  }

  moderate(id: number, status: 'APPROVED' | 'REJECTED'): Observable<Review> {
    return this.http.patch<ApiResponse<Review>>(`${this.baseUrl}/${id}/moderate`, { status }).pipe(map((r) => r.data));
  }

  reply(id: number, reply: string): Observable<Review> {
    return this.http.post<ApiResponse<Review>>(`${this.baseUrl}/${id}/reply`, { reply }).pipe(map((r) => r.data));
  }
}
