# 📦 Guía de Deployment - PyMes Admin

Esta guía documenta el proceso completo de deployment para staging y producción.

---

## 📋 Resumen de Arquitectura

| Componente | Puerto | Red | Docker Image |
|------------|--------|-----|--------------|
| Frontend (Quasar PWA) | 9000 | pymes-global-network | `pymes-frontend` |
| Auth Service (Spring Boot) | 8081 | pymes-global-network + internal | `pymes-auth` |
| PostgreSQL | 5432 | pymes-internal-network | `postgres:15-alpine` |
| Redis | 6379 | pymes-internal-network | `redis:7-alpine` |

---

## 🚀 Quick Deploy (Primera Vez)

### Paso 1: Configurar Secrets en GitHub

Ve a **Settings → Secrets and variables → Actions** y agrega:

| Secret | Descripción |
|--------|-------------|
| `DOCKER_USERNAME` | Tu usuario de Docker Hub |
| `DOCKER_PASSWORD` | Access Token de Docker Hub |
| `STAGING_HOST` | IP pública de tu instancia OCI |
| `STAGING_USER` | Usuario SSH (típicamente `ubuntu`) |
| `STAGING_SSH_KEY` | Llave privada SSH completa |

### Paso 2: Setup del Servidor

```bash
# Conectarse al servidor
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP>

# Clonar repositorio
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin
cd ~/pymes-admin

# Ejecutar setup inicial (SOLO LA PRIMERA VEZ)
chmod +x scripts/setup-server.sh
./scripts/setup-server.sh
```

### Paso 3: Configurar Variables de Entorno

```bash
cd ~/pymes-admin
nano backend/auth/.env
```

Valores mínimos requeridos:
```env
DB_NAME=pymes_auth
DB_USERNAME=postgres
DB_PASSWORD=<password-seguro>
SERVER_PORT=8081
```

### Paso 4: Configurar Nginx Proxy Manager

```bash
# Instalar NPM (si no está instalado)
docker run -d \
  --name=nginx-proxy-manager \
  --restart=unless-stopped \
  -p 80:80 -p 81:81 -p 443:443 \
  -v /etc/nginx/proxy_host:/data/nginx/proxy_host \
  -v /etc/nginx/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest
```

Acceder a `http://<TU_IP>:81` y configurar:

| Proxy | Domain | Forward Host | Forward Port | Network |
|-------|--------|--------------|--------------|---------|
| Frontend | `staging.pymes-admin.com` | `pymes-frontend` | 9000 | `pymes-global-network` |
| Backend API | `staging-api.pymes-admin.com` | `pymes-auth-service` | 8081 | `pymes-global-network` |

### Paso 5: Primer Deploy

```bash
# Opción A: Automático (recomendado)
git checkout develop
git add .
git commit -m "initial commit

Co-authored-by: Qwen-Coder <qwen-coder@alibabacloud.com>"
git push origin develop

# Opción B: Manual
cd ~/pymes-admin
./scripts/deploy-staging.sh
```

---

## 🔄 Deploy Continuo (Después del Setup)

### Flujo Automático (Recomendado)

```bash
# 1. Desarrollar en rama feature
git checkout -b feature/nueva-funcionalidad develop
# ... hacer cambios ...
git commit -m "feat: agregar nueva funcionalidad"
git push origin feature/nueva-funcionalidad

# 2. Merge a develop (trigger deploy automático)
git checkout develop
git merge feature/nueva-funcionalidad
git push origin develop
```

GitHub Actions automáticamente:
1. ✅ Build del backend (Java/Maven)
2. ✅ Build del frontend (Node.js/Quasar)
3. ✅ Tests y linting
4. ✅ Build y push de imágenes Docker
5. ✅ Deploy al servidor staging

### Flujo Manual (En el Servidor)

```bash
# Conectarse al servidor
ssh -i ~/.ssh/<TU_LLAVE> ubuntu@<TU_IP>

# Ejecutar deploy script
cd ~/pymes-admin
./scripts/deploy-staging.sh
```

---

## 🛠️ Comandos Útiles

### En el Servidor

```bash
# Ver estado de servicios
docker compose -f docker-compose.yml ps

# Ver logs en tiempo real
docker compose -f docker-compose.yml logs -f

# Ver logs de un servicio específico
docker compose -f docker-compose.yml logs -f frontend
docker compose -f docker-compose.yml logs -f auth-service

# Reiniciar servicios
docker compose -f docker-compose.yml restart

# Detener servicios
docker compose -f docker-compose.yml down

# Iniciar servicios
docker compose -f docker-compose.yml up -d

# Limpiar imágenes viejas
docker image prune -f --filter "until=24h"

# Ver redes Docker
docker network ls

# Ver volúmenes Docker
docker volume ls
```

### En GitHub Actions

```bash
# Ver workflows corriendo
https://github.com/dio-quincarDev/pymes-admin/actions

# Re-run un workflow fallido
https://github.com/dio-quincarDev/pymes-admin/actions/runs/<RUN_ID>

# Trigger manual deploy (si configurado)
https://github.com/dio-quincarDev/pymes-admin/actions/workflows/cd-staging.yml
```

---

## 🐛 Troubleshooting

### Problema: `network not found`

```bash
# Solución: Recrear redes
cd ~/pymes-admin
./scripts/setup-server.sh
```

### Problema: `Permission denied (publickey)`

```bash
# Verificar que la llave es correcta
cat ~/.ssh/<TU_LLAVE>

# Debe incluir BEGIN y END
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

### Problema: Contenedores no arrancan

```bash
# Ver logs detallados
docker compose -f docker-compose.yml logs

# Verificar variables de entorno
cat backend/auth/.env

# Reiniciar desde cero
docker compose -f docker-compose.yml down -v
docker compose -f docker-compose.yml up -d
```

### Problema: Healthcheck falla

```bash
# Verificar health de cada servicio
docker inspect --format='{{.State.Health.Status}}' pymes-frontend
docker inspect --format='{{.State.Health.Status}}' pymes-auth-service
docker inspect --format='{{.State.Health.Status}}' pymes-postgres-auth
docker inspect --format='{{.State.Health.Status}}' pymes-redis-auth
```

---

## 📊 Monitoreo

### Ver Uso de Recursos

```bash
# Uso de CPU/memoria por contenedor
docker stats

# Uso de disco
docker system df

# Ver espacio en el servidor
df -h
```

### Logs de Auditoría

```bash
# Ver todos los logs
docker compose -f docker-compose.yml logs --tail=100

# Exportar logs a archivo
docker compose -f docker-compose.yml logs > logs-$(date +%Y%m%d).txt
```

---

## 🔐 Seguridad

### Best Practices

1. ✅ **Nunca** commitear archivos `.env`
2. ✅ Usar secrets de GitHub para credenciales
3. ✅ Rotar Docker Hub tokens periódicamente
4. ✅ Mantener servidor actualizado: `sudo apt update && sudo apt upgrade -y`
5. ✅ Usar HTTPS con Let's Encrypt en Nginx Proxy Manager
6. ✅ Firewall UFW configurado (solo puertos 22, 80, 443)

### Actualizar Credenciales

```bash
# En el servidor
nano backend/auth/.env

# Reiniciar servicios
docker compose -f docker-compose.yml restart auth-service
```

---

## 📈 Escalabilidad

### Próximos Pasos

1. **Read Replicas**: Para reportes pesados
2. **Redis Cluster**: Para sesiones distribuidas
3. **Load Balancer**: Múltiples instancias de auth-service
4. **CDN**: Para assets estáticos del frontend
5. **Kubernetes**: Cuando necesites orquestación avanzada

---

## 📞 Soporte

| Recurso | URL |
|---------|-----|
| GitHub Actions | https://github.com/dio-quincarDev/pymes-admin/actions |
| Docker Hub | https://hub.docker.com/r/dio-quincar/pymes-auth |
| Oracle Cloud Console | https://cloud.oracle.com |
| Nginx Proxy Manager Docs | https://nginxproxymanager.com/guide/ |

---

<div align="center">

**PyMes Admin** - Deployment Guide 📦

</div>
