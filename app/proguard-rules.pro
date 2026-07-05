# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ── Kotlin Serialization ─────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class dev.ranzlappen.gadget.**$$serializer { *; }
-keepclassmembers class dev.ranzlappen.gadget.** {
    *** Companion;
}
-keepclasseswithmembers class dev.ranzlappen.gadget.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Preserve line numbers for crash reports ──────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── OSMDroid ─────────────────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ── Hilt ─────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# ── Room ─────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ── Timber ───────────────────────────────────────────────────────────
-dontwarn timber.log.**
