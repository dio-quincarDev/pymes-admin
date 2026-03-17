# 🔐 Secrets Requeridos para CI/CD

Este documento lista todos los secrets que debes configurar en GitHub para que los workflows funcionen correctamente.

## 📍 Dónde Configurar

1. Ve a tu repositorio en GitHub
2. **Settings** → **Secrets and variables** → **Actions**
3. Click en **New repository secret**

---

## 🔑 Secrets Requeridos

### Docker Hub (Para publicar imágenes)

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `DOCKER_USERNAME` | Tu usuario de Docker Hub | `dio-quincar` |
| `DOCKER_PASSWORD` | Password o Access Token de Docker Hub | `ghp_xxxxxxxxxxxx` |

**Cómo obtener DOCKER_PASSWORD:**
1. Ve a https://hub.docker.com/settings/security
2. Genera un "Access Token"
3. Copia y pégalo en el secret

---

### Staging Server (Oracle Cloud Free Tier)

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `STAGING_HOST` | IP pública del servidor de staging | `129.150.123.45` |
| `STAGING_USER` | Usuario SSH del servidor | `ubuntu` |
| `STAGING_SSH_KEY` | Llave privada SSH (contenido completo) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

**Cómo obtener STAGING_SSH_KEY:**
```bash
# Copia el contenido completo de tu llave privada
cat ~/.ssh/id_ed25519
# Copia TODO el output (incluye BEGIN y END)
```

---

### Production Server (Oracle Cloud Free Tier)

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `PROD_HOST` | IP pública del servidor de producción | `129.150.123.46` |
| `PROD_USER` | Usuario SSH del servidor | `ubuntu` |
| `PROD_SSH_KEY` | Llave privada SSH para producción | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

---

## 📝 Resumen de Secrets

```
DOCKER_USERNAME       → Usuario Docker Hub
DOCKER_PASSWORD       → Token Docker Hub
STAGING_HOST          → IP Staging
STAGING_USER          → SSH User Staging
STAGING_SSH_KEY       → SSH Key Staging
PROD_HOST             → IP Producción
PROD_USER             → SSH User Producción
PROD_SSH_KEY          → SSH Key Producción
```

---

## 🛠️ Configuración del Servidor Oracle Cloud

### 1. Crear la instancia Ubuntu

1. Ve a Oracle Cloud Console
2. **Compute** → **Instances** → **Create Instance**
3. Selecciona:
   - Image: **Ubuntu 22.04** o **24.04**
   - Shape: **VM.Standard.E2.1.Micro** (Free Tier)
   - SSH Keys: Sube tu llave pública (`~/.ssh/id_ed25519.pub`)

### 2. Configurar el servidor

```bash
# Conéctate a tu instancia
ssh -i ~/.ssh/id_ed25519 ubuntu@<IP_PUBLICA>

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Instalar Docker Compose Plugin
sudo apt update && sudo apt install -y docker-compose-plugin

# Verificar instalación
docker --version
docker compose version
```

### 3. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/pymes-admin.git ~/pymes-admin
cd ~/pymes-admin

# Configurar variables de entorno
cp backend/auth/.env.example backend/auth/.env
nano backend/auth/.env  # Edita con tus valores reales
```

### 4. Configurar Security List (Oracle Cloud)

En Oracle Cloud Console:
1. Ve a tu **Virtual Cloud Network**
2. **Security Lists** → Agrega las siguientes reglas de ingress:

| Puerto | Protocolo | Descripción |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 8081 | TCP | Auth Service |
| 80 | TCP | HTTP (si usas proxy) |
| 443 | TCP | HTTPS (si usas SSL) |

---

## ✅ Verificar Configuración

Después de configurar los secrets, puedes verificar que todo está correcto:

1. Haz un push a una rama `feature/test`
   - ✅ Debe correr solo el CI (build + tests)

2. Haz un push a `develop`
   - ✅ Debe correr CI + Deploy a Staging

3. Haz un push a `main`
   - ✅ Debe correr CI + Quality Gate + Deploy a Producción

---

## ⚠️ Troubleshooting

| Problema | Solución |
|----------|----------|
| `Permission denied (publickey)` | Verifica que la SSH key esté bien copiada (incluye BEGIN/END) |
| `docker: command not found` | Instala Docker en el servidor |
| `unauthorized: authentication required` | Verifica DOCKER_USERNAME y DOCKER_PASSWORD |
| `Connection refused` | Revisa que el Security List tenga los puertos abiertos |

---

*Última actualización: Marzo 2026*
