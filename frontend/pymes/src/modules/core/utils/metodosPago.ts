import type { MetodoPago } from '../types';

export interface MetodoPagoOption {
  value: MetodoPago;
  label: string;
}

// Unica fuente de opciones y labels (evita drift como CategoriaGasto en 3 archivos).
export const METODOS_PAGO: MetodoPagoOption[] = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'YAPPY', label: 'Yappy' },
  { value: 'TARJETA', label: 'Tarjeta (PVenta/PCobro)' },
  { value: 'ACH', label: 'ACH' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CREDITO', label: 'Crédito' },
];

export const METODOS_PAGO_VALUES: MetodoPago[] = METODOS_PAGO.map((m) => m.value);

export function labelMetodoPago(m: MetodoPago): string {
  return METODOS_PAGO.find((x) => x.value === m)?.label ?? m;
}
