import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Category, CategoryRequest } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class AdminCategoryService {
  private readonly baseUrl = `${environment.apiUrl}/admin/categories`;

  constructor(private http: HttpClient) {}

  list(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(this.baseUrl).pipe(map((r) => r.data));
  }

  create(request: CategoryRequest): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(this.baseUrl, request).pipe(map((r) => r.data));
  }

  update(id: number, request: CategoryRequest): Observable<Category> {
    return this.http.put<ApiResponse<Category>>(`${this.baseUrl}/${id}`, request).pipe(map((r) => r.data));
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }
}
