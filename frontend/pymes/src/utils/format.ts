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
