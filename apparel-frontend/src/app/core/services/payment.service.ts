import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { CreateRazorpayOrderResponse, RazorpaySuccessResponse, VerifyPaymentRequest } from '../models/payment.model';

declare global {
  interface Window {
    Razorpay: any;
  }
}

const RAZORPAY_SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly baseUrl = `${environment.apiUrl}/payments`;
  private scriptLoaded = false;

  constructor(private http: HttpClient) {}

  createRazorpayOrder(orderNumber: string): Observable<CreateRazorpayOrderResponse> {
    return this.http
      .post<ApiResponse<CreateRazorpayOrderResponse>>(`${this.baseUrl}/razorpay/create-order/${orderNumber}`, {})
      .pipe(map((res) => res.data));
  }

  verifyPayment(request: VerifyPaymentRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/razorpay/verify`, request);
  }

  private loadScript(): Promise<void> {
    if (this.scriptLoaded) return Promise.resolve();

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = RAZORPAY_SCRIPT_URL;
      script.onload = () => {
        this.scriptLoaded = true;
        resolve();
      };
      script.onerror = () => reject(new Error('Failed to load Razorpay checkout script'));
      document.body.appendChild(script);
    });
  }

  /**
   * Opens the Razorpay Checkout modal. Resolves with the raw success payload on payment
   * completion (caller is still responsible for calling verifyPayment() afterward — that's
   * what actually confirms the order server-side).
   */
  async openCheckout(
    order: CreateRazorpayOrderResponse,
    customerName: string,
    customerEmail: string,
    customerPhone: string,
  ): Promise<RazorpaySuccessResponse> {
    await this.loadScript();

    return new Promise((resolve, reject) => {
      const options = {
        key: order.keyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: 'Ladies Apparel',
        description: `Order ${order.orderNumber}`,
        order_id: order.razorpayOrderId,
        prefill: {
          name: customerName,
          email: customerEmail,
          contact: customerPhone,
        },
        theme: { color: '#7A2E38' },
        handler: (response: RazorpaySuccessResponse) => resolve(response),
        modal: {
          ondismiss: () => reject(new Error('Payment cancelled')),
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.open();
    });
  }
}
