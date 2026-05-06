# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*

# ---------- Gson ----------
-keep class com.nvv.petber.data.model.** { *; }

# ---------- Ktor / serialization ----------
-keep class kotlinx.serialization.** { *; }
-keep class io.ktor.** { *; }

# ---------- Supabase ----------
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# ---------- Hilt ----------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ---------- Room ----------
-keep class androidx.room.** { *; }

# ---------- Glide ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# ---------- Media3 ----------
-dontwarn androidx.media3.**

# ---------- ML Kit ----------
-dontwarn com.google.mlkit.**

# ---------- General ----------
-dontwarn okhttp3.**
-dontwarn retrofit2.**
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean