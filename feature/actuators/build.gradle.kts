// :feature:actuators — vibration actuator capabilities and rooted PWM control.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.actuators"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:automation"))
}
