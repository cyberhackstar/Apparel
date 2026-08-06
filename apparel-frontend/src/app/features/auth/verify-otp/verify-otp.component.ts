import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, QueryList, ViewChildren, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verify-otp.component.html',
})
export class VerifyOtpComponent implements OnInit {
  @ViewChildren('digitInput') digitInputs!: QueryList<ElementRef<HTMLInputElement>>;

  email = '';
  digits = ['', '', '', '', '', ''];
  loading = signal(false);
  resending = signal(false);

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    if (!this.email) {
      this.router.navigate(['/auth/register']);
    }
  }

  onDigitInput(index: number, event: Event): void {
    const value = (event.target as HTMLInputElement).value.replace(/\D/g, '').slice(-1);
    this.digits[index] = value;

    if (value && index < this.digits.length - 1) {
      this.digitInputs.get(index + 1)?.nativeElement.focus();
    }
  }

  onBackspace(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.digits[index] && index > 0) {
      this.digitInputs.get(index - 1)?.nativeElement.focus();
    }
  }

  onPaste(event: ClipboardEvent): void {
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? '';
    if (pasted.length === 6) {
      this.digits = pasted.split('');
      event.preventDefault();
    }
  }

  get otpComplete(): boolean {
    return this.digits.every((d) => d !== '');
  }

  submit(): void {
    if (!this.otpComplete) return;

    this.loading.set(true);
    this.authService.verifyOtp({ email: this.email, otp: this.digits.join('') }).subscribe({
      next: () => {
        this.toastr.success('Account verified! You can now log in.');
        this.router.navigate(['/auth/login'], { queryParams: { email: this.email } });
      },
      error: () => this.loading.set(false),
    });
  }

  resend(): void {
    this.resending.set(true);
    this.authService.resendOtp({ email: this.email, purpose: 'REGISTER' }).subscribe({
      next: () => {
        this.toastr.success('A new code has been sent to your email.');
        this.resending.set(false);
      },
      error: () => this.resending.set(false),
    });
  }
}
