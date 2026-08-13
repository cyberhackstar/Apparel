import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  exact: boolean;
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Dashboard',
    path: '/admin',
    icon: 'M3 3h8v8H3zM13 3h8v5h-8zM13 11h8v10h-8zM3 14h8v7H3z',
    exact: true,
  },
  {
    label: 'Products',
    path: '/admin/products',
    icon: 'M20 7L12 3 4 7m16 0v10l-8 4m8-14l-8 4m0 10l-8-4V7m8 10V11m0 0L4 7',
    exact: false,
  },
  { label: 'Categories', path: '/admin/categories', icon: 'M4 6h16M4 12h16M4 18h7', exact: false },
  { label: 'Orders', path: '/admin/orders', icon: 'M6 6h15l-1.5 9h-12zM6 6L5 3H2', exact: false },
  {
    label: 'Returns',
    path: '/admin/returns',
    icon: 'M3 10h10a4 4 0 014 4v0a4 4 0 01-4 4H7m-4-8l4-4m-4 4l4 4',
    exact: false,
  },
  {
    label: 'Coupons',
    path: '/admin/coupons',
    icon: 'M4 12a2 2 0 012-2h12a2 2 0 012 2v0a2 2 0 01-2 2H6a2 2 0 01-2-2z',
    exact: false,
  },
  { label: 'Banners', path: '/admin/banners', icon: 'M3 5h18v14H3zM3 9h18', exact: false },
  {
    label: 'Customers',
    path: '/admin/customers',
    icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8z',
    exact: false,
  },
  {
    label: 'Reviews',
    path: '/admin/reviews',
    icon: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z',
    exact: false,
  },
  {
    label: 'Delivery Zones',
    path: '/admin/serviceability',
    icon: 'M12 21s-6.716-4.35-9.428-8.485C.61 9.35 1.5 5 5.5 4 8 3.35 10 4.5 12 7c2-2.5 4-3.65 6.5-3 4 1 4.89 5.35 2.928 8.515C18.716 16.65 12 21 12 21z',
    exact: false,
  },
  {
    label: 'Audit Logs',
    path: '/admin/audit-logs',
    icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    exact: false,
  },
];

const MOBILE_TAB_ITEMS: NavItem[] = [NAV_ITEMS[0], NAV_ITEMS[1], NAV_ITEMS[3], NAV_ITEMS[4]];

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems = NAV_ITEMS;
  readonly mobileTabItems = MOBILE_TAB_ITEMS;
  readonly drawerOpen = signal(false);

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.drawerOpen.set(false));
  }

  toggleDrawer(): void {
    this.drawerOpen.update((v) => !v);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
