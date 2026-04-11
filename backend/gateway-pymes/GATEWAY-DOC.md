# Documentación Técnica del API Gateway

Este documento detalla problemas críticos corregidos y decisiones de implementación en el microservicio **gateway-pymes**.

---

## 🛠️ Correcciones Técnicas

### 1. Gestión de Dependencias (Lombok/SLF4J)
- **Problema:** Errores de compilación en `AuthenticationFilter.java`. El compilador de Java no encontraba los símbolos generados por la anotación `@Slf4j`.
- **Causa:** El proyecto utilizaba anotaciones de **Lombok** para abstraer la instanciación de loggers, pero la dependencia no estaba declarada en el `pom.xml`.
- **Solución:** Se integró la dependencia `org.projectlombok:lombok` como dependencia opcional (`<optional>true</optional>`). Esto permite al procesador de anotaciones (`javac`) inyectar el código del logger (`private static final org.slf4j.Logger log = ...`) durante la fase de compilación.

### 2. Seguridad JWT y Estándar JWA (RFC 7518)
- **Problema:** Fallo en la carga del `ApplicationContext` en las pruebas unitarias debido a una `WeakKeyException`.
- **Causa Técnica:** El secreto JWT utilizado tenía una longitud de **240 bits**. De acuerdo con el **RFC 7518 (sección 3.2)**, los algoritmos HMAC-SHA (como HS256) **DEBEN** utilizar claves con un tamaño mayor o igual al tamaño de salida del hash ($\ge$ 256 bits o 32 bytes) para garantizar la seguridad criptográfica.
- **Implementación Correctiva:**
    - Se configuró `src/test/resources/application-test.yaml` con un secreto seguro generado aleatoriamente de **256 bits**.
    - Se activó el perfil `test` en la clase de prueba principal `GatewayPymesApplicationTests` mediante la anotación `@ActiveProfiles("test")`.
    - Esto asegura que las pruebas se ejecuten en un entorno con configuraciones de seguridad robustas que cumplan con la validación estricta de la librería `jjwt-api`.

---
*Última actualización: 11 de Abril, 2026*
