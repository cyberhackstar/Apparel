import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { Profile } from '../../../core/models/profile.model';
import { AccountService } from '../../../core/services/account.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  // Dependency Injection via inject() before field declarations
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  // State Signals
  readonly profile = signal<Profile | null>(null);
  readonly loading = signal(true);
  readonly savingProfile = signal(false);
  readonly savingPassword = signal(false);

  readonly emailChangeStep = signal<'idle' | 'otp-sent'>('idle');
  readonly requestingEmailChange = signal(false);
  readonly verifyingEmailChange = signal(false);

  // Reactive Forms
  readonly profileForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    phone: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
  });

  readonly emailForm = this.fb.group({
    newEmail: ['', [Validators.required, Validators.email]],
    emailOtp: [''],
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  // Controls Getters
  get pf() {
    return this.profileForm.controls;
  }

  get ef() {
    return this.emailForm.controls;
  }

  get pwf() {
    return this.passwordForm.controls;
  }

  ngOnInit(): void {
    this.accountService
      .getProfile()
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({
          fullName: profile.fullName,
          phone: profile.phone,
        });
      });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.savingProfile.set(true);

    this.accountService
      .updateProfile(this.profileForm.getRawValue())
      .pipe(
        finalize(() => this.savingProfile.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe((updatedProfile) => {
        this.profile.set(updatedProfile);
        this.toastr.success('Profile updated successfully.');
      });
  }

  requestEmailChange(): void {
    const emailControl = this.ef.newEmail;

    if (emailControl.invalid) {
      emailControl.markAsTouched();
      this.toastr.warning('Please enter a valid new email address.');
      return;
    }

    this.requestingEmailChange.set(true);
    const newEmail = emailControl.value;

    this.accountService
      .requestEmailChange({ newEmail })
      .pipe(
        finalize(() => this.requestingEmailChange.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.emailChangeStep.set('otp-sent');
        // Require OTP validation once code is dispatched
        this.ef.emailOtp.setValidators([Validators.required, Validators.minLength(4)]);
        this.ef.emailOtp.updateValueAndValidity();
        this.toastr.success('Verification code sent to your new email.');
      });
  }

  verifyEmailChange(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    this.verifyingEmailChange.set(true);
    const { newEmail, emailOtp } = this.emailForm.getRawValue();

    this.accountService
      .verifyEmailChange({ newEmail, otp: emailOtp })
      .pipe(
        finalize(() => this.verifyingEmailChange.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('Email updated! Please log in again with your new email.');
        this.authService.logout();
        this.router.navigate(['/auth/login']);
      });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.savingPassword.set(true);

    this.accountService
      .changePassword(this.passwordForm.getRawValue())
      .pipe(
        finalize(() => this.savingPassword.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.passwordForm.reset();
        this.toastr.success('Password changed successfully.');
      });
  }

  resetEmailForm(): void {
    this.emailForm.reset();
    this.ef.emailOtp.clearValidators();
    this.ef.emailOtp.updateValueAndValidity();
    this.emailChangeStep.set('idle');
  }
}
