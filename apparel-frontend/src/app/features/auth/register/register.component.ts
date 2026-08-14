import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleSignInButtonComponent } from '../../../shared/components/google-signin-button/google-signin-button.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, GoogleSignInButtonComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  readonly loading = signal(false);

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  get f() {
    return this.form.controls;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Please fix the validation errors before submitting.');
      return;
    }

    this.loading.set(true);

    const raw = this.form.getRawValue();
    const payload = {
      ...raw,
      fullName: raw.fullName.trim(),
      email: raw.email.trim().toLowerCase(),
      phone: raw.phone.trim().replace(/\D/g, '').slice(-10),
    };

    this.authService
      .register(payload)
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('Check your email for the verification code.');
        this.router.navigate(['/auth/verify-otp'], {
          queryParams: { email: payload.email },
        });
      });
  }

  getErrorMessage(controlName: keyof typeof this.form.controls): string {
    const control = this.form.controls[controlName];

    if (!control || !control.errors || !control.touched) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.formatFieldName(controlName)} is required.`;
    }
    if (control.errors['email']) {
      return 'Please enter a valid email address.';
    }
    if (control.errors['pattern'] && controlName === 'phone') {
      return 'Please enter a valid 10-digit Indian mobile number (starts with 6-9).';
    }
    if (control.errors['minlength']) {
      const min = control.errors['minlength'].requiredLength;
      return `${this.formatFieldName(controlName)} must be at least ${min} characters.`;
    }
    if (control.errors['maxlength']) {
      const max = control.errors['maxlength'].requiredLength;
      return `${this.formatFieldName(controlName)} cannot exceed ${max} characters.`;
    }

    return 'Invalid field value.';
  }

  private formatFieldName(name: string): string {
    switch (name) {
      case 'fullName':
        return 'Full name';
      case 'phone':
        return 'Phone number';
      default:
        return name.charAt(0).toUpperCase() + name.slice(1);
    }
  }
}
