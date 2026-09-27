import { describe, expect, it } from 'vitest';
import { normalizeAlerts } from '../analyticsNormalize';
import type { AlertItemWire } from '../../types/analytics';

describe('normalizeAlerts', () => {
  it('traduce cvPct a variationPct (caso Orégano, era 0.0%)', () => {
    const wire: AlertItemWire[] = [
      {
        productId: 'oregano', productName: 'Oregano', avgPrice: 1.54,
        cvPct: 192.2, type: 'PRICE_VARIATION',
      },
    ];

    const result = normalizeAlerts(wire);
    expect(result).toHaveLength(1);
    const a = result[0];

    expect(a?.variationPct).toBeCloseTo(192.2);
    expect(a?.severity).toBe('critical');
    expect(a?.currentPrice).toBe(1.54);
  });

  it('traduce premiumPct y marca warning bajo el umbral', () => {
    const wire: AlertItemWire[] = [
      {
        productId: 'costilla', productName: 'Costilla de Cerdo',
        currentPrice: 5.5, avgPrice: 5.25, premiumPct: 16, type: 'SUPPLIER_PREMIUM',
      },
    ];

    const result = normalizeAlerts(wire);
    expect(result).toHaveLength(1);
    const a = result[0];

    expect(a?.variationPct).toBe(16);
    expect(a?.severity).toBe('warning');
    expect(a?.currentPrice).toBe(5.5);
  });

  it('respeta severity si el backend ya la trae', () => {
    const wire: AlertItemWire[] = [
      {
        productId: 'x', productName: 'X', currentPrice: 10, avgPrice: 9,
        variationPct: 11.1, severity: 'critical',
      },
    ];

    const result = normalizeAlerts(wire);
    expect(result).toHaveLength(1);
    const a = result[0];

    expect(a?.variationPct).toBeCloseTo(11.1);
    expect(a?.severity).toBe('critical');
  });
});
