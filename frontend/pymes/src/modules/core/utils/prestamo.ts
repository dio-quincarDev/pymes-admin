export const interesTotal = (monto: number, tasa: number, plazo: number) =>
  monto * (tasa / 100) * plazo

export const totalConInteres = (monto: number, tasa: number, plazo: number) =>
  monto + interesTotal(monto, tasa, plazo)

export const cuotaMensual = (monto: number, tasa: number, plazo: number) =>
  plazo > 0 ? totalConInteres(monto, tasa, plazo) / plazo : 0