export interface CreateRazorpayOrderResponse {
  razorpayOrderId: string;
  amountInPaise: number;
  currency: string;
  keyId: string;
  orderNumber: string;
}

export interface VerifyPaymentRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

// shape of the object Razorpay Checkout.js hands back in its success handler
export interface RazorpaySuccessResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}
