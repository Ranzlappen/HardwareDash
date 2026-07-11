package dev.ranzlappen.gadget.feature.logbook.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.feature.logbook.LogbookRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogbookActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<LogbookRepository>(relaxed = true)
    private val handler = LogbookActionHandler(context, repository)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `add_entry writes a note through the repository`() = runBlocking {
        val result = handler.dispatch(
            LogbookActionHandler.ACTION_ADD_ENTRY,
            mapOf(LogbookActionHandler.PARAM_TEXT to "battery critical"),
        )
        assertEquals(ActionResult.Success, result)
        coVerify { repository.addEntry("battery critical", LogbookTagColor.None) }
    }

    @Test
    fun `add_entry with blank text fails`() = runBlocking {
        val result = handler.dispatch(
            LogbookActionHandler.ACTION_ADD_ENTRY,
            mapOf(LogbookActionHandler.PARAM_TEXT to "   "),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `assert_open_below succeeds when backlog is at or below the threshold`() = runBlocking {
        every { repository.openCheckpointCount } returns flowOf(3)
        val result = handler.dispatch(
            LogbookActionHandler.ACTION_ASSERT_OPEN_BELOW,
            mapOf(LogbookActionHandler.PARAM_THRESHOLD to "5"),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert_open_below fails when backlog exceeds the threshold`() = runBlocking {
        every { repository.openCheckpointCount } returns flowOf(9)
        val result = handler.dispatch(
            LogbookActionHandler.ACTION_ASSERT_OPEN_BELOW,
            mapOf(LogbookActionHandler.PARAM_THRESHOLD to "5"),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(LogbookActionHandler.FEATURE_ID, handler.featureId)
    }
}
