import { describe, it, expect } from 'vitest';
import { getErrorMessage, isAuthError, isNetworkError } from '../errors';

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
  it('identifies 401 as auth error', () => {
    expect(isAuthError({ response: { status: 401 } })).toBe(true);
  });

  it('identifies auth code without status', () => {
    expect(isAuthError({ response: { data: { codigo: 'AUTH003' } } })).toBe(true);
  });

  it('returns false for non-auth errors', () => {
    expect(isAuthError({ response: { status: 500 } })).toBe(false);
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
