import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ServiceablePincode } from '../models/admin-misc.model';

export interface PincodeRequest {
  pincode: string;
  city?: string;
  state?: string;
  codAvailable: boolean;
  estimatedDeliveryDays: number;
}

@Injectable({ providedIn: 'root' })
export class AdminServiceabilityService {
  private readonly baseUrl = `${environment.apiUrl}/admin/serviceability`;

  constructor(private http: HttpClient) {}

  list(): Observable<ServiceablePincode[]> {
    return this.http.get<ApiResponse<ServiceablePincode[]>>(this.baseUrl).pipe(map((r) => r.data));
  }

  add(request: PincodeRequest): Observable<void> {
    return this.http.post<ApiResponse<null>>(this.baseUrl, request).pipe(map(() => undefined));
  }

  remove(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }
}
