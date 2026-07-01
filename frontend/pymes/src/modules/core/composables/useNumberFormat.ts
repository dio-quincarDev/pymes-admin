const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
});

const percentFormatter = new Intl.NumberFormat('es-PE', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const numberFormatter = new Intl.NumberFormat('es-PE');

export function useNumberFormat() {
  return {
    formatCurrency: (value: number) => currencyFormatter.format(value),
    formatPercent: (value: number) => percentFormatter.format(value / 100),
    formatNumber: (value: number) => numberFormatter.format(value),
  };
}
