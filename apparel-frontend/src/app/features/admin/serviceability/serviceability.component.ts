import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminServiceabilityService } from '../../../core/services/admin-serviceability.service';
import { ServiceablePincode } from '../../../core/models/admin-misc.model';

@Component({
  selector: 'app-admin-serviceability',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './serviceability.component.html',
})
export class AdminServiceabilityComponent implements OnInit {
  pincodes = signal<ServiceablePincode[]>([]);
  loading = signal(true);
  saving = signal(false);

  newPincode = '';
  newCity = '';
  newState = '';
  newCodAvailable = true;
  newDeliveryDays = 5;

  constructor(private serviceabilityService: AdminServiceabilityService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.serviceabilityService.list().subscribe({
      next: (pincodes) => {
        this.pincodes.set(pincodes);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  add(): void {
    if (!/^\d{6}$/.test(this.newPincode)) {
      this.toastr.warning('Please enter a valid 6-digit pincode.');
      return;
    }
    this.saving.set(true);
    this.serviceabilityService
      .add({
        pincode: this.newPincode,
        city: this.newCity,
        state: this.newState,
        codAvailable: this.newCodAvailable,
        estimatedDeliveryDays: this.newDeliveryDays,
      })
      .subscribe({
        next: () => {
          this.toastr.success('Pincode added');
          this.newPincode = '';
          this.newCity = '';
          this.newState = '';
          this.saving.set(false);
          this.fetch();
        },
        error: () => this.saving.set(false),
      });
  }

  remove(id: number): void {
    this.serviceabilityService.remove(id).subscribe(() => {
      this.toastr.info('Pincode removed');
      this.fetch();
    });
  }
}
