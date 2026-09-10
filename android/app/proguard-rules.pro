# PhotoEngine Pro ProGuard / R8 Optimization Rules

# Keep Jetpack Compose runtime & animations
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Google ML Kit (Face Mesh & Selfie Segmentation)
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep NDK & Vulkan native entry points
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Exif & Metadata classes
-keep class androidx.exifinterface.** { *; }

# Keep Coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
