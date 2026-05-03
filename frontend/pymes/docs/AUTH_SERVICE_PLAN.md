# 📋 PLAN DE MEJORA - Auth Service Frontend (2026-05-03)

## 🔍 Problemas Identificados

| # | Problema | Ubicación | Severidad |
|---|---------|----------|----------|
| 1 | **Botón "Iniciar Sesión" no navega** | LandingLayout.vue:14 | ALTA |
| 2 | **AuthOptionsPage innecesaria** | route `/auth-options` | MEDIA |
| 3 | **Slug visible en workflow** | IndexPage → RegisterPage | MEDIA |
| 4 | **Workflow fragmentado** | Múltiples páginas | BAJA |
| 5 | **Código Facebook残留** | AuthOptionsPage, routes | BAJA |
| 6 | **Registro sin confirmar password** | RegisterPage | BAJA |

---

## 🎯 ANÁLISIS DEL PROBLEMA #1: Login no funciona

### Estructura Actual del Routing

```typescript
// router/routes.ts
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/LandingLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      ...authRoutes,  // ← <-- AQUÍ ESTÁ EL PROBLEMA
    ],
  },
```

### Causa Raíz

- `LandingLayout` usa `<q-layout view="lHh Lpr lFf">`
- `LoginPage.vue` también usa `<q-layout view="lHh Lpr lFf">`
- **Conflicto:** Dos layouts anidados con el mismo view

### Solución Propuesta

**Opción A:** Extraer `/login` del nested children y hacerlo route independiente

**Opción B:** Usar `LandingLayout` para login sin children nesting

---

## 🎯 PLAN DE IMPLEMENTACIÓN

### FASE 1: Limpieza Crítica (Inmediata)

| # | Tarea | Descripción |
|---|------|------------|
| 1.1 | Arreglar routing Login | Extraer login del nested layout |
| 1.2 | Eliminar AuthOptionsPage | Página innecesaria |
| 1.3 | Limpiar código Facebook | Código residual |

### FASE 2: Mejora de UX Registro

| # | Tarea | Descripción |
|---|------|------------|
| 2.1 | Slug automático | IndexPage genera slug internamente |
| 2.2 | Confirmar password | Agregar campo en RegisterPage |

### FASE 3: Unificación (Futuro)

| # | Tarea | Descripción |
|---|------|------------|
| 3.1 | Branding consistente | Mismo estilo en todas las páginas |
| 3.2 | Google OAuth directo | Botón desde IndexPage |

---

## 📋 Archivos a Modificar

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `router/routes.ts` | MODIFICAR | Extraer login del nested |
| `modules/auth/router/routes.ts` | MODIFICAR | Eliminar duplicados |
| `layouts/LandingLayout.vue` | VERIFICAR | Botón ya tiene `to="/login"` |
| `pages/IndexPage.vue` | MEJORAR | Slug automático |
| `pages/RegisterPage.vue` | MEJORAR | +confirmar password |
| `pages/AuthOptionsPage.vue` | ELIMINAR | Página innecesaria |

---

## ✅ Checklist de Implementación

### Fase 1 (Inmediata)
- [ ] Arreglar routing Login
- [ ] Eliminar AuthOptionsPage
- [ ] Limpiar Facebook code

### Fase 2 (Próximo Sprint)
- [ ] IndexPage slug automático
- [ ] RegisterPage confirmar password

### Fase 3 (Futuro)
- [ ] Unificar branding
- [ ] Google directo

---

*Documento creado: 2026-05-03*
*Proyecto: Pymeq Admin - Frontend Auth Service*