import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize, firstValueFrom } from 'rxjs';
import { Address } from '../../core/models/address.model';
import { PaymentMethod } from '../../core/models/order.model';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { AddressesComponent } from '../account/addresses/addresses.component';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, AddressesComponent],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  // Dependency Injection via inject() with readonly safety
  readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly paymentService = inject(PaymentService);
  private readonly toastr = inject(ToastrService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Reactive State Signals
  readonly selectedAddress = signal<Address | null>(null);
  readonly paymentMethod = signal<PaymentMethod>('RAZORPAY');
  readonly couponCode = signal<string | null>(null);
  readonly appliedDiscount = signal(0);
  readonly placingOrder = signal(false);

  // Derived Reactive Values using computed()
  readonly shippingCharge = computed(() => {
    const afterDiscount = this.cartService.cart().subtotal - this.appliedDiscount();
    return afterDiscount >= 999 ? 0 : 79;
  });

  readonly grandTotal = computed(() => {
    const cart = this.cartService.cart();
    const afterDiscount = cart.subtotal - this.appliedDiscount();
    return afterDiscount + this.shippingCharge();
  });

  readonly codEligible = computed(() => this.grandTotal() <= 5000);

  ngOnInit(): void {
    this.cartService.loadCart();

    if (this.cartService.cart().items.length === 0) {
      this.router.navigate(['/cart']);
      return;
    }

    const code = this.route.snapshot.queryParamMap.get('coupon');
    if (code) {
      this.couponCode.set(code);
      this.cartService
        .applyCoupon(code)
        .pipe(
          catchError(() => {
            this.couponCode.set(null);
            return EMPTY;
          })
        )
        .subscribe((res) => this.appliedDiscount.set(res.data.discountAmount));
    }
  }

  onAddressSelected(address: Address): void {
    this.selectedAddress.set(address);
  }

  setPaymentMethod(method: PaymentMethod): void {
    this.paymentMethod.set(method);
  }

  async placeOrder(): Promise<void> {
    const address = this.selectedAddress();

    if (!address) {
      this.toastr.warning('Please select a delivery address.');
      return;
    }

    if (this.paymentMethod() === 'COD' && !this.codEligible()) {
      this.toastr.warning('Cash on Delivery is not available for orders above ₹5000.');
      return;
    }

    this.placingOrder.set(true);

    this.orderService
      .placeOrder({
        addressId: address.id,
        couponCode: this.couponCode() ?? undefined,
        paymentMethod: this.paymentMethod(),
      })
      .pipe(
        catchError(() => {
          this.placingOrder.set(false);
          return EMPTY;
        })
      )
      .subscribe((order) => this.handleOrderResponse(order, address));
  }

  private async handleOrderResponse(order: any, address: Address): Promise<void> {
    if (order.paymentMethod === 'COD') {
      this.cartService.reset();
      this.placingOrder.set(false);
      this.router.navigate(['/order-success', order.orderNumber]);
      return;
    }

    // Razorpay Flow
    try {
      const rzpOrder = await firstValueFrom(
        this.paymentService.createRazorpayOrder(order.orderNumber)
      );
      const user = this.authService.currentUser();

      const result = await this.paymentService.openCheckout(
        rzpOrder,
        user?.fullName ?? '',
        user?.email ?? '',
        address.phone
      );

      this.verifyPayment(result, order.orderNumber);
    } catch {
      // User closed the Razorpay modal or checkout script failed
      this.placingOrder.set(false);
      this.toastr.info('Payment was not completed. You can retry from your order details.');
      this.router.navigate(['/orders', order.orderNumber]);
    }
  }

  private verifyPayment(result: any, orderNumber: string): void {
    this.paymentService
      .verifyPayment({
        razorpayOrderId: result.razorpay_order_id,
        razorpayPaymentId: result.razorpay_payment_id,
        razorpaySignature: result.razorpay_signature,
      })
      .pipe(
        finalize(() => this.placingOrder.set(false)),
        catchError(() => {
          this.toastr.error('Payment verification failed. Please check your order status.');
          this.router.navigate(['/orders', orderNumber]);
          return EMPTY;
        })
      )
      .subscribe(() => {
        this.cartService.reset();
        this.router.navigate(['/order-success', orderNumber]);
      });
  }
}