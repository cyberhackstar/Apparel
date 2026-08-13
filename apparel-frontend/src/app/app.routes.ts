import { Routes } from '@angular/router';
import { StorefrontLayoutComponent } from './shared/layout/storefront-layout.component';
import { AdminLayoutComponent } from './shared/layout/admin-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  // ---------- Storefront ----------
  {
    path: '',
    component: StorefrontLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
        title: 'Ladies Apparel — Ethnic & Western Wear',
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/product/product-listing/product-listing.component').then((m) => m.ProductListingComponent),
        title: 'Shop All Products — Ladies Apparel',
      },
      {
        path: 'products/:slug',
        loadComponent: () =>
          import('./features/product/product-detail/product-detail.component').then((m) => m.ProductDetailComponent),
        title: 'Ladies Apparel',
      },
      {
        path: 'cart',
        loadComponent: () => import('./features/cart/cart.component').then((m) => m.CartComponent),
        title: 'Your Cart — Ladies Apparel',
      },
      {
        path: 'wishlist',
        loadComponent: () => import('./features/wishlist/wishlist.component').then((m) => m.WishlistComponent),
        canActivate: [authGuard],
        title: 'Your Wishlist — Ladies Apparel',
      },
      {
        path: 'checkout',
        loadComponent: () => import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent),
        canActivate: [authGuard],
        title: 'Checkout — Ladies Apparel',
      },
      {
        path: 'order-success/:orderNumber',
        loadComponent: () =>
          import('./features/orders/order-success/order-success.component').then((m) => m.OrderSuccessComponent),
        canActivate: [authGuard],
        title: 'Order Confirmed — Ladies Apparel',
      },
      {
        path: 'orders',
        loadComponent: () => import('./features/orders/order-list/order-list.component').then((m) => m.OrderListComponent),
        canActivate: [authGuard],
        title: 'My Orders — Ladies Apparel',
      },
      {
        path: 'orders/:orderNumber',
        loadComponent: () =>
          import('./features/orders/order-detail/order-detail.component').then((m) => m.OrderDetailComponent),
        canActivate: [authGuard],
        title: 'Order Details — Ladies Apparel',
      },
      {
        path: 'account/addresses',
        loadComponent: () => import('./features/account/addresses/addresses.component').then((m) => m.AddressesComponent),
        canActivate: [authGuard],
        title: 'Your Addresses — Ladies Apparel',
      },
      {
        path: 'account/profile',
        loadComponent: () => import('./features/account/profile/profile.component').then((m) => m.ProfileComponent),
        canActivate: [authGuard],
        title: 'My Profile — Ladies Apparel',
      },
    ],
  },

  // ---------- Admin Panel ----------
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/admin/dashboard/dashboard.component').then((m) => m.AdminDashboardComponent),
        title: 'Admin Dashboard',
      },
      {
        path: 'products',
        loadComponent: () => import('./features/admin/products/products.component').then((m) => m.AdminProductsComponent),
        title: 'Manage Products — Admin',
      },
      {
        path: 'products/new',
        loadComponent: () =>
          import('./features/admin/products/product-form/product-form.component').then((m) => m.AdminProductFormComponent),
        title: 'Add Product — Admin',
      },
      {
        path: 'products/:id/edit',
        loadComponent: () =>
          import('./features/admin/products/product-form/product-form.component').then((m) => m.AdminProductFormComponent),
        title: 'Edit Product — Admin',
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./features/admin/categories/categories.component').then((m) => m.AdminCategoriesComponent),
        title: 'Manage Categories — Admin',
      },
      {
        path: 'orders',
        loadComponent: () => import('./features/admin/orders/orders.component').then((m) => m.AdminOrdersComponent),
        title: 'Manage Orders — Admin',
      },
      {
        path: 'returns',
        loadComponent: () => import('./features/admin/returns/returns.component').then((m) => m.AdminReturnsComponent),
        title: 'Returns & Exchanges — Admin',
      },
      {
        path: 'coupons',
        loadComponent: () => import('./features/admin/coupons/coupons.component').then((m) => m.AdminCouponsComponent),
        title: 'Manage Coupons — Admin',
      },
      {
        path: 'banners',
        loadComponent: () => import('./features/admin/banners/banners.component').then((m) => m.AdminBannersComponent),
        title: 'Manage Banners — Admin',
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./features/admin/customers/customers.component').then((m) => m.AdminCustomersComponent),
        title: 'Manage Customers — Admin',
      },
      {
        path: 'reviews',
        loadComponent: () => import('./features/admin/reviews/reviews.component').then((m) => m.AdminReviewsComponent),
        title: 'Moderate Reviews — Admin',
      },
      {
        path: 'serviceability',
        loadComponent: () =>
          import('./features/admin/serviceability/serviceability.component').then((m) => m.AdminServiceabilityComponent),
        title: 'Delivery Zones — Admin',
      },
      {
        path: 'audit-logs',
        loadComponent: () =>
          import('./features/admin/audit-logs/audit-logs.component').then((m) => m.AdminAuditLogsComponent),
        title: 'Audit Logs — Admin',
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
