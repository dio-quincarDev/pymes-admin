# 🔒 Fix de Seguridad - Flujo de Verificación de Email (Frontend)

> Plan de mitigación frontend para vulnerabilidad token-email mismatch
> Fecha: 2026-04-28
> Estado: PENDIENTE

---

## 📋 Resumen

El frontend actual envía solo `{ token }` al backend,ignorando el email del query param. Esto permite que cualquier token válido verifique cualquier cuenta.

### Problema Identificado

```typescript
// auth.service.ts - ACTUAL (vulnerable)
async verifyEmail(token: string) {
  return api.post('/auth/verify-email', { token });
  // email ES IGNORADO a pesar de estar en la URL
}
```

### Solución Requerida

```typescript
// auth.service.ts - CORREGIDO
async verifyEmail(token: string, email: string) {
  return api.post('/auth/verify-email', { token, email });
}
```

---

## ✅ Tareas por Hacer

### Tarea 1: Modificar authService.verifyEmail()

**Archivo:** `src/modules/auth/services/auth.service.ts`

```typescript
// Líneas 25-26 cambiar a:
async verifyEmail(token: string, email: string) {
  return api.post('/auth/verify-email', { token, email });
}
```

### Tarea 2: Actualizar VerifyEmailPage.vue

**Archivo:** `src/modules/auth/pages/VerifyEmailPage.vue`

**Cambios requeridos:**

1. **Línea 80** - Enviar email además del token:
```typescript
await authService.verifyEmail(verificationToken, email.value);
```

2. **Antes del llamado** - Confirmar email al usuario:
```typescript
// Mostrar: "Verificando email: user@email.com"
```

3. **Después del éxito** - Mostrar email verificado:
```typescript
// Mostrar: "Email ver@example.com verificado exitosamente"
```

---

## 🔍 Archivos Involucrados

| Archivo | Cambios |
|---------|--------|
| `src/modules/auth/services/auth.service.ts` | + parámetro email |
| `src/modules/auth/pages/VerifyEmailPage.vue` | UX mejorada |

---

## 📝 Referencia

- Documento técnico completo: `backend/auth/docs/VERIFICATION_SECURITY_FIX.md`
- Estado en reporte: `backend/auth/docs/DAYLY_REPORTS_AUTH_SOLUTIONS.md`

---

## 📝 Historial

| Fecha | Cambios |
|-------|---------|
| 2026-04-28 | Documento creado - Plan de mitigación frontend |