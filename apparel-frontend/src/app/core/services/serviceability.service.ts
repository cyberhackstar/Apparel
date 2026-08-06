import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ServiceabilityResponse } from '../models/serviceability.model';

@Injectable({ providedIn: 'root' })
export class ServiceabilityService {
  private readonly baseUrl = `${environment.apiUrl}/public/serviceability`;

  constructor(private http: HttpClient) {}

  check(pincode: string): Observable<ServiceabilityResponse> {
    return this.http
      .get<ApiResponse<ServiceabilityResponse>>(`${this.baseUrl}/${pincode}`)
      .pipe(map((res) => res.data));
  }
}
