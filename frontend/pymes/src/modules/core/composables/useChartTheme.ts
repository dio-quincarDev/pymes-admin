import { computed } from 'vue'

export function useChartTheme() {
  const getVar = (name: string) =>
    getComputedStyle(document.documentElement).getPropertyValue(`--pq-${name}`).trim()

  const colors = computed(() => ({
    bar: getVar('chart-bar'),
    barHover: getVar('chart-bar-hover'),
    line: getVar('chart-line'),
    area: getVar('chart-area'),
    grid: getVar('chart-grid'),
    text: getVar('chart-text'),
    tooltipBg: getVar('chart-tooltip-bg'),
    tooltipBorder: getVar('chart-tooltip-border'),
    positive: getVar('chart-positive'),
    negative: getVar('chart-negative'),
    abcA: getVar('chart-abc-a'),
    abcB: getVar('chart-abc-b'),
    abcC: getVar('chart-abc-c'),
    info: getVar('info'),
  }))

  const defaults = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: {
          color: colors.value.text,
          font: { family: "'Geist', sans-serif", size: 11 },
        },
      },
      tooltip: {
        backgroundColor: colors.value.tooltipBg,
        titleColor: getVar('text'),
        bodyColor: getVar('text'),
        borderColor: colors.value.tooltipBorder,
        borderWidth: 1,
        titleFont: { family: "'Geist', sans-serif", weight: '600' },
        bodyFont: { family: "'Satoshi', sans-serif" },
        padding: 12,
        cornerRadius: 6,
      },
    },
    scales: {
      x: {
        ticks: {
          color: colors.value.text,
          font: { family: "'Geist Mono', monospace", size: 10 },
        },
        grid: { color: colors.value.grid },
      },
      y: {
        ticks: {
          color: colors.value.text,
          font: { family: "'Geist Mono', monospace", size: 10 },
        },
        grid: { color: colors.value.grid },
      },
    },
  }))

  return {
    colors,
    defaults,
  }
}
