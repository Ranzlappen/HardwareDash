package dev.ranzlappen.gadget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.localization.LocalizationManager
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleDetector
import dev.ranzlappen.gadget.core.root.launch.LaunchGate
import dev.ranzlappen.gadget.core.root.launch.LaunchGateOutcome
import dev.ranzlappen.gadget.root.ui.FatalLaunchScreen
import dev.ranzlappen.gadget.backup.ui.BackupCard
import dev.ranzlappen.gadget.core.permissions.PermissionsDashboardCard
import dev.ranzlappen.gadget.root.ui.RootedFeatureTogglesCard
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import android.nfc.NfcAdapter
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.feature.apps.AppRepository
import dev.ranzlappen.gadget.feature.apps.appsScreen
import dev.ranzlappen.gadget.feature.apps.widget.FolderWidgetController
import dev.ranzlappen.gadget.feature.battery.widget.BatteryWidgetController
import dev.ranzlappen.gadget.feature.storage.widget.StorageWidgetController
import dev.ranzlappen.gadget.core.datastore.CustomThemeOption
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetCustomTheme
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.navigation.GadgetApp
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.navigation.navigateTopLevel
import dev.ranzlappen.gadget.feature.audio.audioScreen
import dev.ranzlappen.gadget.feature.automation.ui.automationScreen
import dev.ranzlappen.gadget.feature.dashboard.dashboardScreen
import dev.ranzlappen.gadget.feature.battery.batteryScreen
import dev.ranzlappen.gadget.feature.gps.gpsScreen
import dev.ranzlappen.gadget.feature.storage.storageScreen
import dev.ranzlappen.gadget.feature.radios.ir.irScreen
import dev.ranzlappen.gadget.feature.camera.cameraScreen
import dev.ranzlappen.gadget.feature.motion.motionScreen
import dev.ranzlappen.gadget.feature.actuators.actuatorsScreen
import dev.ranzlappen.gadget.feature.adbdebug.adbDebugScreen
import dev.ranzlappen.gadget.feature.ambient.ambientScreen
import dev.ranzlappen.gadget.feature.bugreport.bugReportScreen
import dev.ranzlappen.gadget.feature.diagnostics.diagnosticsScreen
import dev.ranzlappen.gadget.feature.display.displayScreen
import dev.ranzlappen.gadget.feature.lock.lockScreen
import dev.ranzlappen.gadget.feature.logbook.logbookScreen
import dev.ranzlappen.gadget.feature.manual.manualScreen
import dev.ranzlappen.gadget.feature.microphone.microphoneScreen
import dev.ranzlappen.gadget.feature.notification.notificationScreen
import dev.ranzlappen.gadget.feature.radios.bt.btScreen
import dev.ranzlappen.gadget.feature.radios.cell.cellScreen
import dev.ranzlappen.gadget.feature.radios.nfc.NfcViewModel
import dev.ranzlappen.gadget.feature.radios.wifi.wifiScreen
import dev.ranzlappen.gadget.feature.radios.subghz.subghzScreen
import dev.ranzlappen.gadget.feature.flipper.flipperScreen
import dev.ranzlappen.gadget.feature.radios.nfc.nfcScreen
import dev.ranzlappen.gadget.feature.sensors.sensorsScreen
import dev.ranzlappen.gadget.feature.settings.settingsScreen
import dev.ranzlappen.gadget.feature.torch.torchScreen
import dev.ranzlappen.gadget.feature.usbdebug.usbDebugScreen
import dev.ranzlappen.gadget.feature.vibration.vibrationScreen
import dev.ranzlappen.gadget.feature.youtubedownloader.youtubeDownloaderScreen
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository

    // Eager-injected so its init { } collects Room flows and re-renders folder
    // widgets reactively for the lifetime of the process.
    @Inject lateinit var folderWidgetController: FolderWidgetController

    // Eager-injected so its init { } observes battery state and repaints
    // placed battery widgets for the lifetime of the process.
    @Inject lateinit var batteryWidgetController: BatteryWidgetController

    // Eager-injected so its init { } observes storage usage and repaints
    // placed storage widgets for the lifetime of the process.
    @Inject lateinit var storageWidgetController: StorageWidgetController

    // Eager-injected so its init { } runs the one-shot legacy gadget_db ->
    // apps.db import (in-place upgrade + legacy backup restore continuity).
    @Inject lateinit var legacyAppsImporter: dev.ranzlappen.gadget.feature.apps.LegacyAppsImporter

    @Inject lateinit var launchGate: LaunchGate
    @Inject lateinit var companionDetector: CompanionModuleDetector

    private val nfcViewModel: NfcViewModel by viewModels()

    // Phase 2 / Batch 1 — new typed user-preferences repository. Drives
    // the GadgetApp theme params reactively (dark mode, dynamic colour,
    // reduced-motion override, reduced-transparency override).
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bootstrapLegacyManagers()

        setContent {
            val outcome by produceState<LaunchGateOutcome?>(initialValue = null) {
                value = launchGate.check()
            }
            when (val resolved = outcome) {
                null -> GadgetTheme { LaunchSplash() }
                LaunchGateOutcome.Allowed -> {
                    val navController = rememberNavController()
                    val preferences by userPreferencesRepository.flow
                        .collectAsState(initial = UserPreferences())
                    val systemDark = isSystemInDarkTheme()
                    val useDarkTheme = when (preferences.darkThemeMode) {
                        DarkThemeMode.Light -> false
                        DarkThemeMode.Dark -> true
                        DarkThemeMode.FollowSystem -> systemDark
                    }
                    val reducedMotionOverride = when (preferences.reducedMotionOverride) {
                        TriStatePreference.On -> true
                        TriStatePreference.Off -> false
                        TriStatePreference.FollowSystem -> null
                    }
                    val customTheme = when (preferences.customTheme) {
                        CustomThemeOption.Default -> GadgetCustomTheme.Default
                        CustomThemeOption.HighContrast -> GadgetCustomTheme.HighContrast
                        CustomThemeOption.AmoledTrue -> GadgetCustomTheme.AmoledTrue
                        CustomThemeOption.Pastel -> GadgetCustomTheme.Pastel
                    }
                    GadgetApp(
                        navController = navController,
                        useDarkTheme = useDarkTheme,
                        useDynamicColor = preferences.dynamicColor,
                        customTheme = customTheme,
                        reducedMotionOverride = reducedMotionOverride,
                        reducedTransparency = preferences.reducedTransparency,
                    ) {
                        dashboardScreen(
                            onNavigate = { destination ->
                                // Rail destinations (Dashboard, Torch, Settings, …)
                                // route via the back-stack-trimming helper; any
                                // non-rail sub-route uses plain navigate.
                                if (destination in GadgetDestination.railDestinations) {
                                    navController.navigateTopLevel(destination)
                                } else {
                                    navController.navigate(destination.route)
                                }
                            },
                        )
                        sensorsScreen()
                        batteryScreen()
                        gpsScreen()
                        storageScreen()
                        irScreen()
                        cameraScreen()
                        motionScreen()
                        audioScreen()
                        nfcScreen()
                        btScreen()
                        wifiScreen()
                        cellScreen()
                        subghzScreen()
                        flipperScreen()
                        ambientScreen()
                        lockScreen()
                        actuatorsScreen()
                        diagnosticsScreen()
                        displayScreen()
                        microphoneScreen()
                        usbDebugScreen()
                        adbDebugScreen()
                        bugReportScreen()
                        manualScreen()
                        logbookScreen()
                        automationScreen()
                        settingsScreen(
                            // Centralized permissions dashboard (W5), from
                            // :core:permissions — aggregates every feature's
                            // @IntoMap permission contributions + the app
                            // baseline.
                            permissionsSection = { PermissionsDashboardCard() },
                            // Backup/restore lives in :app (BackupManager
                            // spans :core:data + :feature:apps, which no
                            // leaf module can see together).
                            backupSection = { BackupCard() },
                            // The rooted opt-in toggles live in :app (they
                            // reach the legacy RootFeaturesEntryPoint); the
                            // card self-hides on standard / no-root.
                            rootFeatureToggles = { RootedFeatureTogglesCard() },
                        )
                        torchScreen(
                            // Deep-link the "turned off in settings" snackbar
                            // action to the Settings screen.
                            onNavigateToSettings = {
                                navController.navigateTopLevel(GadgetDestination.Settings)
                            },
                        )
                        vibrationScreen(
                            onNavigateToSettings = {
                                navController.navigateTopLevel(GadgetDestination.Settings)
                            },
                        )
                        notificationScreen(
                            onNavigateToSettings = {
                                navController.navigateTopLevel(GadgetDestination.Settings)
                            },
                        )
                        appsScreen(navController)
                        youtubeDownloaderScreen(navController)
                    }
                }
                is LaunchGateOutcome.DeniedFatal -> GadgetTheme {
                    FatalLaunchScreen(
                        reason = resolved.reason,
                        onExit = { finishAffinity() },
                        onOpenInstructions = { openCompanionInstructions() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.action
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            nfcViewModel.onNewIntent(intent)
        }
    }

    private fun bootstrapLegacyManagers() {
        LocalizationManager.init(this)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = File(cacheDir, "osmdroid")
            osmdroidBasePath = File(cacheDir, "osmdroid")
        }

        // Legacy widget-cleanup C1: the deleted WidgetUpdateWorker enqueued
        // KEEP unique periodic work on every prior launch, servicing widget
        // providers that are no longer manifest-registered. Cancel it once on
        // upgraded installs so the orphaned 15-min wakeup stops; modular
        // widgets repaint via their own notifier seams.
        androidx.work.WorkManager.getInstance(this).apply {
            cancelUniqueWork("widget_periodic_update")
            // Legacy logbook purge: pending reminder works reference the
            // deleted LogbookReminderWorker class and would fail on fire;
            // cancel them once on upgraded installs.
            cancelAllWorkByTag("logbook_reminder")
        }
        appRepository.requestRefresh()
    }

    private fun openCompanionInstructions() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(companionDetector.installInstructionsUrl)),
            )
        }
    }
}

@Composable
private fun LaunchSplash() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
