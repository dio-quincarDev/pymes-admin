export const tokens = {
  color: {
    background: '#08090D',
    surface: '#12141A',
    elevated: '#1E2129',
    border: '#353945',
    text: '#F5F3EF',
    textMuted: '#9B9790',
    textSubtle: '#6B6863',
    accent: '#C8963E',
    accentHover: '#D4A552',
    accentMuted: '#8B6B3A',
    success: '#3D7A5A',
    warning: '#C8A042',
    danger: '#A04038',
    info: '#6E8BB8',
  },
  radius: {
    xs: '2px',
    sm: '4px',
    md: '6px',
    lg: '8px',
    xl: '12px',
    full: '9999px',
  },
  spacing: {
    '4xs': '4px',
    '3xs': '6px',
    '2xs': '8px',
    xs: '12px',
    sm: '16px',
    md: '24px',
    lg: '32px',
    xl: '40px',
    '2xl': '48px',
    '3xl': '64px',
  },
  shadow: {
    subtle: '0 1px 3px rgba(0,0,0,0.3)',
    md: '0 8px 24px rgba(0,0,0,0.25)',
    lg: '0 16px 48px rgba(0,0,0,0.3)',
  },
  motion: {
    fast: '80ms ease',
    base: '160ms cubic-bezier(0.4, 0, 0.2, 1)',
    emphasis: '240ms cubic-bezier(0.16, 1, 0.3, 1)',
  },
  z: {
    base: 0,
    dropdown: 100,
    sticky: 200,
    fixed: 300,
    modalBackdrop: 400,
    modal: 500,
    popover: 600,
    tooltip: 700,
    skipLink: 800,
  },
  type: {
    display: "'Geist', -apple-system, BlinkMacSystemFont, sans-serif",
    body: "'Satoshi', -apple-system, BlinkMacSystemFont, sans-serif",
    utility: "'Geist Mono', ui-monospace, 'SF Mono', monospace",
  },
} as const

export type TokenColor = keyof typeof tokens.color
export type TokenRadius = keyof typeof tokens.radius
export type TokenSpacing = keyof typeof tokens.spacing
export type TokenShadow = keyof typeof tokens.shadow

export const cssVar = (name: string) => `var(--pq-${name})`
