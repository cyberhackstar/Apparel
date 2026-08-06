import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminBannerService } from '../../../core/services/admin-banner.service';
import { Banner } from '../../../core/models/banner.model';

@Component({
  selector: 'app-admin-banners',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banners.component.html',
})
export class AdminBannersComponent implements OnInit {
  banners = signal<Banner[]>([]);
  loading = signal(true);
  showForm = signal(false);
  saving = signal(false);

  newTitle = '';
  newLinkUrl = '';
  newDisplayOrder = 0;
  selectedFile: File | null = null;

  constructor(private bannerService: AdminBannerService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.bannerService.list().subscribe({
      next: (banners) => {
        this.banners.set(banners);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onFileSelected(event: Event): void {
    this.selectedFile = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  save(): void {
    if (!this.newTitle.trim() || !this.selectedFile) {
      this.toastr.warning('Title and image are both required.');
      return;
    }
    this.saving.set(true);
    this.bannerService.create(this.newTitle, this.newLinkUrl, this.newDisplayOrder, this.selectedFile).subscribe({
      next: () => {
        this.toastr.success('Banner created');
        this.showForm.set(false);
        this.saving.set(false);
        this.newTitle = '';
        this.newLinkUrl = '';
        this.newDisplayOrder = 0;
        this.selectedFile = null;
        this.fetch();
      },
      error: () => this.saving.set(false),
    });
  }

  toggleActive(banner: Banner): void {
    this.bannerService.update(banner.id, banner.title, banner.linkUrl ?? '', banner.displayOrder, !banner.active).subscribe(() => {
      this.toastr.success(banner.active ? 'Banner deactivated' : 'Banner activated');
      this.fetch();
    });
  }

  delete(id: number): void {
    if (!confirm('Delete this banner permanently?')) return;
    this.bannerService.delete(id).subscribe(() => {
      this.toastr.info('Banner deleted');
      this.fetch();
    });
  }
}
