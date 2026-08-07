export interface Profile {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  createdAt: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  phone: string;
}

export interface ChangeEmailRequest {
  newEmail: string;
}

export interface VerifyEmailChangeRequest {
  newEmail: string;
  otp: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
