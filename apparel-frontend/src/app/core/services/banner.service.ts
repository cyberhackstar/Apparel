import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Banner } from '../models/banner.model';

@Injectable({ providedIn: 'root' })
export class BannerService {
  private readonly baseUrl = `${environment.apiUrl}/public/banners`;

  constructor(private http: HttpClient) {}

  getActiveBanners(): Observable<Banner[]> {
    return this.http.get<ApiResponse<Banner[]>>(this.baseUrl).pipe(map((res) => res.data));
  }
}
