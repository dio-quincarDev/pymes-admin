# PyMes Admin - Auth Microservice 🔐

> **Spring Boot OAuth2** - Servicio de autenticación, gestión de usuarios y multi-tenancy para la plataforma PyMes Admin

---

## 📋 Descripción

Este microservicio es el **centro de identidad** de la arquitectura, responsable de la autenticación, identidad del usuario y gestión de la estructura multi-tenant.

### 🏗️ Evolución Arquitectónica: Desacoplamiento de Dominios
Recientemente, el servicio ha sido refactorizado desde un modelo monolítico hacia una **arquitectura orientada a dominios (SRP)**, logrando una separación clara de responsabilidades:

1.  **Auth Domain**: Autenticación pura (Login, Registro, Refresh, Logout).
2.  **User Domain**: Identidad y perfil del usuario autenticado.
3.  **Tenant Domain**: Estructura multi-tenant (Crear y seleccionar empresas).
4.  **Member Domain**: Gestión de usuarios *dentro* de una empresa (Roles y permisos).
5.  **Invitation Domain**: Ciclo de vida completo de invitaciones.

---

## 🌐 Endpoints (V1)

La API está organizada bajo la ruta base `/api/v1` y sigue una estructura RESTful por recursos:

### 🔑 Autenticación (`/auth`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/register` | Registro atómico (User + Tenant FREE + OWNER) |
| `POST` | `/login` | Login email/password (Rate limited) |
| `POST` | `/refresh` | Refresca el access token |
| `POST` | `/logout` | Invalida la sesión actual |

### 👤 Usuarios (`/users`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/me` | Obtiene el perfil del usuario autenticado |

### 🏢 Tenants (`/tenants`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista todas las empresas a las que pertenece el usuario |
| `POST` | `/` | Crea una nueva empresa/tenant |
| `POST` | `/select` | Selecciona la empresa activa y genera nuevos tokens |

### 👥 Miembros (`/tenants/{id}/members`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista los miembros activos del tenant (OWNER/ADMIN) |
| `PUT` | `/{userId}/role` | Cambia el rol de un miembro (Validación de jerarquía) |
| `DELETE` | `/{userId}` | Desvincula a un miembro del tenant (Solo OWNER) |

### ✉️ Invitaciones (`/invitations`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista invitaciones pendientes del usuario actual |
| `POST` | `/` | Envía una invitación a un nuevo usuario |
| `POST` | `/accept` | Acepta una invitación activa |
| `DELETE` | `/{id}` | Cancela una invitación pendiente |

---

## 🛠️ Stack Tecnológico & Calidad

- **Core:** Spring Boot 3.4.3, Java 21, MapStruct (Mapeo automático).
- **Seguridad:** Spring Security OAuth2 (Google/FB), JWT, Redis (Blacklist & Rate Limiting).
- **Persistencia:** PostgreSQL, Flyway, Soft Delete Forense.
- **Calidad:** JUnit 5, Mockito, Pirámide de pruebas (Unitarias + Contexto).

---

<div align="center">

**PyMes Admin - Auth Microservice** | Estado: **Desacoplado & Escalable** 💎

[![Build & Test](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

</div>
