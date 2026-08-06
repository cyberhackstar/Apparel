import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminReviewService } from '../../../core/services/admin-review.service';
import { Review } from '../../../core/models/review.model';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';

@Component({
  selector: 'app-admin-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, StarRatingComponent],
  templateUrl: './reviews.component.html',
})
export class AdminReviewsComponent implements OnInit {
  reviews = signal<Review[]>([]);
  loading = signal(true);
  replyDrafts: Record<number, string> = {};

  constructor(private reviewService: AdminReviewService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.reviewService.getPending().subscribe({
      next: (res) => {
        this.reviews.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  moderate(review: Review, status: 'APPROVED' | 'REJECTED'): void {
    this.reviewService.moderate(review.id, status).subscribe(() => {
      this.toastr.success(`Review ${status.toLowerCase()}`);
      this.reviews.update((list) => list.filter((r) => r.id !== review.id));
    });
  }

  sendReply(review: Review): void {
    const reply = this.replyDrafts[review.id];
    if (!reply?.trim()) return;
    this.reviewService.reply(review.id, reply).subscribe(() => {
      this.toastr.success('Reply added');
      delete this.replyDrafts[review.id];
    });
  }
}
