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
  // Dependency Injection via inject()
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  // State Signals
  readonly loading = signal(false);

  // Strongly-typed reactive form
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

    // Form value is strongly typed non-nullable strings
    const payload = this.form.getRawValue();

    this.authService
      .register(payload)
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY), // Interceptors or global handlers deal with HTTP error UI
      )
      .subscribe(() => {
        this.toastr.success('Check your email for the verification code.');
        this.router.navigate(['/auth/verify-otp'], {
          queryParams: { email: payload.email },
        });
      });
  }
}
