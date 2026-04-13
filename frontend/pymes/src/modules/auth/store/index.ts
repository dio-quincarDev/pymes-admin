import { defineStore } from 'pinia';
import { api } from 'src/boot/axios';
import { authService } from '../services/auth.service';
import type { User, LoginRequest, RegisterRequest, ApiResponse, AuthResponse } from '../types';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('pymeq_user') || 'null') as User | null,
    accessToken: localStorage.getItem('pymeq_token') || null,
    loading: false,
    error: null as string | null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
  },

  actions: {
    async login(credentials: LoginRequest) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authService.login(credentials);
        const { data } = response.data as ApiResponse<AuthResponse>;
        this.setSession(data.accessToken, data.refreshToken, data.user);
        return data;
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Error en la autenticación';
        this.error = errorMessage;
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async register(data: RegisterRequest) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authService.register(data);
        const { data: authData } = response.data as ApiResponse<AuthResponse>;
        this.setSession(authData.accessToken, authData.refreshToken, authData.user);
        return authData;
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Error en el registro';
        this.error = errorMessage;
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async handleOAuthCallback(token: string, refreshToken: string) {
      this.accessToken = token;
      localStorage.setItem('pymeq_token', token);
      localStorage.setItem('pymeq_refresh_token', refreshToken);
      await this.fetchCurrentUser();
    },

    async fetchCurrentUser() {
      try {
        const response = await authService.fetchMe();
        const { data } = response.data as ApiResponse<User>;
        this.user = data;
        localStorage.setItem('pymeq_user', JSON.stringify(this.user));
      } catch {
        this.clearSession();
      }
    },

    async logout() {
      try {
        await authService.logout();
      } finally {
        this.clearSession();
      }
    },

    setSession(token: string, refreshToken: string, user: User) {
      this.accessToken = token;
      this.user = user;
      localStorage.setItem('pymeq_token', token);
      localStorage.setItem('pymeq_refresh_token', refreshToken);
      localStorage.setItem('pymeq_user', JSON.stringify(user));
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    },

    clearSession() {
      this.user = null;
      this.accessToken = null;
      localStorage.removeItem('pymeq_token');
      localStorage.removeItem('pymeq_refresh_token');
      localStorage.removeItem('pymeq_user');
      delete api.defaults.headers.common['Authorization'];
      window.location.href = '#/login';
    }
  },
});
