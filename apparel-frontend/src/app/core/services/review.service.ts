import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { Review, ReviewRequest } from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getApprovedForProduct(productId: number, page = 0): Observable<PagedResponse<Review>> {
    return this.http
      .get<ApiResponse<PagedResponse<Review>>>(`${this.apiUrl}/public/products/${productId}/reviews`, {
        params: { page, pageSize: 10 },
      })
      .pipe(map((res) => res.data));
  }

  submit(request: ReviewRequest): Observable<ApiResponse<Review>> {
    return this.http.post<ApiResponse<Review>>(`${this.apiUrl}/reviews`, request);
  }

  uploadImage(reviewId: number, file: File): Observable<ApiResponse<null>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<null>>(`${this.apiUrl}/reviews/${reviewId}/images`, formData);
  }
}
