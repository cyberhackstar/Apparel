import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { CreateReturnRequest, ReturnRequest } from '../models/return-request.model';

@Injectable({ providedIn: 'root' })
export class ReturnService {
  private readonly baseUrl = `${environment.apiUrl}/returns`;

  constructor(private http: HttpClient) {}

  request(payload: CreateReturnRequest): Observable<ReturnRequest> {
    return this.http.post<ApiResponse<ReturnRequest>>(this.baseUrl, payload).pipe(map((r) => r.data));
  }

  getMyReturns(page = 0): Observable<PagedResponse<ReturnRequest>> {
    return this.http
      .get<ApiResponse<PagedResponse<ReturnRequest>>>(this.baseUrl, { params: { page, pageSize: 10 } })
      .pipe(map((r) => r.data));
  }
}
