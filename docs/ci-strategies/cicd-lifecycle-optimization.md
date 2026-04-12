# 🏗️ Estrategia de CI/CD y Ciclo de Vida del Servidor

Esta guía detalla las optimizaciones para el flujo de despliegue y mantenimiento del servidor de staging/producción.

## 1. Despliegue Multi-arquitectura (Resuelto)
El build se realiza en GitHub (AMD64) y el despliegue en Oracle Cloud (ARM64).
*   **Herramienta**: Docker Buildx con QEMU.
*   **Configuración**: `platforms: linux/amd64,linux/arm64`.
*   **Optimización**: Si los tiempos de build superan los 10 min, usar una imagen específica de build para cada arquitectura en paralelo.

## 2. Despliegue "RAM-Safe" (Anti-OOM)
Docker Compose recrea los servicios con un breve periodo de solapamiento (overlapping). En servidores con <2GB RAM, esto causa picos de memoria (OOM).
*   **Estrategia**: Forzar la parada antes del arranque para servicios críticos.
*   **Modificación en `deploy-staging.sh`**:
    ```bash
    # En lugar de solo 'docker compose up -d'
    docker compose stop pymes-auth-service
    docker compose up -d pymes-auth-service
    ```
*   **Costo**: 10-15 segundos de downtime.
*   **Beneficio**: El servidor no se bloquea por falta de RAM durante el deploy.

## 3. Gestión Agresiva de Espacio en Disco
Las imágenes multi-arquitectura y las compilaciones de Quasar consumen mucho espacio.
*   **Táctica**: Limpieza forzada tras cada deploy.
*   **Comando**: `docker image prune -af` en lugar de solo imágenes colgadas (dangling). Esto borra todo lo que no esté en uso.
*   **Poda de Logs**: Limitar el tamaño de los logs en `docker-compose.yml`:
    ```yaml
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    ```

## 4. Gestión de Secretos via GitHub Environments
No depender de archivos `.env` editados manualmente en el servidor.
*   **Táctica**: Usar GitHub Environments (Staging / Production).
*   **Flujo**: GitHub Actions genera un `.env` dinámico basado en los secrets del entorno y lo transfiere al servidor, eliminando errores de configuración manual.

## 5. Auditoría y Monitoreo Ligero
No instalar Prometheus/Grafana (consumen mucha RAM).
*   **Alternativa**: Usar `docker stats --no-stream` en el script de deploy para reportar el consumo de recursos post-actualización.
*   **Alertas**: Configurar el `GlobalExceptionHandler` del Backend para enviar errores críticos a un canal de Slack/Telegram mediante un webhook ligero.
