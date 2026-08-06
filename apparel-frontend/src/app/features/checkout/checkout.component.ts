import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { firstValueFrom } from 'rxjs';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { Address } from '../../core/models/address.model';
import { PaymentMethod } from '../../core/models/order.model';
import { AddressesComponent } from '../account/addresses/addresses.component';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, AddressesComponent],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  public readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly paymentService = inject(PaymentService);
  public readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  selectedAddress = signal<Address | null>(null);
  paymentMethod = signal<PaymentMethod>('RAZORPAY');
  couponCode: string | null = null;
  appliedDiscount = signal(0);
  placingOrder = signal(false);

  ngOnInit(): void {
    this.cartService.loadCart();

    if (this.cartService.cart().items.length === 0) {
      this.router.navigate(['/cart']);
    }

    this.couponCode = this.route.snapshot.queryParamMap.get('coupon');
    if (this.couponCode) {
      this.cartService.applyCoupon(this.couponCode).subscribe({
        next: (res) => this.appliedDiscount.set(res.data.discountAmount),
        error: () => (this.couponCode = null),
      });
    }
  }

  onAddressSelected(address: Address): void {
    this.selectedAddress.set(address);
  }

  get codEligible(): boolean {
    return this.grandTotal <= 5000;
  }

  get grandTotal(): number {
    const cart = this.cartService.cart();
    const afterDiscount = cart.subtotal - this.appliedDiscount();
    const shipping = afterDiscount >= 999 ? 0 : 79;
    return afterDiscount + shipping;
  }

  get shippingCharge(): number {
    const afterDiscount = this.cartService.cart().subtotal - this.appliedDiscount();
    return afterDiscount >= 999 ? 0 : 79;
  }

  async placeOrder(): Promise<void> {
    const address = this.selectedAddress();
    if (!address) {
      this.toastr.warning('Please select a delivery address.');
      return;
    }
    if (this.paymentMethod() === 'COD' && !this.codEligible) {
      this.toastr.warning('Cash on Delivery is not available for orders above ₹5000.');
      return;
    }

    this.placingOrder.set(true);

    this.orderService
      .placeOrder({
        addressId: address.id,
        couponCode: this.couponCode ?? undefined,
        paymentMethod: this.paymentMethod(),
      })
      .subscribe({
        next: async (order) => {
          if (order.paymentMethod === 'COD') {
            this.cartService.reset();
            this.router.navigate(['/order-success', order.orderNumber]);
            return;
          }

          // RAZORPAY — open checkout immediately
          try {
            // Unwrapping the Observable into a Promise using firstValueFrom
            const rzpOrder = await firstValueFrom(
              this.paymentService.createRazorpayOrder(order.orderNumber),
            );
            const user = this.authService.currentUser();
            const result = await this.paymentService.openCheckout(
              rzpOrder,
              user?.fullName ?? '',
              user?.email ?? '',
              address.phone,
            );

            this.paymentService
              .verifyPayment({
                razorpayOrderId: result.razorpay_order_id,
                razorpayPaymentId: result.razorpay_payment_id,
                razorpaySignature: result.razorpay_signature,
              })
              .subscribe({
                next: () => {
                  this.cartService.reset();
                  this.router.navigate(['/order-success', order.orderNumber]);
                },
                error: () => {
                  this.placingOrder.set(false);
                  this.toastr.error('Payment verification failed. Please check your order status.');
                  this.router.navigate(['/orders', order.orderNumber]);
                },
              });
          } catch {
            // user closed the Razorpay modal, or the script failed — order still exists as PENDING
            this.placingOrder.set(false);
            this.toastr.info('Payment was not completed. You can retry from your order details.');
            this.router.navigate(['/orders', order.orderNumber]);
          }
        },
        error: () => this.placingOrder.set(false),
      });
  }
}
