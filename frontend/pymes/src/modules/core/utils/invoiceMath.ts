// ponytail: pure invoice math — same rule as InvoiceCalculator.java: discount before ITBMS, HALF_UP to 2 decimals
export function calcNeto(cantidad: number | null, valor: number | null, descuentoPct: number | null): number {
  const q = cantidad || 0
  const v = valor || 0
  const d = descuentoPct || 0
  if (!q || !v) return 0
  return q * v * (1 - d / 100)
}

export function calcItbms(neto: number, tasa: number | null | undefined): number {
  const t = tasa ?? 0
  if (t === 0 || !neto) return 0
  // HALF_UP to 2 decimals — matches backend BigDecimal.setScale(2, HALF_UP)
  return Math.round((neto * t) / 100 * 100) / 100
}

export function calcBreakdown(items: { cantidad: number | null; valor: number | null; descuento: number; itbmsTasa: number }[]) {
  let exento = 0
  let gravado = 0
  let itbms = 0
  for (const it of items) {
    const neto = calcNeto(it.cantidad, it.valor, it.descuento)
    if (!neto) continue
    const tasa = it.itbmsTasa ?? 0
    if (tasa === 0) exento += neto
    else {
      gravado += neto
      itbms += calcItbms(neto, tasa)
    }
  }
  // avoid floating noise — round intermediates already to 2, sum then round
  return {
    exento: Math.round(exento * 100) / 100,
    gravado: Math.round(gravado * 100) / 100,
    itbms: Math.round(itbms * 100) / 100,
    total: Math.round((exento + gravado + itbms) * 100) / 100,
  }
}
