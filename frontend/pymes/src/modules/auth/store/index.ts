import { defineStore } from 'pinia';
import { api } from 'src/boot/axios';
import { authService } from '../services/auth.service';
import type { User, LoginRequest, RegisterRequest, ApiResponse, AuthResponse, LogoutResponse } from '../types';

const safeParse = <T>(key: string, defaultValue: T): T => {
  const item = localStorage.getItem(key);
  if (!item) return defaultValue;
  try {
    return JSON.parse(item) as T;
  } catch (e) {
    console.error(`Error parsing localStorage key "${key}":`, e);
    localStorage.removeItem(key);
    return defaultValue;
  }
};

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: safeParse<User | null>('pymeq_user', null),
    accessToken: localStorage.getItem('pymeq_token') || null,
    pendingTenant: safeParse<{ name: string; slug: string } | null>('pymeq_pending_tenant', null),
    loading: false,
    error: null as string | null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
  },

  actions: {
    setPendingTenant(name: string, slug: string) {
      this.pendingTenant = { name, slug };
      localStorage.setItem('pymeq_pending_tenant', JSON.stringify(this.pendingTenant));
    },

    clearPendingTenant() {
      this.pendingTenant = null;
      localStorage.removeItem('pymeq_pending_tenant');
    },

    async login(credentials: LoginRequest) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authService.login(credentials);
        const { data } = response.data as ApiResponse<AuthResponse>;
        const user = data.activeTenant
          ? { ...data.user, tenantId: data.activeTenant.id }
          : data.user;
        this.setSession(data.accessToken, data.refreshToken, user);
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
        
        if (authData && authData.accessToken) {
          const user = authData.activeTenant
            ? { ...authData.user, tenantId: authData.activeTenant.id }
            : authData.user;
          this.setSession(authData.accessToken, authData.refreshToken, user);
        }
        return authData;
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Error en el registro';
        this.error = errorMessage;
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async verifyEmail(token: string, email: string) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authService.verifyEmail(token, email);
        const { data: authData } = response.data as ApiResponse<AuthResponse>;
        
        // Auto-login después de verificar
        if (authData && authData.accessToken) {
          const user = authData.activeTenant
            ? { ...authData.user, tenantId: authData.activeTenant.id }
            : authData.user;
          this.setSession(authData.accessToken, authData.refreshToken, user);
        }
        return authData;
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Error en la verificación';
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
        const response = await authService.logout();
        return response.data as ApiResponse<LogoutResponse>;
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

if (typeof window !== 'undefined') {
  window.addEventListener('auth:401', () => useAuthStore().clearSession());
}
