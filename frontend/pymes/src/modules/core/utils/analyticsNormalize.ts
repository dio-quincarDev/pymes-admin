// ponytail: utils puros — sin estado, no composable
import type { AbcItem, AbcItemWire, AlertItem, AlertItemWire, FinancialHealth, FinancialHealthAlert, FinancialHealthAlertWire, FinancialHealthWire, PriceTrail, SupplierComparisonItem, SupplierRecommendationItem } from '../types/analytics';
import type { Factura, Producto } from '../types/index';

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

// ponytail: ~149 productos guardan base_unit como ID de template_units (ej Azúcar Morena → Kg).
// El setup ya trae el mapa id→nombre; traducir acá evita migración y muestra "Kg" en vez de "por unidad".
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface SetupUnitLike {
  code: string;
  name: string;
}

export function resolveUnitLabel(
  raw: string | undefined,
  units: readonly SetupUnitLike[],
): string | undefined {
  const base = raw?.trim();
  if (!base) return undefined;
  if (!UUID_RE.test(base)) return base;
  return units.find((u) => u.code === base)?.name;
}

// Rango confiable pedido por negocio: fuera de acá el ahorro puede estar contaminado (unidades nulas/mezcladas)
export const TRUSTED_SAVINGS_MIN = 35;
export const TRUSTED_SAVINGS_MAX = 75;

export function withinTrustedRange(savingsPct: number): boolean {
  return savingsPct >= TRUSTED_SAVINGS_MIN && savingsPct <= TRUSTED_SAVINGS_MAX;
}

// Rango confiable pedido por negocio: +100% mensual en insumos es error de registro
// (Huevos +140% por Caja x30 a $0.13, Orégano +112% por "LB" con conv 1) — quedan tras "ver más"
export const TRUSTED_ALERT_MAX_PCT = 100;

export function withinTrustedAlertRange(variationPct: number): boolean {
  return variationPct <= TRUSTED_ALERT_MAX_PCT;
}

// ponytail: junta lo que la página ya recibe — comparativa (quién es el caro) + catálogo (unidad).
// Sin match, devuelve el rec intacto y la tarjeta acorta la frase en vez de romperse.
export function enrichRecommendation(
  rec: SupplierRecommendationItem,
  comparison: SupplierComparisonItem[],
  products: readonly Pick<Producto, 'id' | 'baseUnit'>[],
  units: readonly SetupUnitLike[] = [],
): SupplierRecommendationItem {
  let dearest: SupplierComparisonItem | undefined;
  for (const c of comparison) {
    if (c.productId !== rec.productId) continue;
    if (!dearest || c.avgPrice > dearest.avgPrice) dearest = c;
  }
  const rawUnit = products.find((p) => p.id === rec.productId)?.baseUnit;
  return {
    ...rec,
    comparedProviderName:
      dearest && dearest.providerId !== rec.recommendedProviderId ? dearest.providerName : undefined,
    unitLabel: resolveUnitLabel(rawUnit, units),
  };
}

// ponytail: la variación no trae segundo precio (wire: solo avgPrice + cvPct) —
// el detalle muestra dirección + evidencia, nunca "$X vs $X".
export function alertEvidence(alert: AlertItem): string {
  if (!alert.purchaseCount) return '';
  const buys = `${alert.purchaseCount} compra${alert.purchaseCount === 1 ? '' : 's'}`;
  if (alert.alertKind === 'SUPPLIER_PREMIUM' || !alert.providerCount) return `, basado en ${buys}`;
  return `, basado en ${buys} en ${alert.providerCount} proveedor${alert.providerCount === 1 ? '' : 'es'}`;
}

// ponytail: carril = presentación + conversión; comparar Lb contra suelta es mentir con números
// verdaderos (Harina 2026-09: primera en Lb $0.60, resto suelta). Misma regla que la guarda Fase 1.
function laneKey(presentacionId: string | null | undefined, conversionFactor: unknown): string {
  const conv = toNumber(conversionFactor, 1);
  return `${presentacionId ?? 'NONE'}|${conv > 0 ? conv : 1}`;
}

function unitPriceOf(unitPrice: unknown, conversionFactor: unknown): number {
  const conv = toNumber(conversionFactor, 1);
  return toNumber(unitPrice) / (conv > 0 ? conv : 1);
}

// ponytail: primera y última compra PAGADA por producto, precio normalizado; usa el endpoint
// de facturas que la página ya puede leer (cero cambios backend). Orden estable por fecha.
export function firstLastByProduct(
  facturas: readonly {
    status: Factura['status'];
    issueDate: string;
    items: readonly {
      productId: string;
      unitPrice: number;
      conversionFactor: number;
      presentacionId: string | null;
    }[];
  }[],
): Map<string, PriceTrail> {
  const rows: { productId: string; date: string; price: number; lane: string }[] = [];
  for (const f of facturas) {
    if (f.status !== 'PAGADA') continue;
    for (const it of f.items ?? []) {
      rows.push({
        productId: it.productId,
        date: f.issueDate ?? '',
        price: unitPriceOf(it.unitPrice, it.conversionFactor),
        lane: laneKey(it.presentacionId, it.conversionFactor),
      });
    }
  }
  rows.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
  const map = new Map<string, PriceTrail & { lanes: Set<string> }>();
  for (const r of rows) {
    const e = map.get(r.productId);
    if (!e) {
      map.set(r.productId, {
        firstPrice: r.price,
        lastPrice: r.price,
        count: 1,
        singleLane: true,
        lanes: new Set([r.lane]),
      });
      continue;
    }
    e.lastPrice = r.price;
    e.count += 1;
    e.lanes.add(r.lane);
  }
  for (const e of map.values()) {
    e.singleLane = e.lanes.size === 1;
    delete (e as { lanes?: unknown }).lanes;
  }
  return map;
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

// ponytail: pega el recorrido de facturas + unidad a la alerta; sin trail, intacta (el panel cae al texto viejo)
export function enrichAlertPrices(
  alert: AlertItem,
  trail: PriceTrail | undefined,
  unitLabel?: string,
): AlertItem {
  if (!trail && !unitLabel) return alert;
  return { ...alert, priceTrail: trail ?? alert.priceTrail, unitLabel: unitLabel ?? alert.unitLabel };
}

const money = (n: number): string => `$${n.toFixed(2)}`;

// Frase para dummies, solo precios (cero proveedores, cero promedios, cero "basado en"):
// carril único → primera vs última; carril mezclado → revisar registro, nunca números.
export function alertDummyText(alert: AlertItem): string {
  const t = alert.priceTrail;
  if (!t) return '';
  const unit = alert.unitLabel ? ` por ${alert.unitLabel}` : '';
  if (!t.singleLane) return 'Hay compras en distintas presentaciones, revisa el registro.';
  if (alert.alertKind === 'SUPPLIER_PREMIUM') {
    if (!(alert.avgPrice > 0)) return '';
    return `Se paga cara: hasta ${money((alert.avgPrice * toNumber(alert.variationPct, 0)) / 100)} de más${unit}.`;
  }
  const d = t.lastPrice - t.firstPrice;
  const pct = t.firstPrice > 0 ? (d / t.firstPrice) * 100 : 0;
  const buys = `${t.count} compra${t.count === 1 ? '' : 's'}`;
  const head = `La comprabas a ${money(t.firstPrice)} y ahora a ${money(t.lastPrice)}`;
  if (d < 0) return `${head} — bajó ${money(-d)} (${pct.toFixed(1)}%) en ${buys}.`;
  return `${head} — subió ${money(d)} (+${pct.toFixed(1)}%) en ${buys}.`;
}

// Texto anterior (tendencia + evidencia) como respaldo cuando aún no hay facturas cargadas.
export function alertFallbackText(alert: AlertItem): string {
  const head =
    alert.alertKind === 'SUPPLIER_PREMIUM' && alert.providerName
      ? `${alert.providerName} por encima del resto`
      : 'Tendencia al alza';
  return `${head}${alertEvidence(alert)}.`;
}

// Badge en plata cuando el carril es único; % cuando no hay con qué (tolerante, R3).
export function alertBadge(alert: AlertItem): string {
  const t = alert.priceTrail;
  if (t?.singleLane) {
    if (alert.alertKind === 'SUPPLIER_PREMIUM' && alert.avgPrice > 0) {
      return `+${money((alert.avgPrice * toNumber(alert.variationPct, 0)) / 100)}`;
    }
    if (alert.alertKind !== 'SUPPLIER_PREMIUM') {
      return `↑${money(Math.abs(t.lastPrice - t.firstPrice))}`;
    }
  }
  const p = toNumber(alert.variationPct, 0);
  return `${p > 0 ? '+' : ''}${p.toFixed(1)}%`;
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
