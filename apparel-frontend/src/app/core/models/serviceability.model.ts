export interface ServiceabilityResponse {
  pincode: string;
  serviceable: boolean;
  codAvailable: boolean;
  estimatedDeliveryDays?: number;
  city?: string;
  state?: string;
}
