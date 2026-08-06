import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { AppNotification } from '../models/notification.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  unreadCount = signal(0);
  notifications = signal<AppNotification[]>([]);

  constructor(private http: HttpClient, private authService: AuthService) {}

  refreshUnreadCount(): void {
    if (!this.authService.isLoggedIn()) return;
    this.http.get<ApiResponse<number>>(`${this.baseUrl}/unread-count`).subscribe({
      next: (res) => this.unreadCount.set(res.data),
      error: () => {},
    });
  }

  loadRecent(): void {
    if (!this.authService.isLoggedIn()) return;
    this.http
      .get<ApiResponse<PagedResponse<AppNotification>>>(this.baseUrl, { params: { page: 0, pageSize: 10 } })
      .subscribe({
        next: (res) => this.notifications.set(res.data.content),
        error: () => {},
      });
  }

  markAsRead(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/read`, {}).pipe(
      tap(() => {
        this.notifications.update((list) => list.map((n) => (n.id === id ? { ...n, read: true } : n)));
        this.unreadCount.update((c) => Math.max(0, c - 1));
      }),
    );
  }

  markAllAsRead(): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/read-all`, {}).pipe(
      tap(() => {
        this.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
        this.unreadCount.set(0);
      }),
    );
  }

  reset(): void {
    this.unreadCount.set(0);
    this.notifications.set([]);
  }
}
