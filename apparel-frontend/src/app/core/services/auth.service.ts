import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  ResendOtpRequest,
  ResetPasswordRequest,
  VerifyOtpRequest,
} from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'la_access_token';
const REFRESH_TOKEN_KEY = 'la_refresh_token';
const USER_KEY = 'la_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /** Signal-based auth state — read this from components via `authService.currentUser()` */
  currentUser = signal<AuthResponse | null>(this.loadUserFromStorage());
  isLoggedIn = computed(() => this.currentUser() !== null);
  isAdmin = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'ADMIN' || role === 'SUPER_ADMIN';
  });

  register(request: RegisterRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/register`, request);
  }

  verifyOtp(request: VerifyOtpRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/verify-otp`, request);
  }

  resendOtp(request: ResendOtpRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/resend-otp`, request);
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, request)
      .pipe(tap((res) => this.persistSession(res.data)));
  }

  refreshAccessToken(): Observable<ApiResponse<AuthResponse>> {
    const request: RefreshTokenRequest = { refreshToken: this.getRefreshToken() ?? '' };
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.baseUrl}/refresh-token`, request)
      .pipe(tap((res) => this.persistSession(res.data)));
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/reset-password`, request);
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
    this.currentUser.set(null);
  }

  getAccessToken(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private persistSession(auth: AuthResponse): void {
    if (this.isBrowser) {
      localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
      localStorage.setItem(USER_KEY, JSON.stringify(auth));
    }
    this.currentUser.set(auth);
  }

  private loadUserFromStorage(): AuthResponse | null {
    if (!this.isBrowser) return null;
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  }
}
