package dev.ranzlappen.gadget.feature.radios.nfc

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Parcelable
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplateRepository
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NfcViewModel]'s non-passthrough logic:
 *  - seeding [NfcState] from [NfcAdapterWrapper] / [NfcTemplateRepository] /
 *    [RootCapabilityRegistry] at construction, and [NfcViewModel.refresh]'s
 *    re-read,
 *  - [NfcViewModel.onNewIntent]'s tag/NDEF-record parsing (hex tag id,
 *    tech-list join, and the well-known-text / well-known-uri / mime-media /
 *    fallback-hex branches of the private `parseNdefRecord`),
 *  - [NfcViewModel.activateHce] / [NfcViewModel.applyTemplate]'s NDEF-mode
 *    branching and [NfcViewModel.clearHce], and
 *  - the template-picker open/select/set-value/close flow.
 *
 * Pure passthrough setters (`setHcePayload`) are skipped, matching this
 * repo's established convention (see `BtViewModelTest`).
 *
 * `NdefRecord.createTextRecord` / `createUri`, `Uri.parse` and the
 * `NdefMessage` constructor are intercepted via `mockkStatic` /
 * `mockkConstructor` — the same technique `NfcActionHandlerTest` uses — for
 * the outgoing (`activateHce` / `applyTemplate`) NDEF-encoding path. The
 * incoming (`onNewIntent`) parsing path instead stubs plain `mockk<Tag>()` /
 * `mockk<NdefMessage>()` / `mockk<NdefRecord>()` instances, since it only
 * reads their properties — it never constructs one itself.
 */
class NfcViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val adapter = mockk<NfcAdapterWrapper>()
    private val templateRepository = mockk<NfcTemplateRepository>()
    private val nfcController = mockk<NfcController>(relaxed = true)
    private val hceState = NfcHceState()

    private val outgoingRecord = mockk<NdefRecord>()
    private val parsedUri = mockk<Uri>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { adapter.isAvailable() } returns true
        every { adapter.isEnabled() } returns true
        every { templateRepository.templates } returns emptyList()

        // Outgoing NDEF-encoding seam (activateHce / applyTemplate).
        mockkStatic(NdefRecord::class)
        mockkStatic(Uri::class)
        mockkConstructor(NdefMessage::class)
        every { NdefRecord.createTextRecord(any(), any()) } returns outgoingRecord
        every { Uri.parse(any()) } returns parsedUri
        every { NdefRecord.createUri(any<Uri>()) } returns outgoingRecord
        every { anyConstructed<NdefMessage>().toByteArray() } returns byteArrayOf(0x09, 0x09, 0x09)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun rootRegistry(isRootedFlavor: Boolean): RootCapabilityRegistry {
        val registry = mockk<RootCapabilityRegistry>(relaxed = true)
        every { registry.isRootedFlavor } returns isRootedFlavor
        return registry
    }

    private fun createViewModel(isRootedFlavor: Boolean = false): NfcViewModel =
        NfcViewModel(adapter, hceState, templateRepository, nfcController, rootRegistry(isRootedFlavor))

    private fun mockTag(idBytes: ByteArray, techs: List<String>): Tag {
        val tag = mockk<Tag>()
        every { tag.id } returns idBytes
        every { tag.techList } returns techs.toTypedArray()
        return tag
    }

    private fun mockIntent(tag: Tag?, ndefMessages: Array<Parcelable>?): Intent {
        val intent = mockk<Intent>()
        every { intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) } returns tag
        every { intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) } returns ndefMessages
        return intent
    }

    private fun template(mode: String, template: String) = NfcTemplate(
        id = "1",
        name = "Test",
        category = "custom",
        mode = mode,
        template = template,
    )

    // ---- construction ----

    @Test
    fun `seeds isRootedFlavor from the root capability registry`() {
        assertTrue(createViewModel(isRootedFlavor = true).state.value.isRootedFlavor)
        assertFalse(createViewModel(isRootedFlavor = false).state.value.isRootedFlavor)
    }

    @Test
    fun `seeds adapter availability, enabled state and templates at construction`() {
        val templates = listOf(template("TEXT", "a"), template("URL", "b"))
        every { templateRepository.templates } returns templates

        val state = createViewModel().state.value

        assertTrue(state.adapterPresent)
        assertTrue(state.adapterEnabled)
        assertEquals(templates, state.templates)
    }

    // ---- refresh ----

    @Test
    fun `refresh re-reads adapter availability and enabled state`() {
        val viewModel = createViewModel()
        every { adapter.isAvailable() } returns false
        every { adapter.isEnabled() } returns false

        viewModel.refresh()

        val state = viewModel.state.value
        assertFalse(state.adapterPresent)
        assertFalse(state.adapterEnabled)
    }

    // ---- onNewIntent ----

    @Test
    fun `onNewIntent is a no-op when the intent carries no tag extra`() {
        val viewModel = createViewModel()
        val before = viewModel.state.value

        viewModel.onNewIntent(mockIntent(tag = null, ndefMessages = null))

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun `onNewIntent records the hex tag id and joined tech list with no NDEF payload`() {
        val viewModel = createViewModel()
        val tag = mockTag(
            idBytes = byteArrayOf(0x04, 0xAB.toByte(), 0x12),
            techs = listOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef"),
        )

        viewModel.onNewIntent(mockIntent(tag = tag, ndefMessages = null))

        val state = viewModel.state.value
        assertEquals("04AB12", state.lastTagId)
        assertEquals("NfcA, Ndef", state.lastTagFormat)
        assertNull(state.lastTagPayload)
    }

    @Test
    fun `onNewIntent parses a well-known text NDEF record`() {
        val viewModel = createViewModel()
        val tag = mockTag(byteArrayOf(0x01), listOf("android.nfc.tech.Ndef"))
        val payload = byteArrayOf(2) + "en".toByteArray() + "Hello".toByteArray()
        val record = mockk<NdefRecord>()
        every { record.tnf } returns NdefRecord.TNF_WELL_KNOWN
        every { record.type } returns NdefRecord.RTD_TEXT
        every { record.payload } returns payload
        val message = mockk<NdefMessage>()
        every { message.records } returns arrayOf(record)

        viewModel.onNewIntent(mockIntent(tag = tag, ndefMessages = arrayOf<Parcelable>(message)))

        assertEquals("Hello", viewModel.state.value.lastTagPayload)
    }

    @Test
    fun `onNewIntent parses a well-known uri NDEF record`() {
        val viewModel = createViewModel()
        val tag = mockTag(byteArrayOf(0x01), listOf("android.nfc.tech.Ndef"))
        val record = mockk<NdefRecord>()
        every { record.tnf } returns NdefRecord.TNF_WELL_KNOWN
        every { record.type } returns NdefRecord.RTD_URI
        val incomingUri = mockk<Uri>()
        every { incomingUri.toString() } returns "https://example.com"
        every { record.toUri() } returns incomingUri
        val message = mockk<NdefMessage>()
        every { message.records } returns arrayOf(record)

        viewModel.onNewIntent(mockIntent(tag = tag, ndefMessages = arrayOf<Parcelable>(message)))

        assertEquals("https://example.com", viewModel.state.value.lastTagPayload)
    }

    @Test
    fun `onNewIntent parses a mime-media NDEF record as UTF-8 text`() {
        val viewModel = createViewModel()
        val tag = mockTag(byteArrayOf(0x01), listOf("android.nfc.tech.Ndef"))
        val record = mockk<NdefRecord>()
        every { record.tnf } returns NdefRecord.TNF_MIME_MEDIA
        every { record.payload } returns "image data".toByteArray()
        val message = mockk<NdefMessage>()
        every { message.records } returns arrayOf(record)

        viewModel.onNewIntent(mockIntent(tag = tag, ndefMessages = arrayOf<Parcelable>(message)))

        assertEquals("image data", viewModel.state.value.lastTagPayload)
    }

    @Test
    fun `onNewIntent falls back to a hex dump for any other record type`() {
        val viewModel = createViewModel()
        val tag = mockTag(byteArrayOf(0x01), listOf("android.nfc.tech.Ndef"))
        val record = mockk<NdefRecord>()
        every { record.tnf } returns NdefRecord.TNF_UNKNOWN
        every { record.payload } returns byteArrayOf(0xDE.toByte(), 0xAD.toByte())
        val message = mockk<NdefMessage>()
        every { message.records } returns arrayOf(record)

        viewModel.onNewIntent(mockIntent(tag = tag, ndefMessages = arrayOf<Parcelable>(message)))

        assertEquals("DEAD", viewModel.state.value.lastTagPayload)
    }

    // ---- activateHce ----

    @Test
    fun `activateHce TEXT mode builds a text NDEF payload and updates hceMode`() {
        val viewModel = createViewModel()
        viewModel.setHcePayload("hello")

        viewModel.activateHce(NfcHceMode.TEXT)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcHceMode.TEXT, viewModel.state.value.hceMode)
        assertEquals(listOf<Byte>(0x09, 0x09, 0x09), hceState.payload.value?.toList())
    }

    @Test
    fun `activateHce URL mode builds a uri NDEF payload and updates hceMode`() {
        val viewModel = createViewModel()
        viewModel.setHcePayload("https://example.com")

        viewModel.activateHce(NfcHceMode.URL)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcHceMode.URL, viewModel.state.value.hceMode)
        assertEquals(listOf<Byte>(0x09, 0x09, 0x09), hceState.payload.value?.toList())
    }

    @Test
    fun `activateHce NONE mode clears the hce payload`() {
        val viewModel = createViewModel()
        hceState.setPayload(byteArrayOf(0x01))

        viewModel.activateHce(NfcHceMode.NONE)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcHceMode.NONE, viewModel.state.value.hceMode)
        assertNull(hceState.payload.value)
    }

    // ---- clearHce ----

    @Test
    fun `clearHce clears hce state and resets the hce fields`() {
        val viewModel = createViewModel()
        hceState.setPayload(byteArrayOf(0x01))
        viewModel.setHcePayload("something")

        viewModel.clearHce()

        assertNull(hceState.payload.value)
        assertEquals(NfcHceMode.NONE, viewModel.state.value.hceMode)
        assertEquals("", viewModel.state.value.hcePayload)
    }

    // ---- template picker flow ----

    @Test
    fun `template picker open, select, set-value and close flow updates state`() {
        val viewModel = createViewModel()
        val tmpl = template("TEXT", "{{x}}")

        viewModel.openTemplatePicker()
        assertTrue(viewModel.state.value.showTemplatePicker)

        viewModel.selectTemplate(tmpl)
        assertEquals(tmpl, viewModel.state.value.selectedTemplate)
        assertEquals(emptyMap(), viewModel.state.value.templateValues)

        viewModel.setTemplateValue("x", "1")
        assertEquals(mapOf("x" to "1"), viewModel.state.value.templateValues)

        viewModel.closeTemplatePicker()
        assertFalse(viewModel.state.value.showTemplatePicker)
        assertNull(viewModel.state.value.selectedTemplate)
        assertEquals(emptyMap(), viewModel.state.value.templateValues)
    }

    // ---- applyTemplate ----

    @Test
    fun `applyTemplate is a no-op when no template is selected`() {
        val viewModel = createViewModel()
        val before = viewModel.state.value

        viewModel.applyTemplate()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun `applyTemplate resolves the template, defaults to TEXT mode and updates hce state`() {
        val viewModel = createViewModel()
        viewModel.selectTemplate(template("TEXT", "Hi {{name}}"))
        viewModel.setTemplateValue("name", "Ann")

        viewModel.applyTemplate()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Hi Ann", state.hcePayload)
        assertEquals(NfcHceMode.TEXT, state.hceMode)
        assertFalse(state.showTemplatePicker)
        assertNull(state.selectedTemplate)
        assertEquals(listOf<Byte>(0x09, 0x09, 0x09), hceState.payload.value?.toList())
    }

    @Test
    fun `applyTemplate selects URL mode case-insensitively from the template's mode field`() {
        val viewModel = createViewModel()
        viewModel.selectTemplate(template("url", "https://{{host}}"))
        viewModel.setTemplateValue("host", "example.com")

        viewModel.applyTemplate()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://example.com", viewModel.state.value.hcePayload)
        assertEquals(NfcHceMode.URL, viewModel.state.value.hceMode)
    }

    @Test
    fun `applyTemplate treats any non-URL mode string as TEXT`() {
        val viewModel = createViewModel()
        viewModel.selectTemplate(template("custom", "plain {{value}}"))
        viewModel.setTemplateValue("value", "text")

        viewModel.applyTemplate()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcHceMode.TEXT, viewModel.state.value.hceMode)
    }
}
