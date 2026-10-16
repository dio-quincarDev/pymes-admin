// ponytail: fórmula del plan de retorno en un solo lugar — la tarjeta y los
// reportes usan LA MISMA lógica (el back la replica; no hay endpoint nuevo).
// Puro y sin imports: se ejecuta directo con `node este-archivo.ts`.
export const PLAZOS = [6, 12, 18] as const;
export type Plazo = (typeof PLAZOS)[number];

// Margen mínimo (% sobre ventas) para calificar cada ritmo.
export const BANDA_MARGEN: Record<Plazo, number> = { 6: 38, 12: 19, 18: 13 };
// Tope: nunca apartar más del 70% de lo que quedó en el mes.
export const TOPE_APORTE = 0.7;

export interface PlazoPlan {
  plazo: Plazo;
  montoFijo: number;
  alcanza: boolean;
  aporte: number;
  calificado: boolean;
}

export type EstadoPlan = 'sin-datos' | 'protege-caja' | 'ok';

export interface PlanRecuperacion {
  estado: EstadoPlan;
  filas: PlazoPlan[];
  calificado: Plazo | null;
}

export function calcularPlanRecuperacion(
  capital: number,
  rentabilidad: number | null,
  margenPct: number | null,
): PlanRecuperacion {
  const vacio: PlanRecuperacion = { estado: 'sin-datos', filas: [], calificado: null };
  if (!(capital > 0) || rentabilidad == null || margenPct == null) return vacio;

  const filas: PlazoPlan[] = PLAZOS.map((plazo) => {
    const montoFijo = capital / plazo;
    const alcanza = rentabilidad > 0 && montoFijo <= rentabilidad;
    // ponytail: aunque no alcance, se aparta lo posible (70%) y el plazo se extiende solo.
    const aporte = rentabilidad > 0 ? Math.min(montoFijo, rentabilidad * TOPE_APORTE) : 0;
    return { plazo, montoFijo, alcanza, aporte, calificado: false };
  });

  if (margenPct < BANDA_MARGEN[18]) {
    return { estado: 'protege-caja', filas, calificado: null };
  }
  const elegido = filas.find((f) => margenPct >= BANDA_MARGEN[f.plazo] && f.alcanza) ?? null;
  if (elegido) elegido.calificado = true;
  return { estado: 'ok', filas, calificado: elegido?.plazo ?? null };
}

// ponytail: el único check — falla si la lógica se rompe. Corre con:
// node src/modules/core/utils/recuperacion.ts
export function verificarRecuperacion(): void {
  const eq = (a: number, b: number) => Math.abs(a - b) < 0.01;
  const fail = (msg: string): never => {
    throw new Error(`recuperacion: ${msg}`);
  };

  // capital 10,000, quedaron 2,000 con margen 40% → ritmo 6m, aparta min(1666.67, 1400).
  const p1 = calcularPlanRecuperacion(10000, 2000, 40);
  if (p1.estado !== 'ok' || p1.calificado !== 6) fail('esperaba 6m calificado');
  const f6 = p1.filas[0];
  if (!f6 || !eq(f6.montoFijo, 10000 / 6) || !f6.alcanza || !eq(f6.aporte, 1400)) {
    fail('monto/aporte 6m');
  }
  if (p1.filas[1]?.calificado || p1.filas[2]?.calificado) fail('solo un calificado');

  // Margen 10% (< 13%) → protege la caja, sin calificado.
  const p2 = calcularPlanRecuperacion(10000, 2000, 10);
  if (p2.estado !== 'protege-caja' || p2.calificado !== null) fail('esperaba protege-caja');

  // Mes que no dio (pérdida) → ningún plazo alcanza, aporte 0.
  const p3 = calcularPlanRecuperacion(10000, -500, -5);
  if (!p3.filas.every((f) => !f.alcanza && f.aporte === 0)) fail('pérdida: aporte 0');

  // Sin datos → estado sin-datos.
  const p4 = calcularPlanRecuperacion(10000, null, null);
  if (p4.estado !== 'sin-datos' || p4.filas.length !== 0) fail('esperaba sin-datos');
  const p5 = calcularPlanRecuperacion(0, 2000, 40);
  if (p5.estado !== 'sin-datos') fail('sin capital: sin-datos');
}

if (typeof process !== 'undefined' && process.argv[1]?.endsWith('recuperacion.ts')) {
  verificarRecuperacion();
}
