import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BannerService } from '../../core/services/banner.service';
import { CategoryService } from '../../core/services/category.service';
import { ProductService } from '../../core/services/product.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { Banner } from '../../core/models/banner.model';
import { Category } from '../../core/models/category.model';
import { Product } from '../../core/models/product.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit {
  banners = signal<Banner[]>([]);
  categories = signal<Category[]>([]);
  newArrivals = signal<Product[]>([]);
  bestsellers = signal<Product[]>([]);
  activeSlide = signal(0);

  constructor(
    private bannerService: BannerService,
    private categoryService: CategoryService,
    private productService: ProductService,
    public wishlistService: WishlistService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.bannerService.getActiveBanners().subscribe((banners) => this.banners.set(banners));
    this.categoryService.getTree().subscribe((cats) => this.categories.set(cats));

    this.productService
      .search({ sortBy: 'createdAt', sortDir: 'desc', pageSize: 8 })
      .subscribe((res) => this.newArrivals.set(res.content));

    this.productService
      .search({ sortBy: 'ratingCount', sortDir: 'desc', pageSize: 8 })
      .subscribe((res) => this.bestsellers.set(res.content));
  }

  goToSlide(index: number): void {
    this.activeSlide.set(index);
  }

  onBannerClick(banner: Banner): void {
    if (banner.linkUrl) {
      if (banner.linkUrl.startsWith('http')) {
        window.open(banner.linkUrl, '_blank');
      } else {
        this.router.navigateByUrl(banner.linkUrl);
      }
    }
  }

  toggleWishlist(product: Product): void {
    this.wishlistService.toggle(product.id).subscribe();
  }
}
