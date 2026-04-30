# BugFood ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }

# Accessibility Service
-keep class com.bugfood.service.BugFoodAccessibilityService { *; }
-keep class com.bugfood.service.BugFoodForegroundService { *; }
-keep class com.bugfood.service.BootReceiver { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# General
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
