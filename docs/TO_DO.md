## TO_DO.md

### Core

- [ ] [Alta] Reportes — dashboard consolidado KPIs + alertas (2026-07)
- [ ] [Alta] CRUD configuración tenant (edición) (2026-07)
- [ ] [Media] Integration tests ejecutables en CI (2026-07)
- [ ] [Baja] Refactor Producto → InsumoTemplate (post-MVP)
- [ ] [Baja] Spring Security local JWT (post-MVP)

### Frontend

- [ ] [Alta] Factura descuento porcentaje — input con `%` en vez de `$`, subtotal `qty*price*(1-disc/100)`, save() convierte % a monto (2026-07)
- [ ] [Alta] Factura precio unitario por conversión — al seleccionar presentación con `conversion>1`, auto-calcular `precioUnitario = lastUnitPrice/conversion`. Badge `{presName} = {conv} {baseUnit}`. Cantidad en unidades base (2026-07)
- [ ] [Alta] Quitar listas infinitas — FacturasPage: no cargar `getAll()` productos, usar `search()` paginado por categoría. ProductosPage: tabla con `search()` paginado + filtro categoría (2026-07)
- [ ] [Alta] Tests frontend — composable + component + integration (2026-08)
- [ ] [Baja] SEO — og:image, meta description, JSON-LD (2026-08)
- [ ] [Baja] refreshToken → cookie HttpOnly (post-MVP)

### Gateway

- [ ] [Alta] CORS bug fix (2026-07)
- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [ ] [Baja] Facebook OAuth2 — postergado (Meta no aprobó verificación) (post-MVP)
