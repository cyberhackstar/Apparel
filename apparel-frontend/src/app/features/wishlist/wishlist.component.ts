import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { WishlistService } from '../../core/services/wishlist.service';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './wishlist.component.html',
})
export class WishlistComponent implements OnInit {
  constructor(public wishlistService: WishlistService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.wishlistService.loadWishlist();
  }

  remove(productId: number): void {
    this.wishlistService.toggle(productId).subscribe(() => this.toastr.info('Removed from wishlist.'));
  }
}
