import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';

vi.stubGlobal('window', { ...globalThis, location: { href: '' } } as unknown as Window & typeof globalThis);

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  };
})();

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock });

import { useAuthStore } from '../index';

vi.mock('src/boot/axios', () => ({
  api: {
    defaults: { headers: { common: {} as Record<string, string> } },
    post: vi.fn(),
    get: vi.fn(),
  },
}));

type MockApi = { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorageMock.clear();
    vi.clearAllMocks();
  });

  it('starts unauthenticated', () => {
    const store = useAuthStore();
    expect(store.isAuthenticated).toBe(false);
    expect(store.user).toBeNull();
    expect(store.accessToken).toBeNull();
  });

  it('setSession stores tokens and user', () => {
    const store = useAuthStore();
    store.setSession('access-123', 'refresh-456', { id: 'u1', email: 'a@b.com', name: 'Test' });
    expect(store.accessToken).toBe('access-123');
    expect(store.user?.name).toBe('Test');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('pymeq_token', 'access-123');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('pymeq_refresh_token', 'refresh-456');
    expect(store.isAuthenticated).toBe(true);
  });

  it('clearSession clears everything', () => {
    const store = useAuthStore();
    store.setSession('access-123', 'refresh-456', { id: 'u1', email: 'a@b.com', name: 'Test' });
    store.clearSession();
    expect(store.accessToken).toBeNull();
    expect(store.user).toBeNull();
    expect(store.isAuthenticated).toBe(false);
  });

  it('setPendingTenant stores tenant info', () => {
    const store = useAuthStore();
    store.setPendingTenant('Mi Empresa', 'mi-empresa');
    expect(store.pendingTenant).toEqual({ name: 'Mi Empresa', slug: 'mi-empresa' });
  });

  it('clearPendingTenant removes tenant info', () => {
    const store = useAuthStore();
    store.setPendingTenant('Mi Empresa', 'mi-empresa');
    store.clearPendingTenant();
    expect(store.pendingTenant).toBeNull();
  });

  it('handleOAuthCallback sets token and fetches user', async () => {
    const { api } = await import('src/boot/axios');
    (api.get as MockApi['get']).mockResolvedValue({ data: { data: { id: 'u1', email: 'a@b.com', name: 'OAuth User' } } });

    const store = useAuthStore();
    await store.handleOAuthCallback('oauth-token', 'oauth-refresh');
    expect(store.accessToken).toBe('oauth-token');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('pymeq_token', 'oauth-token');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('pymeq_refresh_token', 'oauth-refresh');
  });
});
