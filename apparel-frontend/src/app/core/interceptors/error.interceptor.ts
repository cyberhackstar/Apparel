import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Global error handling:
 * - 401 on a non-auth request → try refreshing the access token once, then retry the
 *   original request. If the refresh itself fails, log the user out and send them to /login.
 * - Every other error surfaces as a toast with the backend's own message (ApiResponse.message).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastr = inject(ToastrService);

  const isAuthEndpoint = req.url.includes('/auth/');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthEndpoint && authService.getRefreshToken()) {
        return authService.refreshAccessToken().pipe(
          switchMap(() => {
            const retriedReq = req.clone({
              setHeaders: { Authorization: `Bearer ${authService.getAccessToken()}` },
            });
            return next(retriedReq);
          }),
          catchError((refreshError) => {
            authService.logout();
            router.navigate(['/auth/login']);
            toastr.error('Your session has expired. Please login again.');
            return throwError(() => refreshError);
          }),
        );
      }

      const message = error.error?.message || 'Something went wrong. Please try again.';

      // 400s are validation errors — components render those inline on the form instead of a toast
      if (error.status !== 400) {
        toastr.error(message);
      }

      return throwError(() => error);
    }),
  );
};
