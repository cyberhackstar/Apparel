import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminReturnService } from '../../../core/services/admin-return.service';
import { ReturnRequest } from '../../../core/models/return-request.model';

@Component({
  selector: 'app-admin-returns',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './returns.component.html',
})
export class AdminReturnsComponent implements OnInit {
  returns = signal<ReturnRequest[]>([]);
  loading = signal(true);
  notesDraft: Record<number, string> = {};
  refundDraft: Record<number, number | null> = {};

  constructor(private returnService: AdminReturnService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.returnService.getPending().subscribe({
      next: (res) => {
        this.returns.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  approve(ret: ReturnRequest): void {
    this.returnService.approve(ret.id, { adminNotes: this.notesDraft[ret.id] }).subscribe(() => {
      this.toastr.success('Return approved — awaiting item pickup/receipt.');
      this.fetch();
    });
  }

  reject(ret: ReturnRequest): void {
    this.returnService.reject(ret.id, { adminNotes: this.notesDraft[ret.id] }).subscribe(() => {
      this.toastr.info('Return rejected');
      this.fetch();
    });
  }

  complete(ret: ReturnRequest): void {
    const refundAmount = this.refundDraft[ret.id];
    if (!confirm('Mark as complete and process refund (if applicable)?')) return;
    this.returnService.complete(ret.id, { adminNotes: this.notesDraft[ret.id], refundAmount: refundAmount ?? undefined }).subscribe(() => {
      this.toastr.success('Return completed');
      this.fetch();
    });
  }
}
