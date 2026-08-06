import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { AuditLog } from '../models/admin-misc.model';

@Injectable({ providedIn: 'root' })
export class AdminAuditLogService {
  private readonly baseUrl = `${environment.apiUrl}/admin/audit-logs`;

  constructor(private http: HttpClient) {}

  list(page = 0): Observable<PagedResponse<AuditLog>> {
    return this.http
      .get<ApiResponse<PagedResponse<AuditLog>>>(this.baseUrl, { params: { page, pageSize: 50 } })
      .pipe(map((r) => r.data));
  }
}
