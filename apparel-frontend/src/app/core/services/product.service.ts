import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Product, ProductSearchParams } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly baseUrl = `${environment.apiUrl}/public/products`;

  constructor(private http: HttpClient) {}

  search(params: ProductSearchParams): Observable<PagedResponse<Product>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return this.http
      .get<ApiResponse<PagedResponse<Product>>>(this.baseUrl, { params: httpParams })
      .pipe(map((res) => res.data));
  }

  getBySlug(slug: string): Observable<Product> {
    return this.http.get<ApiResponse<Product>>(`${this.baseUrl}/${slug}`).pipe(map((res) => res.data));
  }

  getRelated(productId: number, limit = 8): Observable<Product[]> {
    return this.http
      .get<ApiResponse<Product[]>>(`${this.baseUrl}/${productId}/related`, { params: { limit } })
      .pipe(map((res) => res.data));
  }
}
