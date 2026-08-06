import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AdminProductService, BulkUploadResult } from '../../../core/services/admin-product.service';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './products.component.html',
})
export class AdminProductsComponent implements OnInit {
  products = signal<Product[]>([]);
  loading = signal(true);
  keyword = '';
  page = signal(0);
  totalPages = signal(0);

  showBulkUpload = signal(false);
  uploading = signal(false);
  uploadResult = signal<BulkUploadResult | null>(null);
  selectedFile: File | null = null;

  constructor(private productService: AdminProductService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.productService.list({ keyword: this.keyword, page: this.page(), pageSize: 15 }).subscribe({
      next: (res) => {
        this.products.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  search(): void {
    this.page.set(0);
    this.fetch();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
  }

  deactivate(id: number): void {
    if (!confirm('Deactivate this product? It will be hidden from the storefront.')) return;
    this.productService.deactivate(id).subscribe(() => {
      this.toastr.info('Product deactivated');
      this.fetch();
    });
  }

  totalStock(product: Product): number {
    return product.variants.reduce((sum, v) => sum + v.stockQuantity, 0);
  }

  onFileSelected(event: Event): void {
    this.selectedFile = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  uploadCsv(): void {
    if (!this.selectedFile) {
      this.toastr.warning('Please choose a CSV file first.');
      return;
    }
    this.uploading.set(true);
    this.productService.bulkUpload(this.selectedFile).subscribe({
      next: (result) => {
        this.uploadResult.set(result);
        this.uploading.set(false);
        this.selectedFile = null;
        this.toastr.success(`${result.productsCreated} product(s) created.`);
        this.fetch();
      },
      error: () => this.uploading.set(false),
    });
  }
}
