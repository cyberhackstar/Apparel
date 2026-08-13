import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { Category } from '../../../../core/models/category.model';
import { Product } from '../../../../core/models/product.model';
import { AdminCategoryService } from '../../../../core/services/admin-category.service';
import { AdminProductService } from '../../../../core/services/admin-product.service';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './product-form.component.html',
})
export class AdminProductFormComponent implements OnInit {
  // Dependency Injection using inject() BEFORE field initializations
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly productService = inject(AdminProductService);
  private readonly categoryService = inject(AdminCategoryService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  // State Signals
  readonly isEdit = signal(false);
  readonly productId = signal<number | null>(null);
  readonly product = signal<Product | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly saving = signal(false);
  readonly uploadingImage = signal(false);

  // Form definition
  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    categoryId: [null as number | null, [Validators.required]],
    brand: [''],
    fabric: [''],
    basePrice: [0, [Validators.required, Validators.min(0.01)]],
    mrp: [0, [Validators.required, Validators.min(0.01)]],
    discountPercentage: [0, [Validators.min(0), Validators.max(100)]],
    gstPercentage: [5, [Validators.min(0), Validators.max(100)]],
    tags: [''],
    variants: this.fb.array<ReturnType<typeof this.createVariantGroup>>([]),
  });

  ngOnInit(): void {
    this.loadCategories();
    this.checkEditMode();
  }

  get variants(): FormArray {
    return this.form.controls.variants;
  }

  get f() {
    return this.form.controls;
  }

  createVariantGroup() {
    return this.fb.group({
      size: ['', [Validators.required]],
      color: ['', [Validators.required]],
      sku: ['', [Validators.required]],
      stockQuantity: [0, [Validators.required, Validators.min(0)]],
      additionalPrice: [0, [Validators.min(0)]],
    });
  }

  addVariant(): void {
    this.variants.push(this.createVariantGroup());
  }

  removeVariant(index: number): void {
    this.variants.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Please fill all required fields correctly.');
      return;
    }

    if (!this.isEdit() && this.variants.length === 0) {
      this.toastr.warning('Add at least one size/color variant.');
      return;
    }

    this.saving.set(true);

    const raw = this.form.getRawValue();
    const formattedTags =
      typeof raw.tags === 'string'
        ? raw.tags
            .split(',')
            .map((t) => t.trim())
            .filter(Boolean)
            .join(', ')
        : raw.tags;

    const request = {
      ...raw,
      categoryId: raw.categoryId || 0,
      tags: formattedTags,
    };

    const id = this.productId();
    const save$ = id
      ? this.productService.update(id, request)
      : this.productService.create(request);

    save$
      .pipe(
        finalize(() => this.saving.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((product) => {
        this.toastr.success(id ? 'Product updated' : 'Product created. Now add images below.');
        if (!id) {
          this.router.navigate(['/admin/products', product.id, 'edit']);
        } else {
          this.loadProduct(id);
        }
      });
  }

  // --- Existing Product Actions (Variants & Stock) ---

  addVariantToExisting(): void {
    const id = this.productId();
    if (!id) return;

    const size = prompt('Size (e.g. M)');
    const color = prompt('Color (e.g. Red)');
    const sku = prompt('SKU (unique code)');
    const stock = Number(prompt('Initial stock quantity') ?? 0);

    if (!size || !color || !sku) return;

    this.productService
      .addVariant(id, { size, color, sku, stockQuantity: stock, additionalPrice: 0 })
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.success('Variant added');
        this.loadProduct(id);
      });
  }

  updateStock(variantId: number, currentStock: number): void {
    const newStock = prompt('New stock quantity', String(currentStock));
    if (newStock === null || isNaN(Number(newStock))) return;

    const id = this.productId();
    if (!id) return;

    this.productService
      .updateStock(variantId, Number(newStock))
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.success('Stock updated');
        this.loadProduct(id);
      });
  }

  // --- Image Handling ---

  onImageSelected(event: Event, isPrimary: boolean): void {
    const id = this.productId();
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!id || !file) return;

    this.uploadingImage.set(true);

    this.productService
      .uploadImage(id, file, isPrimary)
      .pipe(
        finalize(() => {
          this.uploadingImage.set(false);
          input.value = '';
        }),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('Image uploaded');
        this.loadProduct(id);
      });
  }

  deleteImage(imageId: number): void {
    const id = this.productId();
    if (!id || !confirm('Delete this image?')) return;

    this.productService
      .deleteImage(imageId)
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.info('Image deleted');
        this.loadProduct(id);
      });
  }

  private loadCategories(): void {
    this.categoryService
      .list()
      .pipe(catchError(() => EMPTY))
      .subscribe((cats) => this.categories.set(cats));
  }

  private checkEditMode(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.isEdit.set(true);
      this.productId.set(id);
      this.loadProduct(id);
    } else {
      this.addVariant();
    }
  }

  private loadProduct(id: number): void {
    this.productService
      .getById(id)
      .pipe(catchError(() => EMPTY))
      .subscribe((product) => {
        this.product.set(product);
        this.form.patchValue({
          name: product.name,
          description: product.description,
          categoryId: product.categoryId,
          brand: product.brand,
          fabric: product.fabric,
          basePrice: product.basePrice,
          mrp: product.mrp,
          discountPercentage: product.discountPercentage,
          gstPercentage: product.gstPercentage,
          tags: Array.isArray(product.tags) ? product.tags.join(', ') : product.tags,
        });
      });
  }
}
