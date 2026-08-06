import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AddToCartRequest, Cart } from '../models/cart.model';
import { AuthService } from './auth.service';

const EMPTY_CART: Cart = {
  items: [],
  totalItems: 0,
  subtotal: 0,
  totalMrp: 0,
  totalDiscount: 0,
  hasOutOfStockItems: false,
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly baseUrl = `${environment.apiUrl}/cart`;

  cart = signal<Cart>(EMPTY_CART);
  itemCount = computed(() => this.cart().totalItems);

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** Call once on app init (if logged in) so the header badge is correct on first paint. */
  loadCart(): void {
    if (!this.authService.isLoggedIn()) return;
    this.http.get<ApiResponse<Cart>>(this.baseUrl).subscribe({
      next: (res) => this.cart.set(res.data),
      error: () => {},
    });
  }

  addItem(request: AddToCartRequest): Observable<ApiResponse<Cart>> {
    return this.http
      .post<ApiResponse<Cart>>(`${this.baseUrl}/items`, request)
      .pipe(tap((res) => this.cart.set(res.data)));
  }

  updateItemQuantity(cartItemId: number, quantity: number): Observable<ApiResponse<Cart>> {
    return this.http
      .put<ApiResponse<Cart>>(`${this.baseUrl}/items/${cartItemId}`, { quantity })
      .pipe(tap((res) => this.cart.set(res.data)));
  }

  removeItem(cartItemId: number): Observable<ApiResponse<Cart>> {
    return this.http
      .delete<ApiResponse<Cart>>(`${this.baseUrl}/items/${cartItemId}`)
      .pipe(tap((res) => this.cart.set(res.data)));
  }

  clearCart(): Observable<ApiResponse<null>> {
    return this.http
      .delete<ApiResponse<null>>(this.baseUrl)
      .pipe(tap(() => this.cart.set(EMPTY_CART)));
  }

  applyCoupon(code: string): Observable<ApiResponse<{ code: string; discountAmount: number; subtotal: number; payableAmount: number }>> {
    return this.http.post<ApiResponse<{ code: string; discountAmount: number; subtotal: number; payableAmount: number }>>(
      `${this.baseUrl}/apply-coupon`,
      { code },
    );
  }

  reset(): void {
    this.cart.set(EMPTY_CART);
  }
}
