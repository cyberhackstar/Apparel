import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { AuthService } from '../../../core/services/auth.service';
import { ReturnService } from '../../../core/services/return.service';
import { Order } from '../../../core/models/order.model';
import { ReturnRequest } from '../../../core/models/return-request.model';

const CANCELLABLE_STATUSES = new Set(['PLACED', 'CONFIRMED']);
const TRACKING_STEPS = [
  'PLACED',
  'CONFIRMED',
  'PACKED',
  'SHIPPED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent implements OnInit {
  order = signal<Order | null>(null);
  loading = signal(true);
  cancelling = signal(false);
  retryingPayment = signal(false);
  downloadingInvoice = signal(false);
  trackingSteps = TRACKING_STEPS;

  existingReturn = signal<ReturnRequest | null>(null);
  showReturnForm = signal(false);
  returnReason = '';
  submittingReturn = signal(false);

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private authService: AuthService,
    private returnService: ReturnService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const orderNumber = params.get('orderNumber');
      if (orderNumber) this.fetchOrder(orderNumber);
    });
  }

  private fetchOrder(orderNumber: string): void {
    this.loading.set(true);
    this.orderService.getOrder(orderNumber).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.checkExistingReturn(orderNumber);
      },
      error: () => this.loading.set(false),
    });
  }

  private checkExistingReturn(orderNumber: string): void {
    this.returnService.getMyReturns().subscribe((res) => {
      const match = res.content.find((r) => r.orderNumber === orderNumber);
      this.existingReturn.set(match ?? null);
    });
  }

  get isCancellable(): boolean {
    return CANCELLABLE_STATUSES.has(this.order()?.status ?? '');
  }

  get isReturnEligible(): boolean {
    return this.order()?.status === 'DELIVERED' && !this.existingReturn();
  }

  get needsPayment(): boolean {
    const o = this.order();
    return (
      !!o &&
      o.paymentMethod === 'RAZORPAY' &&
      o.paymentStatus === 'PENDING' &&
      o.status !== 'CANCELLED'
    );
  }

  currentStepIndex(): number {
    const status = this.order()?.status ?? '';
    return this.trackingSteps.indexOf(status);
  }

  cancelOrder(): void {
    const o = this.order();
    if (!o) return;
    this.cancelling.set(true);
    this.orderService.cancelOrder(o.orderNumber).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.cancelling.set(false);
        this.toastr.success('Order cancelled.');
      },
      error: () => this.cancelling.set(false),
    });
  }

  async retryPayment(): Promise<void> {
    const o = this.order();
    if (!o) return;

    this.retryingPayment.set(true);
    try {
      const rzpOrder = await this.paymentService.createRazorpayOrder(o.orderNumber).toPromise();
      const user = this.authService.currentUser();
      const result = await this.paymentService.openCheckout(
        rzpOrder!,
        user?.fullName ?? '',
        user?.email ?? '',
        o.recipientPhone,
      );

      this.paymentService
        .verifyPayment({
          razorpayOrderId: result.razorpay_order_id,
          razorpayPaymentId: result.razorpay_payment_id,
          razorpaySignature: result.razorpay_signature,
        })
        .subscribe({
          next: () => {
            this.toastr.success('Payment successful!');
            this.fetchOrder(o.orderNumber);
            this.retryingPayment.set(false);
          },
          error: () => {
            this.toastr.error('Payment verification failed.');
            this.retryingPayment.set(false);
          },
        });
    } catch {
      this.toastr.info('Payment was not completed.');
      this.retryingPayment.set(false);
    }
  }

  downloadInvoice(): void {
    const o = this.order();
    if (!o) return;
    this.downloadingInvoice.set(true);
    this.orderService.downloadInvoice(o.orderNumber).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `invoice-${o.orderNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.downloadingInvoice.set(false);
      },
      error: () => this.downloadingInvoice.set(false),
    });
  }

  submitReturnRequest(): void {
    const o = this.order();
    if (!o || !this.returnReason.trim()) {
      this.toastr.warning('Please describe the reason for your return.');
      return;
    }
    this.submittingReturn.set(true);
    this.returnService
      .request({ orderNumber: o.orderNumber, reason: this.returnReason })
      .subscribe({
        next: (returnReq) => {
          this.existingReturn.set(returnReq);
          this.showReturnForm.set(false);
          this.returnReason = '';
          this.submittingReturn.set(false);
          this.toastr.success('Return/exchange request submitted.');
        },
        error: () => this.submittingReturn.set(false),
      });
  }
}
