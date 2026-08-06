import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { WishlistItem } from '../models/wishlist.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly baseUrl = `${environment.apiUrl}/wishlist`;

  items = signal<WishlistItem[]>([]);
  productIds = computed(() => new Set(this.items().map((i) => i.productId)));
  count = computed(() => this.items().length);

  constructor(private http: HttpClient, private authService: AuthService) {}

  loadWishlist(): void {
    if (!this.authService.isLoggedIn()) return;
    this.http.get<ApiResponse<WishlistItem[]>>(this.baseUrl).subscribe({
      next: (res) => this.items.set(res.data),
      error: () => {},
    });
  }

  isWishlisted(productId: number): boolean {
    return this.productIds().has(productId);
  }

  toggle(productId: number): Observable<ApiResponse<null>> {
    const isCurrentlyWishlisted = this.isWishlisted(productId);
    const request = isCurrentlyWishlisted
      ? this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${productId}`)
      : this.http.post<ApiResponse<null>>(`${this.baseUrl}/${productId}`, {});

    return request.pipe(tap(() => this.loadWishlist()));
  }

  reset(): void {
    this.items.set([]);
  }
}
