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
