import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Product, ProductRequest, ProductVariantRequest } from '../models/product.model';

export type AdminProductStatus = 'ALL' | 'ACTIVE' | 'INACTIVE';

export interface AdminProductSearchParams {
  categoryId?: number;
  keyword?: string;
  status?: AdminProductStatus;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

@Injectable({ providedIn: 'root' })
export class AdminProductService {
  private readonly adminUrl = `${environment.apiUrl}/admin/products`;

  constructor(private http: HttpClient) {}

  /**
   * Dedicated admin listing endpoint — deliberately NOT the public storefront one. The public
   * endpoint always forces active=true, so re-using it here meant deactivated products became
   * permanently invisible in the admin panel with no way to find or reactivate them. This one
   * shows everything by default and supports an explicit status filter instead.
   */
  list(params: AdminProductSearchParams): Observable<PagedResponse<Product>> {
    let httpParams: any = {};
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') httpParams[k] = v;
    });
    return this.http
      .get<ApiResponse<PagedResponse<Product>>>(this.adminUrl, { params: httpParams })
      .pipe(map((r) => r.data));
  }

  getById(id: number): Observable<Product> {
    return this.http.get<ApiResponse<Product>>(`${this.adminUrl}/${id}`).pipe(map((r) => r.data));
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<ApiResponse<Product>>(this.adminUrl, request).pipe(map((r) => r.data));
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<ApiResponse<Product>>(`${this.adminUrl}/${id}`, request).pipe(map((r) => r.data));
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.adminUrl}/${id}`).pipe(map(() => undefined));
  }

  activate(id: number): Observable<void> {
    return this.http.patch<ApiResponse<null>>(`${this.adminUrl}/${id}/activate`, {}).pipe(map(() => undefined));
  }

  addVariant(productId: number, variant: ProductVariantRequest): Observable<void> {
    return this.http
      .post<ApiResponse<null>>(`${this.adminUrl}/${productId}/variants`, variant)
      .pipe(map(() => undefined));
  }

  updateStock(variantId: number, quantity: number): Observable<void> {
    return this.http
      .patch<ApiResponse<null>>(`${environment.apiUrl}/admin/variants/${variantId}/stock`, null, { params: { quantity } })
      .pipe(map(() => undefined));
  }

  uploadImage(productId: number, file: File, isPrimary: boolean): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPrimary', String(isPrimary));
    return this.http
      .post<ApiResponse<null>>(`${this.adminUrl}/${productId}/images`, formData)
      .pipe(map(() => undefined));
  }

  deleteImage(imageId: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.adminUrl}/images/${imageId}`).pipe(map(() => undefined));
  }

  bulkUpload(file: File): Observable<BulkUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<BulkUploadResult>>(`${this.adminUrl}/bulk-upload`, formData)
      .pipe(map((r) => r.data));
  }
}

export interface BulkUploadResult {
  totalRowsRead: number;
  productsCreated: number;
  productsFailed: number;
  errors: string[];
}
