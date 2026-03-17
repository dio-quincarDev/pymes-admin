# 🚀 Guía Rápida: Configurar CI/CD con tu Instancia de Staging

Esta guía te llevará paso a paso para tener el CI/CD funcionando con tu instancia de Oracle Cloud.

---

## 📋 Tu Configuración Actual

| Item | Valor |
|------|-------|
| **Instancia Staging** | `149.130.165.200` |
| **Usuario SSH** | `ubuntu` |
| **Llave SSH** | `~/.ssh/cloushellkey` |
| **Producción** | Pendiente |

---

## 🔐 Paso 1: Obtener tu Llave SSH Privada

```bash
# En tu terminal local, ejecuta:
cat ~/.ssh/cloushellkey
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

### Secret 1: DOCKER_USERNAME
```
Name:  DOCKER_USERNAME
Value: tu-usuario-de-docker-hub
```

### Secret 2: DOCKER_PASSWORD
```
Name:  DOCKER_PASSWORD
Value: el-token-que-generaste-en-paso-3
```

### Secret 3: STAGING_HOST
```
Name:  STAGING_HOST
Value: 149.130.165.200
```

### Secret 4: STAGING_USER
```
Name:  STAGING_USER
Value: ubuntu
```

### Secret 5: STAGING_SSH_KEY
```
Name:  STAGING_SSH_KEY
Value: (pega TODO el contenido de tu llave privada, incluyendo BEGIN y END)
```

---

## 🖥️ Paso 5: Configurar tu Instancia Oracle Cloud

### 5.1 Conéctate al servidor

```bash
ssh -i ~/.ssh/cloushellkey ubuntu@149.130.165.200
```

### 5.2 Instalar Docker y dependencias

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
rm get-docker.sh

# Agregar tu usuario al grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Instalar Docker Compose
sudo apt install -y docker-compose-plugin

# Verificar
docker --version
docker compose version
```

### 5.3 Clonar el repositorio

```bash
# Salir y conectar de nuevo para aplicar cambios de grupo docker
exit

# Conectar de nuevo
ssh -i ~/.ssh/cloushellkey ubuntu@149.130.165.200

# Clonar tu repo
cd ~
git clone https://github.com/dio-quincarDev/pymes-admin.git
cd pymes-admin
```

### 5.4 Configurar variables de entorno

```bash
# Copiar el ejemplo
cp backend/auth/.env.example backend/auth/.env

# Editar con tus valores reales
nano backend/auth/.env
```

Edita al menos estos valores:
```env
DB_NAME=pymes_auth
DB_USERNAME=postgres
DB_PASSWORD=tu-password-seguro
SERVER_PORT=8081

# OAuth2 (si ya tienes configurado Google/Facebook)
GOOGLE_CLIENT_ID=tu-client-id
GOOGLE_CLIENT_SECRET=tu-client-secret
```

Guarda con `Ctrl+O`, `Enter`, y sal con `Ctrl+X`.

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
| `0.0.0.0/0` | TCP | 8081 | Auth Service |
| `0.0.0.0/0` | TCP | 80 | HTTP (opcional) |
| `0.0.0.0/0` | TCP | 443 | HTTPS (opcional) |

---

## ✅ Paso 7: Probar el CI/CD

### 7.1 Hacer push de prueba

```bash
# En tu computadora local
cd ~/pymes-admin  # o donde tengas el repo

# Crear rama de test
git checkout -b test/ci-cd

# Hacer un cambio pequeño
echo "# CI/CD Test" >> README.md
git commit -am "Test CI/CD pipeline"

# Push
git push origin test/ci-cd
```

### 7.2 Verificar en GitHub

1. Ve a tu repo en GitHub
2. Click en **Actions**
3. Deberías ver el workflow **"CI - Build and Test"** corriendo
4. Espera a que termine (debería estar en verde ✅)

### 7.3 Probar Deploy a Staging

```bash
# Hacer push a develop
git checkout develop
git merge test/ci-cd
git push origin develop
```

En GitHub Actions:
1. Verás el workflow **"CD - Deploy to Staging"**
2. Cuando termine, conéctate a tu servidor:

```bash
ssh -i ~/.ssh/cloushellkey ubuntu@149.130.165.200

# Ver los contenedores
docker compose -f ~/pymes-admin/backend/docker-compose.yml ps

# Ver logs
docker compose -f ~/pymes-admin/backend/docker-compose.yml logs -f
```

---

## 🎯 Verificación Final

### En tu servidor:

```bash
# Verificar que los servicios estén corriendo
docker compose -f ~/pymes-admin/backend/docker-compose.yml ps

# Deberías ver:
# - pymes-postgres-auth (healthy)
# - pymes-redis-auth (healthy)
# - pymes-auth-service (healthy)

# Probar el servicio auth
curl http://localhost:8081/api/v1/actuator/health
```

### En GitHub:

- ✅ CI workflow pasa en cada push
- ✅ CD Staging deploy funciona en `develop`
- ✅ Docker images se publican en Docker Hub

---

## 🆘 ¿Problemas?

| Error | Solución |
|-------|----------|
| `Permission denied (publickey)` | Verifica que copiaste bien la SSH key (incluye BEGIN/END) |
| `unauthorized: authentication required` | Revisa DOCKER_USERNAME y DOCKER_PASSWORD |
| `Connection refused` | Verifica Security List en Oracle Cloud |
| `docker: command not found` | Ejecuta `newgrp docker` o reconecta la sesión SSH |

---

## 📝 Próximos Pasos

Una vez que staging funcione:

1. **Configurar Producción**: Cuando tengas otra instancia, agrega `PROD_*` secrets
2. **Dominio Personalizado**: Configurar un dominio y SSL
3. **Monitoreo**: Agregar health checks y alertas
4. **Backups**: Configurar backups automáticos de la base de datos

---

*¿Dudas? Revisa el README principal o los logs en GitHub Actions*
