package dev.ranzlappen.gadget.feature.lock.automation

import android.app.KeyguardManager
import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [LockActionHandler] — `:feature:lock`'s automation
 * `ActionHandler` seam. All three actions read straight off a
 * [KeyguardManager] obtained from `Context.getSystemService`; the "unavailable"
 * path (`getSystemService` returning something that isn't a [KeyguardManager],
 * or null) is exercised by having the mocked context return null for the
 * service lookup.
 */
class LockActionHandlerTest {

    private fun handlerWith(keyguard: KeyguardManager?): LockActionHandler {
        val context = mockk<Context>()
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguard
        return LockActionHandler(context)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(LockActionHandler.FEATURE_ID, handlerWith(null).featureId)
        assertEquals("lock", handlerWith(null).featureId)
    }

    @Test
    fun `declares assert-locked, assert-unlocked and assert-secure actions`() {
        val handler = handlerWith(null)

        assertEquals(
            setOf(
                LockActionHandler.ACTION_ASSERT_LOCKED,
                LockActionHandler.ACTION_ASSERT_UNLOCKED,
                LockActionHandler.ACTION_ASSERT_SECURE,
            ),
            handler.actions.map { it.key }.toSet(),
        )
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handlerWith(mockk(relaxed = true)).dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `every action fails when KeyguardManager is unavailable`() = runTest {
        val handler = handlerWith(null)

        for (action in listOf(
            LockActionHandler.ACTION_ASSERT_LOCKED,
            LockActionHandler.ACTION_ASSERT_UNLOCKED,
            LockActionHandler.ACTION_ASSERT_SECURE,
        )) {
            assertEquals(ActionResult.Failure("KeyguardManager unavailable"), handler.dispatch(action, emptyMap()))
        }
    }

    // ---- lock_assert_locked ----

    @Test
    fun `assert-locked succeeds when the keyguard is locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns true

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_LOCKED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-locked fails when the keyguard is not locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_LOCKED, emptyMap())

        assertEquals(ActionResult.Failure("Device is not locked"), result)
    }

    // ---- lock_assert_unlocked ----

    @Test
    fun `assert-unlocked succeeds when the keyguard is not locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_UNLOCKED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-unlocked fails when the keyguard is locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns true

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_UNLOCKED, emptyMap())

        assertEquals(ActionResult.Failure("Device is locked"), result)
    }

    // ---- lock_assert_secure ----

    @Test
    fun `assert-secure succeeds when the device has a screen lock configured`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isDeviceSecure } returns true

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_SECURE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-secure fails when no screen lock is configured`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isDeviceSecure } returns false

        val result = handlerWith(keyguard).dispatch(LockActionHandler.ACTION_ASSERT_SECURE, emptyMap())

        assertEquals(ActionResult.Failure("No screen lock configured"), result)
    }

    @Test
    fun `assert-locked ignores unrecognised params`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns true

        val result = handlerWith(keyguard).dispatch(
            LockActionHandler.ACTION_ASSERT_LOCKED,
            mapOf("unused" to "value"),
        )

        assertEquals(ActionResult.Success, result)
    }
}
