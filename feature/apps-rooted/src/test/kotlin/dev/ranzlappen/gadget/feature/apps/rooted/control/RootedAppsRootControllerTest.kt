package dev.ranzlappen.gadget.feature.apps.rooted.control

import android.content.Context
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.core.RootShellResult
import dev.ranzlappen.gadget.feature.apps.root.AppsRootControllerResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [RootedAppsRootController] — the safety-critical deny-list,
 * package-name validation, and [RootSafetyGate] plumbing the sibling
 * `AppsRootActionHandlerTest` deliberately does not exercise.
 *
 * The invariant under test: a denied or malformed package name must never
 * reach the privileged shell, and the deny-list is consulted **before** the
 * safety gate (so a protected package is refused even with Safety Mode off).
 */
class RootedAppsRootControllerTest {

    private val context = mockk<Context>(relaxed = true).also {
        every { it.packageName } returns OWN_PACKAGE
    }
    private val gate = mockk<RootSafetyGate>(relaxed = true)
    private val shell = mockk<RootShell>()
    private val controller = RootedAppsRootController(context, gate, shell)

    private fun allowGate() {
        coEvery { gate.check(any()) } returns RootGateDecision.Allowed
    }

    private fun shellSucceeds() {
        coEvery { shell.exec(any<String>(), any()) } returns
            RootShellResult(exitCode = 0, stdout = emptyList(), stderr = emptyList(), durationMillis = 1)
    }

    // ── Package-name validation ──────────────────────────────────────────

    @Test
    fun `a malformed package name is denied before the shell or gate`() = runTest {
        val result = controller.freezeApp("not-a-package")

        assertTrue(result is AppsRootControllerResult.Denied)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
        coVerify(exactly = 0) { gate.check(any()) }
    }

    @Test
    fun `a single-segment name is rejected as malformed`() = runTest {
        assertTrue(controller.freezeApp("android") is AppsRootControllerResult.Denied)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    // ── Hard deny-list ───────────────────────────────────────────────────

    @Test
    fun `a hard-deny-list package is refused and never reaches the gate`() = runTest {
        val result = controller.freezeApp("com.android.systemui")

        assertTrue(result is AppsRootControllerResult.Denied)
        coVerify(exactly = 0) { gate.check(any()) }
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    @Test
    fun `the deny-list wins even when the safety gate would allow it`() = runTest {
        allowGate()
        shellSucceeds()

        val result = controller.forceStopApp("com.android.settings")

        assertTrue(result is AppsRootControllerResult.Denied)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    @Test
    fun `this app's own package is denied`() = runTest {
        val result = controller.freezeApp(OWN_PACKAGE)

        assertTrue(result is AppsRootControllerResult.Denied)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    // ── Happy path + command shape ───────────────────────────────────────

    @Test
    fun `freeze issues pm disable-user and records the invocation on success`() = runTest {
        allowGate()
        shellSucceeds()

        val result = controller.freezeApp("com.example.app")

        assertEquals(AppsRootControllerResult.Ok(statusNote = "com.example.app disabled"), result)
        coVerify { shell.exec("pm disable-user --user 0 com.example.app", 10_000) }
        coVerify { gate.recordInvocation(RootFeatureKey.AppsFreezeApp) }
    }

    @Test
    fun `unfreeze issues pm enable`() = runTest {
        allowGate()
        shellSucceeds()

        val result = controller.unfreezeApp("com.example.app")

        assertEquals(AppsRootControllerResult.Ok(statusNote = "com.example.app enabled"), result)
        coVerify { shell.exec("pm enable com.example.app", 10_000) }
    }

    @Test
    fun `force-stop issues am force-stop`() = runTest {
        allowGate()
        shellSucceeds()

        val result = controller.forceStopApp("com.example.app")

        assertEquals(AppsRootControllerResult.Ok(statusNote = "com.example.app force-stopped"), result)
        coVerify { shell.exec("am force-stop com.example.app", 10_000) }
    }

    @Test
    fun `a surrounding-whitespace package name is trimmed before the shell`() = runTest {
        allowGate()
        shellSucceeds()

        controller.freezeApp("  com.example.app  ")

        coVerify { shell.exec("pm disable-user --user 0 com.example.app", 10_000) }
    }

    // ── Gate decisions ───────────────────────────────────────────────────

    @Test
    fun `a user opt-out surfaces as OptedOut and never runs the shell`() = runTest {
        coEvery { gate.check(any()) } returns RootGateDecision.BlockedByUser

        val result = controller.freezeApp("com.example.app")

        assertEquals(AppsRootControllerResult.OptedOut, result)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    @Test
    fun `a limiter block surfaces the retry window`() = runTest {
        coEvery { gate.check(any()) } returns RootGateDecision.BlockedByLimiter(retryAfterMillis = 5_000)

        val result = controller.freezeApp("com.example.app")

        assertEquals(AppsRootControllerResult.RateLimited(retryAfterMillis = 5_000), result)
        coVerify(exactly = 0) { shell.exec(any<String>(), any()) }
    }

    @Test
    fun `an unsupported gate surfaces as Unsupported`() = runTest {
        coEvery { gate.check(any()) } returns RootGateDecision.Unsupported

        assertEquals(AppsRootControllerResult.Unsupported, controller.freezeApp("com.example.app"))
    }

    // ── Shell failure ────────────────────────────────────────────────────

    @Test
    fun `a non-zero shell exit surfaces as HardwareError and does not record success`() = runTest {
        allowGate()
        coEvery { shell.exec(any<String>(), any()) } returns
            RootShellResult(
                exitCode = 1,
                stdout = emptyList(),
                stderr = listOf("Unknown package"),
                durationMillis = 1,
            )

        val result = controller.freezeApp("com.example.app")

        assertTrue(result is AppsRootControllerResult.HardwareError)
        coVerify(exactly = 0) { gate.recordInvocation(any()) }
    }

    private companion object {
        const val OWN_PACKAGE = "dev.ranzlappen.gadget.rooted"
    }
}
