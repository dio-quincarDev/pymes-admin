# PyMes Admin - API Gateway 🚪

> **Spring Cloud Gateway** - Punto de entrada único y validador de seguridad para la plataforma PyMes Admin

---

## 📋 Descripción

Este microservicio actúa como el **API Gateway** central de la arquitectura, encargándose de:

- **Enrutamiento de solicitudes** hacia los microservicios internos (Auth, Core, IA)
- **Validación de tokens JWT** antes de permitir el acceso a rutas protegidas
- **Gestión de CORS** para permitir peticiones desde el frontend PWA
- **Rate limiting** y protección contra abusos (próximamente)

### Rol en la Arquitectura Zero-Trust

```
┌─────────────┐     ┌─────────────────────┐     ┌──────────────────┐
│   Cliente   │────▶│   API Gateway       │────▶│  Auth Service    │
│  (PWA/APP)  │     │  (Puerto 8080)      │     │  (Puerto 8081)   │
└─────────────┘     │  • Valida JWT       │     └──────────────────┘
                    │  • Enruta rutas     │
                    │  • Gestiona CORS    │
                    └─────────────────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  Redis Cache     │
                    │  (Session/Keys)  │
                    └──────────────────┘
```

---

## 🏗️ Arquitectura

### Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Framework** | Spring Boot | 3.3.10 |
| **Gateway** | Spring Cloud Gateway | 2023.0.1 |
| **Java** | Eclipse Temurin | 21 |
| **JWT** | JJWT (io.jsonwebtoken) | 0.12.6 |
| **Build Tool** | Maven | 3.9.9 |

### Dependencias Clave

```xml
<!-- Spring Cloud Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- JWT (Validación de tokens) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

---

## 🚀 Rutas Configuradas

| Ruta | Predicado | Destino | Descripción |
|------|-----------|---------|-------------|
| `/api/v1/auth/**` | Path | `http://auth-service:8081` | Autenticación y gestión de usuarios |
| `/api/v1/login/**` | Path | `http://auth-service:8081` | Endpoints de login |
| `/api/v1/oauth2/**` | Path | `http://auth-service:8081` | OAuth2 (Google/Facebook) |

### Configuración de Rutas (`application.yaml`)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://${AUTH_SERVICE_HOST:pymes-auth-service}:8081
          predicates:
            - Path=/api/v1/auth/**, /api/v1/login/**, /api/v1/oauth2/**
```

---

## ⚙️ Variables de Entorno

| Variable | Valor por Defecto | Descripción |
|----------|-------------------|-------------|
| `AUTH_SERVICE_HOST` | `pymes-auth-service` | Hostname del Auth Service (Docker network) |
| `REDIS_HOST` | `pymes-redis-auth` | Hostname de Redis para validación de sesiones |
| `SERVER_PORT` | `8080` | Puerto de exposición del Gateway |

---

## 🐳 Ejecución con Docker

### Build de la Imagen

```bash
# Desde la raíz del proyecto
docker build -t pymes-gateway:latest ./backend/gateway-pymes
```

### Ejecutar con Docker Compose

```bash
# Levantar toda la infraestructura (Gateway + Auth + PostgreSQL + Redis)
docker compose up -d
```

### Ver Logs

```bash
docker logs -f pymes-gateway
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## 💻 Ejecución Local (Desarrollo)

### Requisitos Previos

- Java 21+ instalado
- Maven 3.9+
- Auth Service corriendo en `localhost:8081`
- Redis corriendo en `localhost:6379` (opcional, para validación de sesiones)

### Pasos

```bash
cd backend/gateway-pymes

# Compilar
./mvnw clean package -DskipTests

# Ejecutar
./mvnw spring-boot:run
```

### Configurar Hosts Locales (Opcional)

Si necesitas apuntar a servicios en Docker desde tu máquina local:

```bash
# En /etc/hosts (Linux/Mac) o C:\Windows\System32\drivers\etc\hosts (Windows)
127.0.0.1 pymes-auth-service
127.0.0.1 pymes-redis-auth
```

---

## 🔒 Seguridad

### Validación de JWT

El Gateway actúa como **guardia de seguridad**:

1. Intercepta todas las solicitudes entrantes
2. Verifica la presencia del header `Authorization: Bearer <token>`
3. Valida la firma del JWT usando la clave pública del Auth Service
4. Si es válido → enruta al microservicio destino
5. Si es inválido → retorna `401 Unauthorized`

### CORS Configurado

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins: "*"
      allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
      allowedHeaders: "*"
```

> ⚠️ **Nota:** La configuración actual permite todos los orígenes (`*`). En producción, restringir a dominios específicos.

---

## 🧪 Testing

### Tests Unitarios

```bash
./mvnw test
```

### Health Endpoints

| Endpoint | Descripción |
|----------|-------------|
| `GET /actuator/health` | Estado de salud del servicio |
| `GET /actuator/info` | Información de la aplicación |

---

## 📊 Métricas y Monitoreo

Spring Boot Actuator está habilitado para:

- **Health Checks**: Verificar estado del servicio
- **Info Endpoint**: Metadata de la aplicación
- **Future**: Métricas de Prometheus, trazas de Micrometer

---

## 🔧 Troubleshooting

### El Gateway no puede conectar al Auth Service

```bash
# Verificar que el Auth Service esté corriendo
docker ps | grep auth

# Ver logs del Gateway
docker logs pymes-gateway

# Probar conectividad desde el contenedor
docker exec -it pymes-gateway wget -qO- http://pymes-auth-service:8081/actuator/health
```

### Error de CORS desde el Frontend

Verificar que el frontend esté usando la URL correcta del Gateway:
- Frontend en Docker: `http://pymes-gateway:8080`
- Frontend local: `http://localhost:8080`

---

## 📁 Estructura del Proyecto

```
gateway-pymes/
├── .mvn/                    # Maven wrapper
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/dioquincar/gateway_pymes/
│   │   │       └── GatewayPymesApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/
│           └── dev/dioquincar/gateway_pymes/
│               └── GatewayPymesApplicationTests.java
├── Dockerfile               # Multi-stage build (Maven → JRE)
├── pom.xml                  # Dependencias Maven
└── README.md
```

---

## 🔗 Enlaces Relacionados

- [Documentación Principal del Proyecto](../../README.md)
- [Auth Service](../auth/README.md)
- [Guía de Quick Start](../../.github/QUICK_START.md)
- [Spring Cloud Gateway Docs](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)

---

<div align="center">

**PyMes Admin - API Gateway** | Parte de la arquitectura SaaS para PYMEs

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.10-6db33f.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0.1-6db33f.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-21-007396.svg)](https://openjdk.java.net/)

</div>
