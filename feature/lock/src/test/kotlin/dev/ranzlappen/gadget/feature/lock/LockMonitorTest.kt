package dev.ranzlappen.gadget.feature.lock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [LockMonitor]'s `readState()` state-transition logic — the
 * one piece of real business logic in this module beyond plain passthrough:
 * the locked/secure/biometric-available reads off [KeyguardManager] and
 * [BiometricManager], and the broadcast-driven state rebuild. `BiometricManager.from`
 * is a static factory, intercepted via `mockkStatic` (same technique already
 * used for `AppCompatDelegate`/`LocaleListCompat` in `SettingsViewModelTest`);
 * `ContextCompat.registerReceiver` is likewise static-mocked, and the
 * captured [BroadcastReceiver] is driven manually to simulate a real
 * broadcast (no Robolectric shadow available).
 */
class LockMonitorTest {

    @Before
    fun setUp() {
        mockkStatic(BiometricManager::class)
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun contextWith(
        keyguard: KeyguardManager?,
        biometricStatus: Int = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
    ): Context {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguard
        val biometricManager = mockk<BiometricManager>()
        every { biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) } returns
            biometricStatus
        every { BiometricManager.from(context) } returns biometricManager
        every { ContextCompat.registerReceiver(context, any(), any(), any()) } returns null
        return context
    }

    @Test
    fun `initial state reports unlocked, insecure, no biometric when nothing is available`() {
        val context = contextWith(keyguard = null)

        val state = LockMonitor(context).state.value

        assertEquals(LockState(isLocked = false, isSecure = false, hasBiometric = false), state)
    }

    @Test
    fun `reports locked when the keyguard is locked`() {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns true
        every { keyguard.isDeviceSecure } returns false
        val context = contextWith(keyguard)

        assertEquals(true, LockMonitor(context).state.value.isLocked)
    }

    @Test
    fun `reports secure when the device has a screen lock configured`() {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false
        every { keyguard.isDeviceSecure } returns true
        val context = contextWith(keyguard)

        assertEquals(true, LockMonitor(context).state.value.isSecure)
    }

    @Test
    fun `reports biometric available when BiometricManager reports BIOMETRIC_SUCCESS`() {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false
        every { keyguard.isDeviceSecure } returns false
        val context = contextWith(keyguard, biometricStatus = BiometricManager.BIOMETRIC_SUCCESS)

        assertEquals(true, LockMonitor(context).state.value.hasBiometric)
    }

    @Test
    fun `reports no biometric for any non-success BiometricManager status`() {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false
        every { keyguard.isDeviceSecure } returns false
        val context = contextWith(keyguard, biometricStatus = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)

        assertEquals(false, LockMonitor(context).state.value.hasBiometric)
    }

    @Test
    fun `a broadcast rebuilds state from the latest keyguard reading`() {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false
        every { keyguard.isDeviceSecure } returns false
        val context = contextWith(keyguard)
        val receiverSlot = slot<BroadcastReceiver>()
        every { ContextCompat.registerReceiver(context, capture(receiverSlot), any(), any()) } returns null

        val monitor = LockMonitor(context)
        assertEquals(false, monitor.state.value.isLocked)

        every { keyguard.isKeyguardLocked } returns true
        receiverSlot.captured.onReceive(context, mockk<Intent>())

        assertEquals(true, monitor.state.value.isLocked)
    }
}
