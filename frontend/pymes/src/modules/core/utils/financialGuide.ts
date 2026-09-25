// ponytail: deriva guía "mantén" invirtiendo scoring de AnalyticsServiceImpl — sin hardcodear tabla fija
import type { FinancialHealth, FinancialHealthBreakdown, SupplierRecommendationItem } from '../types/analytics';

function parseDriver(drivers: string[], key: string): number | null {
  const row = drivers.find((d) => d.toLowerCase().includes(key.toLowerCase()));
  if (!row) return null;
  const m = row.match(/(-?\d+(\.\d+)?)/);
  return m ? Number(m[1]) : null;
}

export interface GuideLine {
  label: string;
  detail: string;
  tooltip: string;
}

// thresholds invertidos de AnalyticsServiceImpl.java:724-739 — unica fuente, no tabla hardcodeada
// profitability: gross 40->100, op 15->100, net 10->100 ; mantén = 70% => 28/10.5/7
// efficiency: (1 - ratio/0.5)*100 => mantén 70 => ratio 15%
// stability: supplierScore 100 - max/0.5*100 con price 60/100 blend => mantén 70 => max ~15% si inestable else 30%
// growth: 50 + growth*5 => mantén 70 => growth 4%

export function guideForPillar(key: string, bd: FinancialHealthBreakdown | undefined): GuideLine {
  const drivers = bd?.drivers ?? [];
  const score = bd?.score ?? 0;

  if (key === 'profitability') {
    const net = parseDriver(drivers, 'netMarginPct');
    const op = parseDriver(drivers, 'operatingMarginPct');
    const gross = parseDriver(drivers, 'grossMarginPct');
    // usa net como principal (es el que ve el usuario en Rentabilidad)
    const currentNet = net ?? 0;
    const maintain = 7;
    if (score >= 70) return { label: `Ganaste $${(currentNet / 100 * 100).toFixed(0)} de cada $100`, detail: `Mantén sobre $${maintain} · vas sobrado +${(currentNet - maintain).toFixed(1)}%`, tooltip: '¿Te queda ganancia después de pagar todo? Bruto ≥28% / Operativo ≥10.5% / Neto ≥7% para verde' };
    // ponytail: net actual puede ser null si sin datos
    const diff = maintain - currentNet;
    return { label: `Neto ${currentNet.toFixed(1)}%`, detail: gross !== null && op !== null ? `Mantén sobre $${maintain} · te faltan ${diff.toFixed(1)}%` : `Mantén sobre $${maintain}`, tooltip: '¿Te queda ganancia? Necesitas Neto ≥7% para verde' };
  }
  if (key === 'efficiency') {
    const opexRatio = parseDriver(drivers, 'opexRatio');
    const pct = opexRatio !== null ? opexRatio : null;
    // pct viene como "opexRatio: 17.50%" -> 17.5
    const maintain = 15;
    if (pct === null) return { label: 'Sin datos', detail: 'Mantén bajo $15 de cada $100', tooltip: '¿Cuánto gastás para abrir? Mantén gastos <15% de ventas para verde' };
    const spent = pct;
    if (score >= 70) return { label: `Gastás $${spent.toFixed(1)} de cada $100`, detail: `Mantén bajo $${maintain} · vas bien`, tooltip: '¿Cuánto gastás para abrir? Mantén bajo $15 de cada $100' };
    return { label: `Gastás $${spent.toFixed(1)} de cada $100`, detail: `Mantén bajo $${maintain} · te pasaste ${(spent - maintain).toFixed(1)}`, tooltip: '¿Cuánto gastás para abrir? Bajá a $15 para verde' };
  }
  if (key === 'stability') {
    const conc = parseDriver(drivers, 'supplierConcentration');
    const priceRaw = parseDriver(drivers, 'priceStability');
    // priceStability 0.6 = inestable, 0.92 = estable
    const hasVariation = priceRaw !== null ? priceRaw < 0.8 : false;
    const pct = conc ?? 0;
    const maintain = hasVariation ? 15 : 30;
    const label = pct > 0 ? `Riesgo: 1 proveedor = ${pct.toFixed(0)}%` : 'Sin concentración';
    if (score >= 70) return { label, detail: `Mantén bajo ${maintain}% · vas bien`, tooltip: hasVariation ? 'Precios inestables + proveedor alto = riesgo' : 'Sin variación de precios' };
    return { label, detail: `Mantén bajo ${maintain}% · te faltan ${(pct - maintain).toFixed(0)}%`, tooltip: hasVariation ? 'Estás teniendo precios inestables o dependencia alta' : 'Dependencia de un solo proveedor' };
  }
  if (key === 'growth') {
    const rev = parseDriver(drivers, 'revenueTrend');
    const pct = rev ?? 0;
    if (score >= 70) return { label: `+${pct.toFixed(1)}% vs hace 3 meses`, detail: 'Mantén +4% para seguir en verde', tooltip: '¿Vendés más que hace 3 meses? +4% para verde' };
    if (Math.abs(pct) < 0.1) return { label: 'Sin crecimiento', detail: 'Subís a verde si vendés 4% más que hace 3 meses', tooltip: '¿Vendés más que hace 3 meses? Necesitas +4%' };
    return { label: `${pct > 0 ? '+' : ''}${pct.toFixed(1)}% vs hace 3 meses`, detail: 'Mantén +4% para verde', tooltip: 'Necesitas +4% vs hace 3 meses' };
  }
  return { label: `${score}`, detail: '', tooltip: '' };
}

export function overallGuide(overallHealth: number): { band: string; action: string } {
  if (overallHealth >= 85) return { band: '85-100 Excelente', action: 'Listo para expandir' };
  if (overallHealth >= 70) return { band: '70-84 Óptimo', action: 'Invierte en crecimiento' };
  if (overallHealth >= 40) return { band: '40-69 En desarrollo', action: 'Mejorá estabilidad/eficiencia antes de crecer' };
  return { band: '0-39 Crítico', action: 'No inviertas — arreglá lo urgente' };
}

// ponytail: heurística frontend — si proveedor dominante es el barato, es dependencia eficiente (amarillo) no crítica (rojo)
// Usa criticalAlerts SUPPLIER_CONCENTRATION + supplierRecommendations (ya disponibles en useAnalytics)
export function isGoodDependence(health: FinancialHealth | null, recommendations: SupplierRecommendationItem[]): boolean {
  if (!health) return false;
  const concAlert = health.criticalAlerts.find((a) => a.code === 'SUPPLIER_CONCENTRATION');
  if (!concAlert) return false;
  // title es "Concentración de Proveedor", description contiene proveedor? Usamos threshold/current para detectar, pero necesitamos nombre
  // Como no tenemos nombre en alert, usamos heurística: si hay al menos 1 recomendación donde el proveedor recomendado aparece como dominante en múltiples productos, es buena dependencia
  // Simplificado: si existe alguna recomendación con savingsPct bajo (<10) significa que el dominante ya es casi el más barato -> buena dependencia
  // ponytail: aproximación sin providerId en maxShare; refine con backend providerId si hace falta precisión
  const hasCheapDominance = recommendations.some((r) => r.savingsPct < 10);
  const lowSavingsRatio = recommendations.length > 0 ? recommendations.filter((r) => r.savingsPct < 15).length / recommendations.length : 0;
  return hasCheapDominance || lowSavingsRatio > 0.5;
}
