import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Category } from '../../../core/models/category.model';
import { AppNotification } from '../../../core/models/notification.model';
import { ClickOutsideDirective } from '../../directives/click-outside.directive';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, ClickOutsideDirective],
  templateUrl: './header.component.html',
})
export class HeaderComponent implements OnInit {
  categories = signal<Category[]>([]);
  searchTerm = '';
  accountMenuOpen = signal(false);
  mobileMenuOpen = signal(false);
  notificationsOpen = signal(false);

  constructor(
    public authService: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    public notificationService: NotificationService,
    private categoryService: CategoryService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.categoryService.getTree().subscribe((cats) => this.categories.set(cats));
    this.notificationService.refreshUnreadCount();
  }

  onSearch(): void {
    if (!this.searchTerm.trim()) return;
    this.router.navigate(['/products'], { queryParams: { keyword: this.searchTerm } });
    this.mobileMenuOpen.set(false);
  }

  toggleNotifications(): void {
    const opening = !this.notificationsOpen();
    this.notificationsOpen.set(opening);
    this.accountMenuOpen.set(false); // close other dropdown
    if (opening) {
      this.notificationService.loadRecent();
    }
  }

  toggleAccountMenu(): void {
    const opening = !this.accountMenuOpen();
    this.accountMenuOpen.set(opening);
    this.notificationsOpen.set(false); // close other dropdown
  }

  onNotificationClick(notification: AppNotification): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id).subscribe();
    }
    this.notificationsOpen.set(false);
    if (notification.link) {
      this.router.navigateByUrl(notification.link);
    }
  }

  markAllNotificationsRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  logout(): void {
    this.authService.logout();
    this.cartService.reset();
    this.wishlistService.reset();
    this.notificationService.reset();
    this.accountMenuOpen.set(false);
    this.router.navigate(['/']);
  }

  closeAllMenus(): void {
    this.accountMenuOpen.set(false);
    this.notificationsOpen.set(false);
  }
}
