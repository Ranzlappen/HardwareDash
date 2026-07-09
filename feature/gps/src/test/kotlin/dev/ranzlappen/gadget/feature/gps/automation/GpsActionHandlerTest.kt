package dev.ranzlappen.gadget.feature.gps.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.gps.GpsLocationTracker
import dev.ranzlappen.gadget.feature.gps.GpsState
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.control.GpsControllerResult
import dev.ranzlappen.gadget.feature.gps.spoof.GpsSpoofController
import dev.ranzlappen.gadget.feature.gps.spoof.SpoofConfig
import dev.ranzlappen.gadget.feature.gps.spoof.SpoofResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [GpsActionHandler]. Unlike torch/vibration, no branch here
 * touches an `Intent`/foreground service — [GpsLocationTracker],
 * [GpsSpoofController], and [GpsController] are called directly — so every
 * branch is reachable from a plain JVM test.
 */
class GpsActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val trackerState = MutableStateFlow(GpsState())
    private val tracker = mockk<GpsLocationTracker>(relaxed = true) {
        every { state } returns trackerState
    }
    private val spoofController = mockk<GpsSpoofController>(relaxed = true)
    private val gpsController = mockk<GpsController>(relaxed = true)
    private val handler = GpsActionHandler(context, tracker, spoofController, gpsController)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `track_start succeeds once tracking reports permission granted`() = runBlocking {
        trackerState.value = GpsState(permissionGranted = true)
        val result = handler.dispatch(GpsActionHandler.ACTION_TRACK_START, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `track_start fails when permission is not granted`() = runBlocking {
        trackerState.value = GpsState(permissionGranted = false)
        val result = handler.dispatch(GpsActionHandler.ACTION_TRACK_START, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `track_stop always succeeds`() = runBlocking {
        val result = handler.dispatch(GpsActionHandler.ACTION_TRACK_STOP, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `spoof_start requires lat and lon`() = runBlocking {
        val result = handler.dispatch(GpsActionHandler.ACTION_SPOOF_START, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `spoof_start builds a Static config from params and maps Ok to Success`() = runBlocking {
        val slot = slot<SpoofConfig>()
        coEvery { spoofController.start(capture(slot)) } returns SpoofResult.Ok
        val result = handler.dispatch(
            GpsActionHandler.ACTION_SPOOF_START,
            mapOf(
                GpsActionHandler.PARAM_LAT to "12.5",
                GpsActionHandler.PARAM_LON to "-3.25",
                GpsActionHandler.PARAM_ALT to "10",
            ),
        )
        assertEquals(ActionResult.Success, result)
        val config = slot.captured as SpoofConfig.Static
        assertEquals(12.5, config.lat)
        assertEquals(-3.25, config.lon)
        assertEquals(10.0, config.alt)
    }

    @Test
    fun `spoof_start defaults altitude to zero when omitted`() = runBlocking {
        val slot = slot<SpoofConfig>()
        coEvery { spoofController.start(capture(slot)) } returns SpoofResult.Ok
        handler.dispatch(
            GpsActionHandler.ACTION_SPOOF_START,
            mapOf(GpsActionHandler.PARAM_LAT to "0", GpsActionHandler.PARAM_LON to "0"),
        )
        assertEquals(0.0, (slot.captured as SpoofConfig.Static).alt)
    }

    @Test
    fun `spoof_start maps LegalNotAcknowledged to a Failure`() = runBlocking {
        coEvery { spoofController.start(any()) } returns SpoofResult.LegalNotAcknowledged
        val result = handler.dispatch(
            GpsActionHandler.ACTION_SPOOF_START,
            mapOf(GpsActionHandler.PARAM_LAT to "0", GpsActionHandler.PARAM_LON to "0"),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `spoof_stop maps Ok to Success`() = runBlocking {
        coEvery { spoofController.stop() } returns SpoofResult.Ok
        val result = handler.dispatch(GpsActionHandler.ACTION_SPOOF_STOP, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `rooted actions are flagged requiresRoot`() {
        val rootActions = handler.actions.filter { it.requiresRoot }.map { it.key }
        assertTrue(GpsActionHandler.ACTION_NMEA_RAW_TAP in rootActions)
        assertTrue(GpsActionHandler.ACTION_CONSTELLATION_DUMP in rootActions)
        assertTrue(GpsActionHandler.ACTION_RESET_GPS_MUTATIONS in rootActions)
    }

    @Test
    fun `standard-flavor tracking and spoof actions do not require root`() {
        val nonRootKeys = handler.actions.filter { !it.requiresRoot }.map { it.key }
        assertTrue(GpsActionHandler.ACTION_TRACK_START in nonRootKeys)
        assertTrue(GpsActionHandler.ACTION_TRACK_STOP in nonRootKeys)
        assertTrue(GpsActionHandler.ACTION_SPOOF_START in nonRootKeys)
        assertTrue(GpsActionHandler.ACTION_SPOOF_STOP in nonRootKeys)
    }

    @Test
    fun `nmea_raw_tap Unsupported maps to a Failure`() = runBlocking {
        coEvery { gpsController.nmeaRawTap(any()) } returns GpsControllerResult.Unsupported
        val result = handler.dispatch(GpsActionHandler.ACTION_NMEA_RAW_TAP, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `nmea_raw_tap snapshot maps to Success`() = runBlocking {
        coEvery { gpsController.nmeaRawTap(any()) } returns GpsControllerResult.NmeaSnapshot(listOf("\$GPGGA"))
        val result = handler.dispatch(GpsActionHandler.ACTION_NMEA_RAW_TAP, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `constellation_dump OptedOut maps to a Failure`() = runBlocking {
        coEvery { gpsController.constellationDump() } returns GpsControllerResult.OptedOut
        val result = handler.dispatch(GpsActionHandler.ACTION_CONSTELLATION_DUMP, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `reset_gps_mutations maps ResetCompleted to Success`() = runBlocking {
        coEvery { gpsController.resetAllGpsMutations() } returns GpsControllerResult.ResetCompleted(0, 0)
        val result = handler.dispatch(GpsActionHandler.ACTION_RESET_GPS_MUTATIONS, emptyMap())
        assertEquals(ActionResult.Success, result)
    }
}
