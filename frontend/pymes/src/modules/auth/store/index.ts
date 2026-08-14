import { defineStore } from 'pinia';
import { api } from 'src/boot/axios';
import { authService } from '../services/auth.service';
import { tenantService } from '../services/tenant.service';
import type { User, LoginRequest, RegisterRequest, ApiResponse, AuthResponse, LogoutResponse } from '../types';
import type { PageResponse } from 'src/modules/core/types';

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
    tenantName: localStorage.getItem('pymeq_tenant_name') || null,
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
        this.setSession(data.accessToken, data.refreshToken, user, data.activeTenant?.name);
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
          this.setSession(authData.accessToken, authData.refreshToken, user, authData.activeTenant?.name);
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
          this.setSession(authData.accessToken, authData.refreshToken, user, authData.activeTenant?.name);
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

    async handleOAuthCallback(token: string, refreshToken: string, tenantName?: string) {
      this.accessToken = token;
      this.tenantName = tenantName ?? null;
      localStorage.setItem('pymeq_token', token);
      localStorage.setItem('pymeq_refresh_token', refreshToken);
      if (tenantName) localStorage.setItem('pymeq_tenant_name', tenantName);
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

    async selectTenant(tenantId: string) {
      this.loading = true;
      this.error = null;
      try {
        const response = await tenantService.selectTenant(tenantId);
        const { data } = response.data as ApiResponse<AuthResponse>;
        const user = data.activeTenant
          ? { ...data.user, tenantId: data.activeTenant.id }
          : data.user;
        this.setSession(data.accessToken, data.refreshToken, user, data.activeTenant?.name);
        return data;
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Error al seleccionar tenant';
        this.error = errorMessage;
        throw err;
      } finally {
        this.loading = false;
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

    setSession(token: string, refreshToken: string, user: User, tenantName?: string) {
      this.accessToken = token;
      this.user = user;
      this.tenantName = tenantName ?? null;
      localStorage.setItem('pymeq_token', token);
      localStorage.setItem('pymeq_refresh_token', refreshToken);
      localStorage.setItem('pymeq_user', JSON.stringify(user));
      if (tenantName) localStorage.setItem('pymeq_tenant_name', tenantName);
      else localStorage.removeItem('pymeq_tenant_name');
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    },

    async ensureTenantName() {
      const tid = this.user?.tenantId;
      if (!tid || this.tenantName) return;
      try {
        const response = await tenantService.getUserTenants(0, 50);
        const page = response.data?.data as PageResponse<{ tenantId: string; tenantName: string }>;
        const match = page?.content?.find(t => t.tenantId === tid);
        if (match?.tenantName) {
          this.tenantName = match.tenantName;
          localStorage.setItem('pymeq_tenant_name', match.tenantName);
        }
      } catch { /* silent — non-critical */ }
    },

    clearSession() {
      this.user = null;
      this.accessToken = null;
      this.tenantName = null;
      localStorage.removeItem('pymeq_token');
      localStorage.removeItem('pymeq_refresh_token');
      localStorage.removeItem('pymeq_user');
      localStorage.removeItem('pymeq_tenant_name');
      delete api.defaults.headers.common['Authorization'];
      window.location.href = '#/login';
    }
  },
});

if (typeof window !== 'undefined') {
  window.addEventListener('auth:401', () => useAuthStore().clearSession());
}
