package dev.ranzlappen.gadget.feature.storage.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.storage.StorageMonitor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val monitor = mockk<StorageMonitor>(relaxed = true)
    private val handler = StorageActionHandler(context, monitor)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `assert-free-space succeeds when free space clears the threshold`() = runBlocking {
        every { monitor.internalFreeBytes() } returns 10L * 1024 * 1024 * 1024
        val result = handler.dispatch(
            StorageActionHandler.ACTION_ASSERT_FREE_SPACE,
            mapOf(StorageActionHandler.PARAM_THRESHOLD_GB to "5"),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-free-space fails when free space is below the threshold`() = runBlocking {
        every { monitor.internalFreeBytes() } returns 2L * 1024 * 1024 * 1024
        val result = handler.dispatch(
            StorageActionHandler.ACTION_ASSERT_FREE_SPACE,
            mapOf(StorageActionHandler.PARAM_THRESHOLD_GB to "5"),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `an unparseable threshold falls back to the default`() = runBlocking {
        every { monitor.internalFreeBytes() } returns 6L * 1024 * 1024 * 1024
        val result = handler.dispatch(
            StorageActionHandler.ACTION_ASSERT_FREE_SPACE,
            mapOf(StorageActionHandler.PARAM_THRESHOLD_GB to "not-a-number"),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(StorageActionHandler.FEATURE_ID, handler.featureId)
    }
}
