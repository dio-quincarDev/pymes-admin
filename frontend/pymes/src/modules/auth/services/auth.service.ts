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
  }
};
