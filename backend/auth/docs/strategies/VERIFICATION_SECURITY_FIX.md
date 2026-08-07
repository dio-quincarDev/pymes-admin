# 🔒 Security Fix — Email Verification Token-Email Mismatch

This document outlines the vulnerability analysis and mitigation strategy implemented to resolve a critical security gap in the email verification flow.

---

## 🔍 Vulnerability Analysis

### The Original Flow (Vulnerable)
1. User receives an onboarding email containing a link: `/verify?token=abc123&email=victim@email.com`
2. The frontend (VerifyEmailPage.vue) parses the query parameters.
3. The frontend sends a POST request to `/auth/verify-email` containing only the `{ token: "abc123" }`. The email parameter was ignored.
4. The backend resolved the email from the Redis key `email:verify:abc123` (which yielded `target@email.com`) and marked that user as verified without verifying if the user requesting the action matched the target user.
5. This allowed anyone with a valid verification token to potentially verify arbitrary accounts if they could guess or intercept tokens.

---

## 🛡️ Mitigation Design

A cross-validation layer was introduced on both the backend and frontend.

```
┌─────────────────────────────────────────────────────────────┐
│                      FRONTEND                               │
│  ═════════════════════════════════════════════════════════  │
│  1. Extracts token + email from query parameters            │
│  2. SENDS: { token, email }                                 │
│  3. Displays target email in UI before verification         │
└─────────────────────────────────────────────────────────────┘
                            ↓
                       API Request
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      BACKEND                                │
│  ═══════════════════════════════════════════════════════════ │
│  1. Receives { token, email }                               │
│  2. Resolves Redis: email:verify:{token} -> storedEmail     │
│  3. CROSS-VALIDATES: storedEmail == request.email           │
│     ✓ Match -> mark verified                                │
│     ✗ Mismatch -> Reject (400 Bad Request)                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Component Details

### Backend
- **VerifyEmailRequest**: DTO updated to require both `token` and `email`.
- **VerifyEmailResponse**: Created to return verification status metadata.
- **EmailVerificationServiceImpl**: Implements case-insensitive cross-validation:
  ```java
  if (!storedEmail.equalsIgnoreCase(email)) {
      throw new EmailVerificationTokenInvalidException("Token-email mismatch");
  }
  ```

### Frontend
- **authService**: Updated `verifyEmail(token, email)` to pass both fields in the request body.
- **VerifyEmailPage.vue**: Displays the email address that is being verified to the user, providing visual confirmation.

---

## 🧪 Validation & Test Cases

| Case | Input | Expected Outcome |
|------|-------|------------------|
| **Happy Path** | Valid token + matching email | `200 OK` (User verified) |
| **Invalid Token** | Expired or non-existent token | `400 Bad Request` (Token invalid) |
| **Email Mismatch** | Valid token + mismatching email | `400 Bad Request` (Token-email mismatch) |
| **Already Verified** | Valid token + email already verified | `409 Conflict` (Email already verified) |
