export interface CartItem {
  cartItemId: number;
  productId: number;
  productName: string;
  productSlug: string;
  imageUrl?: string;
  variantId: number;
  size: string;
  color: string;
  unitPrice: number;
  mrp: number;
  quantity: number;
  lineTotal: number;
  availableStock: number;
  inStock: boolean;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  totalMrp: number;
  totalDiscount: number;
  hasOutOfStockItems: boolean;
}

export interface AddToCartRequest {
  variantId: number;
  quantity: number;
}
