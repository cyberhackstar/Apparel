import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { Category } from '../../../core/models/category.model';
import { AdminCategoryService } from '../../../core/services/admin-category.service';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categories.component.html',
})
export class AdminCategoriesComponent implements OnInit {
  // Dependency Injection via inject() declared before property/form initializations
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly categoryService = inject(AdminCategoryService);
  private readonly toastr = inject(ToastrService);

  // State Signals
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly saving = signal(false);

  // Strongly-typed non-nullable reactive form
  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    imageUrl: [''],
    parentCategoryId: [null as number | null],
    displayOrder: [0, [Validators.required, Validators.min(0)]],
  });

  // Flat list computed: parent-first, children indented immediately under their parent
  readonly orderedForDisplay = computed(() => {
    const all = this.categories();
    const topLevel = all.filter((c) => !c.parentCategoryId);
    const result: Category[] = [];

    for (const parent of topLevel) {
      result.push(parent);
      result.push(...all.filter((c) => c.parentCategoryId === parent.id));
    }

    // Fallback for orphaned children whose parent got filtered out
    const seenIds = new Set(result.map((c) => c.id));
    result.push(...all.filter((c) => !seenIds.has(c.id)));

    return result;
  });

  ngOnInit(): void {
    this.fetch();
  }

  get f() {
    return this.form.controls;
  }

  fetch(): void {
    this.loading.set(true);
    this.categoryService
      .list()
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((cats) => this.categories.set(cats));
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({ displayOrder: 0, parentCategoryId: null });
    this.showForm.set(true);
  }

  openEditForm(cat: Category): void {
    this.editingId.set(cat.id);
    this.form.patchValue({
      name: cat.name,
      description: cat.description ?? '',
      imageUrl: cat.imageUrl ?? '',
      parentCategoryId: cat.parentCategoryId ?? null,
      displayOrder: cat.displayOrder ?? 0,
    });
    this.showForm.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Please fill out all required fields correctly.');
      return;
    }

    this.saving.set(true);
    const request = this.form.getRawValue();
    const editId = this.editingId();

    // CategoryRequest expects parentCategoryId to be undefined when absent
    const payload = {
      ...request,
      parentCategoryId: request.parentCategoryId === null ? undefined : request.parentCategoryId,
    };

    const save$ = editId
      ? this.categoryService.update(editId, payload)
      : this.categoryService.create(payload);

    save$
      .pipe(
        finalize(() => this.saving.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success(
          editId ? 'Category updated successfully' : 'Category created successfully',
        );
        this.showForm.set(false);
        this.fetch();
      });
  }

  deactivate(id: number): void {
    if (
      !confirm(
        'Deactivate this category? It (and any subcategories) will be hidden from the storefront, but stays visible here.',
      )
    ) {
      return;
    }

    this.categoryService
      .deactivate(id)
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.info('Category deactivated');
        this.fetch();
      });
  }

  activate(id: number): void {
    this.categoryService
      .activate(id)
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.success('Category reactivated');
        this.fetch();
      });
  }

  isSubcategory(cat: Category): boolean {
    return !!cat.parentCategoryId;
  }
}
