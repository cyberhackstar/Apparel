import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, EMPTY } from 'rxjs';
import { Category } from '../../../core/models/category.model';
import { AppNotification } from '../../../core/models/notification.model';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ClickOutsideDirective } from '../../../shared/directives/click-outside.directive';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, ClickOutsideDirective],
  templateUrl: './header.component.html',
})
export class HeaderComponent implements OnInit {
  // Dependency Injection using inject() before field initializations
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  readonly wishlistService = inject(WishlistService);
  readonly notificationService = inject(NotificationService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);

  // State Signals
  readonly categories = signal<Category[]>([]);
  readonly accountMenuOpen = signal(false);
  readonly mobileMenuOpen = signal(false);
  readonly notificationsOpen = signal(false);

  // Form State
  searchTerm = '';

  ngOnInit(): void {
    this.loadCategories();
    this.notificationService.refreshUnreadCount();
  }

  onSearch(): void {
    if (!this.searchTerm.trim()) return;

    this.router.navigate(['/products'], {
      queryParams: { keyword: this.searchTerm.trim() },
    });
    this.mobileMenuOpen.set(false);
  }

  toggleAccountMenu(): void {
    const opening = !this.accountMenuOpen();
    if (opening) {
      this.notificationsOpen.set(false); // Close other dropdowns
    }
    this.accountMenuOpen.set(opening);
  }

  toggleNotifications(): void {
    const opening = !this.notificationsOpen();
    if (opening) {
      this.accountMenuOpen.set(false); // Close other dropdowns
      this.notificationService.loadRecent();
    }
    this.notificationsOpen.set(opening);
  }

  onNotificationClick(notification: AppNotification): void {
    if (!notification.read) {
      this.notificationService
        .markAsRead(notification.id)
        .pipe(catchError(() => EMPTY))
        .subscribe();
    }
    this.notificationsOpen.set(false);

    if (notification.link) {
      this.router.navigateByUrl(notification.link);
    }
  }

  markAllNotificationsRead(): void {
    this.notificationService
      .markAllAsRead()
      .pipe(catchError(() => EMPTY))
      .subscribe();
  }

  logout(): void {
    this.authService.logout();
    this.cartService.reset();
    this.wishlistService.reset();
    this.notificationService.reset();
    this.accountMenuOpen.set(false);
    this.mobileMenuOpen.set(false);
    this.notificationsOpen.set(false);
    this.router.navigate(['/']);
  }

  private loadCategories(): void {
    this.categoryService
      .getTree()
      .pipe(catchError(() => EMPTY))
      .subscribe((cats) => this.categories.set(cats));
  }
}
