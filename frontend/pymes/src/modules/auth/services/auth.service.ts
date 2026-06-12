import { api } from 'src/boot/axios';
import type { LoginRequest, RegisterRequest } from '../types';

export const authService = {
  login(credentials: LoginRequest) {
    return api.post('/auth/login', credentials);
  },
  
  register(data: RegisterRequest) {
    return api.post('/auth/register', data);
  },
  
  logout() {
    return api.post('/auth/logout');
  },
  
  fetchMe() {
    return api.get('/auth/me');
  },
  
  refreshToken(token: string) {
    return api.post('/auth/refresh', { refreshToken: token });
  },

  verifyEmail(token: string, email: string) {
    return api.post('/auth/verify-email', { token, email });
  },

  resendVerification(email: string) {
    return api.post('/auth/resend-verification', { email });
  },

  createOAuth2Intent(data: { companyName: string; companySlug: string }) {
    return api.post('/auth/oauth2/intent', data);
  },

  forgotPassword(email: string) {
    return api.post('/auth/forgot-password', { email });
  },

  resetPassword(data: { token: string; newPassword: string }) {
    return api.post('/auth/reset-password', data);
  },

  acceptInvitation(invitationToken: string) {
    return api.post('/invitations/accept', { invitationToken });
  }
};
