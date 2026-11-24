// ponytail: deriva guía "mantén" invirtiendo scoring de AnalyticsServiceImpl — sin hardcodear tabla fija
import type { FinancialHealthBreakdown } from '../types/analytics';
import { formatCurrency } from '../../../utils/format';

function parseDriver(drivers: string[], key: string): number | null {
  const row = drivers.find((d) => d.toLowerCase().includes(key.toLowerCase()));
  if (!row) return null;
  const m = row.match(/(-?\d+(\.\d+)?)/);
  return m ? Number(m[1]) : null;
}

function driverText(drivers: string[], key: string): string | null {
  const prefix = `${key}: `;
  return drivers.find((d) => d.startsWith(prefix))?.slice(prefix.length) ?? null;
}

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const nombreMes = (periodo: string) => MESES[Number(periodo.slice(5, 7)) - 1] ?? periodo;

export interface GuideLine {
  label: string;
  detail: string;
  tooltip: string;
  sinDatos?: boolean;
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
    // ponytail: sin el driver no hay 3 meses cerrados — no es 0% de crecimiento
    if (rev === null) return { label: 'Aún no hay historial', detail: 'Se mide con 3 meses cerrados', tooltip: 'El mes en curso no cuenta: comparamos meses cerrados.', sinDatos: true };
    const desde = driverText(drivers, 'growthFrom');
    const hasta = driverText(drivers, 'growthTo');
    const montoDesde = driverText(drivers, 'growthFromAmount');
    const montoHasta = driverText(drivers, 'growthToAmount');
    const ventana = desde && hasta
      ? `${nombreMes(hasta).replace(/^./, (c) => c.toUpperCase())} vs ${nombreMes(desde)}`
      : 'vs meses anteriores';
    const detalle = montoDesde && montoHasta
      ? `${formatCurrency(Number(montoDesde))} → ${formatCurrency(Number(montoHasta))}`
      : 'Mantén +4% para verde';
    return { label: `${ventana}: ${rev > 0 ? '+' : ''}${rev.toFixed(1)}%`, detail: detalle, tooltip: 'Comparamos el último mes cerrado con el de dos meses antes. El mes en curso no cuenta hasta que termine.' };
  }
  return { label: `${score}`, detail: '', tooltip: '' };
}

export function overallGuide(overallHealth: number): { band: string; action: string } {
  if (overallHealth >= 85) return { band: '85-100 Excelente', action: 'Listo para expandir' };
  if (overallHealth >= 70) return { band: '70-84 Óptimo', action: 'Invierte en crecimiento' };
  if (overallHealth >= 40) return { band: '40-69 En desarrollo', action: 'Mejorá estabilidad/eficiencia antes de crecer' };
  return { band: '0-39 Crítico', action: 'No inviertas — arreglá lo urgente' };
}

