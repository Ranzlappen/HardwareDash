# Consumer ProGuard/R8 rules for :feature:youtubedownloader.
#
# The bundled youtubedl-android runtime (yt-dlp + ffmpeg + Python) reaches its
# native/JNI entry points and JSON model classes via reflection, so R8 in the
# release build (isMinifyEnabled = true) must not rename or strip them.
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Our @Serializable config travels through the foreground service as JSON; keep
# the generated serializer alongside kotlinx-serialization's own rules.
-keep class dev.ranzlappen.gadget.feature.youtubedownloader.**$$serializer { *; }
-keepclassmembers class dev.ranzlappen.gadget.feature.youtubedownloader.DownloadConfig {
    *** Companion;
}
