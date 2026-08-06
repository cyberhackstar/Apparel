export type OrderStatus =
  | 'PLACED' | 'CONFIRMED' | 'PACKED' | 'SHIPPED'
  | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED' | 'RETURNED';

export type PaymentMethod = 'COD' | 'RAZORPAY';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface OrderItem {
  productName: string;
  imageUrl?: string;
  size: string;
  color: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  orderNumber: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  recipientName: string;
  recipientPhone: string;
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  state: string;
  pincode: string;
  subtotal: number;
  discountAmount: number;
  couponCode?: string;
  shippingCharge: number;
  gstAmount: number;
  grandTotal: number;
  items: OrderItem[];
  createdAt: string;
}

export interface PlaceOrderRequest {
  addressId: number;
  couponCode?: string;
  paymentMethod: PaymentMethod;
}
