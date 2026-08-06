import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // =========================
  // PRERENDER (Static Landing)
  // =========================
  {
    path: '',
    renderMode: RenderMode.Prerender,
  },

  // =========================
  // SSR (Dynamic & SEO Pages)
  // =========================
  {
    path: 'products',
    renderMode: RenderMode.Server,
  },
  {
    path: 'products/:slug',
    renderMode: RenderMode.Server,
  },

  // =========================
  // CLIENT ONLY (Auth & User State)
  // =========================
  {
    path: 'auth/**',
    renderMode: RenderMode.Client,
  },
  {
    path: 'cart',
    renderMode: RenderMode.Client,
  },
  {
    path: 'wishlist',
    renderMode: RenderMode.Client,
  },
  {
    path: 'checkout',
    renderMode: RenderMode.Client,
  },
  {
    path: 'order-success/:orderNumber',
    renderMode: RenderMode.Client,
  },
  {
    path: 'orders',
    renderMode: RenderMode.Client,
  },
  {
    path: 'orders/:orderNumber',
    renderMode: RenderMode.Client,
  },
  {
    path: 'account/**',
    renderMode: RenderMode.Client,
  },

  // =========================
  // CLIENT ONLY (Admin Panel)
  // =========================
  {
    path: 'admin/**',
    renderMode: RenderMode.Client,
  },

  // =========================
  // FALLBACK
  // =========================
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
