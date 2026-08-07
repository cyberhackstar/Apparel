import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  ChangeEmailRequest,
  ChangePasswordRequest,
  Profile,
  UpdateProfileRequest,
  VerifyEmailChangeRequest,
} from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly baseUrl = `${environment.apiUrl}/account`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<Profile> {
    return this.http.get<ApiResponse<Profile>>(`${this.baseUrl}/profile`).pipe(map((r) => r.data));
  }

  updateProfile(request: UpdateProfileRequest): Observable<Profile> {
    return this.http.put<ApiResponse<Profile>>(`${this.baseUrl}/profile`, request).pipe(map((r) => r.data));
  }

  requestEmailChange(request: ChangeEmailRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/change-email/request`, request);
  }

  verifyEmailChange(request: VerifyEmailChangeRequest): Observable<Profile> {
    return this.http
      .post<ApiResponse<Profile>>(`${this.baseUrl}/change-email/verify`, request)
      .pipe(map((r) => r.data));
  }

  changePassword(request: ChangePasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/change-password`, request);
  }
}
