import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // ── Dynamic routes (have URL params) → Client Side Rendering ──
  { path: 'products/:slug', renderMode: RenderMode.Client },
  { path: 'order-success/:orderNumber', renderMode: RenderMode.Client },
  { path: 'orders/:orderNumber', renderMode: RenderMode.Client },
  { path: 'admin/products/:id/edit', renderMode: RenderMode.Client },

  // ── Auth-protected routes → Client Side Rendering ──
  { path: 'wishlist', renderMode: RenderMode.Client },
  { path: 'checkout', renderMode: RenderMode.Client },
  { path: 'orders', renderMode: RenderMode.Client },
  { path: 'account/addresses', renderMode: RenderMode.Client },

  // ── Admin routes → Client Side Rendering ──
  { path: 'admin', renderMode: RenderMode.Client },
  { path: 'admin/products', renderMode: RenderMode.Client },
  { path: 'admin/products/new', renderMode: RenderMode.Client },
  { path: 'admin/categories', renderMode: RenderMode.Client },
  { path: 'admin/orders', renderMode: RenderMode.Client },
  { path: 'admin/returns', renderMode: RenderMode.Client },
  { path: 'admin/coupons', renderMode: RenderMode.Client },
  { path: 'admin/banners', renderMode: RenderMode.Client },
  { path: 'admin/customers', renderMode: RenderMode.Client },
  { path: 'admin/reviews', renderMode: RenderMode.Client },
  { path: 'admin/serviceability', renderMode: RenderMode.Client },
  { path: 'admin/audit-logs', renderMode: RenderMode.Client },

  // ── Public static routes → Prerender at build time ──
  { path: '', renderMode: RenderMode.Prerender },
  { path: 'products', renderMode: RenderMode.Prerender },
  { path: 'cart', renderMode: RenderMode.Prerender },
  { path: 'auth', renderMode: RenderMode.Client },

  // ── Fallback ──
  { path: '**', renderMode: RenderMode.Client },
];
