import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminCategoryService } from '../../../core/services/admin-category.service';
import { Category } from '../../../core/models/category.model';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categories.component.html',
})
export class AdminCategoriesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(AdminCategoryService);
  private readonly toastr = inject(ToastrService);

  categories = signal<Category[]>([]);
  loading = signal(true);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  saving = signal(false);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    imageUrl: [''],
    parentCategoryId: [null as number | null],
    displayOrder: [0],
  });

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.categoryService.list().subscribe({
      next: (cats) => {
        this.categories.set(cats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({ displayOrder: 0 });
    this.showForm.set(true);
  }

  openEditForm(cat: Category): void {
    this.editingId.set(cat.id);
    this.form.patchValue(cat);
    this.showForm.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const request = this.form.getRawValue() as any;
    const editId = this.editingId();

    const obs = editId
      ? this.categoryService.update(editId, request)
      : this.categoryService.create(request);
    obs.subscribe({
      next: () => {
        this.toastr.success(editId ? 'Category updated' : 'Category created');
        this.showForm.set(false);
        this.saving.set(false);
        this.fetch();
      },
      error: () => this.saving.set(false),
    });
  }

  deactivate(id: number): void {
    if (
      !confirm(
        'Deactivate this category? Products in it will remain but the category will be hidden.',
      )
    )
      return;
    this.categoryService.deactivate(id).subscribe(() => {
      this.toastr.info('Category deactivated');
      this.fetch();
    });
  }

  get f() {
    return this.form.controls;
  }
}
