import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { Product, ProductSearchParams } from '../../../core/models/product.model';
import { Category } from '../../../core/models/category.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

const SIZES = ['S', 'M', 'L', 'XL', 'XXL'];

@Component({
  selector: 'app-product-listing',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductCardComponent],
  templateUrl: './product-listing.component.html',
})
export class ProductListingComponent implements OnInit {
  products = signal<Product[]>([]);
  categories = signal<Category[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  totalPages = signal(0);

  availableSizes = SIZES;
  filtersOpen = signal(false);

  filters: ProductSearchParams = {
    page: 0,
    pageSize: 12,
    sortBy: 'createdAt',
    sortDir: 'desc',
  };

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    public wishlistService: WishlistService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.categoryService.getTree().subscribe((cats) => this.categories.set(cats));

    this.route.queryParamMap.subscribe((params) => {
      this.filters = {
        categoryId: params.get('categoryId') ? Number(params.get('categoryId')) : undefined,
        minPrice: params.get('minPrice') ? Number(params.get('minPrice')) : undefined,
        maxPrice: params.get('maxPrice') ? Number(params.get('maxPrice')) : undefined,
        size: params.get('size') ?? undefined,
        color: params.get('color') ?? undefined,
        keyword: params.get('keyword') ?? undefined,
        sortBy: params.get('sortBy') ?? 'createdAt',
        sortDir: (params.get('sortDir') as 'asc' | 'desc') ?? 'desc',
        page: params.get('page') ? Number(params.get('page')) : 0,
        pageSize: 12,
      };
      this.fetchProducts();
    });
  }

  fetchProducts(): void {
    this.loading.set(true);
    this.productService.search(this.filters).subscribe({
      next: (res) => {
        this.products.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyFilter(patch: Partial<ProductSearchParams>): void {
    const merged = { ...this.filters, ...patch, page: 0 };
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: merged,
      queryParamsHandling: '',
    });
  }

  onSortChange(value: string): void {
    const [sortBy, sortDir] = value.split(':');
    this.applyFilter({ sortBy, sortDir: sortDir as 'asc' | 'desc' });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.router.navigate([], { relativeTo: this.route, queryParams: { ...this.filters, page } });
  }

  clearFilters(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  toggleWishlist(product: Product): void {
    this.wishlistService.toggle(product.id).subscribe();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
