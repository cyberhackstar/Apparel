export type ReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface ReturnRequest {
  id: number;
  orderNumber: string;
  customerEmail: string;
  reason: string;
  status: ReturnStatus;
  adminNotes?: string;
  refundAmount?: number;
  requestedAt: string;
  resolvedAt?: string;
}

export interface CreateReturnRequest {
  orderNumber: string;
  reason: string;
}

export interface ResolveReturnRequest {
  adminNotes?: string;
  refundAmount?: number;
}
