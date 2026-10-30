import { describe, expect, it } from 'vitest';
import { METODOS_PAGO, METODOS_PAGO_VALUES, labelMetodoPago } from '../metodosPago';
import type { MetodoPago } from '../../types';

const ALL: MetodoPago[] = ['EFECTIVO', 'ACH', 'YAPPY', 'TARJETA', 'CHEQUE', 'CREDITO'];

describe('metodosPago', () => {
  it('cubre todos los valores del enum', () => {
    expect([...METODOS_PAGO_VALUES].sort()).toEqual([...ALL].sort());
  });
  it('cada valor tiene label no vacio', () => {
    for (const m of ALL) {
      expect(labelMetodoPago(m)).toBeTruthy();
    }
  });
  it('METODOS_PAGO tiene una entrada por valor', () => {
    expect(METODOS_PAGO.length).toBe(ALL.length);
  });
});
