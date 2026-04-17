import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';

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

// Interceptor para manejar errores globales (401, 403, 500)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Limpiar sesión y redirigir al login si el token es inválido/expirado
      localStorage.removeItem('pymeq_token');
      localStorage.removeItem('pymeq_refresh_token');
      localStorage.removeItem('pymeq_user');
      // window.location.href = '#/login';
    }
    return Promise.reject(new Error(error instanceof Error ? error.message : String(error)));
  }
);

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
});

export { api };
