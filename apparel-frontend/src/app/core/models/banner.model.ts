export interface Banner {
  id: number;
  title: string;
  imageUrl: string;
  linkUrl?: string;
  displayOrder: number;
  active: boolean;
}
