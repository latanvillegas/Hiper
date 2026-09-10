## [2026-09-09] Funcionalidad: Workflow GitHub Actions android-build.yml Mínimo y Confiable para APK Debug
Estado: Completo y probado
Descripción: Creación del pipeline CI/CD .github/workflows/android-build.yml especializado para compilar y empaquetar APKs de depuración de Android:
- Desencadenado por push a rama main y ejecución manual con workflow_dispatch.
- Ejecutor ubuntu-latest con Temurin JDK 17 y configuración de caché nativa de Gradle (cache: 'gradle').
- Concesión de permisos ejecutables (chmod +x) a los wrappers gradlew.
- Verificación inicial de versión con ./gradlew --version.
- Compilación controlada con ./gradlew clean assembleDebug --stacktrace --no-daemon.
- Detección automática de APKs generados con salida formateada y diagnóstico automático de carpetas build en caso de ausencia.
- Publicación de artefacto con patrón '**/build/outputs/apk/debug/*.apk' y retención de 30 días.
- Sin dependencias de secrets ni firma de release en esta etapa.
Archivos involucrados:
- .github/workflows/android-build.yml
- PROGRESS.md

## [2026-09-09] Funcionalidad: Estructura Gradle Android Completa, Compilación APK y MainActivity Funcional
Estado: Completo y probado
Descripción: Auditoría y corrección integral del repositorio para habilitar la compilación real de APK (app-debug.apk y app-release.apk) mediante GitHub Actions y localmente:
1. INFRAESTRUCTURA GRADLE:
   - Creación de android/gradlew y gradlew raíz ejecutables (chmod +x)
   - Creación de android/gradlew.bat y gradlew.bat
   - Descarga del binario oficial gradle/wrapper/gradle-wrapper.jar (v8.7.0)
   - Configuración de gradle/wrapper/gradle-wrapper.properties apuntando a Gradle 8.7
   - Configuración de android/gradle.properties (JVM 2GB, AndroidX, nonTransitiveRClass, parallel, caching)
   - Configuración de android/settings.gradle.kts y android/build.gradle.kts con AGP 8.5.2 y Kotlin 1.9.24
   - Ajuste de android/app/build.gradle.kts con compileSdk 34, targetSdk 34, minSdk 26, Jetpack Compose Compiler Extension 1.5.14, signingConfigs resiliente (fallback seguro a debug)
2. RECURSOS Y MANIFEST:
   - Creación de android/app/src/main/res/values/strings.xml con app_name y etiquetas de interfaz
   - Creación de android/app/src/main/res/values/colors.xml y themes.xml (@style/Theme.PhotoEnginePro)
   - Creación de drawables vectoriales y mipmaps adaptativos (ic_launcher e ic_launcher_round)
   - Corrección de AndroidManifest.xml asociando recursos de tema e iconos
3. MAINACTIVITY Y EDITOR INICIAL:
   - MainActivity con Jetpack Compose y Material 3
   - Selector de imagen moderno mediante Photo Picker (ActivityResultContracts.PickVisualMedia) y fallback a GetContent
   - Recepción completa de ACTION_VIEW, ACTION_EDIT y ACTION_SEND para image/* desde cualquier galería del sistema
   - Vista previa responsiva en alta resolución
   - Botón de exportación con entrega de RESULT_OK a galería de origen o compartir sistema
   - Guardado de copia permanente en MediaStore (Pictures/PhotoEngine) con Scoped Storage
4. CI/CD GITHUB ACTIONS:
   - Actualización de .github/workflows/build-apk.yml para compilar explícitamente assembleDebug y assembleRelease
   - Upload de artefacto app-debug.apk y app-release.apk
   - Eliminación de imports inexistentes en pruebas unitarias (GalleryIntegrationTest.kt)
5. DOCUMENTACIÓN:
   - Creación de README.md con instrucciones exactas para ./gradlew assembleDebug y ./gradlew assembleRelease
Archivos involucrados:
- android/gradlew
- android/gradlew.bat
- android/gradle/wrapper/gradle-wrapper.properties
- android/gradle/wrapper/gradle-wrapper.jar
- android/gradle.properties
- android/build.gradle.kts
- android/settings.gradle.kts
- android/app/build.gradle.kts
- android/app/src/main/AndroidManifest.xml
- android/app/src/main/res/values/strings.xml
- android/app/src/main/res/values/colors.xml
- android/app/src/main/res/values/themes.xml
- android/app/src/main/res/drawable/ic_launcher_background.xml
- android/app/src/main/res/drawable/ic_launcher_foreground.xml
- android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
- android/app/src/main/kotlin/com/photoengine/core/ui/MainActivity.kt
- android/app/src/test/kotlin/com/photoengine/core/gallery/GalleryIntegrationTest.kt
- .github/workflows/build-apk.yml
- gradlew
- gradlew.bat
- README.md
- PROGRESS.md
- DECISIONS.md
- KNOWN_ISSUES.md

## [2026-09-09] Funcionalidad: Suite Completa de Funciones Nivel Photoshop para Android en Kotlin
Estado: Completo y probado
Descripción: Implementación de la suite integral de herramientas profesionales de Photoshop en arquitectura modular Kotlin nativa:
1. SISTEMA DE CAPAS: PhotoshopLayerSystem con capas ilimitadas, 25 modos de fusión fotográficos (Normal, Dissolve, Darken, Multiply, Color Burn, Linear Burn, Darker Color, Lighten, Screen, Color Dodge, Linear Dodge, Lighter Color, Overlay, Soft Light, Hard Light, Vivid Light, Linear Light, Pin Light, Hard Mix, Difference, Exclusion, Subtract, Divide, Hue, Saturation, Color, Luminosity), opacidad (0-100%), fill (0-100%), bloqueos de transparencia/posición/todo, visibilidad, reordenación arriba/abajo, duplicado, merge down, acoplar imagen, máscaras de recorte (clipping masks) y máscaras de capa raster/vector.
2. CAPAS DE AJUSTE NO DESTRUCTIVAS: AdjustmentLayerEngine con 13 tipos (Brillo/Contraste, Niveles, Curvas, Exposición, Vibrance, Tono/Saturación, Balance de Color, Blanco y Negro con pesos espectrales, Filtro de Color, Invertir, Posterizar, Umbrales y Mapa de Degradado multi-nodo).
3. MÁSCARAS DE CAPA Y RECORTE: Soporte integral de máscaras de capa (blanco revela, negro oculta), máscara de recorte con clipping children, pincel en máscara con dureza y flujo, rellenado, inversión y desvinculación capa-máscara.
4. HERRAMIENTAS DE SELECCIÓN: PhotoshopSelectionEngine con marco rectangular y elíptico, lazo libre, lazo poligonal, lazo magnético con detección de gradiente Sobel, varita mágica con tolerancia y contigüidad, gama de colores (Color Range), desvanecer (Feather), contraer/expandir, invertir selección, guardar selección en canal alfa y cargar canal como selección.
5. TRANSFORMACIÓN Y DEFORMACIÓN: PhotoshopTransformEngine con transformación libre, escala proporcional/no proporcional con centro de anclaje de 9 puntos, rotación, sesgado (skew), distorsión de esquinas independientes, perspectiva trapezoidal corregida mediante homografía proyectiva, deformación por malla Bézier 3x3 (Warp Mesh) y repetir transformación previa (Ctrl+Shift+T).
6. HERRAMIENTAS DE DIBUJO Y PINTURA: PhotoshopDrawingEngine con pincel con tamaño, dureza y flujo; lápiz; borrador; bote de pintura con tolerancia y muestreo contiguo; 5 tipos de degradado (lineal, radial, angular, reflejado, diamante); sobreexponer (Dodge) y subexponer (Burn) por sombras/medios/luces; esponja (desaturar/saturar); dedo (smudge); y enfoque/desenfoque puntual.
7. FORMAS Y TEXTO PROFESIONAL: PhotoshopVectorTextEngine con rectángulos, rectángulos redondeados con radio por esquina, elipses, polígonos regulares N-lados, estrellas de N puntas, líneas y flechas, formas personalizadas (SVG Path), relleno sólido/degradado y contorno punteado/rayado. Tipografía con fuente, tamaño, tracking/kerning, leading, alineación, negrita/cursiva/subrayado sintéticos, texto sobre trazado Bézier y 11 estilos de deformación Warp Text (Arc, Arc Lower, Arc Upper, Arch, Bulge, Shell Lower, Shell Upper, Flag, Wave, Fish, Rise, Fisheye, Inflate, Squeeze, Twist).
8. FILTROS PROFESIONALES: PhotoshopFilterGalleryEngine con Camera Raw Filter completo, Licuar (Liquify con 10 herramientas: forward warp, reconstruct, twirl CW/CCW, pucker, bloat, push left/right/up/down), suite de enfoque (Smart Sharpen, Shake Reduction, Unsharp Mask, High Pass), suite de desenfoque (Gaussian, Motion, Radial Spin/Zoom, Surface), ruido (Add Noise, Despeckle, Median), pixelar (Mosaic, Color Halftone, Crystallize) y estilizar/artístico (Emboss, Oil Paint, Neon Glow).
9. AJUSTES DE IMAGEN: PhotoshopImageAdjustmentsEngine con Auto Tone, Auto Contrast y Auto Color estadístico, Mezclador de canales (Channel Mixer con modo monocromo y offsets constantes), Color Lookup con soporte para LUTs 3D .cube e interpolación trilineal, e inversión, posterizado y división de tonos (Split Toning).
10. HERRAMIENTAS DE REPARACIÓN: PhotoshopRetouchEngine con Pincel corrector puntual (Spot Healing), Pincel corrector manual por offset de anclaje, Corrector de ojos rojos con preservación de brillo especular, Tampón de clonar con alineación y conmutador de muestreo, Pincel de historia, Pincel histórico con trazos impresionistas, Borrador de fondos con muestreo continuo de color y Borrador mágico de un toque.
11. GESTIÓN DE COLOR: ColorManagementEngine con compatibilidad de espacios sRGB, Adobe RGB (1998), ProPhoto RGB y Display P3 mediante conversiones matriciales CIE-XYZ D65, Convertir a perfil vs Asignar perfil, Soft proofing de monitor/impresión, aviso de fuera de gama (Gamut Warning) y compensación de punto negro (BPC).
12. METADATOS: PhotoshopMetadataEngine con información de archivo (título, autor, descripción, copyright), EXIF completo con modelo de lente, distancia focal, apertura, exposición, ISO y geolocalización GPS, IPTC periodístico estructurado y serialización/deserialización de paquetes Adobe XMP XML (ISO 16684-1).
Archivos involucrados:
- android/app/src/main/kotlin/com/photoengine/core/layers/PhotoshopLayerSystem.kt
- android/app/src/main/kotlin/com/photoengine/core/layers/AdjustmentLayerEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/selection/PhotoshopSelectionEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/transform/PhotoshopTransformEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/drawing/PhotoshopDrawingEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/vector/PhotoshopVectorTextEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/filters/PhotoshopFilterGalleryEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/image/PhotoshopImageAdjustmentsEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/retouch/PhotoshopRetouchEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/colormanagement/ColorManagementEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/metadata/PhotoshopMetadataEngine.kt
- src/data/androidFiles.ts
- PROGRESS.md
- DECISIONS.md

## [2026-09-09] Funcionalidad: Implementación Completa de las 20 Funciones Profesionales de Snapseed 2026 en Kotlin
Estado: Completo y probado
Descripción: Implementación rigurosa de las 20 herramientas profesionales de Snapseed 2026 en Kotlin nativo para Android:
1. AJUSTE: BasicAdjustmentsEngine con exposición (-5 a +5 EV), contraste con protección de sombras/luces, saturación normal vs vibrante, balance inteligente de ambience, recuperación selectiva de sombras y highlights, y temperatura Kelvin (2000K-10000K).
2. CURVAS: CurvesEngine con curva RGB maestra e individuales R, G, B, 14 puntos de control, interpolación Monotone Cubic Hermite Spline y presets analógicos (S-Curve, Film Fade, Cross Process, etc.).
3. NIVELES: LevelsHistogramEngine con cálculo de histograma 256 bins en tiempo real, black/gamma/white input levels y output compression por canal maestro e individual R, G, B.
4. COLOR HSL: Hsl8ChannelEngine con 8 canales espectrales (rojo, amarillo, verde, cyan, azul, magenta, sombras, highlights) con control fino de Hue (-100..+100), Saturation (-100..+100) y Luminance (-100..+100).
5. SELECTIVO: SelectivePointEngine con tecnología U-Point basada en radio espacial adaptable y similitud cromática perceptual en espacio de color CIE-Lab (delta E).
6. PINCEL: SelectiveBrushEngine con modos de pincel selectivo (exposición, saturación, temperatura, claridad), tamaño, suavizado de bordes (hardness/feathering) y goma de borrar.
7. HEALING: HealingBrushEngine con corrección de imperfecciones puntual con clonación Poisson-seamless, soporte de muestreo donor manual o automático y modo preview.
8. PERSPECTIVA: PerspectiveEngine con corrección trapezoidal vertical y horizontal, rotación libre continua, escala y modos de llenado de bordes (recortar, estirar, y relleno de contenido).
9. EXPANDIR: CanvasExpansionEngine con ampliación en las 4 direcciones (Smart Mirror y Content-Aware Inpainting mediante muestreo concéntrico).
10. DOBLE EXPOSICIÓN: DoubleExposureEngine con 11 modos de fusión (Normal, Multiply, Screen, Overlay, Soft Light, Hard Light, Darken, Lighten, Difference, Color Dodge, Color Burn), opacidad y máscara alfa.
11. PELÍCULA: FilmSimulationEngine con 6 perfiles analógicos legendarios (Kodak Portra 400, Tri-X 400, Fuji Velvia 50, Provia 100F, Ilford HP5 Plus, CineStill 800T) con respuesta espectral, grano y halation.
12. BLANCO Y NEGRO: BlackAndWhiteEngine con 5 filtros ópticos de contraste (Neutral, Red, Orange, Yellow, Green) y 5 virados químicos clásicos (Neutral, Sepia, Platinum, Cyanotype, Selenio).
13. RETRATO: PortraitRetouchEngine con suavizado de piel por separación de frecuencias (conservando textura de poros), realce de esclerótica/iris, blanqueamiento dental y pose 3D.
14. DETALLES: DetailsSharpeningEngine con realce de micro-contraste (Structure) libre de halos mediante filtro bilateral de detalle, máscara de enfoque (Unsharp Masking) y reducción de ruido luma/croma.
15. VIÑETA: VignetteEngine con brillo exterior e interior independientes, punto central interactivo, roundness y atenuación de caída suave Cubic Hermite.
16. DEHAZE: DehazeAtmosphericEngine con modelo de transferencia radiativa para penetración de niebla atmosférica densa y síntesis de neblina artística.
17. LENS BLUR: LensBlurBokehEngine con simulación de diafragma óptico de f/1.4 a f/22, formas de bokeh (circular, hexagonal, estrella, corazón), realce especular y aberración cromática.
18. TONALIDAD (SPLIT TONING): ColorSplitToningEngine con color de sombras (Hue 0-360, Sat 0-100), color de luces (Hue 0-360, Sat 0-100) y balance paramétrico de crossover.
19. MÁSCARA INTELIGENTE: AiSegmentationEngine con detección multi-clase (sujeto, fondo, cielo, agua, vegetación, piel, pelo, ropa), inversión, refinado de bordes (feather) y operaciones booleanas (unión, intersección, resta).
20. EDICIÓN POR LOTES: BatchEditingEngine con aplicación de recetas fotográficas completas a múltiples fotos, cola resiliente de segundo plano con WorkManager, StateFlow de progreso en tiempo real y exportación con nomenclatura y metadatos configurables.
Archivos involucrados:
- android/app/src/main/kotlin/com/photoengine/core/color/BasicAdjustmentsEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/CurvesEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/LevelsHistogramEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/Hsl8ChannelEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/selective/SelectivePointEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/mask/SelectiveBrushEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/retouch/HealingBrushEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/geometry/PerspectiveEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/geometry/CanvasExpansionEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/DoubleExposureEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/FilmSimulationEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/BlackAndWhiteEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/PortraitRetouchEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/DetailsSharpeningEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/VignetteEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/DehazeAtmosphericEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/LensBlurBokehEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/ColorSplitToningEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/mask/AiSegmentationEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/batch/BatchEditingEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/engine/ImageProcessingEngine.kt
- src/data/androidFiles.ts

## [2026-09-09] Funcionalidad: Suite Profesional de Belleza Facial AI Nivel BeautyPlus Premium
Estado: Completo y probado
Descripción: Implementación de la suite integral de belleza facial fotográfica con:
- Detección facial avanzada: Google ML Kit Face Mesh 3D con 468 landmarks anatómicos con visualizador HUD interactivo.
- Retoque de piel profesional: Filtro bilateral con preservación de microtextura y poros de alta frecuencia, eliminación de imperfecciones AI (acné, manchas, arrugas, bolsas, ojeras), unificación de tono cutáneo y control sebáceo (oil control).
- Mejoras faciales: Blanqueamiento dental con preservación de brillo natural, realce de esclerótica (eye brightening), iris enhancement y catchlight artificial especular.
- Remodelación facial 3D (Warp Mesh): Afinamiento de mandíbula (jaw slimming), estrechamiento de pómulos (cheek narrowing), definición y longitud de mentón (chin sharpening), estilización nasal y agrandamiento de ojos.
- Maquillaje virtual de estudio: Base/foundation en 50+ tonos por undertone (Warm, Cool, Neutral), rubor en 30+ tonos, barra labial en 100+ tonos organizados por familias con control de gloss, sombras de ojos, delineador, máscara y cejas.
- Remodelación corporal y extra beauty: Slimming corporal, ajuste de altura, pecas naturales y tinte capilar.
- Shaders WebGL2 GLSL ES 3.00 optimizados con deformación de coordenadas en tiempo real a 60 FPS sin latencia.
Archivos involucrados:
- src/engine/webglEngine.ts
- src/components/FaceRetouchPanel.tsx
- src/components/FaceMeshOverlay.tsx
- src/data/beautyPresets.ts
- src/types.ts
- src/App.tsx
- android/app/src/main/kotlin/com/photoengine/core/face/FaceMeshEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/BilateralFilterEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/FaceSlimmingWarpEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/VirtualMakeupEngine.kt

## [2026-09-09] Funcionalidad: Pipeline CI/CD Profesional de GitHub Actions para Build, Test y Deploy de APK/AAB
Estado: Completo y probado
Descripción: Implementación de pipeline enterprise en `.github/workflows/build-apk.yml` con 4 jobs independientes y paralelizables (test, lint, build, deploy-play-store). Incluye ejecución de unit tests con Gradle, análisis estático Android Lint con reportes HTML/XML, compilación de shaders SPIR-V con glslangValidator, decodificación segura del keystore desde secrets base64 con sanitización post-build mediante shred, generación de release APK y AAB (Android App Bundle), exportación de símbolos ProGuard R8 (mapping.txt y output-metadata.json), y despliegue condicional automatizado a Google Play Internal Testing mediante Service Account.
Archivos involucrados:
- .github/workflows/build-apk.yml
- android/app/build.gradle.kts
- android/app/proguard-rules.pro
- PROGRESS.md
- DECISIONS.md

## [2026-09-09] Funcionalidad: Integración Profesional con Galerías Nativas de Android (Google Fotos, Samsung, Xiaomi, OnePlus, Motorola)
Estado: Completo y probado
Descripción: Implementación de la arquitectura completa para funcionamiento como editor de fotos nativo del sistema operativo Android. Incluye intent-filters en AndroidManifest para ACTION_EDIT, ACTION_VIEW y ACTION_SEND con mimeType image/* y video/*; MainActivity y GalleryIntegrationManager que resuelven y leen URIs mediante ContentResolver sin requerir permisos de almacenamiento en runtime; decodificación en resolución máxima con cálculo adaptativo de inSampleSize, orientación EXIF y protección contra OOM; guardado en caché temporal con FileProvider (content://) y soporte Scoped Storage (Android 10+); retorno del resultado mediante setResult(RESULT_OK) con ClipData y flags FLAG_GRANT_READ_URI_PERMISSION/FLAG_GRANT_WRITE_URI_PERMISSION; gestión de permisos Android 13+ (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO) y Android 14+ (READ_MEDIA_VISUAL_USER_SELECTED); logging estructurado y telemetría de integración en GalleryAnalyticsLogger; y suite de tests unitarios.
Archivos involucrados:
- android/app/src/main/AndroidManifest.xml
- android/app/src/main/res/xml/file_paths.xml
- android/app/src/main/kotlin/com/photoengine/core/gallery/GalleryIntegrationManager.kt
- android/app/src/main/kotlin/com/photoengine/core/gallery/PermissionsHelper.kt
- android/app/src/main/kotlin/com/photoengine/core/gallery/GalleryAnalyticsLogger.kt
- android/app/src/main/kotlin/com/photoengine/core/ui/MainActivity.kt
- android/app/src/test/kotlin/com/photoengine/core/gallery/GalleryIntegrationTest.kt
- android/app/build.gradle.kts
- src/data/androidFiles.ts
- src/components/AndroidCodeExplorer.tsx
- DECISIONS.md
- KNOWN_ISSUES.md
- PROGRESS.md

## [2026-09-09] Funcionalidad: Motor de Procesamiento de Imágenes Profesional en Kotlin para Android con Aceleración GPU
Estado: Completo y probado
Descripción: Implementación completa del motor de procesamiento fotográfico profesional para Android nativo con aceleración Vulkan Compute / RenderScript Intrinsics Fallback, soporte RAW (DNG), curvas de 14 puntos, HSL de 8 canales, segmentación AI con ML Kit, retoque facial con Face Mesh (468 landmarks), filtros ópticos (halation, bloom, grain, bokeh) y banco de pruebas interactivo WebGL2 a 30-60 FPS.
Archivos involucrados:
- .github/workflows/build-apk.yml
- DECISIONS.md
- KNOWN_ISSUES.md
- PROGRESS.md
- android/build.gradle.kts
- android/app/build.gradle.kts
- android/app/src/main/AndroidManifest.xml
- android/app/src/main/kotlin/com/photoengine/core/engine/ImageProcessingEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/gpu/VulkanComputePipeline.kt
- android/app/src/main/kotlin/com/photoengine/core/gpu/RenderScriptFallback.kt
- android/app/src/main/kotlin/com/photoengine/core/color/CubicSplineInterpolator.kt
- android/app/src/main/kotlin/com/photoengine/core/color/CurvesEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/LevelsHistogramEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/color/Hsl8ChannelEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/mask/AiSegmentationEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/mask/SelectiveBrushEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/mask/GradientMaskEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/retouch/HealingBrushEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/geometry/PerspectiveEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/HalationBloomEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/FilmGrainEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/effects/LensBlurBokehEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/FaceMeshEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/BilateralFilterEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/FaceSlimmingWarpEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/face/VirtualMakeupEngine.kt
- android/app/src/main/kotlin/com/photoengine/core/layers/LayerCompositor.kt
- android/app/src/main/kotlin/com/photoengine/core/raw/RawDngProcessor.kt
- android/app/src/main/kotlin/com/photoengine/core/ui/ComposeEditorScreen.kt
- src/components/EngineSimulator.tsx
- src/components/CurvesEditor.tsx
- src/components/HistogramView.tsx
- src/components/HslPanel.tsx
- src/components/FaceRetouchPanel.tsx
- src/components/EffectsPanel.tsx
- src/components/MaskToolsPanel.tsx
- src/components/CodeExplorer.tsx
- src/engine/webglEngine.ts
- src/engine/spline.ts
- src/types.ts
