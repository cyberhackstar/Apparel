import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Mutex & Queue for handling concurrent 401 calls during token refresh
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastr = inject(ToastrService);

  const isAuthEndpoint = req.url.includes('/auth/');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 1. Check if the error is 401 Unauthorized on a protected endpoint
      if (error.status === 401 && !isAuthEndpoint) {
        const refreshToken = authService.getRefreshToken();

        if (refreshToken) {
          return handle401Error(req, next, authService, router, toastr);
        } else {
          // No refresh token available -> clean logout without generic error
          authService.logout();
          router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
          return throwError(() => error);
        }
      }

      // 2. Handle expired refresh token attempt specifically
      if (req.url.includes('/auth/refresh-token')) {
        authService.logout();
        router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
        toastr.info('Your session has expired. Please log in again.');
        return throwError(() => error);
      }

      // 3. Normal API Errors
      const message = error.error?.message || 'Unable to complete request. Please try again.';

      // Skip 400 (inline form errors) and 401 (handled above)
      if (error.status !== 400 && error.status !== 401) {
        toastr.error(message);
      }

      return throwError(() => error);
    }),
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router,
  toastr: ToastrService,
): Observable<any> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshAccessToken().pipe(
      switchMap((res: any) => {
        isRefreshing = false;
        const newAccessToken = authService.getAccessToken();
        refreshTokenSubject.next(newAccessToken);

        return next(
          req.clone({
            setHeaders: { Authorization: `Bearer ${newAccessToken}` },
          }),
        );
      }),
      catchError((refreshErr) => {
        isRefreshing = false;
        refreshTokenSubject.next(null);

        authService.logout();
        router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
        toastr.info('Your session has expired. Please log in again.');

        return throwError(() => refreshErr);
      }),
    );
  } else {
    // Queue secondary requests until the main refresh call completes
    return refreshTokenSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) =>
        next(
          req.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
          }),
        ),
      ),
    );
  }
}
