import type { ApiError, BackendError } from '../types/error';

interface AxiosErrorResponse {
  data?: BackendError;
  status?: number;
}

interface AxiosError {
  response?: AxiosErrorResponse;
  message?: string;
  code?: string;
}

export function parseBackendError(error: unknown): {
  code: string | undefined;
  message: string;
  details: Record<string, string> | undefined;
  status: number | undefined;
  isBackendError: boolean;
} {
  if (typeof error !== 'object' || error === null) {
    return {
      code: undefined,
      message: 'Error de conexión',
      details: undefined,
      status: undefined,
      isBackendError: false,
    };
  }

  const axiosError = error as AxiosError & { response?: AxiosErrorResponse };

  if (axiosError.response?.data) {
    const backendError = axiosError.response.data;
    return {
      code: backendError.codigo ?? undefined,
      message: backendError.mensaje || getDefaultMessage(axiosError.response.status),
      details: backendError.detalles ?? undefined,
      status: axiosError.response.status ?? undefined,
      isBackendError: true,
    };
  }

  if (!axiosError.response && axiosError.message) {
    return {
      code: undefined,
      message: axiosError.message,
      details: undefined,
      status: undefined,
      isBackendError: false,
    };
  }

  return {
    code: undefined,
    message: 'Error de conexión',
    details: undefined,
    status: undefined,
    isBackendError: false,
  };
}

function getDefaultMessage(status?: number): string {
  switch (status) {
    case 400:
      return 'Solicitud inválida';
    case 401:
      return 'Sesión expirada';
    case 403:
      return 'Acceso denegado';
    case 404:
      return 'Recurso no encontrado';
    case 409:
      return 'Conflicto de datos';
    case 429:
      return 'Demasiadas solicitudes';
    case 500:
      return 'Error interno del servidor';
    case 502:
      return 'Servicio no disponible';
    case 503:
      return 'Servicio temporalmente недоступен';
    default:
      return 'Ha ocurrido un error';
  }
}

export function getErrorMessage(error: unknown): string {
  const parsed = parseBackendError(error);
  return parsed.message;
}

const AUTH_ERROR_CODES = ['AUTH001', 'AUTH002', 'AUTH003', 'AUTH004', 'AUTH005', 'AUTH006', 'AUTH007'];

function getNormalizedStatus(error: unknown): number | undefined {
  return (error as ApiError).status ?? (error as { response?: { status?: number } }).response?.status;
}

function getNormalizedCode(error: unknown): string | undefined {
  return (error as ApiError).code ?? (error as { response?: { data?: { codigo?: string } } }).response?.data?.codigo ?? undefined;
}

export function isAuthError(error: unknown): boolean {
  const status = getNormalizedStatus(error);
  const code = getNormalizedCode(error);
  return status === 401 || status === 403 || (!!code && AUTH_ERROR_CODES.includes(code));
}

export function isTokenExpiredError(error: unknown): boolean {
  const code = getNormalizedCode(error);
  return code === 'AUTH003' || code === 'AUTH006';
}

export function isTokenRevokedError(error: unknown): boolean {
  const code = getNormalizedCode(error);
  return code === 'AUTH005';
}

export function isValidationError(error: unknown): boolean {
  const code = getNormalizedCode(error);
  return code === 'VAL001' || code === 'VAL002';
}

export function isNetworkError(error: unknown): boolean {
  const axiosError = error as AxiosError;
  return !axiosError.response && Boolean(axiosError.message?.includes('Network Error'));
}

export function wrapError(error: unknown): ApiError {
  const parsed = parseBackendError(error);

  const wrappedError = new Error(parsed.message) as ApiError;
  wrappedError.code = parsed.code;
  wrappedError.status = parsed.status;
  wrappedError.details = parsed.details;
  wrappedError.isBackendError = parsed.isBackendError;

  return wrappedError;
}