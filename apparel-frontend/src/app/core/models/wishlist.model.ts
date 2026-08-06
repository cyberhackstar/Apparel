export interface WishlistItem {
  productId: number;
  name: string;
  slug: string;
  imageUrl?: string;
  basePrice: number;
  mrp: number;
  discountPercentage: number;
  inStock: boolean;
}
