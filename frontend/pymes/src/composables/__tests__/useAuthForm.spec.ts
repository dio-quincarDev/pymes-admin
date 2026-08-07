import { describe, it, expect } from 'vitest';
import { useAuthForm } from '../useAuthForm';

describe('useAuthForm', () => {
  it('starts with initialLoading true', () => {
    const { initialLoading } = useAuthForm();
    expect(initialLoading.value).toBe(true);
  });

  it('starts with loading false', () => {
    const { loading } = useAuthForm();
    expect(loading.value).toBe(false);
  });

  it('starts with showPassword false', () => {
    const { showPassword } = useAuthForm();
    expect(showPassword.value).toBe(false);
  });
});
