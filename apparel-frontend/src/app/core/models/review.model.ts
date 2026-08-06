export interface Review {
  id: number;
  productId: number;
  productName: string;
  userFullName: string;
  rating: number;
  comment?: string;
  verifiedPurchase: boolean;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  adminReply?: string;
  imageUrls: string[];
  createdAt: string;
}

export interface ReviewRequest {
  productId: number;
  rating: number;
  comment?: string;
}
