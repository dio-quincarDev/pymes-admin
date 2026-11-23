# Guía de operación

> Para desplegar, monitorear y recuperar el servicio. Antes de tocar secretos o
> workflows, lee la [guía de administración](admin-guide.md).

## Puertos y red

| Servicio | Puerto | Expuesto a internet |
|----------|--------|---------------------|
| Gateway | 8080 | No (Caddy proxy) |
| Auth | 8081 | No |
| Core | 8082 | No |
| Frontend | 9200 | No |

Caddy termina TLS y redirige al gateway o al frontend. Las redes Docker son
`proxy-caddy-network` (externa) y `pymes-internal-network`. Si hay Cloudflare
adelante: SSL/TLS en modo **Full** (no Flexible) y **Bot Fight Mode apagado**
(bloquea los XHR POST con 403).

`Source: AGENTS.md`, `.github/DEPLOYMENT.md`

## Comandos en el servidor

```bash
docker compose ps                      # estado
docker compose logs -f                 # todos los logs
docker compose logs -f gateway         # logs de un servicio
docker compose restart                 # reiniciar
docker compose down                    # parar
docker compose up -d                   # arrancar
docker image prune -f --filter "until=24h"  # limpieza
```

`Source: .github/DEPLOYMENT.md`

## CI/CD

- **CI** corre en push/PR a `main`, `develop` y `feature/*`. Un push a `feature/**`
  se salta si solo cambian `*.md`.
- **CD staging** se dispara automáticamente cuando CI pasa en `develop`.
- **CD producción** se dispara cuando CI pasa en `main`.
- Los PRs apuntan a `develop`, nunca a `main`.

Si el CD no dispara, revisa que CI haya terminado bien primero. Los workflows de CD
solo se registran desde `main`.

`Source: .github/DEPLOYMENT.md`, `AGENTS.md`

## Reenviar un reporte ya enviado

El scheduler corre solo el día 1 a las 06:00 (Panamá, `0 0 6 1 * *`) y solo retoma
filas `FAILED`: un período `SENT` **no** se regenera solo. Para reenviar un período
ya enviado, desde el host de staging (sin usuario ni contraseña, por la red interna
de Docker):

**1. Marcar la fila como `FAILED`:**

```sql
docker exec -i pymes-postgres-db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
update core.report_log r
   set status = 'FAILED', error_msg = null
  from auth.tenants t
 where t.id = r.tenant_id
   and t.name = '<TENANT>'
   and r.period = '<YYYY-MM>' and r.format = 'PDF'
returning r.tenant_id, r.period, r.status;
```

**2. Validar sin enviar** (genera el PDF en memoria, no escribe ni manda correo):

```bash
IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' pymes-core-service)
curl -s -w '\nHTTP %{http_code}\n' -X POST \
  "http://$IP:8082/api/v1/core/reportes/monthly?period=<YYYY-MM>&dryRun=true" \
  -H "X-User-Role: ADMIN"
```

Si el `dryRun` sale bien, quita `&dryRun=true` para enviarlo de verdad.

`Source: docs/DAILY_REPORTS_PROJECT.md` (2026-11-02)

## Problemas conocidos

| Síntoma | Causa / solución |
|---------|------------------|
| El navegador sirve un bundle JS viejo con URLs `localhost:8080` | Caché stale de la PWA. Limpiar datos del sitio (DevTools → Storage → Clear All). El service worker limpia `core-api-cache` al hacer logout. |
| Un SVG (logos) no cambia tras actualizarlo | No tiene hash en el nombre y Cloudflare lo cachea. Purgar caché de Cloudflare. En Caddy usar `path_regexp \.svg$` (no `path *.svg`). |
| `no matching manifest for linux/arm64/v8` | Imágenes no construidas para ARM64. Push a `develop` para reconstruir multi-arch. |
| `network not found` | Crear las redes: `docker network create proxy-caddy-network` y `pymes-internal-network`. |
| `Permission denied (publickey)` | La llave SSH en `STAGING_SSH_KEY` está incompleta. |
| POST con 403 detrás de Cloudflare | Bot Fight Mode activo o SSL en Flexible. Apagar Bot Fight Mode y usar Full. |

`Source: AGENTS.md`, `.github/DEPLOYMENT.md`

## Runbooks pendientes

| Runbook | Estado |
|---------|--------|
| Reenvío de reporte mensual/trimestral | **Implemented** (arriba) |
| Gateway caído / 502, auth caído, core caído | **Planned** |
| Redis sin espacio o blacklist caída (fail-open) | **Planned** |
| Migración Flyway fallida | **Planned** |
| Rollback de deploy y recuperación de backup | **Planned** |

Fuentes para escribirlos: `.github/DEPLOYMENT.md`, `docs/strategies/INFRA_STRATEGY.md`,
`docker-compose.yml`, `infra/monitoring/`.
