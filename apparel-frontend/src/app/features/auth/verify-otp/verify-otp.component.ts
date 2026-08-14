import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, EMPTY, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verify-otp.component.html',
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  @ViewChildren('digitInput') digitInputs!: QueryList<ElementRef<HTMLInputElement>>;

  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);

  email = '';
  digits = ['', '', '', '', '', ''];
  readonly loading = signal(false);
  readonly resending = signal(false);

  // 30s Cooldown for OTP Resend
  readonly resendCountdown = signal(0);
  private timerInterval?: any;

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    if (!this.email) {
      this.router.navigate(['/auth/register']);
    }
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  onDigitInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '').slice(-1);
    this.digits[index] = value;
    input.value = value;

    if (value && index < this.digits.length - 1) {
      this.digitInputs.get(index + 1)?.nativeElement.focus();
    } else if (this.otpComplete) {
      this.submit();
    }
  }

  onBackspace(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace') {
      if (!this.digits[index] && index > 0) {
        this.digitInputs.get(index - 1)?.nativeElement.focus();
      } else {
        this.digits[index] = '';
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? '';
    if (pasted.length === 6) {
      this.digits = pasted.split('');
      this.digitInputs.last?.nativeElement.focus();
      this.submit();
    }
  }

  get otpComplete(): boolean {
    return this.digits.every((d) => d.trim() !== '');
  }

  submit(): void {
    if (!this.otpComplete || this.loading()) return;

    this.loading.set(true);
    const otp = this.digits.join('');

    this.authService
      .verifyOtp({ email: this.email.trim().toLowerCase(), otp })
      .pipe(
        finalize(() => this.loading.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('Account verified! You can now log in.');
        this.router.navigate(['/auth/login'], { queryParams: { email: this.email } });
      });
  }

  resend(): void {
    if (this.resending() || this.resendCountdown() > 0) return;

    this.resending.set(true);
    this.authService
      .resendOtp({ email: this.email.trim().toLowerCase(), purpose: 'REGISTER' })
      .pipe(
        finalize(() => this.resending.set(false)),
        catchError(() => EMPTY),
      )
      .subscribe(() => {
        this.toastr.success('A new verification code has been sent.');
        this.startResendTimer();
      });
  }

  private startResendTimer(): void {
    this.resendCountdown.set(30);
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      if (this.resendCountdown() > 0) {
        this.resendCountdown.update((v) => v - 1);
      } else {
        clearInterval(this.timerInterval);
      }
    }, 1000);
  }
}
