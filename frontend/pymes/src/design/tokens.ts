export const tokens = {
  color: {
    background: '#0B1210',
    surface: '#14211D',
    elevated: '#1B2C26',
    border: '#4D7063',
    text: '#F2F5F1',
    textMuted: '#A9B6AF',
    textSubtle: '#829189',
    accent: '#A3785E',
    accentHover: '#B78B70',
    accentMuted: '#7A5B48',
    success: '#4E8A6D',
    warning: '#C49A52',
    danger: '#C06A60',
    info: '#6F94A8',
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
