import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AccountService } from '../../../core/services/account.service';
import { AuthService } from '../../../core/services/auth.service';
import { Profile } from '../../../core/models/profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  profile = signal<Profile | null>(null);
  loading = signal(true);
  savingProfile = signal(false);
  savingPassword = signal(false);

  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastr = inject(ToastrService);

  emailChangeStep = signal<'idle' | 'otp-sent'>('idle');
  newEmail = '';
  emailOtp = '';
  requestingEmailChange = signal(false);
  verifyingEmailChange = signal(false);

  profileForm = this.fb.group({
    fullName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
  });

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.accountService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({ fullName: profile.fullName, phone: profile.phone });
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    this.accountService.updateProfile(this.profileForm.getRawValue() as any).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.savingProfile.set(false);
        this.toastr.success('Profile updated');
      },
      error: () => this.savingProfile.set(false),
    });
  }

  requestEmailChange(): void {
    if (!this.newEmail.trim()) {
      this.toastr.warning('Please enter your new email address.');
      return;
    }
    this.requestingEmailChange.set(true);
    this.accountService.requestEmailChange({ newEmail: this.newEmail }).subscribe({
      next: () => {
        this.emailChangeStep.set('otp-sent');
        this.requestingEmailChange.set(false);
        this.toastr.success('Verification code sent to your new email.');
      },
      error: () => this.requestingEmailChange.set(false),
    });
  }

  verifyEmailChange(): void {
    if (!this.emailOtp.trim()) {
      this.toastr.warning('Please enter the verification code.');
      return;
    }
    this.verifyingEmailChange.set(true);
    this.accountService
      .verifyEmailChange({ newEmail: this.newEmail, otp: this.emailOtp })
      .subscribe({
        next: () => {
          this.toastr.success('Email updated! Please log in again with your new email.');
          this.authService.logout();
          this.router.navigate(['/auth/login']);
        },
        error: () => this.verifyingEmailChange.set(false),
      });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.accountService.changePassword(this.passwordForm.getRawValue() as any).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordForm.reset();
        this.toastr.success('Password changed successfully');
      },
      error: () => this.savingPassword.set(false),
    });
  }

  get pf() {
    return this.profileForm.controls;
  }
  get pwf() {
    return this.passwordForm.controls;
  }
}
