import { describe, expect, it } from 'vitest';
import { isItemComplete, scaledPriceOnPresentationChange, SUELTO, toPresentacionPayload } from '../invoiceItemGuards';

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

describe('scaledPriceOnPresentationChange', () => {
  it('bulto decimal 43.2: autollenado por kilo se convierte a precio por bulto', () => {
    expect(scaledPriceOnPresentationChange(0.87, 0.87, 43.2, null)).toBe(37.58);
  });
  it('caja entera x24: convierte igual', () => {
    expect(scaledPriceOnPresentationChange(2.5, 2.5, 24, null)).toBe(60);
  });
  it('galon decimal 3.785: convierte igual', () => {
    expect(scaledPriceOnPresentationChange(1.2, 1.2, 3.785, null)).toBe(4.54);
  });
  it('precio tocado por el usuario se respeta', () => {
    expect(scaledPriceOnPresentationChange(40, 0.87, 43.2, null)).toBe(40);
  });
  it('valor vacio se llena con el precio por empaque', () => {
    expect(scaledPriceOnPresentationChange(null, 0.87, 43.2, null)).toBe(37.58);
  });
  it('volver a suelto revierte al precio por unidad base', () => {
    expect(scaledPriceOnPresentationChange(37.58, 0.87, null, 43.2)).toBe(0.87);
  });
  it('cambiar de un empaque a otro re-escala desde la base', () => {
    expect(scaledPriceOnPresentationChange(37.58, 0.87, 24, 43.2)).toBe(20.88);
  });
  it('sin precio base no toca nada', () => {
    expect(scaledPriceOnPresentationChange(10, null, 43.2, null)).toBe(10);
  });
});
