// ponytail: utils puros — sin estado, no composable
import type { AbcItem, AbcItemWire, AlertItem, AlertItemWire, FinancialHealth, FinancialHealthAlert, FinancialHealthAlertWire, FinancialHealthWire, SupplierComparisonItem, SupplierRecommendationItem } from '../types/analytics';
import type { Producto } from '../types/index';

export function toNumber(v: unknown, fallback = 0): number {
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string') {
    const n = Number(v);
    return Number.isFinite(n) ? n : fallback;
  }
  return fallback;
}

export function normalizeAbc(items: AbcItemWire[]): AbcItem[] {  return items.map((i) => ({
    productId: i.productId,
    productName: i.productName,
    spend: toNumber(i.spend ?? i.totalSpend),
    pctTotal: toNumber(i.pctTotal ?? i.pct),
    cumulativePct: toNumber(i.cumulativePct),
    category: i.category,
  }));
}

// ponytail: traduce cvPct/premiumPct → variationPct; sin esto el badge pinta 0.0% (AlertsPanel.vue:54)
export function normalizeAlerts(items: AlertItemWire[]): AlertItem[] {
  return items.map((a) => {
    const pct = toNumber(a.variationPct ?? a.cvPct ?? a.premiumPct);
    const threshold = a.type === 'SUPPLIER_PREMIUM' ? 30 : 50;
    const severity: AlertItem['severity'] =
      a.severity === 'critical' || a.severity === 'warning'
        ? a.severity
        : pct > threshold
          ? 'critical'
          : 'warning';
    return {
      productId: a.productId,
      productName: a.productName,
      currentPrice: toNumber(a.currentPrice ?? a.avgPrice),
      avgPrice: toNumber(a.avgPrice),
      variationPct: pct,
      severity,
      alertKind: a.type === 'SUPPLIER_PREMIUM' ? 'SUPPLIER_PREMIUM' : 'PRICE_VARIATION',
      providerId: a.providerId,
      providerName: a.providerName,
    };
  });
}

// ponytail: junta lo que la página ya recibe — comparativa (quién es el caro) + catálogo (unidad).
// Sin match, devuelve el rec intacto y la tarjeta acorta la frase en vez de romperse.
export function enrichRecommendation(
  rec: SupplierRecommendationItem,
  comparison: SupplierComparisonItem[],
  products: readonly Pick<Producto, 'id' | 'baseUnit'>[],
): SupplierRecommendationItem {
  let dearest: SupplierComparisonItem | undefined;
  for (const c of comparison) {
    if (c.productId !== rec.productId) continue;
    if (!dearest || c.avgPrice > dearest.avgPrice) dearest = c;
  }
  const unit = products.find((p) => p.id === rec.productId)?.baseUnit?.trim();
  return {
    ...rec,
    comparedProviderName:
      dearest && dearest.providerId !== rec.recommendedProviderId ? dearest.providerName : undefined,
    unitLabel: unit || undefined,
  };
}

// ponytail: evidencia de la alerta desde la comparativa del mismo mes (Σ compras, # proveedores).
// Premium: compras del proveedor señalado; variación: total. Sin match, intacta.
export function enrichAlert(alert: AlertItem, comparison: SupplierComparisonItem[]): AlertItem {
  const rows = comparison.filter((c) => c.productId === alert.productId);
  if (!rows.length) return alert;
  const mine = alert.providerId ? rows.find((c) => c.providerId === alert.providerId) : undefined;
  return {
    ...alert,
    purchaseCount: mine
      ? mine.purchaseCount
      : rows.reduce((sum, c) => sum + c.purchaseCount, 0),
    providerCount: rows.length,
  };
}

export function normalizeFinancialHealth(wire: FinancialHealthWire | FinancialHealth | undefined): FinancialHealth | null {
  if (!wire) return null;
  const w = wire as FinancialHealthWire;
  const alerts: FinancialHealthAlert[] = (w.criticalAlerts ?? []).map((a: FinancialHealthAlertWire) => ({
    code: a.code ?? a.type ?? '',
    title: a.title ?? '',
    description: a.description ?? a.message ?? '',
    current: toNumber(a.current ?? a.metric),
    threshold: toNumber(a.threshold),
    action: a.action ?? '',
  }));
  return {
    overallHealth: toNumber(w.overallHealth),
    breakdown: w.breakdown ?? {},
    criticalAlerts: alerts,
    investmentSignals: w.investmentSignals ?? [],
    expansionReadiness: w.expansionReadiness ?? { score: 0, status: 'SIN_DATOS', requirements: [] },
    recommendations: w.recommendations ?? [],
  };
}
