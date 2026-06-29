package dev.ranzlappen.gadget.feature.bugreport.rooted.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.bugreport.rooted.PermissionGrantResult
import dev.ranzlappen.gadget.feature.bugreport.rooted.RootedPermissionGranter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootedBugReportActionHandlerTest {

    private val granter = mockk<RootedPermissionGranter>()
    private val handler = RootedBugReportActionHandler(granter)

    @Test
    fun `unknown action is unsupported`() = runTest {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `force grant forwards the permission param and maps Ok to Success`() = runTest {
        coEvery { granter.forceGrant(any()) } returns PermissionGrantResult.Ok
        val result = handler.dispatch(
            RootedBugReportActionHandler.ACTION_FORCE_GRANT,
            mapOf(RootedBugReportActionHandler.PARAM_PERMISSION to "android.permission.CAMERA"),
        )
        assertEquals(ActionResult.Success, result)
        coVerify { granter.forceGrant("android.permission.CAMERA") }
    }

    @Test
    fun `invalid and opted-out map to failures`() = runTest {
        coEvery { granter.forceGrant(any()) } returns PermissionGrantResult.InvalidPermission
        assertTrue(
            handler.dispatch(RootedBugReportActionHandler.ACTION_FORCE_GRANT, emptyMap())
                is ActionResult.Failure,
        )
        coEvery { granter.forceGrant(any()) } returns PermissionGrantResult.OptedOut
        assertTrue(
            handler.dispatch(
                RootedBugReportActionHandler.ACTION_FORCE_GRANT,
                mapOf(RootedBugReportActionHandler.PARAM_PERMISSION to "x"),
            ) is ActionResult.Failure,
        )
    }

    @Test
    fun `unsupported maps to Unsupported`() = runTest {
        coEvery { granter.forceGrant(any()) } returns PermissionGrantResult.Unsupported
        assertEquals(
            ActionResult.Unsupported,
            handler.dispatch(
                RootedBugReportActionHandler.ACTION_FORCE_GRANT,
                mapOf(RootedBugReportActionHandler.PARAM_PERMISSION to "x"),
            ),
        )
    }

    @Test
    fun `the action is declared requiresRoot with a permission param`() {
        val action = handler.actions.single { it.key == RootedBugReportActionHandler.ACTION_FORCE_GRANT }
        assertTrue(action.requiresRoot)
        assertTrue(action.params.any { it.name == RootedBugReportActionHandler.PARAM_PERMISSION })
    }
}
