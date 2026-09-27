// ponytail: utils puros — sin estado, no composable
import type { AbcItem, AbcItemWire, AlertItem, AlertItemWire, FinancialHealth, FinancialHealthAlert, FinancialHealthAlertWire, FinancialHealthWire } from '../types/analytics';

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
    };
  });
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
