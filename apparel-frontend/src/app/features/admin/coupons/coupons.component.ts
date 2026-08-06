import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminCouponService } from '../../../core/services/admin-coupon.service';
import { Coupon } from '../../../core/models/admin-misc.model';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './coupons.component.html',
})
export class AdminCouponsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly couponService = inject(AdminCouponService);
  private readonly toastr = inject(ToastrService);

  coupons = signal<Coupon[]>([]);
  loading = signal(true);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  saving = signal(false);

  readonly form = this.fb.group({
    code: ['', Validators.required],
    description: [''],
    discountType: ['FLAT', Validators.required],
    discountValue: [0, [Validators.required, Validators.min(1)]],
    minOrderValue: [0],
    maxDiscountAmount: [null as number | null],
    usageLimitPerUser: [1],
    totalUsageLimit: [null as number | null],
    validFrom: ['', Validators.required],
    validTo: ['', Validators.required],
  });

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.couponService.list().subscribe({
      next: (coupons) => {
        this.coupons.set(coupons);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({ discountType: 'FLAT', minOrderValue: 0, usageLimitPerUser: 1 });
    this.showForm.set(true);
  }

  openEditForm(coupon: Coupon): void {
    this.editingId.set(coupon.id);
    this.form.patchValue({
      ...coupon,
      validFrom: coupon.validFrom.slice(0, 16),
      validTo: coupon.validTo.slice(0, 16),
    });
    this.showForm.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request = {
      ...raw,
      validFrom: new Date(raw.validFrom!).toISOString(),
      validTo: new Date(raw.validTo!).toISOString(),
    } as any;

    const editId = this.editingId();
    const obs = editId
      ? this.couponService.update(editId, request)
      : this.couponService.create(request);

    obs.subscribe({
      next: () => {
        this.toastr.success(editId ? 'Coupon updated' : 'Coupon created');
        this.showForm.set(false);
        this.saving.set(false);
        this.fetch();
      },
      error: () => this.saving.set(false),
    });
  }

  deactivate(id: number): void {
    if (!confirm('Deactivate this coupon?')) return;
    this.couponService.deactivate(id).subscribe(() => {
      this.toastr.info('Coupon deactivated');
      this.fetch();
    });
  }

  get f() {
    return this.form.controls;
  }
}
