# 📜 Contrato de Propagación de Identidad (Identity Headers)

Este estándar define cómo el API Gateway comunica la identidad del usuario a los microservicios internos tras una validación exitosa.

## 1. Contexto de Seguridad
El API Gateway realiza la validación primaria (Firma JWT + Expiración + Blacklist Redis). Tras el éxito, inyecta la identidad en la petición interna.

## 2. Cabeceras Obligatorias (Gateway -> Microservicio)

| Cabecera | Tipo | Descripción |
|----------|------|-------------|
| `X-User-Id` | `Long` | ID del usuario autenticado. |
| `X-User-Email` | `String` | Email del usuario (Subject del JWT). |
| `X-Tenant-Id` | `Long` | ID de la empresa/tenant activa. |
| `X-User-Role` | `String` | Rol jerárquico (OWNER, ADMIN, etc). |

## 3. Estrategia de Consumo
Los microservicios internos (Auth, Core, IA) pueden utilizar estas cabeceras para:
1.  **Contexto Multi-tenant**: Filtrar datos por `X-Tenant-Id`.
2.  **Auditoría**: Registrar acciones usando `X-User-Id`.
3.  **Autorización Ligera**: Validar permisos basados en `X-User-Role`.

## 4. Aislamiento de Red (Seguridad)
Para garantizar la integridad de estas cabeceras:
*   **Solo el Gateway** debe estar expuesto al exterior.
*   Los microservicios internos **deben rechazar** tráfico que no provenga de la red del Gateway o que intente suplantar estas cabeceras desde el exterior (el Gateway debe limpiar estas cabeceras si vienen en la petición original del cliente).
