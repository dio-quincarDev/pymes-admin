## TO_DO.md

### Core

- [ ] [Alta] Reportes — dashboard consolidado KPIs + alertas (2026-07)
- [ ] [Alta] CRUD configuración tenant (edición) (2026-07)
- [ ] [Media] Integration tests ejecutables en CI (2026-07)
- [ ] [Baja] Refactor Producto → InsumoTemplate (post-MVP)
- [ ] [Baja] Spring Security local JWT (post-MVP)

### Frontend — Completado

- [x] [Alta] Factura descuento porcentaje — input `%` en vez de `$`, subtotal formula, save() convierte % a monto
- [x] [Alta] Factura precio unitario por conversión — auto-calcular `precioUnitario / conv`, badge conversión
- [x] [Alta] Quitar listas infinitas — FacturasPage: `search()` por categoría; ProductosPage: tabla paginada
- [x] Spin buttons eliminados — `type="text" inputmode="decimal"` en cantidad/precio/descuento
- [x] Docker healthcheck fix — `localhost` → `127.0.0.1` (IPv6 Alpine)
- [x] Conversion UX — helper text + preview dinámico en ProductosPage

### Frontend — Pendiente (UX/UI Review)

- [ ] [Alta] **Fix UUID visible en formulario** — El `q-select` de producto muestra el UUID crudo cuando el valor seleccionado no está en la lista filtrada. Causa: `map-options` no encuentra la opción y muestra el raw value. Solución: asegurar que `filteredProducts` siempre contenga la opción seleccionada, o usar `option-label` explícito con fallback.
- [ ] [Alta] **Responsive dialog factura** — Los `col-3` en el grid de inputs no responden en pantallas pequeñas. Necesita `col-xs-6 col-sm-3` para que los inputs se reorganicen en mobile.
- [ ] [Alta] **Compactar dialog** — Padding excesivo (`14px 16px 12px` en card + `16px 20px` en body), `standout` en todos los inputs, elementos decorativos innecesarios. Reducir a un layout más denso y funcional.
- [ ] [Media] **Simplificar CategoryTabs** — Volver a chips de Quasar (más compactos y accesibles) en vez de botones custom. Solo mejorar colores y transiciones sutiles.
- [ ] [Media] **No exponer UUIDs en dropdown** — Template del dropdown muestra `opt.category` (código que puede parecer UUID). Mostrar solo `productName` y `sku`, ocultar datos internos.
- [ ] [Baja] **ProductosPage pres-dialog** — Revisar que el layout de presentaciones sea responsive.

### Gateway

- [ ] [Alta] CORS bug fix (2026-07)
- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [ ] [Baja] Facebook OAuth2 — postergado (Meta no aprobó verificación) (post-MVP)
