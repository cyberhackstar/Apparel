import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';

declare global {
  interface Window {
    google: any;
  }
}

const GSI_SCRIPT_URL = 'https://accounts.google.com/gsi/client';

@Component({
  selector: 'app-google-signin-button',
  standalone: true,
  template: `
    <div class="relative">
      <div #googleBtn class="flex justify-center"></div>
      @if (loading) {
        <div class="absolute inset-0 flex items-center justify-center bg-ivory/70 font-body text-sm text-ink/50">
          Signing in…
        </div>
      }
    </div>
  `,
})
export class GoogleSignInButtonComponent implements OnInit {
  @ViewChild('googleBtn', { static: true }) buttonRef!: ElementRef<HTMLDivElement>;
  @Output() success = new EventEmitter<void>();

  loading = false;

  constructor(private authService: AuthService, private router: Router, private toastr: ToastrService) {}

  ngOnInit(): void {
    if (!environment.googleClientId || environment.googleClientId.includes('your_google')) {
      // not configured — silently skip rendering rather than showing a broken button
      return;
    }
    this.loadScript().then(() => this.initializeButton());
  }

  private loadScript(): Promise<void> {
    if (window.google?.accounts?.id) return Promise.resolve();

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = GSI_SCRIPT_URL;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Failed to load Google Sign-In script'));
      document.head.appendChild(script);
    });
  }

  private initializeButton(): void {
    window.google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: { credential: string }) => this.handleCredential(response.credential),
    });

    window.google.accounts.id.renderButton(this.buttonRef.nativeElement, {
      theme: 'outline',
      size: 'large',
      width: 360,
      text: 'continue_with',
    });
  }

  private handleCredential(idToken: string): void {
    this.loading = true;
    this.authService.loginWithGoogle({ idToken }).subscribe({
      next: (res) => {
        this.loading = false;
        const redirectTo = res.data.role === 'ADMIN' || res.data.role === 'SUPER_ADMIN' ? '/admin' : '/';
        this.success.emit();
        this.router.navigate([redirectTo]);
      },
      error: () => (this.loading = false),
    });
  }
}
