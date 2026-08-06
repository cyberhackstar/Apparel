import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cart.component.html',
})
export class CartComponent implements OnInit {
  couponCode = '';
  applyingCoupon = signal(false);
  appliedDiscount = signal<number | null>(null);
  updatingItemId = signal<number | null>(null);

  constructor(
    public cartService: CartService,
    public authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.cartService.loadCart();
  }

  updateQuantity(cartItemId: number, quantity: number): void {
    if (quantity < 1) return;
    this.updatingItemId.set(cartItemId);
    this.cartService.updateItemQuantity(cartItemId, quantity).subscribe({
      next: () => this.updatingItemId.set(null),
      error: () => this.updatingItemId.set(null),
    });
  }

  removeItem(cartItemId: number): void {
    this.cartService.removeItem(cartItemId).subscribe(() => this.toastr.info('Item removed from cart.'));
  }

  applyCoupon(): void {
    if (!this.couponCode.trim()) return;
    this.applyingCoupon.set(true);
    this.cartService.applyCoupon(this.couponCode.trim()).subscribe({
      next: (res) => {
        this.appliedDiscount.set(res.data.discountAmount);
        this.toastr.success(`Coupon applied! You saved ₹${res.data.discountAmount}.`);
        this.applyingCoupon.set(false);
      },
      error: () => this.applyingCoupon.set(false),
    });
  }

  proceedToCheckout(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.router.navigate(['/checkout'], {
      queryParams: this.couponCode.trim() ? { coupon: this.couponCode.trim() } : {},
    });
  }
}
