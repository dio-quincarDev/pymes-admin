import { describe, expect, it } from 'vitest';
import { isItemComplete, SUELTO, toPresentacionPayload } from '../invoiceItemGuards';

const base = { productoId: 'p1', presentacionId: 'pres1', fueSuelto: false, cantidad: 2, valor: 10 };

describe('isItemComplete', () => {
  it('completo con empaque', () => {
    expect(isItemComplete(base)).toBe(true);
  });
  it('completo suelto sin presentacionId', () => {
    expect(isItemComplete({ ...base, presentacionId: null, fueSuelto: true })).toBe(true);
  });
  it('incompleto sin empaque ni suelto', () => {
    expect(isItemComplete({ ...base, presentacionId: null })).toBe(false);
  });
  it('incompleto sin cantidad', () => {
    expect(isItemComplete({ ...base, cantidad: 0 })).toBe(false);
  });
});

describe('toPresentacionPayload', () => {
  it('suelto nunca viaja como id', () => {
    expect(toPresentacionPayload({ presentacionId: SUELTO, fueSuelto: true }))
      .toEqual({ presentacionId: null, fueSuelto: true });
  });
  it('empaque viaja con su id', () => {
    expect(toPresentacionPayload({ presentacionId: 'pres1', fueSuelto: false }))
      .toEqual({ presentacionId: 'pres1', fueSuelto: false });
  });
});
