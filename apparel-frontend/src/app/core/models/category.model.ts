export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  parentCategoryId?: number;
  parentCategoryName?: string;
  displayOrder: number;
  active: boolean;
  subCategories?: Category[];
}

export interface CategoryRequest {
  name: string;
  description?: string;
  imageUrl?: string;
  parentCategoryId?: number;
  displayOrder?: number;
}
