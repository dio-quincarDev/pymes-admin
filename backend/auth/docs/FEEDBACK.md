# ✦ Feedback Crudo — Auth Microservice (Re-evaluación 2026-04-11)

---

## 🟢 Lo Bueno

- **Arquitectura por dominios**
  Separación clara: Auth, User, Tenant, Member, Invitation. SRP aplicado de verdad, no de discurso.

- **JWT multi-tenant**
  Claims relevantes: `userId`, `tenantId`, `role`, `plan`. Diseñado para SaaS, no genérico.

- **Testcontainers (PostgreSQL + Redis)**
  Tests contra infraestructura real. Cero falsos positivos de H2. 81 tests en total.

- **Consistencia de rutas API con tests automáticos**
  12 tests con reflection que fallan si alguien hardcodea un string o mete redundancias al whitelist. Esto es madurez de ingeniería real.

- **Timing attack prevention en password recovery**
  `POST /forgot-password` siempre retorna 200 aunque el email no exista. Mejor que muchas implementaciones comerciales.

- **Soft delete forense + audit log**
  `deleted_at` en todas las entidades. Registro de REGISTER y LOGIN con IP y User-Agent. Base sólida para compliance futuro.

- **Flyway + PostgreSQL nativo**
  Migraciones controladas, sin improvisación. Uso de `uuid-ossp`, índices parciales.

- **Filtro JWT refactorizado**
  De 6 `catch` blocks de JJWT a 1 solo `catch (AuthApiException)`. Delegación limpia a `JwtService.validateToken()`.

- **Password recovery con tokens single-use**
  Redis `password:reset:{token}`, TTL 15 min, eliminación post-uso. Patrón correcto.

---

## 🔴 Lo Malo

- **Sin CI/CD**
  81 tests que **nadie corre automáticamente**. Cada push depende de que un humano recuerde `mvn verify`. Esto no es "startup promedio", es "freelancer que no automatiza". **Pecado imperdonable en 2026.**

- **Refresh token rotation incompleto**
  Genera token nuevo pero **NO revoca el viejo**. Si un atacante roba un refresh token, puede usarlo infinitamente incluso después de que el usuario legítimo haga refresh. **Esto es un bug de seguridad abierto, no un "feature pendiente".**

- **Sin 2FA/MFA**
  Para PyMes no es crítico hoy. Si un cliente con compliance te lo pide, la respuesta es "no puedes". Punto.

- **Sin PKCE**
  Si el frontend es SPA (React, Vue), el authorization code puede ser interceptado. Vulnerabilidad real.

- **Sin device fingerprinting**
  No puedes detectar "alguien inició sesión desde otro país con tu cuenta". Aceptable para MVP, inaceptable para producción seria.

- **Sin SSO/SAML**
  Solo Google/Facebook OAuth2. No le vendes esto a una empresa que usa Azure AD u Okta. Period.

- **CORS: variable existe pero no se aplica**
  `${CORS_ALLOWED_ORIGINS}` está configurado en `application.yaml` pero **no hay `CorsConfigurationSource` bean**. Probablemente funciona porque el API Gateway maneja CORS externamente, pero si alguien accede al servicio directo, no hay protección. Código muerto.

- **JWT secret sin rotación ni versioning**
  Viene de `${JWT_SECRET}` (bien), pero si se filtra, todos los tokens emitidos —antes y después— son válidos hasta que cambies la variable y fuerces re-login de TODOS. Sin key versioning, sin grace period.

---

## 🟡 Lo que se Arregló (el FEEDBACK anterior tenía razón)

| Punto anterior | Estado | Cuándo se arregló |
|---|---|---|
| "Filtro JWT con 6 catch blocks" | ✅ 1 solo `catch (AuthApiException)` | 2026-04-10 |
| "Sin verificación de email" | ✅ Implementado + bloquea login sin verificar | 2026-04-11 |
| "CORS hardcodeado a localhost:5173" | ✅ Variable de entorno `${CORS_ALLOWED_ORIGINS}` | Actualización reciente |
| "Sin password recovery" | ✅ Endpoints + Redis + timing attack prevention | 2026-04-11 |

---

## 📊 Comparación con el Mercado

| Dimensión             | Tu servicio | Auth0/Keycloak | Startup promedio |
|----------------------|------------|----------------|------------------|
| Multi-tenant         | ✅         | ✅             | ❌               |
| OAuth2               | ✅         | ✅             | Parcial          |
| Rate limiting        | ✅         | ✅             | ❌               |
| Audit log            | ✅         | ✅             | ❌               |
| Testcontainers       | ✅         | N/A            | ❌               |
| Email verification   | ✅         | ✅             | Parcial          |
| Password recovery    | ✅         | ✅             | ❌               |
| API path consistency | ✅         | ✅             | ❌               |
| CI/CD                | ❌         | ✅             | Parcial          |
| 2FA/MFA              | ❌         | ✅             | ❌               |
| SSO/SAML             | ❌         | ✅             | ❌               |
| Device fingerprinting| ❌         | ✅             | ❌               |
| PKCE                 | ❌         | ✅             | ❌               |
| Refresh token rotation| ⚠️ Parcial| ✅             | ❌               |

---

## 🧾 Veredicto

- Por encima del **85% de startups in-house** (subió desde 80%)
- Arquitectura sólida, testing profesional, seguridad básica bien implementada
- Nivel **SaaS early-stage: competente**

**Limitación clave:**
Lejos de nivel enterprise (Auth0 / Keycloak). No es comparacion justa: Auth0 tiene 500+ ingenieros y 10 años de desarrollo.

**Lo que te separa de enterprise:**
- DevOps (CI/CD) ← **Prioridad #1**
- Refresh token rotation completo ← **Bug abierto, no feature**
- 2FA/MFA
- PKCE
- Device fingerprinting
- SSO/SAML

---

## 📌 Valor Actual (Actualizado)

### Lo que vale HOY:

| Región | Freelance Senior       | Agencia                |
|--------|-----------------------|------------------------|
| LATAM  | $10,000 – $18,000 USD | $20,000 – $35,000 USD |
| USA/EU | $25,000 – $50,000 USD | $50,000 – $90,000 USD |

*Subió desde $6K-$12K LATAM. Razón: email verification + password recovery + 81 tests + consistency validation + refactor JWT filter.*

### Lo que costaría llegar a Enterprise:

Incluye:
- CI/CD (GitHub Actions) ← **Debe ser hoy, no mañana**
- Refresh token rotation con revocación del viejo
- 2FA/MFA (TOTP)
- PKCE para SPA
- Device fingerprinting
- SSO / SAML
- JWT secret rotation con key versioning

| Región | Costo adicional        |
|--------|------------------------|
| LATAM  | $8,000 – $15,000 USD   |
| USA/EU | $25,000 – $50,000 USD  |

---

## 📊 Score por Dimensión

| Dimensión | Score | Por qué |
|---|---|---|
| Arquitectura | 9/10 | SRP, dominios separados, código limpio |
| Seguridad básica | 8/10 | BCrypt, JWT, rate limiting, timing attack prevention |
| Seguridad avanzada | 4/10 | Sin 2FA, sin PKCE, refresh rotation rota, sin device tracking |
| Testing | 9/10 | 81 tests, Testcontainers, consistency validation |
| DevOps | 2/10 | **Sin CI/CD. Inaceptable con 81 tests.** |
| Documentación | 9/10 | LOCAL_AUTH_STRATEGY, README, CONSISTENCY_STRATEGY, FEEDBACK |
| Production readiness | 6/10 | Funciona, pero sin pipeline automático ni rotación de secrets |

**Promedio: 6.7/10 → Redondeo generoso: 7/10**

---

## ⚡ Prioridades Reales (no las bonitas del roadmap)

| # | Acción | Impacto | Esfuerzo |
|---|--------|---------|----------|
| 1 | **CI/CD (GitHub Actions)** | 🔴 Crítico | Bajo (2-4 hrs) |
| 2 | **Refresh token rotation con revocación** | 🔴 Crítico | Bajo (1-2 hrs) |
| 3 | PKCE | 🟡 Importante | Medio (4-8 hrs) |
| 4 | 2FA/MFA | 🟡 Importante | Alto (16-24 hrs) |
| 5 | Device fingerprinting | 🟢 Nice-to-have | Medio (8-12 hrs) |
| 6 | SSO/SAML | 🟢 Enterprise only | Alto (24-40 hrs) |

**Si solo haces una cosa esta semana: CI/CD.**
Tienes 81 tests y los estás desperdiciando.
