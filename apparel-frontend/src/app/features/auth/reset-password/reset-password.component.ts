import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toastr = inject(ToastrService);

  readonly loading = signal(false);
  email = '';

  readonly form = this.fb.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  get f() {
    return this.form.controls;
  }

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    if (!this.email) {
      this.router.navigate(['/auth/forgot-password']);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const { otp, newPassword } = this.form.getRawValue();

    this.authService
      .resetPassword({
        email: this.email.trim().toLowerCase(),
        otp: otp.trim(),
        newPassword: newPassword,
      })
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('Password reset successfully! You can now log in.');
        this.router.navigate(['/auth/login'], { queryParams: { email: this.email } });
      });
  }

  getErrorMessage(controlName: keyof typeof this.form.controls): string {
    const control = this.form.controls[controlName];

    if (!control || !control.errors || !control.touched) {
      return '';
    }

    if (control.errors['required']) {
      return controlName === 'otp' ? 'Verification code is required.' : 'New password is required.';
    }
    if (control.errors['pattern'] && controlName === 'otp') {
      return 'Please enter the 6-digit numeric code sent to your email.';
    }
    if (control.errors['minlength'] && controlName === 'newPassword') {
      const min = control.errors['minlength'].requiredLength;
      return `Password must be at least ${min} characters long.`;
    }

    return 'Invalid field value.';
  }
}
