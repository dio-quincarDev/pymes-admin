# Estrategia de pruebas entre microservicios

## Modelo combinado

| Tipo | Cuándo | Qué usa | Qué valida |
|---|---|---|---|
| **Pruebas por microservicio** | Cada PR | WireMock | Contratos, manejo de errores, rutas |
| **End-to-end** | Nightly | Todos los servicios reales | Integración completa entre microservicios |

## Pruebas por microservicio (WireMock)

Cada microservicio simula las respuestas de sus dependencias. No levanta otros containers.

**Cómo funciona:**
1. Test levanta containers propios (PostgreSQL, Redis)
2. WireMock simula respuestas del servicio dependiente
3. Se valida que el microservicio procesa correctamente las respuestas

**Qué valida:** Rutas, manejo de errores, timeouts, retries

**Qué no valida:** Que el servicio real funcione ni el formato exacto de sus respuestas

**Costo:** WireMock es un JAR local. Sin overhead de containers extra.

**Riesgo:** Si el servicio real cambia, el mock queda desactualizado. Se valida contra el servicio real en el e2e nightly.

## Pruebas end-to-end (Nightly)

Todos los microservicios reales corren juntos. Se ejecuta un flujo completo.

**Cómo funciona:**
1. Se levanta el compose completo con todos los servicios
2. Se envía un request por el gateway
3. Se valida que traverse auth → servicios intermedios → respuesta final
4. Se destruye el compose al terminar

**Qué valida:** Que todo funciona junto. Atrapa drift entre APIs, problemas de red, incompatibilidades de versión.

**Costo:** Un solo job secuencial. Lento. Corre de noche para no bloquear PRs.

## En CI

- **Cada PR:** Jobs paralelos por microservicio con WireMock. Rápido.
- **Nightly:** Un job secuencial levanta compose completo → corre flujo integral → destruye todo.

## Regla

WireMock para desarrollo diario. E2E para validar que no hay drift entre servicios.
