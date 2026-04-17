# PyMes Admin - Frontend (Quasar PWA) 🎨

Este proyecto es la interfaz de usuario de la plataforma PyMes Admin, construida con **Quasar 2**, **Vue 3** y **TypeScript**.

## 🚀 Conectividad con el Backend

La aplicación se comunica exclusivamente a través del **API Gateway** (`pymes-gateway`), que centraliza la seguridad y el enrutamiento.

### Configuración de Axios (`src/boot/axios.ts`)
Hemos implementado una instancia de Axios centralizada (`api`) con los siguientes comportamientos automáticos:

1.  **Interceptor de Peticiones**: Extrae automáticamente el JWT de `localStorage` (`pymes_auth_token`) y lo inyecta en la cabecera `Authorization: Bearer <token>`.
2.  **Manejo de Sesión**: Detecta respuestas `401 Unauthorized` (token expirado o inválido) y limpia automáticamente el estado local de la sesión.

### Variables de Entorno
Para configurar la URL del API Gateway en desarrollo o producción, utiliza la variable:
```bash
# Ejemplo en .env
API_URL=http://localhost:8080/api/v1
```

## 🛠️ Comandos de Desarrollo

```bash
# Instalar dependencias
npm install

# Iniciar en modo desarrollo (Hot Reload)
quasar dev

# Build para producción
quasar build

# Lint de archivos
npm run lint
```

## 🔐 Seguridad
*   El token JWT nunca debe ser almacenado en cookies sin la bandera `HttpOnly`. 
*   Actualmente se persiste en `localStorage` para facilitar el desarrollo, pero se recomienda migrar a un manejo de estado reactivo seguro con **Pinia** y cookies seguras en producción.

---

<div align="center">

**PyMes Admin - UI** | Construido con Quasar Framework ⚡

</div>
