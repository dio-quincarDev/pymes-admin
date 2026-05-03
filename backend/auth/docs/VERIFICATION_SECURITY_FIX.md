# 🔒 Fix de Seguridad - Flujo de Verificación de Email

> Plan de mitigación para vulnerabilidad token-email mismatch
> Fecha: 2026-04-28
> Estado: PENDIENTE

---

## 📋 Resumen Ejecutivo

### Problema
El flujo de verificación de email del backend permite que cualquier token válido verifique cualquier cuenta, sin validar que el email del query param coincida con el almacenado en Redis.

### Severidad
**Alta** - Vulnerabilidad de security que permite verificación no autorizada de cuentas.

### Solución
Validación cruzada token-email en backend + envío de email en request + respuesta enriquecida.

---

## 🔍 Análisis de Vulnerabilidad

### Flujo Actual (Vulnerable)

```
1. Usuario recibe email con link:
   /verify?token=abc123&email=victima@email.com

2. Frontend (VerifyEmailPage.vue) extrae:
   - token: "abc123"
   - email: "victima@email.com" (DEL QUERY PARAM)

3. Frontend ENVÍA al backend (auth.service.ts:80):
   POST /auth/verify-email
   { token: "abc123" }  ← EMAIL IGNORADO

4. Backend (EmailVerificationServiceImpl.verifyEmail):
   - Busca en Redis: email:verify:abc123 → "cualquier@email.com"
   - Marca como verificado SIN comparar con query param
   - Retorna éxito
```

### Fallas Identificadas

| # | Falla | Impacto |
|---|-------|---------|
| 1 | DTO no acepta email | Token puede verificar cualquier cuenta |
| 2 | No hay validación cruzada | Backend no vincula token-email |
| 3 | UX ciega | Usuario no sabe qué email verifica |
| 4 | Sin confirmación | No puede revertir si hizo clic por error |

---

## ✅ Plan de Mitigación

### Arquitectura de la Solución

```
┌─────────────────────────────────────────────────────────────┐
│                      FRONTEND                               │
│  ═════════════════════════════════════════════════════════════  │
│  1. Extrae token + email del query param                   │
│  2. ENVÍA: { token, email }                               │
│  3. Muestra email antes de verificar                      │
│  4. Confirma éxito mostrando email verificado            │
└─────────────────────────────────────────────────────────────┘
                            ↓
                        API Request
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      BACKEND                                │
│  ═══════════════════════════════════════════════════════════ │
│  1. recibe { token, email }                              │
│  2. Redis: email:verify:{token} → storedEmail              │
│  3. COMPARAR: storedEmail == request.email             │
│     ✓ Si coincide → verificar                             │
│     ✗ Si no coincide → reject                          │
│  4. Retorna: { email, verified: true }                   │
└─────────────────────────────────────────────────────────────┘
```

---

### Tareas - backend/auth

#### Tarea 1: Modificar VerifyEmailRequest DTO

**Archivo:** `src/main/java/auth/pymes/dto/VerifyEmailRequest.java`

```java
public record VerifyEmailRequest(
    String token,
    String email  // AGREGAR - requerido
) {}
```

#### Tarea 2: Crear VerifyEmailResponse DTO

**Archivo:** `src/main/java/auth/pymes/dto/VerifyEmailResponse.java`

```java
public record VerifyEmailResponse(
    String email,
    boolean verified
) {}
```

#### Tarea 3: Modificar EmailVerificationServiceImpl.verifyEmail()

**Archivo:** `src/main/java/auth/pymes/service/impl/EmailVerificationServiceImpl.java`

```java
@Override
@Transactional
public VerifyEmailResponse verifyEmail(String token, String email) {
    String key = VERIFY_KEY_PREFIX + token;
    Object emailObj = redisTemplate.opsForValue().get(key);
    
    if (emailObj == null) {
        throw new EmailVerificationTokenInvalidException();
    }
    
    String storedEmail = emailObj.toString();
    
    // VALIDACIÓN CRUZADA - SEGURIDAD
    if (!storedEmail.equalsIgnoreCase(email)) {
        throw new EmailVerificationTokenInvalidException("Token-email mismatch");
    }
    
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new AuthenticationException(
            CodigoError.USER_NOT_FOUND_BY_EMAIL, email));
    
    if (user.isEmailVerified()) {
        throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
    }
    
    user.markEmailAsVerified();
    userRepository.save(user);
    redisTemplate.delete(key);
    
    return new VerifyEmailResponse(email, true);
}
```

#### Tarea 4: Actualizar AuthApiController.verifyEmail()

**Archivo:** `src/main/java/auth/pymes/controller/impl/AuthApiController.java`

```java
@Override
public ResponseEntity<ApiResponse<VerifyEmailResponse>> verifyEmail(VerifyEmailRequest request) {
    VerifyEmailResponse response = emailVerificationService.verifyEmail(
        request.token(), 
        request.email()
    );
    return ResponseEntity.ok(ApiResponse.ok(response));
}
```

---

### Tareas - frontend/pymes

#### Tarea 5: Modificar authService.verifyEmail()

**Archivo:** `src/modules/auth/services/auth.service.ts`

```typescript
// Antes:
async verifyEmail(token: string) {
  return api.post('/auth/verify-email', { token });
}

// Después:
async verifyEmail(token: string, email: string) {
  return api.post('/auth/verify-email', { token, email });
}
```

#### Tarea 6: Actualizar VerifyEmailPage.vue

**Archivo:** `src/modules/auth/pages/VerifyEmailPage.vue`

```typescript
// Mostrar email antes de verificar
// Enviar token + email
// Mostrar email verificado en respuesta
```

---

## 🧪 Validación Post-Fix

### Tests a ejecutar

```bash
# Backend
./mvnw test -Dtest=EmailVerificationServiceImplTest

# Integration
./mvnw test -Dtest=EmailVerificationIntegrationTest
```

### Casos de prueba

| Caso | Input | Expected |
|------|-------|----------|
| Happy path | token + email válidos que coinciden | 200 OK + { email, verified: true } |
| Token inválido | token no existe en Redis | 400 TOKEN_INVALID |
| Email mismatch | token válido pero email diferente | 400 TOKEN_EMAIL_MISMATCH |
| Email ya verificado | token válido + email correcto | 409 EMAIL_ALREADY_VERIFIED |

---

## 📊 Métricas de Seguridad Post-Fix

- [ ] Token solo verifica email específico
- [ ] Validación cruzada obligatoria
- [ ] Frontend puede confirmar antes de verificar
- [ ] Respuesta enriquecida muestra email verificado
- [ ] Tests actualizados y pasando

---

## 📝Historial

| Fecha | Cambios |
|-------|--------|
| 2026-04-28 | Documento creada - Plan de mitigación |