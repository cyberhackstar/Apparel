export type DiscountType = 'FLAT' | 'PERCENTAGE';

export interface Coupon {
  id: number;
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue: number;
  maxDiscountAmount?: number;
  usageLimitPerUser: number;
  totalUsageLimit?: number;
  totalUsedCount: number;
  validFrom: string;
  validTo: string;
  active: boolean;
}

export interface CouponRequest {
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue?: number;
  maxDiscountAmount?: number;
  usageLimitPerUser?: number;
  totalUsageLimit?: number;
  validFrom: string;
  validTo: string;
}

export interface AuditLog {
  id: number;
  adminEmail: string;
  httpMethod: string;
  path: string;
  statusCode: number;
  timestamp: string;
}

export interface Customer {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  enabled: boolean;
  blocked: boolean;
  createdAt: string;
}

export interface ServiceablePincode {
  id: number;
  pincode: string;
  city?: string;
  state?: string;
  codAvailable: boolean;
  estimatedDeliveryDays: number;
  active: boolean;
}
