import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Category } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly baseUrl = `${environment.apiUrl}/public/categories`;
  private treeCache$?: Observable<Category[]>;

  constructor(private http: HttpClient) {}

  /** Cached — the category tree barely changes and is used on every page (header nav + home). */
  getTree(): Observable<Category[]> {
    if (!this.treeCache$) {
      this.treeCache$ = this.http
        .get<ApiResponse<Category[]>>(this.baseUrl)
        .pipe(map((res) => res.data), shareReplay(1));
    }
    return this.treeCache$;
  }
}
