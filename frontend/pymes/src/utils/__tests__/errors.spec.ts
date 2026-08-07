import { describe, it, expect } from 'vitest';
import { getErrorMessage, isAuthError, isNetworkError, isTokenExpiredError, isTokenRevokedError, isValidationError, wrapError } from '../errors';
import type { ApiError } from 'src/types/error';

describe('getErrorMessage', () => {
  it('returns backend message when present', () => {
    const err = { response: { data: { codigo: 'AUTH001', mensaje: 'Credenciales inválidas', path: null, timestamp: null, detalles: null } } };
    expect(getErrorMessage(err)).toBe('Credenciales inválidas');
  });

  it('returns default for unknown errors', () => {
    expect(getErrorMessage(null)).toBe('Error de conexión');
  });
});

describe('isAuthError', () => {
  it('identifies 401 as auth error (raw axios)', () => {
    expect(isAuthError({ response: { status: 401 } })).toBe(true);
  });

  it('identifies auth code without status (raw axios)', () => {
    expect(isAuthError({ response: { data: { codigo: 'AUTH003' } } })).toBe(true);
  });

  it('returns false for non-auth errors', () => {
    expect(isAuthError({ response: { status: 500 } })).toBe(false);
  });

  it('identifies 401 from normalized error', () => {
    const normalized = new Error('Sesión expirada') as ApiError;
    normalized.status = 401;
    expect(isAuthError(normalized)).toBe(true);
  });

  it('identifies auth code from normalized error', () => {
    const normalized = new Error('Token expired') as ApiError;
    normalized.code = 'AUTH003';
    expect(isAuthError(normalized)).toBe(true);
  });

  it('identifies 403 from normalized error', () => {
    const normalized = new Error('Forbidden') as ApiError;
    normalized.status = 403;
    expect(isAuthError(normalized)).toBe(true);
  });
});

describe('isTokenExpiredError', () => {
  it('identifies AUTH003 as expired', () => {
    expect(isTokenExpiredError({ response: { data: { codigo: 'AUTH003' } } })).toBe(true);
  });

  it('identifies AUTH006 as expired', () => {
    expect(isTokenExpiredError({ response: { data: { codigo: 'AUTH006' } } })).toBe(true);
  });

  it('returns false for non-expired codes', () => {
    expect(isTokenExpiredError({ response: { data: { codigo: 'AUTH001' } } })).toBe(false);
  });

  it('identifies from normalized error', () => {
    const err = new Error('expired') as ApiError;
    err.code = 'AUTH003';
    expect(isTokenExpiredError(err)).toBe(true);
  });
});

describe('isTokenRevokedError', () => {
  it('identifies AUTH005 as revoked', () => {
    expect(isTokenRevokedError({ response: { data: { codigo: 'AUTH005' } } })).toBe(true);
  });

  it('identifies from normalized error', () => {
    const err = new Error('revoked') as ApiError;
    err.code = 'AUTH005';
    expect(isTokenRevokedError(err)).toBe(true);
  });
});

describe('isValidationError', () => {
  it('identifies VAL001', () => {
    expect(isValidationError({ response: { data: { codigo: 'VAL001' } } })).toBe(true);
  });

  it('identifies VAL002', () => {
    expect(isValidationError({ response: { data: { codigo: 'VAL002' } } })).toBe(true);
  });
});

describe('wrapError', () => {
  it('wraps backend error into ApiError', () => {
    const err = { response: { status: 400, data: { codigo: 'VAL001', mensaje: 'Bad request', path: null, timestamp: null, detalles: { field: 'email' } } } };
    const wrapped = wrapError(err);
    expect(wrapped).toBeInstanceOf(Error);
    expect(wrapped.message).toBe('Bad request');
    expect(wrapped.code).toBe('VAL001');
    expect(wrapped.status).toBe(400);
    expect(wrapped.isBackendError).toBe(true);
    expect(wrapped.details).toEqual({ field: 'email' });
  });

  it('wraps connection error', () => {
    const wrapped = wrapError(null);
    expect(wrapped.message).toBe('Error de conexión');
    expect(wrapped.isBackendError).toBe(false);
  });
});

describe('isNetworkError', () => {
  it('identifies network errors', () => {
    expect(isNetworkError({ message: 'Network Error' })).toBe(true);
  });

  it('returns false for other errors', () => {
    expect(isNetworkError({ response: { status: 500 } })).toBe(false);
  });
});
