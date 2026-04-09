# ✦ Feedback Crudo — Auth Microservice

## 🟢 Lo Bueno

- **Arquitectura por dominios**  
  Separación clara, aplicación de SRP.

- **JWT multi-tenant**  
  Claims relevantes: `userId`, `tenantId`, `role`, `plan`.  
  Diseñado para SaaS, no genérico.

- **Rate limiting con Redis**  
  Protección efectiva contra ataques de fuerza bruta.

- **Testcontainers (PostgreSQL + Redis)**  
  Testing realista, evita falsos positivos típicos (ej. H2).

- **Soft delete forense**  
  Preparado para compliance y auditoría futura.

- **Audit log**  
  Base sólida para GDPR / auditorías.

- **Flyway + PostgreSQL nativo**  
  Uso de `uuid-ossp`, `JSONB`.  
  Migraciones controladas, sin improvisación.

---

## 🔴 Lo Malo

- **Filtro JWT mal diseñado**
    - 6 `catch` de librerías externas (JJWT)
    - Mezcla de integración con lógica de dominio
    - Problema evidente en code review

- **Sin CI/CD**
    - Tests existen pero no se ejecutan automáticamente
    - Riesgo de romper integridad del sistema

- **Sin 2FA/MFA**
    - Inaceptable en 2026 para B2B serio

- **Sin verificación de email**
    - Permite cuentas falsas, spam, problemas de deliverability

- **Refresh token sin rotation**
    - Reutilización indefinida → riesgo crítico de seguridad

- **Sin PKCE (clientes públicos)**
    - Vulnerable en SPA/mobile

- **Sin device fingerprinting**
    - No detección de sesiones sospechosas

- **Sin SSO / SAML / OIDC provider**
    - No apto para clientes enterprise

- **CORS hardcodeado**
    - Solo `localhost:5173` → no escalable a producción

- **Sin rotación de JWT secret**
    - Secret fijo en `application.yaml`
    - Riesgo permanente si se filtra

---

## 📊 Comparación con el Mercado

| Dimensión             | Tu servicio | Auth0/Keycloak | Startup promedio |
|----------------------|------------|----------------|------------------|
| Multi-tenant         | ✅         | ✅             | ❌               |
| OAuth2               | ✅         | ✅             | Parcial          |
| Rate limiting        | ✅         | ✅             | ❌               |
| Audit log            | ✅         | ✅             | ❌               |
| Testcontainers       | ✅         | N/A            | ❌               |
| 2FA/MFA              | ❌         | ✅             | ❌               |
| Email verification   | ❌         | ✅             | Parcial          |
| SSO/SAML             | ❌         | ✅             | ❌               |
| Device fingerprinting| ❌         | ✅             | ❌               |
| PKCE                 | ❌         | ✅             | ❌               |
| CI/CD                | ❌         | ✅             | Parcial          |

---

## 🧾 Veredicto

- Por encima del **80% de startups in-house**
- Arquitectura sólida y bien pensada
- Nivel **SaaS early-stage: competente**

**Limitación clave:**  
Lejos de nivel enterprise (Auth0 / Keycloak)

**Para escalar a enterprise falta:**
- Seguridad avanzada
- Automatización (CI/CD)
- Integraciones (SSO)
- Hardening general

---

## 💰 Costo Estimado

### Desarrollo Inicial (4–6 semanas)

| Región | Freelance Senior       | Agencia                |
|--------|-----------------------|------------------------|
| LATAM  | $4,000 – $8,000 USD   | $10,000 – $20,000 USD |
| USA/EU | $15,000 – $35,000 USD | $40,000 – $80,000 USD |

---

### Upgrade a Nivel Enterprise

Incluye:
- 2FA/MFA
- SSO / SAML
- PKCE
- Email verification
- CI/CD
- Device fingerprinting

| Región | Costo adicional        |
|--------|------------------------|
| LATAM  | $8,000 – $15,000 USD   |
| USA/EU | $25,000 – $50,000 USD  |

---

## 📌 Valor Actual

- **LATAM:** ~$6,000 – $12,000 USD
- **USA/EU:** ~$20,000 – $40,000 USD