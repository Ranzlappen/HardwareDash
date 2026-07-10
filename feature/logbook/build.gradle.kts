// :feature:logbook — session-note log entries + a lightweight
// checkpoint/process tracker with per-checkpoint reminders.
//
// Standard-flavor only: a pure productivity feature with no hardware or
// root surface (no `-rooted` sibling module, matching :feature:manual /
// :feature:dashboard). Ships monitoring (LogbookMetricSource) and
// automation (LogbookActionHandler) surfaces per the Module Authoring
// Contract's non-negotiable items 6/7.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.logbook"
}

dependencies {
    // :core:ui — ModuleScreenScaffold, DashCard/CompactCard,
    // GadgetExpandableCard, GadgetStatusKind badges, the text-field /
    // button / bottom-sheet component family.
    implementation(project(":core:ui"))
    // :core:data — the Logbook Room layer (LogbookDao + entities), owned
    // by :core:data per the apps.db precedent ("Repo convention: Room
    // lives here"; feature modules read through the DAO, never through
    // Room.databaseBuilder directly).
    implementation(project(":core:data"))
    // :core:navigation surfaces GadgetDestination.Logbook + the
    // NavGraphBuilder.logbookScreen() extension target, and the shared
    // EXTRA_ROUTE constant the reminder notification's tap PendingIntent
    // uses to deep-link back into this screen.
    implementation(project(":core:navigation"))
    // :core:monitoring — the MetricSource seam (api-exposes :core:model).
    implementation(project(":core:monitoring"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    implementation(project(":core:automation"))
    // :core:notifications — NotificationChannelRegistry, so the reminder
    // worker doesn't hand-roll channel creation/idempotency.
    implementation(project(":core:notifications"))

    // WorkManager — LogbookReminderWorker schedules one OneTimeWorkRequest
    // per checkpoint reminder. First CoroutineWorker implementation in the
    // modular codebase; the WorkManager runtime + Hilt worker-injection
    // plumbing already exists at the :app level (GadgetApplication is a
    // Configuration.Provider wired to HiltWorkerFactory) with no worker
    // consuming it yet.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    // androidx.core for NotificationCompat (the reminder notification
    // builder).
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
