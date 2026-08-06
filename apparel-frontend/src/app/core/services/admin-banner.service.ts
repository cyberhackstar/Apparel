import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Banner } from '../models/banner.model';

@Injectable({ providedIn: 'root' })
export class AdminBannerService {
  private readonly baseUrl = `${environment.apiUrl}/admin/banners`;

  constructor(private http: HttpClient) {}

  list(): Observable<Banner[]> {
    return this.http.get<ApiResponse<Banner[]>>(this.baseUrl).pipe(map((r) => r.data));
  }

  create(title: string, linkUrl: string, displayOrder: number, file: File): Observable<Banner> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('linkUrl', linkUrl);
    formData.append('displayOrder', String(displayOrder));
    formData.append('file', file);
    return this.http.post<ApiResponse<Banner>>(this.baseUrl, formData).pipe(map((r) => r.data));
  }

  update(id: number, title: string, linkUrl: string, displayOrder: number, active: boolean): Observable<Banner> {
    return this.http
      .put<ApiResponse<Banner>>(`${this.baseUrl}/${id}`, null, { params: { title, linkUrl, displayOrder, active } })
      .pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }
}
