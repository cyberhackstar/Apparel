import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { Coupon, DiscountType } from '../../../core/models/admin-misc.model';
import { AdminCouponService } from '../../../core/services/admin-coupon.service';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './coupons.component.html',
})
export class AdminCouponsComponent implements OnInit {
  // Dependency Injection via inject() placed BEFORE class field initializations
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly couponService = inject(AdminCouponService);
  private readonly toastr = inject(ToastrService);

  // State Signals
  readonly coupons = signal<Coupon[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly saving = signal(false);

  // Strongly-typed non-nullable reactive form
  readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    description: [''],
    discountType: ['FLAT', [Validators.required]],
    discountValue: [0, [Validators.required, Validators.min(1)]],
    minOrderValue: [0, [Validators.min(0)]],
    maxDiscountAmount: [null as number | null],
    usageLimitPerUser: [1, [Validators.min(1)]],
    totalUsageLimit: [null as number | null],
    validFrom: ['', [Validators.required]],
    validTo: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.fetch();
  }

  get f() {
    return this.form.controls;
  }

  fetch(): void {
    this.loading.set(true);
    this.couponService
      .list()
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((coupons) => this.coupons.set(coupons));
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({
      discountType: 'FLAT',
      discountValue: 0,
      minOrderValue: 0,
      usageLimitPerUser: 1,
      maxDiscountAmount: null,
      totalUsageLimit: null,
    });
    this.showForm.set(true);
  }

  openEditForm(coupon: Coupon): void {
    this.editingId.set(coupon.id);
    this.form.patchValue({
      code: coupon.code,
      description: coupon.description ?? '',
      discountType: coupon.discountType,
      discountValue: coupon.discountValue,
      minOrderValue: coupon.minOrderValue ?? 0,
      maxDiscountAmount: coupon.maxDiscountAmount ?? null,
      usageLimitPerUser: coupon.usageLimitPerUser ?? 1,
      totalUsageLimit: coupon.totalUsageLimit ?? null,
      validFrom: coupon.validFrom ? coupon.validFrom.slice(0, 16) : '',
      validTo: coupon.validTo ? coupon.validTo.slice(0, 16) : '',
    });
    this.showForm.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Please fill all required fields correctly.');
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();

    // Map payload & ensure nulls/undefined types align with the DTO model
    const request = {
      ...raw,
      discountType: raw.discountType as unknown as DiscountType,
      maxDiscountAmount: raw.maxDiscountAmount ?? undefined,
      totalUsageLimit: raw.totalUsageLimit ?? undefined,
      validFrom: new Date(raw.validFrom).toISOString(),
      validTo: new Date(raw.validTo).toISOString(),
    };

    const editId = this.editingId();
    const save$ = editId
      ? this.couponService.update(editId, request)
      : this.couponService.create(request);

    save$
      .pipe(
        finalize(() => this.saving.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success(editId ? 'Coupon updated' : 'Coupon created');
        this.showForm.set(false);
        this.fetch();
      });
  }

  deactivate(id: number): void {
    if (!confirm('Deactivate this coupon?')) return;

    this.couponService
      .deactivate(id)
      .pipe(catchError(() => EMPTY))
      .subscribe(() => {
        this.toastr.info('Coupon deactivated');
        this.fetch();
      });
  }
}
