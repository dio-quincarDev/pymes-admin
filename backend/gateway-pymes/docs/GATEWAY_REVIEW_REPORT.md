# Gateway Review Report

Hallazgos del code review usando skills: java-springboot, spring-webflux-testing, spring-testing-fundamentals, spring-security-testing.

---

## Resumen

| Severidad | Cantidad | Resueltos |
|-----------|----------|-----------|
| Critico | 1 | ✅ 1 |
| Medio | 5 | ✅ 5 |
| Bajo | 3 | ⬜ 3 |

---

## CRITICO

### C1 — AuthenticationFilter ausente en profiles

AuthenticationFilter esta definido en application.yaml base. Los profiles dev/stg/prod reemplazan la lista de rutas y omiten el filtro. En esos perfiles, /api/v1/** no valida JWT.

Estado: ✅ RESUELTO — agregado `filters: [PreserveHostHeader, AuthenticationFilter]` a auth-service en los 3 profiles.

Archivos: application-dev.yaml, application-stg.yaml, application-prod.yaml

---

## MEDIO

### C2 — Doble parsing del JWT

AuthenticationFilter.parsea el token en isInvalid() y luego nuevamente en getClaims(). La misma operacion dos veces por request.

Estado: ✅ RESUELTO — try-catch con getClaims() una sola vez, claims reusados.

Archivo: AuthenticationFilter.java:54-60

### C3 — Null claims generan string "null"

String.valueOf(claims.get("userId")) produce "null" literal si el claim no existe en el JWT. Ese string viaja a los servicios downstream como header.

Estado: ✅ RESUELTO — null-check ternario en userId, tenantId y role.

Archivo: AuthenticationFilter.java:63-66

### T1 — Cero tests unitarios

Sin tests para JwtUtils, AuthenticationFilter, RouterValidator.

Estado: ✅ RESUELTO — 32 tests unitarios creados (4 JwtUtils + 21 RouterValidator + 7 AuthenticationFilter).

Archivos: JwtUtilsTest.java, AuthenticationFilterTest.java, RouterValidatorTest.java

### T2 — Cero tests de integracion

No hay WebTestClient, Testcontainers, ni Redis real. El flujo de blacklist en Redis nunca se prueba contra una instancia real.

Estado: ⬜ PENDIENTE — requiere Testcontainers + reactor-test. Agregar cuando CI tenga Redis disponible.

### F1 — RouterValidator sin cobertura

La whitelist de endpoints publicos no tiene tests que verifiquen que cada ruta esta correctamente clasificada.

Estado: ✅ RESUELTO — 21 tests parametrizados (14 open + 7 secured) en RouterValidatorTest.

Archivo: RouterValidatorTest.java

---

## BAJO

### C4 — Sin validacion de issuer/audience

JwtUtils solo verifica firma y expiracion. No valida iss ni aud.

Estado: ⬜ SKIPPED — auth service ya valida internamente (defensa en profundidad no crítica).

Archivo: JwtUtils.java

### C5 — Ruta whitelist sin route en YAML

/api/v1/auth/oauth2/** esta en RouterValidator.openEndPoints pero no hay una ruta en los YAMLs que referencie ese path.

Estado: ✅ NO ACCIÓN — la ruta `auth-service-api` con `Path=/api/v1/**` ya lo cubre + whitelist salta el filtro.

Archivo: RouterValidator.java

### T3 — Sin StepVerifier

El gateway usa WebFlux pero ningun test emplea StepVerifier o WebTestClient.

Estado: ⬜ PENDIENTE — agregar reactor-test dep + WebTestClient cuando se hagan tests de integración (T2).

---

## Archivos involucrados

MODIFICADOS (4):
  application-dev.yaml          ✅ +AuthenticationFilter
  application-stg.yaml          ✅ +AuthenticationFilter
  application-prod.yaml         ✅ +AuthenticationFilter
  AuthenticationFilter.java     ✅ parseo único + null-safe claims

CREADOS (3):
  JwtUtilsTest.java             ✅ 4 tests (valido, expirado, firma invalida, malformado)
  RouterValidatorTest.java      ✅ 21 tests parametrizados (14 open + 7 secured)
  AuthenticationFilterTest.java ✅ 7 tests (whitelist, 401s, claims null y con valores)

PENDIENTES:
  GatewayPymesApplicationTests.java (WebTestClient + Testcontainers Redis)

---
Actualizado: 2026-06-17
Creado: 2026-06-16 — Code review session
