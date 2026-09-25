const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
})

const pctFormatter = new Intl.NumberFormat('en-US', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

export const formatCurrency = (n: number) =>
  Number.isFinite(n) ? currencyFormatter.format(n) : '$0.00'
export const formatPct = (n: number) =>
  Number.isFinite(n) ? pctFormatter.format(n / 100) : '0.0%'

// ponytail: local date (America/Panama UTC-5 sin DST) — evita desfase de toISOString UTC
export const toLocalISODate = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export const formatDate = (dateStr: string, withYear = false) => {
  const d = new Date(dateStr.includes('T') ? dateStr : dateStr + 'T00:00:00')
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('es-PE', {
    day: 'numeric',
    month: 'short',
    ...(withYear ? { year: 'numeric' } : {}),
  })
}
