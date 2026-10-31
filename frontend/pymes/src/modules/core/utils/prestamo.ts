export const interesTotal = (monto: number, tasa: number, plazo: number) =>
  monto * (tasa / 100) * plazo

export const totalConInteres = (monto: number, tasa: number, plazo: number) =>
  monto + interesTotal(monto, tasa, plazo)

// shortcut: simple interest amortization; upgrade to French (P*r*(1+r)^n/((1+r)^n-1)) when Fase 7 (AMORTIZACION_PRESTAMOS_STRATEGY.md) is implemented
export const cuotaMensual = (monto: number, tasa: number, plazo: number) =>
  plazo > 0 ? totalConInteres(monto, tasa, plazo) / plazo : 0