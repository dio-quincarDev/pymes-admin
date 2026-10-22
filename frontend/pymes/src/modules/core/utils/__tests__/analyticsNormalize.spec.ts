import { describe, expect, it } from 'vitest';
import { alertBadge, alertDummyText, alertEvidence, alertFallbackText, enrichAlert, enrichAlertPrices, enrichRecommendation, firstLastByProduct, normalizeAlerts, resolveUnitLabel, withinTrustedAlertRange, withinTrustedRange } from '../analyticsNormalize';
import type { AlertItem, AlertItemWire, SupplierComparisonItem, SupplierRecommendationItem } from '../../types/analytics';
import type { Factura } from '../../types';

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
    const r = enrichRecommendation(rec, comparison, [{ id: 'harina', baseUnit: 'Libra' }], []);

    expect(r.comparedProviderName).toBe('Distral');
    expect(r.unitLabel).toBe('Libra');
  });

  it('traduce base_unit guardado como ID a su nombre (caso Azúcar Morena → Kg)', () => {
    const azucar: SupplierRecommendationItem = {
      ...rec, productId: 'azucar', productName: 'Azucar Morena', savingsPct: 58,
    };
    const units = [{ code: '1e949c0d-fc2f-4282-9fae-a5a4ccac2cfb', name: 'Kg' }];

    const r = enrichRecommendation(
      azucar, comparison, [{ id: 'azucar', baseUnit: '1e949c0d-fc2f-4282-9fae-a5a4ccac2cfb' }], units,
    );

    expect(r.unitLabel).toBe('Kg');
  });

  it('UUID desconocido se descarta (la tarjeta cae a "por unidad")', () => {
    expect(resolveUnitLabel('1e949c0d-fc2f-4282-9fae-a5a4ccac2cfb', [])).toBeUndefined();
    expect(resolveUnitLabel('Libra', [])).toBe('Libra');
    expect(resolveUnitLabel(undefined, [])).toBeUndefined();
  });

  it('rango confiable 35–75% con bordes incluidos', () => {
    expect(withinTrustedRange(35)).toBe(true);
    expect(withinTrustedRange(58)).toBe(true);
    expect(withinTrustedRange(75)).toBe(true);
    expect(withinTrustedRange(34.9)).toBe(false);
    expect(withinTrustedRange(75.1)).toBe(false);
  });

  it('alertas: solo ≤ +100% por defecto (contaminados VPS quedan fuera)', () => {
    expect(withinTrustedAlertRange(72.8)).toBe(true);
    expect(withinTrustedAlertRange(100)).toBe(true);
    expect(withinTrustedAlertRange(100.1)).toBe(false);
    expect(withinTrustedAlertRange(140.8)).toBe(false); // Huevos Caja x30
    expect(withinTrustedAlertRange(112.3)).toBe(false); // Orégano "LB" conv 1
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

describe('alertEvidence', () => {
  const base: AlertItem = {
    productId: 'x', productName: 'X', currentPrice: 1, avgPrice: 1,
    variationPct: 50, severity: 'warning',
  };

  it('variación con plural correcto (caso Huevos: 2 compras, 2 proveedores)', () => {
    expect(alertEvidence({ ...base, alertKind: 'PRICE_VARIATION', purchaseCount: 2, providerCount: 2 }))
      .toBe(', basado en 2 compras en 2 proveedores');
  });

  it('singular sin "proveedore" (caso Miró: 2 compras, 1 proveedor)', () => {
    expect(alertEvidence({ ...base, alertKind: 'PRICE_VARIATION', purchaseCount: 2, providerCount: 1 }))
      .toBe(', basado en 2 compras en 1 proveedor');
  });

  it('premium solo compras (caso Parmigiana: 1 compra)', () => {
    expect(alertEvidence({ ...base, alertKind: 'SUPPLIER_PREMIUM', providerName: 'La Parmigiana', purchaseCount: 1, providerCount: 3 }))
      .toBe(', basado en 1 compra');
  });

  it('sin evidencia devuelve vacío (caché vieja no rompe)', () => {
    expect(alertEvidence(base)).toBe('');
  });
});

describe('firstLastByProduct', () => {
  // Harina real 2026-09: primera en Lb, resto suelta → carriles mezclados, sin frase con números
  const harina: Factura[] = [
    { status: 'PAGADA', issueDate: '2026-09-07', items: [{ productId: 'harina', unitPrice: 0.6, conversionFactor: 1, presentacionId: 'lb-id' }] },
    { status: 'PAGADA', issueDate: '2026-09-08', items: [{ productId: 'harina', unitPrice: 1.15, conversionFactor: 1, presentacionId: null }] },
    { status: 'PAGADA', issueDate: '2026-09-08', items: [{ productId: 'harina', unitPrice: 0.93, conversionFactor: 1, presentacionId: null }] },
  ] as unknown as Factura[];

  it('Harina: primera $0.60, última $0.93, pico $1.15, carril mezclado', () => {
    const t = firstLastByProduct(harina).get('harina');

    expect(t?.firstPrice).toBeCloseTo(0.6);
    expect(t?.lastPrice).toBeCloseTo(0.93);
    expect(t?.maxPrice).toBeCloseTo(1.15);
    expect(t?.count).toBe(3);
    expect(t?.singleLane).toBe(false);
  });

  it('mismo carril (misma presentación) pasa', () => {
    const fs = [
      { status: 'PAGADA', issueDate: '2026-09-01', items: [{ productId: 'a', unitPrice: 2, conversionFactor: 1, presentacionId: 'p1' }] },
      { status: 'PAGADA', issueDate: '2026-09-02', items: [{ productId: 'a', unitPrice: 3, conversionFactor: 1, presentacionId: 'p1' }] },
    ] as unknown as Factura[];

    expect(firstLastByProduct(fs).get('a')?.singleLane).toBe(true);
  });

  it('ambas sueltas misma conversión pasan; distinta conversión o ANULADA se ignoran', () => {
    const ok = [
      { status: 'PAGADA', issueDate: '2026-09-01', items: [{ productId: 'b', unitPrice: 2, conversionFactor: 1, presentacionId: null }] },
      { status: 'PAGADA', issueDate: '2026-09-02', items: [{ productId: 'b', unitPrice: 3, conversionFactor: 1, presentacionId: null }] },
    ] as unknown as Factura[];
    const mixed = [
      { status: 'PAGADA', issueDate: '2026-09-01', items: [{ productId: 'c', unitPrice: 2, conversionFactor: 1, presentacionId: null }] },
      { status: 'PAGADA', issueDate: '2026-09-02', items: [{ productId: 'c', unitPrice: 60, conversionFactor: 30, presentacionId: null }] },
      { status: 'ANULADA', issueDate: '2026-09-03', items: [{ productId: 'c', unitPrice: 99, conversionFactor: 1, presentacionId: null }] },
    ] as unknown as Factura[];

    expect(firstLastByProduct(ok).get('b')?.singleLane).toBe(true);
    const mc = firstLastByProduct(mixed).get('c');
    expect(mc?.singleLane).toBe(false);
    expect(mc?.count).toBe(2);
  });
});

describe('alertDummyText', () => {
  const base: AlertItem = {
    productId: 'x', productName: 'X', currentPrice: 1, avgPrice: 1,
    variationPct: 50, severity: 'warning', alertKind: 'PRICE_VARIATION',
  };

  it('carril mezclado (Harina) → revisar registro, sin números', () => {
    const a = enrichAlertPrices(base, { firstPrice: 0.6, lastPrice: 0.93, maxPrice: 1.15, count: 3, singleLane: false });

    expect(alertDummyText(a)).toBe('Hay compras en distintas presentaciones, revisa el registro.');
  });

  it('carril único que sube → primera vs última con $ y %', () => {
    const a = enrichAlertPrices(base, { firstPrice: 0.6, lastPrice: 0.93, maxPrice: 0.93, count: 3, singleLane: true });

    expect(alertDummyText(a)).toBe('La comprabas a $0.60 y ahora a $0.93 — subió $0.33 (+55.0%) en 3 compras.');
  });

  it('carril único que baja → dice bajó, no subió', () => {
    const a = enrichAlertPrices(base, { firstPrice: 1, lastPrice: 0.8, maxPrice: 1, count: 2, singleLane: true });

    expect(alertDummyText(a)).toBe('La comprabas a $1.00 y ahora a $0.80 — bajó $0.20 (-20.0%) en 2 compras.');
  });

  it('frase +720% (Bolsas) → se calla aunque el carril sea único', () => {
    const a = enrichAlertPrices(base, { firstPrice: 0.25, lastPrice: 2.05, maxPrice: 2.05, count: 3, singleLane: true });

    expect(alertDummyText(a)).toBe('Hay compras en distintas presentaciones, revisa el registro.');
  });

  it('primera==última con pico (Azúcar $1.40→$3.35→$1.40) → dice el pico, no +0.0%', () => {
    const a = enrichAlertPrices(base, { firstPrice: 1.4, lastPrice: 1.4, maxPrice: 3.35, count: 3, singleLane: true });

    expect(alertDummyText(a)).toBe('Tuvo un pico de $3.35, hoy está en $1.40. En 3 compras.');
  });

  it('primera==última sin pico (Mayonesa $3.25) → se mantiene', () => {
    const a = enrichAlertPrices(base, { firstPrice: 3.25, lastPrice: 3.25, maxPrice: 3.25, count: 5, singleLane: true });

    expect(alertDummyText(a)).toBe('Se mantiene a $3.25 en 5 compras.');
  });

  it('premium carril único → de-más en plata, sin proveedor', () => {
    const a = enrichAlertPrices(
      { ...base, alertKind: 'SUPPLIER_PREMIUM', avgPrice: 2.12, variationPct: 112.3 },
      { firstPrice: 0.32, lastPrice: 4.5, maxPrice: 4.5, count: 3, singleLane: true },
      'unidad',
    );

    expect(alertDummyText(a)).toBe('Se paga cara: hasta $2.38 de más por unidad.');
  });

  it('sin trail → vacío (el panel cae al texto anterior)', () => {
    expect(alertDummyText(base)).toBe('');
    expect(alertFallbackText({ ...base, purchaseCount: 2, providerCount: 1 }))
      .toBe('Tendencia al alza, basado en 2 compras en 1 proveedor.');
  });
});

describe('alertBadge', () => {
  const base: AlertItem = {
    productId: 'x', productName: 'X', currentPrice: 1, avgPrice: 1,
    variationPct: 140.8, severity: 'critical', alertKind: 'PRICE_VARIATION',
  };

  it('variación carril único → subida en plata', () => {
    expect(alertBadge(enrichAlertPrices(base, { firstPrice: 0.6, lastPrice: 0.93, maxPrice: 0.93, count: 3, singleLane: true })))
      .toBe('↑$0.33');
  });

  it('bajada (Miró $3.70→$1.68) → flecha para abajo', () => {
    expect(alertBadge(enrichAlertPrices(base, { firstPrice: 3.7, lastPrice: 1.68, maxPrice: 3.7, count: 2, singleLane: true })))
      .toBe('↓$2.02');
  });

  it('sin cambio (Azúcar $1.40) → % del backend, nada de ↑$0.00', () => {
    expect(alertBadge(enrichAlertPrices(
      { ...base, variationPct: 54.9 },
      { firstPrice: 1.4, lastPrice: 1.4, maxPrice: 3.35, count: 3, singleLane: true },
    ))).toBe('+54.9%');
  });

  it('premium carril único → de-más en plata', () => {
    expect(alertBadge(enrichAlertPrices(
      { ...base, alertKind: 'SUPPLIER_PREMIUM', avgPrice: 2.12, variationPct: 112.3 },
      { firstPrice: 0.32, lastPrice: 4.5, maxPrice: 4.5, count: 3, singleLane: true },
    ))).toBe('+$2.38');
  });

  it('sin carril único → % de antes (tolerante)', () => {
    expect(alertBadge(base)).toBe('+140.8%');
  });
});
