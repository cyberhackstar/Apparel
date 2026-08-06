import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { AdminAuditLogService } from '../../../core/services/admin-audit-log.service';
import { AuditLog } from '../../../core/models/admin-misc.model';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-logs.component.html',
})
export class AdminAuditLogsComponent implements OnInit {
  logs = signal<AuditLog[]>([]);
  loading = signal(true);
  page = signal(0);
  totalPages = signal(0);

  constructor(private auditLogService: AdminAuditLogService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.auditLogService.list(this.page()).subscribe({
      next: (res) => {
        this.logs.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.fetch();
  }

  methodColor(method: string): string {
    switch (method) {
      case 'POST': return 'bg-green-50 text-green-700';
      case 'DELETE': return 'bg-blush text-wine';
      case 'PUT': case 'PATCH': return 'bg-ink/5 text-ink/70';
      default: return 'bg-ink/5 text-ink/70';
    }
  }
}
