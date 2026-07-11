package dev.ranzlappen.gadget.feature.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorGlobalPrefs
import io.mockk.EqMatcher
import io.mockk.Runs
import io.mockk.anyConstructed
import io.mockk.constructedWith
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [SettingsViewModel]'s non-passthrough logic:
 *
 *  - seeding [SettingsViewModel.language] from `AppCompatDelegate.getApplicationLocales()`
 *    at construction time,
 *  - [SettingsViewModel.setLanguage] updating the `language` `StateFlow` and forwarding the
 *    right [LocaleListCompat] to `AppCompatDelegate.setApplicationLocales`, and
 *  - [SettingsViewModel.setFloatingTorchButtonEnabled] building the right
 *    implicit-action `Intent` and swallowing an exception from the `Context` call.
 *
 * The straight `viewModelScope.launch { repository.set…(…) }` setters (dark theme,
 * dynamic color, etc.) are pure passthrough over [UserPreferencesRepository] and are
 * deliberately not covered here — there is no branching logic to pin.
 *
 * `AppCompatDelegate` / `LocaleListCompat` are mocked via `mockkStatic` and `Intent`
 * construction is intercepted via `mockkConstructor` — both are supported by the plain
 * `io.mockk:mockk` JVM artifact already on this module's test classpath (no
 * `mockk-android` needed; that variant only matters for on-device instrumented tests).
 *
 * `Context.startForegroundService` is intentionally not exercised: plain JVM unit tests
 * have no Robolectric shadow, so `Build.VERSION.SDK_INT` resolves to the stub jar's
 * default of `0`, meaning `setFloatingTorchButtonEnabled`'s `SDK_INT >= O` branch never
 * trips and `Context.startService` is always the method actually invoked here — mirroring
 * the existing repo convention (see `TorchActionHandlerTest`) of deferring the
 * foreground-service branch to instrumented tests.
 */
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var repository: UserPreferencesRepository
    private lateinit var monitorGlobalPrefs: MonitorGlobalPrefs

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(AppCompatDelegate::class)
        mockkStatic(LocaleListCompat::class)
        mockkConstructor(Intent::class)

        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        monitorGlobalPrefs = mockk(relaxed = true)

        every { repository.flow } returns MutableStateFlow(UserPreferences())
        every { monitorGlobalPrefs.notificationActionsEnabled } returns MutableStateFlow(false)

        // Default seed: no pre-existing per-app locale override. Individual tests
        // override this before calling createViewModel() when they need a specific seed.
        every { AppCompatDelegate.getApplicationLocales() } returns localeListReporting("")
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs
        every { LocaleListCompat.forLanguageTags(any()) } returns mockk(relaxed = true)
        every { LocaleListCompat.getEmptyLocaleList() } returns mockk(relaxed = true)

        every { anyConstructed<Intent>().setPackage(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun localeListReporting(languageTags: String): LocaleListCompat {
        val locales = mockk<LocaleListCompat>()
        every { locales.toLanguageTags() } returns languageTags
        return locales
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(context, repository, monitorGlobalPrefs)

    // ---- constructor seeding of `language` ----

    @Test
    fun `seeds language from a pre-existing locale tag`() {
        every { AppCompatDelegate.getApplicationLocales() } returns localeListReporting("de")

        val viewModel = createViewModel()

        assertEquals(AppLanguage.German, viewModel.language.value)
    }

    @Test
    fun `seeds SystemDefault when there is no pre-existing locale override`() {
        every { AppCompatDelegate.getApplicationLocales() } returns localeListReporting("")

        val viewModel = createViewModel()

        assertEquals(AppLanguage.SystemDefault, viewModel.language.value)
    }

    // ---- setLanguage ----

    @Test
    fun `setLanguage updates the language StateFlow`() {
        val viewModel = createViewModel()

        viewModel.setLanguage(AppLanguage.French)

        assertEquals(AppLanguage.French, viewModel.language.value)
    }

    @Test
    fun `setLanguage applies forLanguageTags for a tagged language`() {
        val frenchLocales = mockk<LocaleListCompat>(relaxed = true)
        every { LocaleListCompat.forLanguageTags("fr") } returns frenchLocales
        val viewModel = createViewModel()

        viewModel.setLanguage(AppLanguage.French)

        verify { AppCompatDelegate.setApplicationLocales(frenchLocales) }
    }

    @Test
    fun `setLanguage applies an empty LocaleListCompat for SystemDefault`() {
        val emptyLocales = mockk<LocaleListCompat>(relaxed = true)
        every { LocaleListCompat.getEmptyLocaleList() } returns emptyLocales
        val viewModel = createViewModel()

        viewModel.setLanguage(AppLanguage.SystemDefault)

        verify { AppCompatDelegate.setApplicationLocales(emptyLocales) }
    }

    // ---- setFloatingTorchButtonEnabled ----

    @Test
    fun `setFloatingTorchButtonEnabled(true) fires an Intent carrying the start action`() {
        val viewModel = createViewModel()

        viewModel.setFloatingTorchButtonEnabled(true)

        verify {
            constructedWith<Intent>(EqMatcher("dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_START"))
                .setPackage(any())
        }
        verify { context.startService(any()) }
    }

    @Test
    fun `setFloatingTorchButtonEnabled(false) fires an Intent carrying the stop action`() {
        val viewModel = createViewModel()

        viewModel.setFloatingTorchButtonEnabled(false)

        verify {
            constructedWith<Intent>(EqMatcher("dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_STOP"))
                .setPackage(any())
        }
        verify { context.startService(any()) }
    }

    @Test
    fun `setFloatingTorchButtonEnabled swallows an exception thrown by the context call`() {
        every { context.startService(any()) } throws RuntimeException("service unavailable")
        val viewModel = createViewModel()

        // Must not throw: the try/catch exists precisely because the overlay service may
        // not be installed yet (wrong flavor / first launch before TorchOverlayService
        // ships its manifest entry).
        viewModel.setFloatingTorchButtonEnabled(true)
    }
}
