# PyMes Admin - CI/CD Setup

> Toolkit para administrar PyMes y pequeños negocios

---

## 📋 Estado Actual del Proyecto

| Componente | Estado | Tecnología |
|------------|--------|------------|
| **Backend Auth** | ✅ Configurado | Java 21 + Spring Boot 3.4.3 |
| **Frontend** | ✅ Configurado | Quasar 2 + Vue 3 + TypeScript |
| **Database** | ✅ Configurado | PostgreSQL 15 |
| **Cache** | ✅ Configurado | Redis 7 |
| **CI/CD** | 🟡 Staging Listo | GitHub Actions |
| **Deploy Staging** | 📝 Pendiente | Oracle Cloud (149.130.165.200) |
| **Deploy Producción** | ⏸️ Pendiente | Oracle Cloud Free Tier |

---

## 🚀 Quick Start

### Para Desarrolladores

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/pymes-admin.git
cd pymes-admin

# Frontend
cd frontend/pymes
npm install
npm run dev

# Backend (en otra terminal)
cd backend/auth
./mvnw spring-boot:run
```

### Para Deploy en Servidor

```bash
# En tu servidor Ubuntu (Oracle Cloud)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Clonar y configurar
git clone https://github.com/tu-usuario/pymes-admin.git ~/pymes-admin
cd ~/pymes-admin
cp backend/auth/.env.example backend/auth/.env
# Editar .env con tus valores reales

# Levantar servicios
docker compose -f backend/docker-compose.yml up -d
```

---

## 🏗️ Arquitectura del Proyecto

```
pymes-admin/
├── backend/
│   ├── auth/              # Microservicio de Autenticación (Spring Boot)
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── docker-compose.yml
├── frontend/
│   └── pymes/             # Aplicación Quasar/Vue.js
│       ├── src/
│       ├── public/
│       └── package.json
├── .github/
│   ├── workflows/         # CI/CD Pipelines
│   └── SECRETS.md         # Guía de secrets
└── scripts/
    └── setup-server.sh    # Script de setup del servidor
```

---

## 🔄 CI/CD Pipeline

### Ramas y Estrategia de Deploy

| Rama | Propósito | Pipeline | Deploy |
|------|-----------|----------|--------|
| `main` | Producción estable | CI + CD Prod | ✅ Producción |
| `develop` | Integración de features | CI + CD Staging | ✅ Staging |
| `feature/*` | Desarrollo de features | CI (build + test) | ❌ Solo test |

### Workflows Disponibles

| Workflow | Archivo | Descripción |
|----------|---------|-------------|
| **CI** | `.github/workflows/ci.yml` | Build y tests en cada push/PR |
| **CD Staging** | `.github/workflows/cd-staging.yml` | Deploy automático a staging |
| **CD Prod** | `.github/workflows/cd-prod.yml` | Deploy automático a producción |

### Pipeline CI (cada push/PR)

```
1. 🔒 Security Check
   ├── Verifica que no haya .env trackeados
   └── Busca patrones de secrets/API keys

2. 🔨 Build Backend Auth
   ├── Setup Java 21 + Maven cache
   ├── mvn clean package
   └── mvn test

3. 🎨 Build Frontend
   ├── Setup Node.js 20
   ├── npm ci
   ├── npm run lint
   └── npm run build

4. 🐳 Docker Build (opcional)
   ├── Build de imagen Docker
   └── Test de que el contenedor arranca
```

---

## 🔐 Secrets Requeridos

> 📘 **Guía completa paso a paso:** Ver [`.github/QUICK_START.md`](.github/QUICK_START.md)

Configura estos secrets en **GitHub Settings → Secrets and variables → Actions**:

### 🔑 Secrets Obligatorios (Staging)

| Secret | Value | Descripción |
|--------|-------|-------------|
| `DOCKER_USERNAME` | Tu usuario Docker Hub | Ej: `dio-quincar` |
| `DOCKER_PASSWORD` | Access Token Docker Hub | dockerhub.com/settings/security |
| `STAGING_HOST` | `149.130.165.200` | IP de tu instancia Oracle |
| `STAGING_USER` | `ubuntu` | Usuario SSH |
| `STAGING_SSH_KEY` | Contenido de `~/.ssh/cloushellkey` | Tu llave privada |

### 🔒 Secrets Opcionales (Producción - Pendiente)

| Secret | Descripción |
|--------|-------------|
| `PROD_HOST` | IP de producción (cuando la tengas) |
| `PROD_USER` | Usuario SSH de producción |
| `PROD_SSH_KEY` | Llave privada de producción |

> ⚠️ **Nota:** El deploy a producción está deshabilitado hasta que configures `PROD_HOST`.

---

## ☁️ Setup en Oracle Cloud Free Tier

### 1. Conéctate a tu Instancia (Staging)

```bash
# Tu instancia ya está creada
ssh -i ~/.ssh/cloushellkey ubuntu@149.130.165.200
```

### 2. Configurar con Script Automático

```bash
# Una vez conectado al servidor
cd ~
git clone https://github.com/dio-quincarDev/pymes-admin.git
cd pymes-admin/scripts

# Ejecutar
chmod +x setup-server.sh
./setup-server.sh
```

### 3. Configurar Security List en Oracle Cloud

```
Oracle Cloud Console → Virtual Cloud Network → Security Lists → Add Ingress Rules
```

| Puerto | Protocolo | Descripción |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 8081 | TCP | Auth Service |
| 80 | TCP | HTTP (opcional) |
| 443 | TCP | HTTPS (opcional) |

---

## 🛠️ Comandos Útiles

### Local Development

```bash
# Backend
cd backend/auth
./mvnw clean package
./mvnw test
./mvnw spring-boot:run

# Frontend
cd frontend/pymes
npm install
npm run dev
npm run build
npm run lint
```

### Docker

```bash
# Levantar todos los servicios
docker compose -f backend/docker-compose.yml up -d

# Ver logs
docker compose -f backend/docker-compose.yml logs -f

# Detener servicios
docker compose -f backend/docker-compose.yml down

# Rebuildar contenedores
docker compose -f backend/docker-compose.yml up -d --build
```

### CI/CD

```bash
# Verificar workflows (GitHub CLI)
gh run list

# Ver logs de un run
gh run view <run-id> --log

# Trigger manual de deploy a prod
gh workflow run cd-prod.yml --field version=v1.0.0
```

---

## 📊 Servicios

| Servicio | Puerto | URL Local | Descripción |
|----------|--------|-----------|-------------|
| Auth Service | 8081 | http://localhost:8081 | API REST de autenticación |
| PostgreSQL | 5435 | localhost:5435 | Base de datos Auth |
| Redis | 6379 | localhost:6379 | Cache para JWT/Sessions |
| Swagger UI | 8081 | http://localhost:8081/swagger-ui.html | Documentación API |

---

## ✅ Checklist de Configuración

### Secrets en GitHub
- [ ] Crear cuenta en Docker Hub
- [ ] Generar Access Token en Docker Hub
- [ ] Configurar `DOCKER_USERNAME` en GitHub Secrets
- [ ] Configurar `DOCKER_PASSWORD` en GitHub Secrets
- [ ] Configurar `STAGING_HOST` (`149.130.165.200`) en GitHub Secrets
- [ ] Configurar `STAGING_USER` (`ubuntu`) en GitHub Secrets
- [ ] Configurar `STAGING_SSH_KEY` (contenido de `~/.ssh/cloushellkey`) en GitHub Secrets

### Setup del Servidor
- [ ] Conectarse a la instancia: `ssh -i ~/.ssh/cloushellkey ubuntu@149.130.165.200`
- [ ] Clonar el repositorio en el servidor
- [ ] Editar `scripts/setup-server.sh` con tu URL de GitHub
- [ ] Ejecutar `./setup-server.sh`
- [ ] Configurar Security List en Oracle Cloud (puertos 22, 8081)
- [ ] Copiar `.env.example` a `.env` y configurar valores reales

### Pruebas
- [ ] Hacer push a `develop` para testear staging
- [ ] Verificar logs en GitHub Actions
- [ ] Verificar que el servicio esté corriendo en el servidor

---

## 🆘 Troubleshooting

| Problema | Solución |
|----------|----------|
| Build falla en CI pero funciona local | Verificar versión de Java, limpiar caché Maven |
| Tests fallan solo en CI | Revisar variables de entorno en el workflow |
| Deploy falla con error de SSH | Verificar que la SSH key esté bien copiada (incluye BEGIN/END) |
| Docker no encuentra la imagen | Verificar DOCKER_USERNAME y DOCKER_PASSWORD |
| Servicio no arranca en el servidor | Revisar logs: `docker compose logs -f` |
| Puerto no accesible | Verificar Security List en Oracle Cloud |

---

## 📚 Recursos

- [Documentación CI/CD](.github/SECRETS.md)
- [GitHub Actions Docs](https://docs.github.com/es/actions)
- [Spring Boot + Docker](https://spring.io/guides/topicals/spring-boot-docker/)
- [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/)

---

## 📄 Licencia

Ver [LICENSE](LICENSE)

---

*Última actualización: Marzo 2026*
