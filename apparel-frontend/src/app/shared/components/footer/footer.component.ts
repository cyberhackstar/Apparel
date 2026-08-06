import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="bg-ink text-ivory mt-24">
      <div class="max-w-7xl mx-auto px-6 py-16 grid grid-cols-2 md:grid-cols-4 gap-10">
        <div class="col-span-2">
          <p class="font-display text-2xl mb-3">Ladies Apparel</p>
          <p class="font-body text-sm text-ivory/60 max-w-xs">
            Thoughtfully designed ethnic and western wear for the modern Indian woman.
          </p>
        </div>
        <div>
          <p class="eyebrow text-gold mb-3">Shop</p>
          <ul class="space-y-2 font-body text-sm text-ivory/70">
            <li><a routerLink="/products" class="hover:text-ivory">All Products</a></li>
            <li><a routerLink="/wishlist" class="hover:text-ivory">Wishlist</a></li>
            <li><a routerLink="/orders" class="hover:text-ivory">Track Order</a></li>
          </ul>
        </div>
        <div>
          <p class="eyebrow text-gold mb-3">Support</p>
          <ul class="space-y-2 font-body text-sm text-ivory/70">
            <li><a href="mailto:support@ladiesapparel.com" class="hover:text-ivory">Contact Us</a></li>
            <li><a routerLink="/returns" class="hover:text-ivory">Returns &amp; Exchanges</a></li>
            <li><a routerLink="/shipping" class="hover:text-ivory">Shipping Info</a></li>
          </ul>
        </div>
      </div>
      <div class="border-t border-ivory/10 py-6 text-center font-body text-xs text-ivory/40">
        &copy; 2026 Ladies Apparel. All rights reserved.
      </div>
    </footer>
  `,
})
export class FooterComponent {}
