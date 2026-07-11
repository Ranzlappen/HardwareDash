package dev.ranzlappen.gadget.feature.radios.nfc.automation

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NfcActionHandler] — `:feature:radios-nfc`'s automation
 * `ActionHandler` seam. Both emulate-* actions build a real `NdefRecord` /
 * `NdefMessage` and push the encoded bytes into [NfcHceState]; since this repo
 * has no Robolectric shadow, `NdefRecord.createTextRecord` / `createUri` and
 * `Uri.parse` are intercepted via `mockkStatic` and the `NdefMessage`
 * constructor via `mockkConstructor` — the same "static + constructor"
 * technique `AudioRecorderTest` uses for `AudioRecord`. [NfcHceState] itself
 * carries no Android surface, so it's exercised for real (not mocked) to
 * assert on the bytes the handler actually pushes.
 */
class NfcActionHandlerTest {

    private val hceState = NfcHceState()
    private val handler = NfcActionHandler(hceState)

    private val textRecord = mockk<NdefRecord>()
    private val uriRecord = mockk<NdefRecord>()
    private val parsedUri = mockk<Uri>()

    @Before
    fun setUp() {
        mockkStatic(NdefRecord::class)
        mockkStatic(Uri::class)
        mockkConstructor(NdefMessage::class)
        every { NdefRecord.createTextRecord(any(), any()) } returns textRecord
        every { Uri.parse(any()) } returns parsedUri
        every { NdefRecord.createUri(any<Uri>()) } returns uriRecord
        every { anyConstructed<NdefMessage>().toByteArray() } returns byteArrayOf(0x01, 0x02, 0x03)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(NfcActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("nfc", handler.featureId)
    }

    @Test
    fun `declares the emulate-text, emulate-url and clear-hce actions`() {
        assertEquals(
            setOf(
                NfcActionHandler.ACTION_EMULATE_TEXT,
                NfcActionHandler.ACTION_EMULATE_URL,
                NfcActionHandler.ACTION_CLEAR_HCE,
            ),
            handler.actions.map { it.key }.toSet(),
        )
        // None of the NFC actions touch the rooted-only NCI surface.
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `emulate-text action declares a single text param`() {
        val action = handler.actions.single { it.key == NfcActionHandler.ACTION_EMULATE_TEXT }

        assertEquals(1, action.params.size)
        assertEquals(NfcActionHandler.PARAM_TEXT, action.params.single().name)
        assertEquals(ActionParamType.Text, action.params.single().type)
    }

    @Test
    fun `emulate-url action declares a single url param`() {
        val action = handler.actions.single { it.key == NfcActionHandler.ACTION_EMULATE_URL }

        assertEquals(1, action.params.size)
        assertEquals(NfcActionHandler.PARAM_URL, action.params.single().name)
        assertEquals(ActionParamType.Text, action.params.single().type)
    }

    @Test
    fun `clear-hce action declares no params`() {
        val action = handler.actions.single { it.key == NfcActionHandler.ACTION_CLEAR_HCE }

        assertTrue(action.params.isEmpty())
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())

        assertEquals(ActionResult.Unsupported, result)
    }

    // ---- nfc_emulate_text ----

    @Test
    fun `emulate-text fails when the text param is missing`() = runTest {
        val result = handler.dispatch(NfcActionHandler.ACTION_EMULATE_TEXT, emptyMap())

        assertEquals(ActionResult.Failure("text param required"), result)
        assertNull(hceState.payload.value)
    }

    @Test
    fun `emulate-text fails when the text param is blank`() = runTest {
        val result = handler.dispatch(
            NfcActionHandler.ACTION_EMULATE_TEXT,
            mapOf(NfcActionHandler.PARAM_TEXT to "   "),
        )

        assertEquals(ActionResult.Failure("text param required"), result)
        assertNull(hceState.payload.value)
    }

    @Test
    fun `emulate-text succeeds and pushes the encoded NDEF bytes into hce state`() = runTest {
        val result = handler.dispatch(
            NfcActionHandler.ACTION_EMULATE_TEXT,
            mapOf(NfcActionHandler.PARAM_TEXT to "hello"),
        )

        assertEquals(ActionResult.Success, result)
        assertEquals(listOf<Byte>(0x01, 0x02, 0x03), hceState.payload.value?.toList())
        verify { NdefRecord.createTextRecord("en", "hello") }
    }

    // ---- nfc_emulate_url ----

    @Test
    fun `emulate-url fails when the url param is missing`() = runTest {
        val result = handler.dispatch(NfcActionHandler.ACTION_EMULATE_URL, emptyMap())

        assertEquals(ActionResult.Failure("url param required"), result)
        assertNull(hceState.payload.value)
    }

    @Test
    fun `emulate-url fails when the url param is blank`() = runTest {
        val result = handler.dispatch(
            NfcActionHandler.ACTION_EMULATE_URL,
            mapOf(NfcActionHandler.PARAM_URL to ""),
        )

        assertEquals(ActionResult.Failure("url param required"), result)
        assertNull(hceState.payload.value)
    }

    @Test
    fun `emulate-url succeeds and pushes the encoded NDEF bytes into hce state`() = runTest {
        val result = handler.dispatch(
            NfcActionHandler.ACTION_EMULATE_URL,
            mapOf(NfcActionHandler.PARAM_URL to "https://example.com"),
        )

        assertEquals(ActionResult.Success, result)
        assertEquals(listOf<Byte>(0x01, 0x02, 0x03), hceState.payload.value?.toList())
        verify { Uri.parse("https://example.com") }
        verify { NdefRecord.createUri(parsedUri) }
    }

    // ---- nfc_clear_hce ----

    @Test
    fun `clear-hce clears a previously set payload`() = runTest {
        hceState.setPayload(byteArrayOf(0x09))

        val result = handler.dispatch(NfcActionHandler.ACTION_CLEAR_HCE, emptyMap())

        assertEquals(ActionResult.Success, result)
        assertNull(hceState.payload.value)
    }

    @Test
    fun `clear-hce ignores unrecognised params`() = runTest {
        val result = handler.dispatch(NfcActionHandler.ACTION_CLEAR_HCE, mapOf("unused" to "value"))

        assertEquals(ActionResult.Success, result)
    }
}
