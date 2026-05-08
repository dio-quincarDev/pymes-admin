# 📱 Guía: Quasar PWA + Capacitor Android

Guía rápida para configurar y ejecutar el entorno de desarrollo móvil.

## 1. Requisitos Previos
- **Android Studio:** Instalado y con un Emulador configurado.
- **Android SDK:** Configurado en el sistema (ej: `~/Android/Sdk`).
- **Quasar CLI:** Global (`npm i -g @quasar/cli`).

## 2. Configuración Inicial (Una sola vez)
Si la carpeta `src-capacitor/android` no existe o está corrupta:

```bash
# Dentro de frontend/pymes
quasar mode add capacitor
cd src-capacitor
npx cap add android
```

## 3. Configuración del IDE (Opcional - Automatización)
En `quasar.config.ts`, añade la ruta de tu Android Studio para usar el flag `--ide`:

```typescript
// quasar.config.ts
bin: {
  linuxAndroidStudio: '/snap/bin/android-studio' // O tu ruta correspondiente
}
```

## 4. Flujo de Desarrollo Diario

### Paso A: Lanzar Servidor y IDE
Este comando compila el código, sincroniza con Android y abre el IDE.
```bash
quasar dev -m capacitor -T android --ide
```

### Paso B: Ejecutar en Emulador
1. En **Android Studio**, espera a que termine el "Gradle Sync".
2. Selecciona tu emulador (ej: `Pixel_7`).
3. Presiona el botón **Play (Run)**.

### Paso C: Hot Reload
- Modifica archivos en `src/` (VS Code).
- Los cambios se verán **automáticamente** en el emulador sin reiniciar.

## 5. Sincronización Manual
Si instalas un nuevo plugin de Capacitor o cambias configuraciones nativas:
```bash
quasar build -m capacitor -T android --skip-pkg
# O directamente vía Capacitor
cd src-capacitor && npx cap sync android
```

## 6. Solución de Problemas (Troubleshooting)
- **Error "Gradlew not found":** Borra `src-capacitor/android` y repite el Paso 2.
- **Cambios no se ven:** Ejecuta `npx cap copy android` para forzar la copia de assets.
- **Error de IDE:** En Android Studio: `File > Invalidate Caches / Restart`.
