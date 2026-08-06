import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../components/header/header.component';
import { FooterComponent } from '../components/footer/footer.component';
import { CartService } from '../../core/services/cart.service';
import { WishlistService } from '../../core/services/wishlist.service';

@Component({
  selector: 'app-storefront-layout',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <app-header />
    <main class="min-h-[60vh]">
      <router-outlet />
    </main>
    <app-footer />
  `,
})
export class StorefrontLayoutComponent implements OnInit {
  constructor(private cartService: CartService, private wishlistService: WishlistService) {}

  ngOnInit(): void {
    this.cartService.loadCart();
    this.wishlistService.loadWishlist();
  }
}
