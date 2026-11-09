const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
});

const percentFormatter = new Intl.NumberFormat('en-US', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const numberFormatter = new Intl.NumberFormat('en-US');

const compactCurrencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  notation: 'compact',
  maximumFractionDigits: 1,
});

export function useNumberFormat() {
  return {
    formatCurrency: (value: number) =>
      Number.isFinite(value) ? currencyFormatter.format(value) : '$0.00',
    formatCompactCurrency: (value: number) =>
      Number.isFinite(value) ? compactCurrencyFormatter.format(value) : '$0',
    formatPercent: (value: number) =>
      Number.isFinite(value) ? percentFormatter.format(value / 100) : '0.0%',
    formatNumber: (value: number) => (Number.isFinite(value) ? numberFormatter.format(value) : '0'),
  };
}
