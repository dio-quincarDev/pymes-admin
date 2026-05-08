import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';
import { parseBackendError } from 'src/utils/errors';
import type { BackendError, ApiError } from 'src/types/error';

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

// URL base del API Gateway (Configurar en .env después)
const api = axios.create({ 
  baseURL: process.env.API_URL || 'http://localhost:8080/api/v1' 
});

// Interceptor para añadir el Token en cada petición
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
}

function isAxiosError(error: unknown): error is AxiosErrorType {
  return typeof error === 'object' && error !== null && 'response' in error;
}

// Interceptor para manejar errores globales (401, 403, 500)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const parsedError = parseBackendError(error);
    const status = isAxiosError(error) ? error.response?.status : undefined;
    const backendData = isAxiosError(error) ? error.response?.data : undefined;

    if (status === 401) {
      localStorage.removeItem('pymeq_token');
      localStorage.removeItem('pymeq_refresh_token');
      localStorage.removeItem('pymeq_user');

      if (backendData?.codigo === 'AUTH005') {
        window.location.href = '#/login?reason=session_revoked';
      }
    }

    if (status === 403 && backendData?.codigo === 'VER001') {
      window.location.href = '#/login?verified=false';
    }

    const customError = new Error(parsedError.message);
    Object.assign(customError, {
      code: parsedError.code,
      status: parsedError.status,
      details: parsedError.details,
      isBackendError: parsedError.isBackendError,
    });
    return Promise.reject(customError as ApiError);
  }
);

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
});

export { api };
