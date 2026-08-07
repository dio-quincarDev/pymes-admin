# 🔐 Secrets Requeridos para CI/CD

Todos los secrets deben configurarse en **GitHub → Settings → Secrets and variables → Actions → New repository secret**.

---

## 🐳 Docker Hub

| Secret | Descripción | Cómo obtenerlo |
|--------|-------------|----------------|
| `DOCKER_USERNAME` | Usuario de Docker Hub | Tu usuario en hub.docker.com |
| `DOCKER_PASSWORD` | Access Token de Docker Hub | hub.docker.com/settings/security → Generate Access Token |

---

## ☁️ Staging Server (Oracle Cloud Free Tier)

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `STAGING_HOST` | IP pública del servidor staging | `<IP_OCI_STAGING>` |
| `STAGING_USER` | Usuario SSH | `ubuntu` |
| `STAGING_SSH_KEY` | Llave privada SSH (contenido completo) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

---

## 🚀 Production Server (Oracle Cloud Free Tier)

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `PROD_HOST` | IP pública del servidor producción | `<IP_OCI_PROD>` |
| `PROD_USER` | Usuario SSH | `ubuntu` |
| `PROD_SSH_KEY` | Llave privada SSH (contenido completo) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

---

## 🗄️ Base de Datos (PostgreSQL)

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `DB_NAME` | Nombre de la base de datos | `pymes_db` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | *(password real)* |

---

## 🔑 JWT (JSON Web Tokens)

| Secret | Descripción | Valor recomendado |
|--------|-------------|-------------------|
| `JWT_SECRET` | Clave secreta para firmar tokens (≥256 bits) | `openssl rand -hex 32` |
| `JWT_ACCESS_EXPIRATION` | Expiración del access token (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION` | Expiración del refresh token (ms) | `604800000` (7 días) |

---

## 🔐 OAuth2 (Google & Facebook)

| Secret | Descripción | Dónde obtenerlo |
|--------|-------------|-----------------|
| `GOOGLE_CLIENT_ID` | Client ID de Google OAuth | console.cloud.google.com → APIs & Services → Credentials |
| `GOOGLE_CLIENT_SECRET` | Client Secret de Google OAuth | console.cloud.google.com → APIs & Services → Credentials |
| `FACEBOOK_CLIENT_ID` | App ID de Facebook OAuth | developers.facebook.com → My Apps |
| `FACEBOOK_CLIENT_SECRET` | App Secret de Facebook OAuth | developers.facebook.com → My Apps → App Settings |
| `OAUTH2_REDIRECT_URI` | URI de redirección OAuth2 | `http://staging.tudominio.com` / `https://tudominio.com` |

---

## 📧 Email (SMTP - Gmail)

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `SPRING_MAIL_USERNAME` | Correo Gmail para envío de emails | `devpruebas.zar@gmail.com` |
| `SPRING_MAIL_PASSWORD` | App Password de Gmail | *(16 caracteres)* |

**Cómo obtener SPRING_MAIL_PASSWORD:**
1. Ve a myaccount.google.com/security
2. Activa **2-Step Verification**
3. Ve a myaccount.google.com/apppasswords
4. Genera un "App Password" para "Mail"
5. Copia los 16 caracteres

---

## 🌐 CORS (Cross-Origin Resource Sharing)

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `CORS_ALLOWED_ORIGINS_STAGING` | Orígenes permitidos en staging | `http://staging.tudominio.com:9200` |
| `CORS_ALLOWED_ORIGINS_PROD` | Orígenes permitidos en producción | `https://tudominio.com,https://app.tudominio.com` |

---

## 📝 Resumen completo (23 secrets)

```
# Docker
DOCKER_USERNAME
DOCKER_PASSWORD

# Staging
STAGING_HOST
STAGING_USER
STAGING_SSH_KEY

# Producción
PROD_HOST
PROD_USER
PROD_SSH_KEY

# Base de datos
DB_NAME
DB_USERNAME
DB_PASSWORD

# JWT
JWT_SECRET
JWT_ACCESS_EXPIRATION
JWT_REFRESH_EXPIRATION

# OAuth2
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
FACEBOOK_CLIENT_ID
FACEBOOK_CLIENT_SECRET
OAUTH2_REDIRECT_URI

# Email
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD

# CORS
CORS_ALLOWED_ORIGINS_STAGING
CORS_ALLOWED_ORIGINS_PROD
```

---

## 🛠️ Configuración del Servidor Oracle Cloud

```bash
# Conectar
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP_PUBLICA>

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker
sudo apt update && sudo apt install -y docker-compose-plugin

# Clonar repositorio
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin
```

### Security List (Oracle Cloud)

| Puerto | Protocolo | Descripción |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 8080 | TCP | Gateway Service |
| 8081 | TCP | Auth Service |
| 9200 | TCP | Frontend |
| 80 | TCP | HTTP (opcional) |
| 443 | TCP | HTTPS (opcional) |

---

## ✅ Verificar Configuración

| Acción | Workflow que se ejecuta |
|--------|------------------------|
| Push a `feature/**` | CI (build + tests). Salta si solo cambian `**/*.md` |
| Push a `develop` | CI + CD Staging |
| Push a `main` | CI + CD Producción |

---

## ⚠️ Troubleshooting

| Problema | Solución |
|----------|----------|
| `Permission denied (publickey)` | Verifica que STAGING_SSH_KEY / PROD_SSH_KEY tenga el contenido **completo** de la llave privada |
| `docker: command not found` | Ejecuta `newgrp docker` en el servidor |
| `Connection timed out` | Verifica Security List en Oracle Cloud Console |
| `unauthorized: authentication required` | Verifica DOCKER_USERNAME y DOCKER_PASSWORD |
| `.env` file found in repo | Los `.env` están en `.gitignore`, pero si ya están trackeados, ejecuta `git rm --cached .env` |
