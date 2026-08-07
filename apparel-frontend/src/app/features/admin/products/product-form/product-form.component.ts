import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AdminProductService } from '../../../../core/services/admin-product.service';
import { AdminCategoryService } from '../../../../core/services/admin-category.service';
import { Category } from '../../../../core/models/category.model';
import { Product } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './product-form.component.html',
})
export class AdminProductFormComponent implements OnInit {
  // 1. Inject services using `inject()` at the field level
  private fb = inject(FormBuilder);
  private productService = inject(AdminProductService);
  private categoryService = inject(AdminCategoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toastr = inject(ToastrService);

  isEdit = signal(false);
  productId = signal<number | null>(null);
  product = signal<Product | null>(null);
  categories = signal<Category[]>([]);
  saving = signal(false);
  uploadingImage = signal(false);

  // 2. `this.fb` is now fully initialized and safe to use here
  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    categoryId: [null as number | null, Validators.required],
    brand: [''],
    fabric: [''],
    basePrice: [0, [Validators.required, Validators.min(1)]],
    mrp: [0, [Validators.required, Validators.min(1)]],
    discountPercentage: [0],
    gstPercentage: [5],
    tags: [''],
    variants: this.fb.array([]),
  });

  ngOnInit(): void {
    this.categoryService.list().subscribe((cats) => this.categories.set(cats));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      this.productId.set(Number(idParam));
      this.loadProduct(Number(idParam));
    } else {
      this.addVariant(); // start with one blank variant row for new products
    }
  }

  get variants(): FormArray {
    return this.form.get('variants') as FormArray;
  }

  private loadProduct(id: number): void {
    this.productService.getById(id).subscribe((product) => {
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
        tags: product.tags.join(','),
      });
    });
  }

  addVariant(): void {
    this.variants.push(
      this.fb.group({
        size: ['', Validators.required],
        color: ['', Validators.required],
        sku: ['', Validators.required],
        stockQuantity: [0, [Validators.required, Validators.min(0)]],
        additionalPrice: [0],
      }),
    );
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
    const request: any = {
      name: raw.name,
      description: raw.description,
      categoryId: raw.categoryId,
      brand: raw.brand,
      fabric: raw.fabric,
      basePrice: raw.basePrice,
      mrp: raw.mrp,
      discountPercentage: raw.discountPercentage,
      gstPercentage: raw.gstPercentage,
      tags: raw.tags,
      variants: raw.variants,
    };

    const id = this.productId();
    const obs = id ? this.productService.update(id, request) : this.productService.create(request);

    obs.subscribe({
      next: (product) => {
        this.toastr.success(id ? 'Product updated' : 'Product created. Now add images below.');
        this.saving.set(false);
        if (!id) {
          this.router.navigate(['/admin/products', product.id, 'edit']);
        } else {
          this.loadProduct(id);
        }
      },
      error: () => this.saving.set(false),
    });
  }

  addVariantToExisting(): void {
    const id = this.productId();
    if (!id) return;
    const group = this.fb.group({
      size: ['', Validators.required],
      color: ['', Validators.required],
      sku: ['', Validators.required],
      stockQuantity: [0],
      additionalPrice: [0],
    });
    const size = prompt('Size (e.g. M)');
    const color = prompt('Color (e.g. Red)');
    const sku = prompt('SKU (unique code)');
    const stock = Number(prompt('Initial stock quantity') ?? 0);
    if (!size || !color || !sku) return;

    this.productService
      .addVariant(id, { size, color, sku, stockQuantity: stock, additionalPrice: 0 })
      .subscribe({
        next: () => {
          this.toastr.success('Variant added');
          this.loadProduct(id);
        },
      });
  }

  updateStock(variantId: number, currentStock: number): void {
    const newStock = prompt('New stock quantity', String(currentStock));
    if (newStock === null || isNaN(Number(newStock))) return;
    this.productService.updateStock(variantId, Number(newStock)).subscribe(() => {
      this.toastr.success('Stock updated');
      this.loadProduct(this.productId()!);
    });
  }

  onImageSelected(event: Event, isPrimary: boolean): void {
    const id = this.productId();
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!id || !file) return;

    this.uploadingImage.set(true);
    this.productService.uploadImage(id, file, isPrimary).subscribe({
      next: () => {
        this.toastr.success('Image uploaded');
        this.uploadingImage.set(false);
        this.loadProduct(id);
      },
      error: () => this.uploadingImage.set(false),
    });
  }

  deleteImage(imageId: number): void {
    if (!confirm('Delete this image?')) return;
    this.productService.deleteImage(imageId).subscribe(() => {
      this.toastr.info('Image deleted');
      this.loadProduct(this.productId()!);
    });
  }

  get f() {
    return this.form.controls;
  }
}
