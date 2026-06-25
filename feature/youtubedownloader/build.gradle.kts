// :feature:youtubedownloader — YouTube video / audio downloader.
//
// Standard-flavor feature module (no rooted sibling): the bundled
// yt-dlp + ffmpeg + Python runtime from youtubedl-android run unprivileged.
// Builds yt-dlp argument sets (ported from tools.ranzlappen.com's YouTube
// MP3 Studio), drives downloads from a dataSync foreground service, and
// captures session cookies via an in-app WebView for private playlists.
//
// Monitoring- and automation-ready per the Module Authoring Contract:
//   * DownloadMetricSource  — active-download progress %  (@IntoMap MetricSource)
//   * DownloadActionHandler — download/cancel actions     (@IntoMap ActionHandler)

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.youtubedownloader"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:model"))
    implementation(project(":core:automation"))
    implementation(project(":core:datastore"))
    implementation(project(":core:notifications"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    // yt-dlp + ffmpeg + Python runtime, bundled as per-ABI native libs.
    // Maven Central (NOT JitPack), so no seed-jitpack-cache wiring is needed.
    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
