import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product } from '../../../core/models/product.model';
import { StarRatingComponent } from '../star-rating/star-rating.component';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink, StarRatingComponent],
  templateUrl: './product-card.component.html',
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Input() wishlisted = false;
  @Output() wishlistToggle = new EventEmitter<Product>();

  get primaryImage(): string {
    const primary = this.product.images?.find((i) => i.primary) ?? this.product.images?.[0];
    return primary?.imageUrl ?? 'https://placehold.co/400x500/F1DDE0/7A2E38?text=Ladies+Apparel';
  }

  get inStock(): boolean {
    return this.product.variants?.some((v) => v.inStock) ?? false;
  }

  onWishlistClick(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.wishlistToggle.emit(this.product);
  }
}
