## Documentation: OAuth2 User Service

### Technical Overview
* **Component:** `CustomOAuth2UserService`
* **Base Class:** `DefaultOAuth2UserService`
* **Responsibility:** Orchestrates social identity federation and JIT (Just-In-Time) user provisioning.
* **Mechanism:** Intercepts the OAuth2 successful authentication flow to synchronize external provider data with the internal `UserEntity` schema.

---

### Federation Strategy

The service implements a unified extraction layer for multiple OAuth2 providers, normalizing external attributes into a standard internal format.

#### 1. Provider Mapping
The `registrationId` from the `OAuth2UserRequest` is dynamically mapped to the internal `AuthProvider` enum:
* **Google:** Maps `sub` as the unique `providerId`.
* **Generic:** Defaults to `id` for other providers (e.g., Facebook, GitHub).

#### 2. Attribute Normalization
The implementation handles nested attribute structures specific to each provider:
* **Profile Picture:**
    * **Google:** Direct access via the `picture` key.
    * **Facebook:** Traverses a nested map structure (`picture -> data -> url`).
* **Identity:** Standardizes `email` and `name` from the shared `OAuth2User` attribute map.

---

### Just-In-Time (JIT) Provisioning

The service follows a **"Find or Create"** pattern during the authentication handshake:

1.  **Lookup:** Queries `userEntityRepository` using the composite key `[provider, providerId]`.
2.  **Creation:** If no record exists, a new `UserEntity` is persisted with:
    * `isActive: true` (Auto-activation for social logins).
    * `providerId`: The unique ID provided by the external issuer (Google `sub`, etc.).
    * `pictureUrl`: The normalized URL from the provider.

---

### Technical Specifications

#### Data Normalization Matrix

| Provider | ID Attribute | Picture Path |
| :--- | :--- | :--- |
| **Google** | `sub` | `attributes["picture"]` |
| **Facebook** | `id` | `attributes["picture"]["data"]["url"]` |
| **Generic** | `id` | `null` |

#### Security Considerations
* **Authentication vs. Registration:** This service treats a first-time social login as an implicit registration.
* **Persistence:** The `DefaultOAuth2UserService.loadUser` method is called after the authorization code exchange, ensuring only authenticated identities are persisted.
* **State Management:** The service returns the original `OAuth2User` object to maintain compatibility with Spring Security's session management, while the side-effect (DB persistence) ensures local identity availability.

---

### Key Methods

* **`loadUser`**: Entry point that triggers the external fetch and internal sync.
* **`getProviderId`**: Normalizes the unique identifier across different OAuth2 implementations.
* **`getPictureUrl`**: Logic for deep-parsing provider-specific metadata structures.