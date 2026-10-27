# ──────────────────────────────────────────────────────────────────────────────
# ProGuard / R8 — GloubiIdle
# Ce fichier est utilisé uniquement en build release (isMinifyEnabled = true)
# ──────────────────────────────────────────────────────────────────────────────

# ── Règles générales Android ──────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Conserver les classes utilisées par la réflexion Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ── Room ──────────────────────────────────────────────────────────────────────
# Conserver les entités et DAOs Room (utilisés via réflexion/KSP)
-keep class com.vgarcia.gloubi.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Lazy { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.vgarcia.gloubi.notifications.** { *; }

# ── Google AdMob & UMP ────────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.ump.**

# ── Règles de débogage (crash reports lisibles) ───────────────────────────────
# Conserve les noms de fichiers et numéros de ligne dans les stack traces
# pour que les crashs soient lisibles dans la Play Console
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ── Divers ────────────────────────────────────────────────────────────────────
# Supprimer les logs en production
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}