import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Address, AddressRequest } from '../models/address.model';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly baseUrl = `${environment.apiUrl}/addresses`;

  constructor(private http: HttpClient) {}

  getMyAddresses(): Observable<Address[]> {
    return this.http.get<ApiResponse<Address[]>>(this.baseUrl).pipe(map((res) => res.data));
  }

  add(request: AddressRequest): Observable<Address> {
    return this.http.post<ApiResponse<Address>>(this.baseUrl, request).pipe(map((res) => res.data));
  }

  update(id: number, request: AddressRequest): Observable<Address> {
    return this.http.put<ApiResponse<Address>>(`${this.baseUrl}/${id}`, request).pipe(map((res) => res.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }

  setDefault(id: number): Observable<Address> {
    return this.http.patch<ApiResponse<Address>>(`${this.baseUrl}/${id}/default`, {}).pipe(map((res) => res.data));
  }
}
