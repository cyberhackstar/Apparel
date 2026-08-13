import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AddressService } from '../../../core/services/address.service';
import { Address, AddressType } from '../../../core/models/address.model';

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './addresses.component.html',
})
export class AddressesComponent implements OnInit {
  /** When used inside checkout, hides the page title and emits the selected address instead of just managing the list. */
  @Input() selectionMode = false;
  @Input() selectedAddressId: number | null = null;
  @Output() addressSelected = new EventEmitter<Address>();

  private fb = inject(FormBuilder);
  private addressService = inject(AddressService);
  private toastr = inject(ToastrService);

  addresses = signal<Address[]>([]);
  loading = signal(true);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  saving = signal(false);

  form = this.fb.group({
    fullName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    landmark: [''],
    city: ['', Validators.required],
    state: ['', Validators.required],
    pincode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    addressType: ['HOME' as AddressType, Validators.required],
    isDefault: [false],
  });

  

  ngOnInit(): void {
    this.fetchAddresses();
  }

  fetchAddresses(): void {
    this.loading.set(true);
    this.addressService.getMyAddresses().subscribe({
      next: (addresses) => {
        this.addresses.set(addresses);
        this.loading.set(false);
        // auto-select the default address when used inside checkout
        if (this.selectionMode && !this.selectedAddressId) {
          const def = addresses.find((a) => a.isDefault) ?? addresses[0];
          if (def) this.select(def);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({ addressType: 'HOME', isDefault: this.addresses().length === 0 });
    this.showForm.set(true);
  }

  openEditForm(address: Address): void {
    this.editingId.set(address.id);
    this.form.patchValue(address);
    this.showForm.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const request = this.form.getRawValue() as any;
    const editId = this.editingId();

    const obs = editId
      ? this.addressService.update(editId, request)
      : this.addressService.add(request);
    obs.subscribe({
      next: (address) => {
        this.toastr.success(editId ? 'Address updated' : 'Address added');
        this.showForm.set(false);
        this.saving.set(false);
        this.fetchAddresses();
        if (this.selectionMode) this.select(address);
      },
      error: () => this.saving.set(false),
    });
  }

  delete(id: number): void {
    this.addressService.delete(id).subscribe(() => {
      this.toastr.info('Address deleted');
      this.fetchAddresses();
    });
  }

  setDefault(id: number): void {
    this.addressService.setDefault(id).subscribe(() => {
      this.toastr.success('Default address updated');
      this.fetchAddresses();
    });
  }

  select(address: Address): void {
    this.selectedAddressId = address.id;
    this.addressSelected.emit(address);
  }

  get f() {
    return this.form.controls;
  }
}
