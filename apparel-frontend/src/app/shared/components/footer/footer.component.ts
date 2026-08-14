import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="bg-ink text-ivory mt-24">
      <div class="max-w-7xl mx-auto px-6 py-16 grid grid-cols-2 md:grid-cols-4 gap-10">
        <!-- Brand Description -->
        <div class="col-span-2 space-y-3">
          <p class="font-display text-2xl text-ivory">Ladies Apparel</p>
          <p class="font-body text-sm text-ivory/60 max-w-xs leading-relaxed">
            Thoughtfully designed ethnic and western wear for the modern Indian woman.
          </p>
        </div>

        <!-- Shop Column -->
        <div>
          <p class="eyebrow text-gold mb-3 text-xs tracking-wider uppercase font-semibold">Shop</p>
          <ul class="space-y-2 font-body text-sm text-ivory/70">
            <li>
              <a routerLink="/products" class="hover:text-ivory transition-colors">All Products</a>
            </li>
            <li>
              <a routerLink="/wishlist" class="hover:text-ivory transition-colors">Wishlist</a>
            </li>
            <li>
              <a routerLink="/orders" class="hover:text-ivory transition-colors">Track Order</a>
            </li>
            <li>
              <a routerLink="/about-us" class="hover:text-ivory transition-colors">About Us</a>
            </li>
          </ul>
        </div>

        <!-- Support Column -->
        <div>
          <p class="eyebrow text-gold mb-3 text-xs tracking-wider uppercase font-semibold">
            Support
          </p>
          <ul class="space-y-2 font-body text-sm text-ivory/70">
            <li>
              <a routerLink="/contact-us" class="hover:text-ivory transition-colors">Contact Us</a>
            </li>
            <li>
              <a routerLink="/shipping-and-returns" class="hover:text-ivory transition-colors"
                >Returns &amp; Exchanges</a
              >
            </li>
            <li>
              <a routerLink="/shipping-and-returns" class="hover:text-ivory transition-colors"
                >Shipping Info</a
              >
            </li>
            <li>
              <a routerLink="/privacy-policy" class="hover:text-ivory transition-colors"
                >Privacy Policy</a
              >
            </li>
            <li>
              <a routerLink="/terms-and-conditions" class="hover:text-ivory transition-colors"
                >Terms &amp; Conditions</a
              >
            </li>
          </ul>
        </div>
      </div>

      <!-- Copyright Bottom Bar -->
      <div class="border-t border-ivory/10 py-6 text-center font-body text-xs text-ivory/40">
        &copy; 2026 Ladies Apparel. All rights reserved.
      </div>
    </footer>
  `,
})
export class FooterComponent {}
