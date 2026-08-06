import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Product } from '../models/product.model';

const STORAGE_KEY = 'la_recently_viewed';
const MAX_ITEMS = 10;

@Injectable({ providedIn: 'root' })
export class RecentlyViewedService {
  private readonly batchUrl = `${environment.apiUrl}/public/products/batch`;

  constructor(private http: HttpClient) {}

  track(productId: number): void {
    if (typeof localStorage === 'undefined') return;

    const ids = this.getIds().filter((id) => id !== productId);
    ids.unshift(productId);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.slice(0, MAX_ITEMS)));
  }

  getIds(): number[] {
    if (typeof localStorage === 'undefined') return [];
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as number[]) : [];
  }

  /** Fetches full product details for the tracked ids, excluding the one currently being viewed. */
  getRecentlyViewed(excludeProductId?: number): Observable<Product[]> {
    const ids = this.getIds().filter((id) => id !== excludeProductId);
    if (ids.length === 0) return of([]);

    return this.http
      .get<ApiResponse<Product[]>>(this.batchUrl, { params: { ids: ids.join(',') } })
      .pipe(map((res) => {
        // preserve most-recently-viewed-first order (the backend doesn't guarantee it)
        const byId = new Map(res.data.map((p) => [p.id, p]));
        return ids.map((id) => byId.get(id)).filter((p): p is Product => !!p);
      }));
  }
}
