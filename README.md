# PhotoEngine Pro • Android Native Photo Editor

Motor de edición fotográfica profesional en tiempo real para Android, acelerado por hardware (Vulkan / RenderScript GPU), compatible con el ecosistema de galerías nativas de Android (Google Photos, Samsung Gallery, Xiaomi, OnePlus, Motorola) y equipado con selector de imágenes Photo Picker y exportación a MediaStore.

## Requisitos de Entorno

- **JDK:** Java Development Kit 17 (Temurin / OpenJDK 17)
- **Android SDK:** Compile SDK 34, Target SDK 34, Min SDK 26
- **Gradle:** 8.7 (Wrapper incluido)
- **Android Gradle Plugin (AGP):** 8.5.2
- **Kotlin:** 1.9.24 con Jetpack Compose (Compiler Extension 1.5.14)

## Comandos de Compilación

Para compilar el proyecto directamente desde la raíz o dentro del directorio `android/`:

### Compilar APK Debug:
```bash
./gradlew assembleDebug
```
El archivo generado se ubicará en:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Compilar APK Release:
```bash
./gradlew assembleRelease
```
El archivo generado se ubicará en:
`android/app/build/outputs/apk/release/app-release-unsigned.apk` (o firmado con debug/release keystore según variables de entorno).

### Compilar Android App Bundle (AAB):
```bash
./gradlew bundleRelease
```

### Ejecutar Pruebas Unitarias:
```bash
./gradlew testDebugUnitTest
```

### Ejecutar Análisis de Lint:
```bash
./gradlew lintDebug
```

## Características Principales

1. **Selector de Imagen Moderno (Photo Picker):** Integración nativa con `ActivityResultContracts.PickVisualMedia` compatible con Android 13+ y retrocompatible vía Google Play Services.
2. **Recepción de Intents:** Manejo completo de `Intent.ACTION_VIEW`, `Intent.ACTION_EDIT` e `Intent.ACTION_SEND` para imágenes provenientes de cualquier galería del sistema.
3. **Vista Previa en Alta Resolución:** Visualizador interactivo Jetpack Compose con soporte para relaciones de aspecto dinámicas.
4. **Exportación & Retorno:** Exportación a través de FileProvider con retorno de `RESULT_OK` a la app llamadora.
5. **Persistencia Scoped Storage:** Guardado directo de copias permanentes en `MediaStore.Images.Media` (`Pictures/PhotoEngine`).
