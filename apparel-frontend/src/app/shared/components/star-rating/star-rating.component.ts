import { CommonModule } from '@angular/common';
import { Component, Input, computed, signal } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-1" [attr.aria-label]="rating() + ' out of 5 stars'">
      @for (i of [1, 2, 3, 4, 5]; track i) {
        <svg
          [attr.width]="size"
          [attr.height]="size"
          viewBox="0 0 24 24"
          [class]="i <= Math.round(rating()) ? 'fill-gold' : 'fill-ink/15'"
        >
          <path
            d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
          />
        </svg>
      }
      @if (showCount && count() > 0) {
        <span class="text-xs text-ink/50 ml-1 font-body">({{ count() }})</span>
      }
    </div>
  `,
})
export class StarRatingComponent {
  @Input() set value(v: number) {
    this.rating.set(v ?? 0);
  }
  @Input() set reviewCount(v: number) {
    this.count.set(v ?? 0);
  }
  @Input() size = 16;
  @Input() showCount = true;

  rating = signal(0);
  count = signal(0);
  Math = Math;
}
