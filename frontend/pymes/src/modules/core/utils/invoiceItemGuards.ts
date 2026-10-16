// ponytail: guardias puras de items (spec .ulpi/design/unidades-presentaciones.md) — testeadas en invoiceItemGuards.spec.ts
export const SUELTO = '__SUELTO__'

export interface ItemDraft {
  productoId: string | null
  presentacionId: string | null
  fueSuelto: boolean
  cantidad: number | null
  valor: number | null
}

/** pre-flight en palabras del usuario: producto + (empaque o suelto) + cantidad + valor */
export function isItemComplete(i: ItemDraft): boolean {
  return !!i.productoId && !!(i.presentacionId || i.fueSuelto) && !!i.cantidad && i.cantidad > 0 && i.valor != null
}

/** el centinela '__SUELTO__' nunca viaja: se traduce a presentacionId null + fueSuelto true */
export function toPresentacionPayload(i: Pick<ItemDraft, 'presentacionId' | 'fueSuelto'>): {
  presentacionId: string | null
  fueSuelto: boolean
} {
  return i.fueSuelto ? { presentacionId: null, fueSuelto: true } : { presentacionId: i.presentacionId || null, fueSuelto: false }
}

function round2(n: number): number {
  return Math.round(n * 100) / 100
}

/**
 * ponytail: al cambiar de unidad, el Valor sigue al precio auto-llenado (por unidad base)
 * SOLO si el usuario no lo toco. Agnostico: lee la conversion de cada presentacion,
 * entera o decimal (Bulto 43.2, Caja x24, Galon 3.785...). conv null = suelto (x1).
 */
export function scaledPriceOnPresentationChange(
  currentValor: number | null,
  basePrice: number | null,
  newConv: number | null,
  prevConv: number | null,
): number | null {
  if (basePrice == null) return currentValor
  const prev = prevConv ?? 1
  const next = newConv ?? 1
  const untouched = currentValor == null || round2(currentValor) === round2(basePrice * prev)
  return untouched ? round2(basePrice * next) : currentValor
}
