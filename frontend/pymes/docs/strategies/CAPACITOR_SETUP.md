# Quasar PWA + Capacitor Android — Guía de Desarrollo

Configuración y flujo diario para desarrollo móvil.

## Requisitos

- Android Studio instalado con emulador configurado
- Android SDK (`~/Android/Sdk` o equivalente)
- Quasar CLI global: `npm i -g @quasar/cli`

## Setup inicial (una sola vez)

Si `src-capacitor/android` no existe:

```bash
# Dentro de frontend/pymes
quasar mode add capacitor
cd src-capacitor
npx cap add android
```

## Configuración opcional del IDE

En `quasar.config.ts`, agrega la ruta de Android Studio para usar `--ide`:

```typescript
bin: {
  linuxAndroidStudio: '/snap/bin/android-studio'  // ajustar a tu ruta
}
```

## Flujo diario

```bash
# 1. Compila, sincroniza con Android y abre el IDE
quasar dev -m capacitor -T android --ide

# 2. En Android Studio: espera Gradle Sync → selecciona emulador → Play
# 3. Hot reload: modifica src/ → cambios aparecen automáticamente en el emulador
```

## Sincronización manual

Después de instalar un nuevo plugin de Capacitor o cambiar configuraciones nativas:

```bash
quasar build -m capacitor -T android --skip-pkg
# o directamente:
cd src-capacitor && npx cap sync android
```

## Troubleshooting

| Error | Solución |
|-------|----------|
| "Gradlew not found" | Borrar `src-capacitor/android` y repetir Setup inicial |
| Cambios no aparecen | `npx cap copy android` para forzar copia de assets |
| Error de IDE | Android Studio → `File > Invalidate Caches / Restart` |
