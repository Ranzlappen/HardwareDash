// :feature:files — file metadata / EXIF viewer (v3 B12, approved in
// MASTER-PLAN §8.3; v1's FileMetadataScreen brought back).
//
// Standard-flavor only, no root surface: a file comes in through the
// system picker (SAF), so no storage permission is needed. Opt-in
// MetricSource / ActionHandler surfaces are deliberately absent — they
// would give the user nothing here (Module Authoring Contract, v3).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.files"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // Read + write EXIF (JPEG / PNG / WebP) through a temp copy.
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
