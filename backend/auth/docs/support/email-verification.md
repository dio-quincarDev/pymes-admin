## Documentation: Email Verification Service

### Technical Overview
* **Component:** `EmailVerificationServiceImpl`
* **Infrastructure:** Redis (Ephemeral token storage), PostgreSQL (Account status), JavaMail (SMTP/Mime).
* **Responsibility:** Management of the account activation lifecycle and identity validation via email.
* **Mechanism:** Opaque token generation with high entropy, mapped to user identity in an in-memory store for rapid validation.

---

### Verification Flow

The service ensures that users cannot access protected resources until their email address is confirmed, following a multi-step secure process.

#### 1. Token Orchestration
* **Entropy:** Generates 256-bit (32-byte) tokens using `SecureRandom`, formatted as Hex strings.
* **Storage:** Maps the token to the user's email in Redis with the prefix `email:verify:`.
* **TTL (Time To Live):** Hardcoded to **15 minutes** to minimize the window for potential interception.

#### 2. Messaging & Delivery
* **Engine:** `JavaMailSender` using HTML `MimeMessage`.
* **Configuration:** * `app.frontend.url`: Base URL for constructing activation links.
    * `spring.mail.username`: Authenticated sender address.
* **Security:** Links point to the frontend application, ensuring the verification token is processed within the application context.

---

### Core Workflows

#### Registration/Resend Workflow
1.  **Identity Lookup:** Verifies the user exists and is currently unverified.
2.  **Token Generation:** Creates a new secure token and resets the 15-minute TTL.
3.  **Communication:** Builds an HTML template with an embedded verification link.
4.  **Audit:** Logs the generation and successful dispatch of the verification email.

#### Activation Workflow (`verifyEmail`)
1.  **Lookup:** Retrieves the associated email from Redis using the provided token.
2.  **Validation:** * Throws `EmailVerificationTokenInvalidException` if the token is expired or non-existent.
    * Throws `DuplicateResourceException` if the user is already verified (preventing double-processing).
3.  **Persistence:** Updates the `isEmailVerified` flag in the `UserEntity` via `markEmailAsVerified()`.
4.  **Cleanup:** Immediately deletes the token from Redis to prevent replay.

---

### Technical Specifications

| Feature | Detail |
| :--- | :--- |
| **Token Format** | 64-character Hex string (32 bytes) |
| **Storage Type** | String (Redis Key-Value) |
| **Email Format** | Multipart HTML (UTF-8) |
| **Key Invalidation** | Immediate upon successful verification |

---

### Security Logic

#### Anti-Replay & Expiration
Tokens are strictly **one-time use**. Even if a user attempts to use a valid token twice within the 15-minute window, the `redisTemplate.delete(key)` call ensures subsequent attempts fail.

#### Acount Integrity
The service acts as a guardrail for the `AuthService`. By updating the `isEmailVerified` state in PostgreSQL, it enables the `login` method to reject authentication attempts for unverified identities, mitigating the risk of bot-created accounts.

#### HTML Template Policy
The email template utilizes inline CSS and a clean hierarchy (Header, Content, CTA Button, Footer) to ensure maximum compatibility across different mail clients (Outlook, Gmail, Mobile) while maintaining professional branding.

---

### Testing

#### Unit Tests (`EmailVerificationServiceImplTest`)

**Location:** `src/test/java/auth/pymes/unit/EmailVerificationServiceImplTest.java`

**Mocks:**
- `RedisTemplate<String, Object>` - Token storage
- `ValueOperations<String, Object>` - Redis operations
- `UserEntityRepository` - User persistence
- `JavaMailSender` - Email sending

**Test Coverage:**
| Test | Scenario |
|------|----------|
| `generateVerificationToken_ReturnsTokenAndStoresInRedis` | Token generation and Redis storage |
| `verifyEmail_WithValidToken_MarksUserAsVerified` | Successful email verification |
| `verifyEmail_WithInvalidToken_ThrowsEmailVerificationTokenInvalidException` | Invalid/expired token handling |
| `verifyEmail_WithAlreadyVerifiedUser_ThrowsDuplicateResourceException` | Duplicate verification prevention |
| `verifyEmail_UserNotFoundInDB_ThrowsAuthenticationException` | Orphan token handling |
| `resendVerificationToken_GeneratesNewTokenForUnverifiedUser` | Token regeneration |
| `resendVerificationToken_ForVerifiedUser_ThrowsDuplicateResourceException` | Verified user cannot resend |
| `resendVerificationToken_ForNonExistentUser_ThrowsAuthenticationException` | Non-existent user handling |

**Key Setup:**
```java
@BeforeEach
void setUp() {
    emailVerificationService = new EmailVerificationServiceImpl(redisTemplate, userRepository, mailSender);
    ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:9200");
    ReflectionTestUtils.setField(emailVerificationService, "fromEmail", "noreply@pymes.com");

    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
}
```

#### Integration Tests (`AuthApiIntegrationTest$EmailVerificationTests`)

**Location:** `src/test/java/auth/pymes/integration/api/AuthApiIntegrationTest.java`

**Infrastructure:**
- Testcontainers for PostgreSQL and Redis
- Mocked `JavaMailSender` to avoid SMTP dependencies
- `application-integration.yaml` profile

**Test Coverage:**
| Test | Scenario |
|------|----------|
| `verifyEmailWithInvalidToken` | Returns 400 for invalid token |
| `resendVerificationWithNonExistentEmail` | Returns 404 for unknown email |
| `resendVerificationWithAlreadyVerifiedEmail` | Returns 400 for verified user |
| `loginWithoutVerifiedEmail` | Returns 401 for unverified user |
| `loginWithVerifiedEmail` | Returns 201 after verification |

#### Running Tests

```bash
# Unit tests only
./mvnw test

# Integration tests (requires Docker)
./mvnw verify

# Specific test class
./mvnw test -Dtest=EmailVerificationServiceImplTest
```

#### Common Issues

| Error | Solution |
|-------|----------|
| `mimeMessage is null` | Ensure `JavaMailSender` is properly mocked in test setup |
| `MailHealthContributorAutoConfiguration` failure | Add to `spring.autoconfigure.exclude` in test profile |
| `app.cors.allowed-origins` not resolved | Define in `application-integration.yaml` |
| Redis connection failure | Verify Testcontainers is running (`docker ps`) |