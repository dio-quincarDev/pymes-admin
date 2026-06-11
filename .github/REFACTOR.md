# Estrategia de simplificación de CI/CD para el proyecto Pymes

## 1. Objetivo

Reducir la complejidad de los workflows de GitHub Actions (`ci.yml`, `cd-staging.yml`, `cd-prod.yml`) y del `docker-compose.yml` del proyecto Pymes, tomando como referencia las buenas prácticas observadas en el proyecto Portfolio (más simple y robusto). Se busca:

- Eliminar dependencias de archivos `.env` locales en los pipelines.
- Unificar la gestión de variables sensibles usando **GitHub Secrets**.
- Simplificar los comandos de ejecución de tests.
- Eliminar scripts externos (como `deploy-staging.sh`).
- Mantener la separación de entornos (staging / producción) y todas las capacidades actuales (testcontainers, multi‑arquitectura, despliegue por SSH).

## 2. Análisis de problemas actuales

| Componente | Problema detectado | Causa |
|------------|--------------------|-------|
| `docker-compose.yml` | Uso de `env_file: backend/auth/.env` en los servicios `gateway` y `auth-service` | Ese archivo no existe en el servidor de destino ni en el runner de GitHub Actions. |
| `ci.yml` | Ejecución de tests con perfiles específicos (`-Dspring.profiles.active=test/integration`) | Provoca conflictos con `@DynamicPropertySource` de Testcontainers y añade parámetros innecesarios. |
| `cd-staging.yml` / `cd-prod.yml` | Llamada a un script externo (`./scripts/deploy-staging.sh`) | Añade complejidad, dependencia de ficheros en el repositorio y dificulta la trazabilidad. |
| `cd-staging.yml` / `cd-prod.yml` | Copia de `.env.example` (`cp .env.example .env`) | El pipeline no debe generar archivos `.env` porque las credenciales reales deben venir de GitHub Secrets. |
| General | Los workflows duplican lógica de construcción de imágenes y despliegue | Aumenta el mantenimiento y la posibilidad de errores. |

## 3. Soluciones adoptadas (basadas en el proyecto Portfolio)

### 3.1. `docker-compose.yml` – Eliminar `env_file` y unificar variables de entorno

**Antes:**
```yaml
gateway:
  env_file:
    - backend/auth/.env
  environment:
    - JWT_SECRET=${JWT_SECRET:-...}
auth-service:
  env_file:
    - backend/auth/.env
```

**Después:**
```yaml
gateway:
  environment:
    - JWT_SECRET=${JWT_SECRET}
    - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
    # etc.
auth-service:
  environment:
    - DB_HOST=${DB_HOST:-pymes-postgres-auth}
    - DB_USERNAME=${DB_USERNAME}
    - DB_PASSWORD=${DB_PASSWORD}
    - JWT_SECRET=${JWT_SECRET}
    - REDIS_HOST=${REDIS_HOST:-pymes-redis-auth}
    - REDIS_PORT=${REDIS_PORT:-6379}
postgres-auth:
  environment:
    POSTGRES_DB: ${DB_NAME:?required}
    POSTGRES_USER: ${DB_USERNAME:?required}
    POSTGRES_PASSWORD: ${DB_PASSWORD:?required}
```

**Por qué:**  
- Se elimina la dependencia de un archivo `.env` que no existe en producción/CI.  
- Todas las variables se resuelven mediante `${VAR}` que `docker compose` leerá de un archivo `.env` ubicado en el mismo directorio (creado dinámicamente en el despliegue).  
- Se evitan conflictos entre valores por defecto y secrets reales.

### 3.2. Workflows – Eliminar perfiles Spring y comandos innecesarios

**En `ci.yml`** (tests):

- **Antes:**
  ```yaml
  run: ./mvnw test -B -Dspring.profiles.active=test          # unit tests
  run: ./mvnw verify -B -Dspring-boot.run.profiles=integration  # integration
  ```
- **Después:**
  ```yaml
  run: ./mvnw test -B         # unit tests (sin perfil)
  run: ./mvnw verify -B       # integration tests (sin perfil)
  ```

**Por qué:**  
- `AbstractIntegrationTest` ya usa `@DynamicPropertySource` con Testcontainers, que inyecta automáticamente las URLs de PostgreSQL y Redis.  
- El perfil `integration` no es necesario y puede interferir con la configuración dinámica.  
- Los tests unitarios tampoco necesitan un perfil específico porque `application-test.yml` se carga automáticamente al estar en `src/test/resources`.

### 3.3. Workflows – Eliminar scripts externos y generar `.env` remoto

**En `cd-staging.yml` y `cd-prod.yml`**:

- **Antes (llamada a script):**
  ```yaml
  - name: Deploy via SSH
    uses: appleboy/ssh-action@v1.0.3
    with:
      script: ./scripts/deploy-staging.sh
  ```
- **Después (comandos directos):**
  ```yaml
  - name: Deploy via SSH
    uses: appleboy/ssh-action@v1.0.3
    with:
      host: ${{ secrets.STAGING_HOST }}
      username: ${{ secrets.STAGING_USER }}
      key: ${{ secrets.STAGING_SSH_KEY }}
      script: |
        cd ~/pymes-admin
        cat > .env <<EOF
        DOCKER_USERNAME=${{ secrets.DOCKER_USERNAME }}
        TAG=${{ needs.build-and-test.outputs.version }}
        DB_NAME=pymes_auth
        DB_USERNAME=${{ secrets.DB_USERNAME }}
        DB_PASSWORD=${{ secrets.DB_PASSWORD }}
        JWT_SECRET=${{ secrets.JWT_SECRET }}
        CORS_ALLOWED_ORIGINS=${{ secrets.CORS_ALLOWED_ORIGINS_STAGING }}
        FRONTEND_PORT=9200
        GATEWAY_PORT=8080
        REDIS_HOST=pymes-redis-auth
        REDIS_PORT=6379
        EOF
        docker compose pull
        docker compose up -d
        docker image prune -af
  ```

**Por qué:**  
- Se elimina la dependencia de scripts externos que pueden desactualizarse.  
- Se genera el archivo `.env` directamente en el servidor con los valores provenientes de GitHub Secrets (nunca se escribe en el runner).  
- Todo el proceso de despliegue queda autocontenido en el workflow, más fácil de auditar y modificar.

### 3.4. Workflows – Eliminar copia de `.env.example`

En todos los workflows donde aparezca:
```yaml
- name: Create .env from example
  run: cp .env.example .env
```
Se **elimina** ese paso.

**Por qué:**  
- Los tests unitarios e integración no necesitan ningún `.env` porque Testcontainers y las configuraciones de prueba son suficientes.  
- Las credenciales reales jamás deben ser almacenadas en el repositorio ni generadas a partir de un ejemplo dentro del pipeline.  

### 3.5. Workflows – Unificar generación de versión (opcional)

Se mantiene el mismo método de generación de versión (fecha + commit short) en un solo job `build-and-test` cuyos outputs se reutilizan en jobs posteriores. No se unifican los archivos (seguirán separados `ci.yml`, `cd-staging.yml`, `cd-prod.yml`), pero se evita duplicar lógica de construcción.

**Por qué:**  
- Separar CI (tests) de CD (despliegue) por entorno sigue siendo útil para claridad.  
- No se fusionan para no introducir complejidad adicional.

## 4. Implementación paso a paso

### 4.1. Modificaciones en `docker-compose.yml`
- [ ] Eliminar las líneas `env_file:` de los servicios `gateway` y `auth-service`.
- [ ] Asegurar que todas las variables necesarias estén definidas con `${VAR}` o `${VAR:-default}`.
- [ ] Verificar que el servicio `postgres-auth` use las mismas variables `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`.

### 4.2. Configurar GitHub Secrets
Crear los siguientes secrets en el repositorio (Settings → Secrets and variables → Actions):

| Secret name | Purpose |
|-------------|---------|
| `DOCKER_USERNAME` | Usuario de Docker Hub |
| `DOCKER_PASSWORD` | Token/contraseña de Docker Hub |
| `STAGING_HOST` | IP o dominio del servidor staging |
| `STAGING_USER` | Usuario SSH (ej. `ubuntu`, `root`) |
| `STAGING_SSH_KEY` | Clave privada SSH para staging |
| `PROD_HOST` | IP o dominio del servidor producción |
| `PROD_USER` | Usuario SSH producción |
| `PROD_SSH_KEY` | Clave privada SSH producción |
| `DB_USERNAME` | Usuario de PostgreSQL (común para staging/prod o diferente) |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Secreto para firmar JWT |
| `CORS_ALLOWED_ORIGINS_STAGING` | Origen CORS para staging (ej. `http://staging.pymes-admin.com`) |
| `CORS_ALLOWED_ORIGINS_PROD` | Origen CORS para producción (ej. `https://pymeq.dioquincar.dev`) |

### 4.3. Modificar `ci.yml`
- [ ] Eliminar los flags `-Dspring.profiles.active=...` en los pasos de tests.
- [ ] Eliminar el paso `Create .env from example`.
- [ ] Mantener la subida de artefactos (reportes) como está.

### 4.4. Modificar `cd-staging.yml`
- [ ] Eliminar el paso `Create .env from example`.
- [ ] En el job `docker-build-push` (si se usa), eliminar referencias a `.env`. No se necesitan build-args para Spring Boot, ya que las variables se inyectan al correr el contenedor.
- [ ] Reemplazar la llamada al script `deploy-staging.sh` por los comandos directos mostrados en 3.3, usando `secrets.STAGING_*` y `secrets.CORS_ALLOWED_ORIGINS_STAGING`.
- [ ] Ajustar la generación de versión (output) para que sea la misma que se usa en el `.env` remoto.

### 4.5. Modificar `cd-prod.yml`
- [ ] Mismos cambios que en staging, pero usando secrets de producción (`PROD_HOST`, `PROD_USER`, etc.) y `CORS_ALLOWED_ORIGINS_PROD`.
- [ ] Conservar el entorno `environment: production` para requerir aprobación manual si así se desea.

### 4.6. Eliminar archivos obsoletos
- [ ] Borrar `scripts/deploy-staging.sh` (y cualquier otro script de despliegue similar).
- [ ] Asegurar que `.env` está en `.gitignore` (ya debería estarlo).

## 5. Resultados esperados

- Los pipelines de **CI** (`ci.yml`) ejecutan todos los tests (unitarios y de integración) correctamente en GitHub Actions sin necesidad de servicios Docker anidados ni archivos `.env`.
- Los pipelines de **CD** (`cd-staging.yml`, `cd-prod.yml`) construyen las imágenes multi‑arquitectura, las publican en Docker Hub y despliegan en los servidores respectivos creando el archivo `.env` remoto sobre la marcha.
- El `docker-compose.yml` es más limpio y portable, funcionando tanto en local (con un `.env` manual) como en los servidores (con el `.env` generado automáticamente).
- Se elimina la duplicación de lógica y scripts externos, reduciendo la superficie de errores.

## 6. Nota final

Esta estrategia respeta la separación de entornos y mantiene todas las características avanzadas del proyecto (Testcontainers, Flyway, OAuth2, etc.). Los cambios sugeridos no alteran la lógica de negocio ni la configuración de seguridad, solo mejoran la integración continua y el despliegue.
