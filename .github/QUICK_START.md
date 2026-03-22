# 🚀 Guía Rápida: Configurar CI/CD con tu Instancia de Staging

> ⚠️ **Nota sobre CI/CD:** El pipeline utiliza **Docker Buildx** para gestionar el caché de imágenes y requiere un archivo `.env` temporal durante los tests del backend. Si añades nuevas variables críticas en `application.yaml`, asegúrate de incluirlas también en `.env.example`.

Esta guía te llevará paso a paso para tener el CI/CD funcionando con tu instancia de Oracle Cloud Free Tier.

---

## 📋 Arquitectura del Deploy

```
┌─────────────────────────────────────────────────────────┐
│                    Oracle Cloud (Staging)                │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Docker Network: pymes-global-network        │ │
│  │         (Para Nginx Proxy Manager)                  │ │
│  │                                                     │ │
│  │  ┌──────────────────┐  ┌──────────────────┐        │ │
│  │  │   Frontend       │  │   Backend Auth   │        │ │
│  │  │   (Quasar PWA)   │  │   (Spring Boot)  │        │ │
│  │  │   Puerto: 9000   │  │   Puerto: 8081   │        │ │
│  │  └──────────────────┘  └──────────────────┘        │ │
│  └────────────────────────────────────────────────────┘ │
│                          ▲                              │
│                          │                              │
│  ┌───────────────────────┴──────────────────────────┐  │
│  │        Nginx Proxy Manager (en el host)          │  │
│  │        Expone: staging.pymes-admin.com           │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 Paso 1: Obtener tu Llave SSH Privada

```bash
# En tu terminal local, ejecuta:
cat ~/.ssh/<TU_LLAVE_PRIVADA>
```

Copia **TODO** el output, incluyendo:
```
-----BEGIN OPENSSH PRIVATE KEY-----
... (todo el contenido)
...
-----END OPENSSH PRIVATE KEY-----
```

---

## 🐳 Paso 2: Crear Cuenta en Docker Hub (si no tienes)

1. Ve a https://hub.docker.com
2. Click en "Sign Up"
3. Crea tu cuenta gratuita
4. Confirma tu email

---

## 🔑 Paso 3: Generar Docker Hub Access Token

1. Ve a https://hub.docker.com/settings/security
2. Click en **"New Access Token"**
3. Ponle un nombre: `github-actions-pymes`
4. Click en **"Generate"**
5. **Copia el token** (solo se muestra una vez, no se puede recuperar)

---

## ⚙️ Paso 4: Configurar Secrets en GitHub

1. Ve a tu repositorio en GitHub
2. Click en **Settings** (pestaña a la derecha)
3. En el menú izquierdo: **Secrets and variables** → **Actions**
4. Click en **"New repository secret"**

### Agrega estos 5 secrets:

| Secret | Value |
|--------|-------|
| `DOCKER_USERNAME` | tu-usuario-de-docker-hub |
| `DOCKER_PASSWORD` | el-token-que-generaste-en-paso-3 |
| `STAGING_HOST` | `<TU_IP_PUBLICA_DE_ORACLE_CLOUD>` |
| `STAGING_USER` | `ubuntu` |
| `STAGING_SSH_KEY` | (pega TODO el contenido de tu llave privada) |

---

## 🖥️ Paso 5: Configurar tu Instancia Oracle Cloud

### 5.1 Conéctate al servidor (PRIMERA VEZ)

```bash
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP_PUBLICA>
```

### 5.2 Ejecutar setup inicial

```bash
# Clonar el repositorio
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin
cd ~/pymes-admin

# Dar permisos de ejecución
chmod +x scripts/setup-server.sh

# Ejecutar setup (SOLO LA PRIMERA VEZ)
./scripts/setup-server.sh
```

**Este script:**
- ✅ Actualiza el sistema
- ✅ Instala Docker
- ✅ Instala Docker Compose
- ✅ Instala Git (si no existe)
- ✅ Agrega tu usuario al grupo docker
- ✅ **Crea redes Docker: `pymes-global-network` y `pymes-internal-network`**
- ✅ Clona el repositorio
- ✅ Configura .env desde .env.example
- ✅ Configura firewall (UFW)

### 5.3 Configurar variables de entorno

```bash
cd ~/pymes-admin
nano backend/auth/.env
```

Edita al menos estos valores:
```env
DB_NAME=pymes_auth
DB_USERNAME=postgres
DB_PASSWORD=tu-password-seguro
SERVER_PORT=8081
```

Guarda con `Ctrl+O`, `Enter`, y sal con `Ctrl+X`.

### 5.4 Reiniciar sesión SSH (para aplicar grupo docker)

```bash
exit

# Conectar de nuevo
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP_PUBLICA>

# Verificar que docker funciona sin sudo
docker --version
docker network ls
```

---

## 🔥 Paso 6: Configurar Security List en Oracle Cloud

1. Ve a Oracle Cloud Console
2. **Networking** → **Virtual Cloud Networks**
3. Click en tu VCN
4. **Security Lists** → **Default Security List**
5. **Add Ingress Rules**

Agrega estas reglas:

| Source CIDR | Protocol | Destination Port Range | Description |
|-------------|----------|----------------------|-------------|
| `0.0.0.0/0` | TCP | 22 | SSH |
| `0.0.0.0/0` | TCP | 80 | HTTP (Nginx Proxy Manager) |
| `0.0.0.0/0` | TCP | 443 | HTTPS (Nginx Proxy Manager) |

---

## 🌐 Paso 7: Configurar Nginx Proxy Manager

### 7.1 Instalar Nginx Proxy Manager (si no está instalado)

```bash
# En tu servidor
docker run -d \
  --name=nginx-proxy-manager \
  --restart=unless-stopped \
  -p 80:80 \
  -p 81:81 \
  -p 443:443 \
  -v /etc/nginx/proxy_host:/data/nginx/proxy_host \
  -v /etc/nginx/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest
```

### 7.2 Configurar Proxy Host

1. Accede a Nginx Proxy Manager: `http://<TU_IP_PUBLICA>:81`
2. Login por defecto: `admin@example.com` / `changeme`
3. Ve a **Hosts** → **Proxy Hosts** → **Add Proxy Host**

### 7.3 Configurar Frontend Proxy

| Campo | Valor |
|-------|-------|
| **Domain Name** | `staging.pymes-admin.com` |
| **Scheme** | `http` |
| **Forward Hostname/IP** | `pymes-frontend` |
| **Forward Port** | `9000` |
| **Network** | `pymes-global-network` |
| **Cache** | ✅ Checked |
| **Websockets Support** | ✅ Checked |

### 7.4 Configurar Backend Proxy (opcional, para API)

| Campo | Valor |
|-------|-------|
| **Domain Name** | `staging-api.pymes-admin.com` |
| **Scheme** | `http` |
| **Forward Hostname/IP** | `pymes-auth-service` |
| **Forward Port** | `8081` |
| **Network** | `pymes-global-network` |

---

## ✅ Paso 8: Probar el CI/CD

### 8.1 Hacer push de prueba

```bash
# En tu computadora local
cd ~/pymes-admin

# Asegúrate de estar en develop
git checkout develop
git pull origin develop

# Hacer un cambio menor
echo "# Test CI/CD" >> README.md
git add README.md
git commit -m "test: probar CI/CD staging

Co-authored-by: Qwen-Coder <qwen-coder@alibabacloud.com>"
git push origin develop
```

### 8.2 Verificar el workflow

1. Ve a https://github.com/dio-quincarDev/pymes-admin/actions
2. Deberías ver el workflow **"CD - Deploy to Staging"** corriendo
3. Espera a que complete (aprox. 5-7 minutos)

### 8.3 Verificar deployment

```bash
# Conéctate al servidor
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP_PUBLICA>

# Verificar contenedores
docker compose -f ~/pymes-admin/docker-compose.yml ps

# Ver logs
docker compose -f ~/pymes-admin/docker-compose.yml logs -f frontend
docker compose -f ~/pymes-admin/docker-compose.yml logs -f auth-service
```

---

## 🔄 Flujo de Trabajo (Después del Setup Inicial)

### Para futuros deploys (automáticos)

Solo haz push a `develop`:
```bash
git checkout develop
git add .
git commit -m "feat: nueva funcionalidad"
git push origin develop
```

GitHub Actions automáticamente:
1. Build del backend (Java/Maven)
2. Build del frontend (Node.js/Quasar)
3. Build y push de imágenes Docker
4. Deploy al servidor staging

### Para futuros deploys (manuales)

Si necesitas deploy manual en el servidor:
```bash
# Conéctate al servidor
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP_PUBLICA>

# Ejecutar deploy script
cd ~/pymes-admin
./scripts/deploy-staging.sh
```

---

## 🛠️ Troubleshooting

| Problema | Solución |
|----------|----------|
| `Permission denied (publickey)` | Verifica que la llave privada en STAGING_SSH_KEY sea correcta |
| `docker: command not found` | Ejecuta `newgrp docker` o reconecta la sesión SSH |
| `Connection timed out` | Verifica Security List en Oracle Cloud |
| `Error: unauthorized` | Verifica DOCKER_USERNAME y DOCKER_PASSWORD |
| `network not found` | Ejecuta `./scripts/setup-server.sh` para crear las redes |

---

## 📝 Scripts Disponibles

| Script | Propósito | Cuándo usar |
|--------|-----------|-------------|
| `scripts/setup-server.sh` | Setup inicial del servidor | **Solo la primera vez** |
| `scripts/deploy-staging.sh` | Deploy de la aplicación | Cada vez que quieras deploy manual |

---

## 📊 Resumen de la Arquitectura

| Componente | Puerto | Red | Exposto |
|------------|--------|-----|---------|
| Frontend (Quasar) | 9000 | pymes-global-network | Sí (vía NPM) |
| Auth Service | 8081 | pymes-global-network + internal | Sí (vía NPM, opcional) |
| PostgreSQL | 5432 | pymes-internal-network | No |
| Redis | 6379 | pymes-internal-network | No |

---

<div align="center">

**PyMes Admin** - CI/CD Staging en OCI Free Tier 🚀

</div>
