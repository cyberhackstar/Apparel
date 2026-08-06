export interface ProductVariant {
  id: number;
  size: string;
  color: string;
  sku: string;
  stockQuantity: number;
  additionalPrice: number;
  finalPrice: number;
  active: boolean;
  inStock: boolean;
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  displayOrder: number;
  primary: boolean;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description?: string;
  categoryId: number;
  categoryName: string;
  brand?: string;
  fabric?: string;
  basePrice: number;
  mrp: number;
  discountPercentage: number;
  gstPercentage: number;
  tags: string[];
  averageRating: number;
  ratingCount: number;
  active: boolean;
  variants: ProductVariant[];
  images: ProductImage[];
}

export interface ProductSearchParams {
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  size?: string;
  color?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ProductVariantRequest {
  size: string;
  color: string;
  sku: string;
  stockQuantity: number;
  additionalPrice: number;
}

export interface ProductRequest {
  name: string;
  description?: string;
  categoryId: number;
  brand?: string;
  fabric?: string;
  basePrice: number;
  mrp: number;
  discountPercentage?: number;
  gstPercentage?: number;
  tags?: string;
  variants: ProductVariantRequest[];
}
