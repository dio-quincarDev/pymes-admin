import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';
import { parseBackendError } from 'src/utils/errors';
import type { BackendError } from 'src/types/error';

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('pymeq_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(new Error(error instanceof Error ? error.message : String(error)));
});

interface AxiosErrorType {
  response?: {
    status?: number;
    data?: BackendError;
  };
  config?: { url?: string };
}

function isAxiosError(error: unknown): error is AxiosErrorType {
  return typeof error === 'object' && error !== null && 'response' in error;
}

// ponytail: refresh queue — prevents N simultaneous refreshes when N requests 401
let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token!);
  });
  failedQueue = [];
}

function clearSession() {
  isRefreshing = false;
  failedQueue = [];
  localStorage.removeItem('pymeq_token');
  localStorage.removeItem('pymeq_refresh_token');
  localStorage.removeItem('pymeq_user');
  window.dispatchEvent(new CustomEvent('auth:401'));
}

const NON_REFRESHABLE_CODES = ['AUTH005', 'AUTH006', 'AUTH007'];

function shouldAttemptRefresh(code: string | undefined): boolean {
  return !code || !NON_REFRESHABLE_CODES.includes(code);
}

async function refreshTokens(): Promise<{ accessToken: string; refreshToken: string } | null> {
  const storedRefreshToken = localStorage.getItem('pymeq_refresh_token');
  if (!storedRefreshToken) return null;
  try {
    // ponytail: use raw axios to bypass interceptor loop
    const res = await axios.post(
      `${api.defaults.baseURL}/auth/refresh`,
      { refreshToken: storedRefreshToken }
    );
    const { accessToken, refreshToken } = res.data?.data ?? {};
    if (!accessToken) return null;
    localStorage.setItem('pymeq_token', accessToken);
    localStorage.setItem('pymeq_refresh_token', refreshToken);
    return { accessToken, refreshToken };
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const parsedError = parseBackendError(error);
    const status = isAxiosError(error) ? error.response?.status : undefined;
    const backendData = isAxiosError(error) ? error.response?.data : undefined;
    const isRefreshEndpoint = isAxiosError(error)
      ? error.config?.url?.includes('/auth/refresh')
      : false;

    if (status === 403 && backendData?.codigo === 'VER001') {
      window.location.href = '#/login?verified=false';
    }

    if (status === 401) {
      // Never retry refresh endpoint itself, nor non-refreshable codes
      if (isRefreshEndpoint || !shouldAttemptRefresh(backendData?.codigo ?? undefined)) {
        clearSession();
        if (backendData?.codigo === 'AUTH005') {
          window.location.href = '#/?reason=session_revoked';
        }
        const customError = new Error(parsedError.message);
        Object.assign(customError, { code: parsedError.code, status: parsedError.status, details: parsedError.details, isBackendError: parsedError.isBackendError });
        return Promise.reject(customError);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        const result = await refreshTokens();
        isRefreshing = false;
        if (result) {
          processQueue(null, result.accessToken);
          error.config!.headers!.Authorization = `Bearer ${result.accessToken}`;
          return api(error.config);
        } else {
          processQueue(new Error('Refresh failed'), null);
          clearSession();
          return Promise.reject(error instanceof Error ? error : new Error(String(error)));
        }
      }

      // Another request is already refreshing — queue this one
      return new Promise((resolve, reject) => {
        failedQueue.push({
          resolve: (token: string) => {
            error.config!.headers!.Authorization = `Bearer ${token}`;
            resolve(api(error.config));
          },
          reject,
        });
      });
    }

    const customError = new Error(parsedError.message);
    Object.assign(customError, {
      code: parsedError.code,
      status: parsedError.status,
      details: parsedError.details,
      isBackendError: parsedError.isBackendError,
    });
    return Promise.reject(customError);
  }
);

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
});

export { api };
