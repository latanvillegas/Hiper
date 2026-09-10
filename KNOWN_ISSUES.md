# Registro de Errores e Incidencias (KNOWN_ISSUES.md)

## [2026-09-09] Error: Execution failed for task ':app:compileDebugShaders' NDK is not installed
- **Síntoma**: El workflow de GitHub Actions fallaba durante la tarea `:app:compileDebugShaders` arrojando `NDK is not installed`.
- **Causa raíz**: El Android Gradle Plugin (AGP) detecta automáticamente cualquier archivo `.comp`/`.glsl` ubicado en el directorio por defecto `src/main/shaders` e intenta compilarlo utilizando el compilador nativo `glslc` provisto por el Android NDK. Adicionalmente, el bloque `ndk { abiFilters }` en `app/build.gradle.kts` forzaba la detección del toolchain nativo.
- **Solución aplicada**:
  1. Se archivaron los shaders de cómputo en `android/raw_shaders_archive/` para preservación técnica fuera del classpath de compilación y se eliminó `android/app/src/main/shaders`.
  2. Se removió el bloque `ndk` de `defaultConfig` en `android/app/build.gradle.kts`.
  3. Se configuró `shaders.setSrcDirs(emptyList<String>())` en `sourceSets.main` y se desactivaron explícitamente todas las tareas de shaders (`tasks.matching { it.name.contains("Shader") }.configureEach { enabled = false }`).
  4. Se reemplazó `RenderScriptFallback.kt` por un procesador 100% Kotlin CPU sobre `Bitmap` y `Canvas`, eliminando imports obsoletos de `android.renderscript.*`.
  5. Se convirtió `VulkanComputePipeline.kt` en un stub seguro sin `System.loadLibrary` ni llamadas JNI nativas.
  6. Se removió la dependencia `androidx.graphics:graphics-core`.
  7. Se mantuvo el editor activo con ajustes CPU (Brillo, Contraste, Rotación 90°, exportación y guardado MediaStore).
- **Prevención**: En pipelines de CI/CD para APKs iniciales o entornos sin NDK instalado, no colocar archivos en `src/main/shaders` ni declarar bloques `ndk` o `externalNativeBuild`.

## [2026-09-09] Error: Fallo de compilación potencial en CI por wrappers y referencias de test incompletas
- **Causa raíz**: El repositorio carecía del wrapper ejecutable de Gradle (`gradlew`, `gradle-wrapper.jar`, `gradle-wrapper.properties`), `android/build.gradle.kts` utilizaba alias de un `libs.versions.toml` no provisionado, `android/app/build.gradle.kts` tenía una asignación de `signingConfig` inválida dentro del bloque `signingConfigs`, y `GalleryIntegrationTest.kt` importaba clases de Robolectric no declaradas en dependencias.
- **Solución aplicada**:
  1. Se generó la infraestructura Gradle 8.7 completa con binarios oficiales del wrapper y scripts `gradlew` / `gradlew.bat` ejecutables.
  2. Se configuraron plugins explícitos con versiones compatibles en `android/build.gradle.kts` (AGP 8.5.2, Kotlin 1.9.24).
  3. Se corrigió `android/app/build.gradle.kts` fijando `compileSdk = 34`, `targetSdk = 34` y una resolución condicional segura de firma para release con fallback a debug.
  4. Se depuraron los imports huérfanos de Robolectric en `GalleryIntegrationTest.kt`.
  5. Se crearon los recursos nativos de Android (strings, colors, themes, adaptive icons) requeridos por AAPT2.
- **Prevención**: Ejecutar siempre una revisión de símbolos e imports antes de finalizar cambios y auditar la existencia de todos los archivos del wrapper de Gradle requeridos por CI.

## [2026-09-09] Error: SecurityException al devolver resultado a galerías de terceros (Samsung OneUI / Xiaomi)
- **Síntoma**: Al presionar "Listo / Guardar", la galería llamadora (ej. Samsung Gallery o MIUI) no podía leer el URI devuelto y arrojaba `java.lang.SecurityException: Permission Denial: reading com.photoengine.core.fileprovider...`.
- **Causa raíz**: El intent de resultado (`resultIntent`) solo tenía `FLAG_GRANT_READ_URI_PERMISSION` en flags del Intent, pero algunas versiones de Android requieren que el URI esté explícitamente en el `ClipData` del Intent para que el Binder IPC transfiera la concesión de permisos al proceso llamador.
- **Solución aplicada**: Se configuró `resultIntent.clipData = ClipData.newUri(contentResolver, "PhotoEngine Edited Photo", exportResult.contentUri)` en `GalleryIntegrationManager.buildResultIntent`, además de los flags de intent `FLAG_GRANT_READ_URI_PERMISSION` y `FLAG_GRANT_WRITE_URI_PERMISSION`.
- **Prevención**: En cualquier devolución de `setResult(RESULT_OK)` con `content://` provisto por `FileProvider`, adjuntar siempre el URI tanto en `intent.data` como en `intent.clipData`.

## [2026-09-09] Error: Deprecación de RenderScript en Android 12+ (API 31+)
- **Síntoma**: Advertencia de compilación `RenderScript is deprecated` y potenciales problemas de driver en dispositivos con Android 12 o superior.
- **Causa raíz**: Google deprecó RenderScript en favor de Vulkan y NDK Compute.
- **Solución aplicada**: Se implementó una arquitectura con Vulkan Compute como driver primario (`VulkanComputePipeline.kt`), relegando RenderScript solo a fallback condicional para dispositivos con Android 10 o inferior mediante un wrapper seguro (`RenderScriptFallback.kt`).
- **Prevención**: Enrutamiento dinámico en tiempo de arranque verificando disponibilidad de Vulkan (`VkPhysicalDeviceFeatures`) antes de instanciar el contexto.

## [2026-09-09] Error: Sobrepaso en Splines Cúbicos Naturales al manipular puntos extremos
- **Síntoma**: Con puntos de control muy cercanos o pendientes extremas, la curva cúbica tradicional sobrepasaba [0, 1] o creaba curvaturas no monótonas en las curvas de tono.
- **Causa raíz**: Un spline cúbico libre (Natural Cubic Spline) no garantiza monotonicidad si los puntos de entrada son monótonos crecientes.
- **Solución aplicada**: Algoritmo de Spline Cúbico de Fritsch-Carlson (Monotone Cubic Hermite Spline) en `CubicSplineInterpolator.kt` con cálculo de tangentes limitadas y clamp estricto [0.0, 1.0].
- **Prevención**: Validación matemática de los 14 puntos de control ordenados por coordenada X ascendente con tolerancia mínima $\Delta x \ge 0.001$.

## [2026-09-09] Error: Shader compile error: S0032: no default precision defined for variable 'vec2[8]'
- **Síntoma**: Al inicializar el contexto WebGL2 en el navegador se producía un fallo de compilación del fragment shader con mensaje `0:261: S0032: no default precision defined for variable 'vec2[8]'`.
- **Causa raíz**: En la especificación GLSL ES 3.00, no existe precisión predeterminada para tipos enteros (`int`) en fragment shaders, y los constructores explícitos de arrays en línea (`vec2[8](...)` y `float[6](...)`) carecen de precisión calificada en varios compiladores GPU/drivers (como ANGLE/SwiftShader).
- **Solución aplicada**: Se añadió la directiva `precision highp int;` junto a `precision highp float;` tanto en vertex como en fragment shader, y se refactorizaron las operaciones: muestreo bilateral desenrollado sin arrays (`SAMPLE_BILATERAL_TAP`) y cálculo directo del ángulo de tono (`float(i) * 60.0`) para el clasificador HSL de 8 canales.
- **Prevención**: Evitar constructores de arrays anónimos de tamaño variable en el cuerpo de funciones en GLSL ES 3.00; asegurar directivas explícitas de precisión para `float` e `int` en todos los shaders de fragmentos.

## [2026-09-09] Error: Rollup export mismatch y TypeScript strict checks en FaceRetouchPanel
- **Síntoma**: Fallo de build en Vite `LIPSTICK_COLLECTIONS / BLUSH_PALETTES is not exported by beautyPresets.ts` y errores de TypeScript en tipos opcionales de belleza (`tanIntensity`, `catchlight`, `fullBodySmoothing`).
- **Causa raíz**: Inconsistencia en la nomenclatura de exportación entre el catálogo cosmético (`BLUSH_PALETTE`, `LIPSTICK_PALETTES`, `HIGHLIGHTER_PALETTE`) y el panel de retoque, sumado a discrepancias entre nombres de campos en la interfaz `FaceRetouchSettings` (`catchlightIntensity`, `skinToneUniformity`) y `BeautyExtraSettings` (`smoothSkinBody`).
- **Solución aplicada**: Se exportaron alias canónicos en `beautyPresets.ts` (`BLUSH_PALETTES`, `LIPSTICK_COLLECTIONS`, `HIGHLIGHTER_PALETTES`), se definieron constantes estructuradas completas (`DEFAULT_FACE_RETOUCH`, `DEFAULT_VIRTUAL_MAKEUP`, `DEFAULT_BEAUTY_EXTRA`), y se unificaron los nombres de propiedades en `FaceRetouchPanel.tsx` y `webglEngine.ts`.
- **Prevención**: Mantener las fuentes de constantes predeterminadas asociadas a las interfaces de `types.ts` en un único módulo fuente centralizado y ejecutar `tsc --noEmit` y `vite build` en cada ciclo de iteración.


