import { describe, expect, it } from 'vitest';
import { enrichAlert, enrichRecommendation, normalizeAlerts } from '../analyticsNormalize';
import type { AlertItem, AlertItemWire, SupplierComparisonItem, SupplierRecommendationItem } from '../../types/analytics';

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

  it('preserva tipo y proveedor para la frase de evidencia', () => {
    const wire: AlertItemWire[] = [
      {
        productId: 'costilla', productName: 'Costilla de Cerdo',
        currentPrice: 5.5, avgPrice: 5.25, premiumPct: 16,
        type: 'SUPPLIER_PREMIUM', providerId: 'provb', providerName: 'ProvB',
      },
    ];

    const a = normalizeAlerts(wire)[0];

    expect(a?.alertKind).toBe('SUPPLIER_PREMIUM');
    expect(a?.providerId).toBe('provb');
    expect(a?.providerName).toBe('ProvB');
  });
});

describe('enrichRecommendation', () => {
  const rec: SupplierRecommendationItem = {
    productId: 'harina', productName: 'Harina de Trigo',
    recommendedProviderId: 'dorado', recommendedProviderName: 'Dorado',
    recommendedPrice: 0.6, currentAvgPrice: 0.93,
    savingsPerUnit: 0.33, savingsPct: 35, supplierCount: 3,
  };
  const comparison: SupplierComparisonItem[] = [
    { productId: 'harina', productName: 'Harina de Trigo', providerId: 'dorado', providerName: 'Dorado', purchaseCount: 4, avgPrice: 0.6, minPrice: 0.6, maxPrice: 0.6, priceStddev: 0 },
    { productId: 'harina', productName: 'Harina de Trigo', providerId: 'distral', providerName: 'Distral', purchaseCount: 3, avgPrice: 0.93, minPrice: 0.93, maxPrice: 0.93, priceStddev: 0 },
  ];

  it('encuentra al caro y la unidad (caso Harina Dorado vs Distral)', () => {
    const r = enrichRecommendation(rec, comparison, [{ id: 'harina', baseUnit: 'Libra' }]);

    expect(r.comparedProviderName).toBe('Distral');
    expect(r.unitLabel).toBe('Libra');
  });

  it('sin datos devuelve el rec intacto (la tarjeta acorta la frase)', () => {
    const r = enrichRecommendation(rec, [], []);

    expect(r.comparedProviderName).toBeUndefined();
    expect(r.unitLabel).toBeUndefined();
    expect(r.recommendedPrice).toBe(0.6);
  });
});

describe('enrichAlert', () => {
  const comparison: SupplierComparisonItem[] = [
    { productId: 'oregano', productName: 'Oregano', providerId: 'p1', providerName: 'Uno', purchaseCount: 2, avgPrice: 1, minPrice: 1, maxPrice: 1, priceStddev: 0 },
    { productId: 'oregano', productName: 'Oregano', providerId: 'p2', providerName: 'Dos', purchaseCount: 1, avgPrice: 3, minPrice: 3, maxPrice: 3, priceStddev: 0 },
  ];

  it('variación suma compras y cuenta proveedores', () => {
    const alert: AlertItem = {
      productId: 'oregano', productName: 'Oregano', currentPrice: 7.6, avgPrice: 2.6,
      variationPct: 192.2, severity: 'critical', alertKind: 'PRICE_VARIATION',
    };

    const a = enrichAlert(alert, comparison);

    expect(a.purchaseCount).toBe(3);
    expect(a.providerCount).toBe(2);
  });

  it('premium usa las compras del proveedor señalado', () => {
    const alert: AlertItem = {
      productId: 'oregano', productName: 'Oregano', currentPrice: 3, avgPrice: 2.6,
      variationPct: 16, severity: 'warning', alertKind: 'SUPPLIER_PREMIUM',
      providerId: 'p2', providerName: 'Dos',
    };

    const a = enrichAlert(alert, comparison);

    expect(a.purchaseCount).toBe(1);
    expect(a.providerCount).toBe(2);
  });

  it('sin comparativa devuelve la alerta intacta', () => {
    const alert: AlertItem = {
      productId: 'oregano', productName: 'Oregano', currentPrice: 7.6, avgPrice: 2.6,
      variationPct: 192.2, severity: 'critical',
    };

    const a = enrichAlert(alert, []);

    expect(a.purchaseCount).toBeUndefined();
    expect(a.variationPct).toBe(192.2);
  });
});
