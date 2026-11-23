# Guía de usuario

> Para propietarios y colaboradores que usan la app. Si gestionas usuarios, permisos o
> secretos, va en la [guía de administración](admin-guide.md).

## Acceso

La app tiene registro e ingreso con correo y contraseña, e ingreso con Google/Facebook.
Después de registrarte debes **verificar el correo**. Si olvidaste la contraseña, usa
"¿Olvidaste tu contraseña?" en `/login`. Si un administrador te invitó, abre el enlace
de invitación (`/accept-invitation`) para crear tu cuenta y unirte al equipo.

Todas las pantallas de acceso están bajo `/login`, `/register`, `/verify`,
`/forgot-password`, `/reset-password` y `/accept-invitation`.

`Source: frontend/pymes/src/modules/auth/router/routes.ts`

## Primer ingreso: configurar el negocio

Al entrar sin negocio configurado, la app te lleva a `/onboarding`. Son **2 pasos**:

1. **Configura tu empresa** — eliges el rubro: Restaurante, Bares y Cantinas,
   Salón de Belleza, Ferretería, Mini Super, Taller Mecánico, Farmacia o General.
2. **Preview de tu plantilla** — ves la plantilla que se crea para ese rubro y confirmas.

`Source: frontend/pymes/src/modules/core/pages/OnboardingPage.vue`

## La pantalla principal

El menú tiene tres grupos (`Source: frontend/pymes/src/layouts/MainLayout.vue`):

| Grupo | Pantallas |
|-------|-----------|
| **Operaciones** | Dashboard, Productos, Proveedores, Facturas, Ventas |
| **Análisis** | Análisis, Préstamos, Costos, Inversión |
| **Sistema** | Equipo (solo OWNER y ADMIN) |

El **Dashboard** es el resumen del mes: semáforo de estado, actividad reciente,
facturas pendientes y un desglose por categoría. En la celda de salud financiera
verás la alerta principal del mes (con un enlace "Revisar" a Análisis) o el mensaje
"Sin riesgos críticos este mes", y el panel con los cuatro pilares:
**Rentabilidad, Eficiencia, Estabilidad, Crecimiento**.

### Cómo leer el pilar Crecimiento

Crecimiento compara **solo meses cerrados**; descarta el mes en curso para no comparar
un mes a medio terminar contra uno completo. Si hay **menos de 3 meses cerrados**, el
pilar muestra 50 neutro y la tarjeta dice **"Aún no hay historial"** — no es un dato
de rendimiento, es falta de datos. Con historial, el driver nombra el período real
comparado (por ejemplo, "de marzo a abril").

`Source: frontend/pymes/docs/strategies/FINANCIAL_HEALTH_STRATEGY.md`, `backend/core/docs/CORE.md`

## Registro diario

- **Productos y Proveedores** — catálogos base.
- **Facturas** — compras con ITBMS por ítem. El total separa subtotal exento,
  gravado e ITBMS total.
- **Ventas** — ventas diarias.
- **Costos** — gastos fijos y costos OPERACIONALES.
- **Préstamos e Inversión** — deuda y patrimonio.
- **Análisis** — donde se ven las alertas accionables y el detalle de gastos.

`Source: frontend/pymes/src/modules/core/router/routes.ts`, `backend/core/docs/CORE.md`

## Reporte al propietario

El sistema genera un **PDF mensual** (y un trimestral) para el OWNER con ranking de
productos por dinero, ROI, métodos de pago y comparativas cuando hay historial.
Se envía automáticamente el día 1 a las 06:00 (hora Panamá). Si no llega, el
procedimiento de reenvío está en la [guía de operación](operations-guide.md#reenviar-un-reporte-ya-enviado).

`Source: docs/DAILY_REPORTS_PROJECT.md`, `backend/core/docs/CORE.md`

## Estado de las funciones

| Función | Estado |
|---------|--------|
| Acceso, onboarding, registro diario, dashboard y reportes | **Implemented** |
| Escaneo de QR en facturas (PWA) | **Planned** — roadmap Q3 2026 |
| Detección básica de anomalías | **Planned** — roadmap Q3 2026 |

`Source: README.md` (Roadmap)
