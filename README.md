# PyMes Admin - SaaS Financial Management Platform 🚀

> **Sistema de Gestión Financiera con IA para PYMEs**  
> Multi-tenant SaaS platform con toolkit de contabilidad forense impulsado por IA

---

## 📋 Estado del Proyecto

| Componente | Estado | Tecnología |
|------------|--------|------------|
| **Backend Auth** | ✅ Configurado | Java 21 + Spring Boot 3.4.3 |
| **Frontend** | ✅ Configurado | Quasar 2 + Vue 3 + TypeScript (PWA) |
| **Database** | ✅ Configurado | PostgreSQL 15 (multi-tenant ready) |
| **Cache** | ✅ Configurado | Redis 7 |
| **CI/CD Staging** | ✅ Configurado | GitHub Actions + OCI Free Tier |
| **CI/CD Producción** | ⏳ Pendiente | Oracle Cloud Free Tier |
| **IA Toolkit** | 📝 En planificación | Python + FastAPI + Claude API |

---

## 🎯 Visión del Producto

### De MVP a SaaS Platform

**Modelo de Negocio:**
- **Básico** - $15/mes: Core features, 1 usuario, 100 transacciones/mes
- **Profesional** - $40/mes: Análisis avanzados, 3 usuarios, transacciones ilimitadas
- **Enterprise** - $80/mes: **IA Toolkit completo**, usuarios ilimitados, API access

**Diferenciadores Clave:**
| Característica | PyMes Admin | Competencia |
|---|---|---|
| Escaneo QR facturas | ✅ PWA nativo | ❌ Manual |
| IA Contabilidad Forense | ✅ Incluida | ❌ $200+/mes |
| Asistente Conversacional | ✅ Claude API | ❌ Inexistente |
| Precio PYME LATAM | ✅ Desde $15/mes | ⚠️ $50-200/mes |

---

## 🏗️ Arquitectura SaaS

### Stack Tecnológico

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (PWA)                        │
│              Vue.js 3 + Quasar + TypeScript              │
│         Multi-tenant UI configurables por industria      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Nginx Proxy Manager                     │
│                    (Reverse Proxy)                       │
└─────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   Auth Service   │ │   Core Service   │ │    IA Service    │
│  Spring Boot 3   │ │  Spring Boot 3   │ │   Python/FastAPI │
│    (JWT + RBAC)  │ │  (Multi-tenant)  │ │  (Forensic AI)   │
└──────────────────┘ └──────────────────┘ └──────────────────┘
            │               │               │
            ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   PostgreSQL     │ │     Redis        │ │   MinIO / S3     │
│ (Schema/tenant)  │ │  (Cache/Sessions)│ │  (Facturas/Img)  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

### Multi-tenancy Strategy

```java
// Estrategia: Schema por tenant (PostgreSQL)
@Entity
@Table(name = "gastos", schema = "tenant_schema")
public class Gasto {
    // Aislamiento total de datos por cliente
}

// Alternative: Row-level security con tenant_id
@Where(clause = "tenant_id = :tenantId")
public class Transaccion { ... }
```

---

## 🤖 IA Toolkit - Contabilidad Forense

### Módulos Inteligentes

| Módulo | Funcionalidad | Impacto |
|--------|---------------|---------|
| **Detección de Anomalías** | Gastos fuera de patrón, transacciones duplicadas | 🔴 Alertas en tiempo real |
| **Análisis Predictivo** | Proyección flujo de caja (30/60/90 días) | 📈 Previene déficits |
| **Optimización Automática** | Sugerencias de ahorro, proveedores caros | 💰 Ahorro promedio 18% |
| **Análisis Forense** | Auditoría automática, benchmarks industria | ⚖️ Cumplimiento normativo |
| **Asistente Conversacional** | Claude API para queries en lenguaje natural | 💬 "¿Por qué bajaron mis ventas?" |

### Ejemplo: Reporte Forense Automático

```
📊 Reporte Semanal Automático:

🔴 ANOMALÍAS DETECTADAS:
- Gasto "Limpieza" aumentó 250% ($50 → $175)
- Transacción duplicada: Proveedor X, $45, 05/03

⚠️ ALERTAS PREDICTIVAS:
- Flujo de caja negativo proyectado en 15 días

💡 RECOMENDACIONES:
1. Cambiar Proveedor A → B (ahorro: $120/mes)
2. Reducir gasto "Servicios" 15% = $90/mes extra

📈 BENCHMARK:
- Tu margen: 12% | Promedio industria: 18%
```

---

## 🚀 Quick Start

### Para Desarrolladores

```bash
# Clonar repositorio
git clone https://github.com/dio-quincarDev/pymes-admin.git
cd pymes-admin

# Frontend
cd frontend/pymes
npm install
npm run dev

# Backend (otra terminal)
cd backend/auth
./mvnw spring-boot:run
```

### Deploy en Servidor (Staging)

```bash
# En tu instancia Oracle Cloud Ubuntu
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Clonar y configurar
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin
cd ~/pymes-admin
cp backend/auth/.env.example backend/auth/.env
# Editar .env con valores reales

# Levantar servicios
docker compose -f docker-compose.yml up -d
```

---

## 🔄 CI/CD Pipeline

### Ramas y Estrategia de Deploy

| Rama | Propósito | Pipeline | Deploy |
|------|-----------|----------|--------|
| `main` | Producción estable | CI + CD Prod | ✅ Producción |
| `develop` | Integración features | CI + CD Staging | ✅ Staging (OCI) |
| `feature/*` | Desarrollo features | CI (build + test) | ❌ Solo test |

### Workflows GitHub Actions

| Workflow | Trigger | Descripción |
|----------|---------|-------------|
| **CI** | Push/PR a `main`, `develop`, `feature/*` | Build, tests, security scan |
| **CD Staging** | Push a `develop` | Deploy automático a OCI Free Tier |
| **CD Prod** | Push a `main` | Deploy a producción (manual gate) |

### Pipeline CI (cada push/PR)

```yaml
1. 🔒 Security Check
   ├── Verifica .env no trackeados
   └── Escanea secrets/API keys

2. 🔨 Build Backend Auth
   ├── Java 21 + Maven cache
   ├── mvn clean package
   └── mvn test

3. 🎨 Build Frontend
   ├── Node.js 20
   ├── npm ci
   ├── npm run lint
   └── npx quasar build

4. 🐳 Docker Build & Push
   ├── Build imagen Auth Service
   ├── Build imagen Frontend
   └── Push a Docker Hub
```

---

## 🔐 Secrets Requeridos

Configura en **GitHub Settings → Secrets and variables → Actions**:

### Secrets Obligatorios (Staging)

| Secret | Value | Descripción |
|--------|-------|-------------|
| `DOCKER_USERNAME` | Tu usuario Docker Hub | Ej: `dio-quincar` |
| `DOCKER_PASSWORD` | Access Token Docker Hub | dockerhub.com/settings/security |
| `STAGING_HOST` | IP de tu instancia OCI | Ver `.github/QUICK_START.md` |
| `STAGING_USER` | `ubuntu` | Usuario SSH |
| `STAGING_SSH_KEY` | Contenido de tu llave privada SSH | Ver `.github/QUICK_START.md` |

### Secrets Opcionales (Producción)

| Secret | Value | Descripción |
|--------|-------|-------------|
| `PROD_HOST` | IP de instancia producción | Oracle Cloud Free Tier |
| `PROD_USER` | `ubuntu` | Usuario SSH |
| `PROD_SSH_KEY` | Llave privada producción | SSH key |

📘 **Guía completa:** Ver [`.github/QUICK_START.md`](.github/QUICK_START.md)

---

## 📁 Estructura del Proyecto

```
pymes-admin/
├── docker-compose.yml         # Orquestación principal (raíz)
├── backend/
│   └── auth/                  # Microservicio Autenticación (Spring Boot)
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
├── frontend/
│   └── pymes/                 # Aplicación Quasar/Vue.js (PWA)
│       ├── Dockerfile
│       ├── nginx.conf
│       ├── package.json
│       └── src/
├── .github/
│   ├── workflows/             # CI/CD Pipelines
│   │   ├── ci.yml             # Build + Tests
│   │   ├── cd-staging.yml     # Deploy a OCI Staging
│   │   └── cd-prod.yml        # Deploy a Producción
│   ├── QUICK_START.md         # Guía setup CI/CD
│   └── SECRETS.md             # Documentación secrets
├── scripts/
│   ├── setup-server.sh        # Setup servidor Ubuntu (primera vez)
│   └── deploy-staging.sh      # Deploy manual (cada vez)
└── README.md
```

---

## 🏛️ Infraestructura OCI Free Tier

### Instancia Staging

| Especificación | Valor |
|----------------|-------|
| **Shape** | VM.Standard.A1.Flex (ARM) |
| **OCPUs** | 2 |
| **RAM** | 12 GB |
| **Storage** | 50 GB |
| **OS** | Ubuntu 22.04 |

### Docker Networks

| Red | Propósito | Tipo |
|-----|-----------|------|
| `pymes-global-network` | Para Nginx Proxy Manager | Externa |
| `pymes-internal-network` | Para DB, Redis, backend | Bridge |

### Security List Rules

| Puerto | Protocolo | Descripción |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (Nginx Proxy Manager) |
| 443 | TCP | HTTPS (Nginx Proxy Manager) |

---

## 📊 Roadmap

### Fase 1: MVP Multi-tenant (Q2 2026)
- [ ] Core features multi-tenant
- [ ] Escaneo QR facturas
- [ ] Reportes básicos
- [ ] IA básica: detección anomalías

### Fase 2: Beta con Usuarios Piloto (Q3 2026)
- [ ] 10-20 negocios piloto
- [ ] Feedback y ajustes
- [ ] Mejora modelos IA con datos reales

### Fase 3: Lanzamiento (Q4 2026)
- [ ] Marketing PYMEs Panamá/LATAM
- [ ] Integración Claude API
- [ ] Certificaciones seguridad

---

## 🤝 Contribuir

1. Fork del repositorio
2. Crear rama `feature/nueva-funcionalidad`
3. PR a `develop` (no directamente a `main`)
4. CI debe pasar antes de merge

---

## 📄 Licencia

MIT License - ver [LICENSE](LICENSE)

---

## 📞 Contacto

- **Repo:** https://github.com/dio-quincarDev/pymes-admin

---

<div align="center">

**PyMes Admin** - Empoderando PYMEs con IA 💡

[![CI/CD](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>
