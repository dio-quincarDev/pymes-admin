# Guía de administración

> Para quien gestiona cuentas, equipos, secretos y base de datos. El uso diario de la
> app está en la [guía de usuario](user-guide.md).

## Roles

Hay cuatro roles, de mayor a menor jerarquía (`Source: backend/auth/src/main/java/auth/pymes/common/models/enums/RoleName.java`):

| Rol | Nivel |
|-----|-------|
| OWNER | 4 |
| ADMIN | 3 |
| CONTABLE | 2 |
| VIEWER | 1 |

La jerarquía se aplica al invitar: **no puedes invitar a alguien con un rol igual o
superior al tuyo**. OWNER y ADMIN pueden invitar; CONTABLE y VIEWER no.

`Source: backend/auth/src/main/java/auth/pymes/service/impl/InvitationServiceImpl.java`

## Equipo e invitaciones

En la app, **Equipo** (`/dashboard/teams`) solo aparece para OWNER y ADMIN.

| Acción | Quién puede |
|--------|-------------|
| Listar miembros del tenant | OWNER o ADMIN |
| Invitar miembros | OWNER o ADMIN (sin rol igual o superior) |
| Cambiar el rol de un usuario | **Solo OWNER** |
| Desvincular un usuario (borrado lógico) | **Solo OWNER** |

Cada tenant tiene un límite de usuarios (`max_users`). Al alcanzarlo, la invitación
falla con el error `TNT007` ("Tenant has reached maximum number of users").

`Source: backend/auth/src/main/java/auth/pymes/controller/MemberApi.java`,
`backend/auth/src/main/java/auth/pymes/utils/exception/CodigoError.java`

## Planes

El plan del tenant viaja en el JWT (claim `plan`, por defecto `FREE`) y se usa para
el límite de usuarios. No hay cobro ni cambio de plan implementado en el código.

Estado: **Parcial** — identidad del plan y límite implementados; facturación y
cambio de plan **Planned**.

`Source: backend/auth/src/main/java/auth/pymes/service/impl/JwtServiceImpl.java`,
`backend/auth/src/main/java/auth/pymes/service/impl/UserServiceImpl.java`

## Configuración por servicio

Cada servicio lee su propio archivo `.env` (copiar de `.env.example`; los `.env`
reales no están en Git). Las variables que hay que llenar:

| Servicio | Variables principales |
|----------|-----------------------|
| Auth (8081) | `DB_*`, `REDIS_*`, `GOOGLE_CLIENT_*`, `FACEBOOK_CLIENT_*`, `JWT_SECRET`, `JWT_*_EXPIRATION`, `SPRING_MAIL_*`, `MAIL_FROM`, `CORS_ALLOWED_ORIGINS`, `APP_FRONTEND_URL`, `SWAGGER_*` |
| Gateway (8080) | `SERVER_PORT`, `REDIS_*`, `JWT_SECRET`, `AUTH_SERVICE_HOST` |
| Core (8082) | `DB_*`, `REDIS_*`, `SPRING_MAIL_*`, `MAIL_FROM`, `AUTH_SERVICE_URL` |
| Frontend | `VITE_API_URL` |

`JWT_SECRET` debe ser el **mismo** en auth y gateway (el gateway valida el token con
ella). En producción usa un secreto de al menos 256 bits, nunca el valor de ejemplo.

`Source: backend/*/.env.example`, `frontend/pymes/.env.example`

## Correo

El correo es agnóstico: funciona con Gmail (contraseña de aplicación) o con OCI
Email Delivery. En OCI, `MAIL_FROM` debe ser un *Approved Sender* (por ejemplo
`sa-bogota-1`); con Gmail déjalo igual que `SPRING_MAIL_USERNAME` o vacío.

`Source: backend/auth/.env.example`, `.github/SECRETS.md`

## Secretos en CI/CD

Los secretos de GitHub (Docker Hub, SSH a staging/producción, DB, JWT, OAuth2, SMTP,
CORS, perfiles Spring) están listados en `.github/SECRETS.md`. Cualquier cambio de
secretos o de workflows de CI/CD requiere revisión humana antes de mergear.

`Source: .github/SECRETS.md`

## Base de datos

Un solo PostgreSQL con dos esquemas: `auth` (usuarios, tenants, invitaciones,
refresh tokens, auditoría) y `core` (datos de negocio). **Nunca cambiar
`spring.flyway.schemas: auth` sin verificar el aislamiento de core.** Los servicios
nunca tocan el esquema del otro.

`Source: backend/auth/src/main/resources/db/migration/V1__initial_schema.sql`,
`docs/strategies/DB_STRATEGY.md`, `AGENTS.md`
