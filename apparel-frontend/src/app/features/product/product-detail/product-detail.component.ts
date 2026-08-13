import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductService } from '../../../core/services/product.service';
import { ReviewService } from '../../../core/services/review.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';
import { ServiceabilityService } from '../../../core/services/serviceability.service';
import { RecentlyViewedService } from '../../../core/services/recently-viewed.service';
import { Product, ProductVariant } from '../../../core/models/product.model';
import { Review } from '../../../core/models/review.model';
import { ServiceabilityResponse } from '../../../core/models/serviceability.model';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, StarRatingComponent, ProductCardComponent],
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  product = signal<Product | null>(null);
  loading = signal(true);
  activeImageIndex = signal(0);
  selectedSize = signal<string | null>(null);
  selectedColor = signal<string | null>(null);
  quantity = signal(1);
  addingToCart = signal(false);

  relatedProducts = signal<Product[]>([]);
  recentlyViewed = signal<Product[]>([]);

  reviews = signal<Review[]>([]);
  reviewRating = 5;
  reviewComment = '';
  submittingReview = signal(false);

  pincodeInput = '';
  serviceability = signal<ServiceabilityResponse | null>(null);
  checkingPincode = signal(false);

  availableSizes = computed(() => {
    const p = this.product();
    if (!p) return [];
    return [...new Set(p.variants.map((v) => v.size))];
  });

  availableColors = computed(() => {
    const p = this.product();
    const size = this.selectedSize();
    if (!p) return [];
    const variants = size ? p.variants.filter((v) => v.size === size) : p.variants;
    return [...new Set(variants.map((v) => v.color))];
  });

  selectedVariant = computed<ProductVariant | undefined>(() => {
    const p = this.product();
    if (!p) return undefined;
    return p.variants.find((v) => v.size === this.selectedSize() && v.color === this.selectedColor());
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private reviewService: ReviewService,
    private cartService: CartService,
    public wishlistService: WishlistService,
    public authService: AuthService,
    private serviceabilityService: ServiceabilityService,
    private recentlyViewedService: RecentlyViewedService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const slug = params.get('slug');
      if (slug) this.loadProduct(slug);
    });
  }

  private loadProduct(slug: string): void {
    this.loading.set(true);
    this.productService.getBySlug(slug).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loading.set(false);
        this.activeImageIndex.set(0);
        this.quantity.set(1);

        const firstInStock = product.variants.find((v) => v.inStock) ?? product.variants[0];
        this.selectedSize.set(firstInStock?.size ?? null);
        this.selectedColor.set(firstInStock?.color ?? null);

        this.loadReviews(product.id);

        this.productService.getRelated(product.id, 8).subscribe((related) => this.relatedProducts.set(related));

        this.recentlyViewedService.track(product.id);
        this.recentlyViewedService.getRecentlyViewed(product.id).subscribe((items) => this.recentlyViewed.set(items));
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/products']);
      },
    });
  }

  private loadReviews(productId: number): void {
    this.reviewService.getApprovedForProduct(productId).subscribe((res) => this.reviews.set(res.content));
  }

  selectSize(size: string): void {
    this.selectedSize.set(size);
    // reset color if it's not valid for the new size
    const p = this.product();
    if (p && !p.variants.some((v) => v.size === size && v.color === this.selectedColor())) {
      const firstMatch = p.variants.find((v) => v.size === size);
      this.selectedColor.set(firstMatch?.color ?? null);
    }
  }

  selectColor(color: string): void {
    this.selectedColor.set(color);
  }

  incrementQty(): void {
    const max = this.selectedVariant()?.stockQuantity ?? 10;
    this.quantity.update((q) => Math.min(q + 1, max));
  }

  decrementQty(): void {
    this.quantity.update((q) => Math.max(q - 1, 1));
  }

  addToCart(): void {
    const variant = this.selectedVariant();
    if (!variant) {
      this.toastr.warning('Please select a size and color.');
      return;
    }
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.addingToCart.set(true);
    this.cartService.addItem({ variantId: variant.id, quantity: this.quantity() }).subscribe({
      next: () => {
        this.toastr.success('Added to cart!');
        this.addingToCart.set(false);
      },
      error: () => this.addingToCart.set(false),
    });
  }

  toggleWishlist(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    const p = this.product();
    if (p) this.wishlistService.toggle(p.id).subscribe();
  }

  /** Used by the related/recently-viewed product cards, which emit the specific Product clicked. */
  toggleWishlistFor(product: Product): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.wishlistService.toggle(product.id).subscribe();
  }

  checkPincode(): void {
    if (!/^\d{6}$/.test(this.pincodeInput)) {
      this.toastr.warning('Please enter a valid 6-digit pincode.');
      return;
    }
    this.checkingPincode.set(true);
    this.serviceabilityService.check(this.pincodeInput).subscribe({
      next: (res) => {
        this.serviceability.set(res);
        this.checkingPincode.set(false);
      },
      error: () => this.checkingPincode.set(false),
    });
  }

  submitReview(): void {
    const p = this.product();
    if (!p) return;
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.submittingReview.set(true);
    this.reviewService.submit({ productId: p.id, rating: this.reviewRating, comment: this.reviewComment }).subscribe({
      next: () => {
        this.toastr.success('Thanks! Your review has been submitted for moderation.');
        this.reviewComment = '';
        this.reviewRating = 5;
        this.submittingReview.set(false);
      },
      error: () => this.submittingReview.set(false),
    });
  }

  get discountedPrice(): number {
    const p = this.product();
    const variant = this.selectedVariant();
    if (!p) return 0;
    return p.basePrice + (variant?.additionalPrice ?? 0);
  }

  get mrpPrice(): number {
    const p = this.product();
    const variant = this.selectedVariant();
    if (!p) return 0;
    return p.mrp + (variant?.additionalPrice ?? 0);
  }
}
