import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { VerifyOtpComponent } from './verify-otp/verify-otp.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: LoginComponent, title: 'Log in — Ladies Apparel' },
  { path: 'register', component: RegisterComponent, title: 'Create account — Ladies Apparel' },
  { path: 'verify-otp', component: VerifyOtpComponent, title: 'Verify email — Ladies Apparel' },
  { path: 'forgot-password', component: ForgotPasswordComponent, title: 'Forgot password — Ladies Apparel' },
  { path: 'reset-password', component: ResetPasswordComponent, title: 'Reset password — Ladies Apparel' },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
