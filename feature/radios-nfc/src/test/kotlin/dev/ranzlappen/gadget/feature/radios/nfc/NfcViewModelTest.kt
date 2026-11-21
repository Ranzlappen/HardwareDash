package dev.ranzlappen.gadget.feature.radios.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagInbox
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagInfo
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagIo
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteMode
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteOutcome
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
 *  - [NfcViewModel.onTag]: read via [NfcTagIo], tags handed over by
 *    [NfcTagInbox], and the armed-write flow (v3 B11; record decoding itself
 *    is pure and covered by `NfcParsingTest`),
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
 * the outgoing NDEF-encoding paths (HCE and tag writes); tags are opaque
 * `mockk<Tag>()` handed to a mocked [NfcTagIo].
 */
class NfcViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val adapter = mockk<NfcAdapterWrapper>()
    private val templateRepository = mockk<NfcTemplateRepository>()
    private val nfcController = mockk<NfcController>(relaxed = true)
    private val hceState = NfcHceState()
    private val tagIo = mockk<NfcTagIo>()
    private val tagInbox = NfcTagInbox()
    private val tagInfo = NfcTagInfo(uid = "04:A1", techs = listOf("NfcA", "Ndef"), chip = "NTAG215")

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
        every { NdefRecord.createUri(any<String>()) } returns outgoingRecord
        coEvery { tagIo.read(any()) } returns tagInfo
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
        NfcViewModel(adapter, hceState, templateRepository, nfcController, tagIo, tagInbox, rootRegistry(isRootedFlavor))

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

    // ---- tag read / write (v3 B11) ----

    @Test
    fun `onTag reads the tag and shows it`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val tag = mockk<Tag>()

        viewModel.onTag(tag)
        advanceUntilIdle()

        assertEquals(tagInfo, viewModel.state.value.tag)
        assertFalse(viewModel.state.value.readingTag)
        coVerify(exactly = 0) { tagIo.write(any(), any()) }
    }

    @Test
    fun `a tag offered to the inbox is read and consumed`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        tagInbox.offer(mockk())

        advanceUntilIdle()

        assertEquals(tagInfo, viewModel.state.value.tag)
        assertTrue(tagInbox.incoming.replayCache.isEmpty())
    }

    @Test
    fun `armWrite is ignored while the write text is blank`() {
        val viewModel = createViewModel()

        viewModel.armWrite()

        assertFalse(viewModel.state.value.writeArmed)
    }

    @Test
    fun `an armed write writes the next tag, disarms and reads it back`() = runTest(dispatcher) {
        val tag = mockk<Tag>()
        coEvery { tagIo.write(tag, any()) } returns NfcWriteOutcome.Written(12, formatted = false)
        val viewModel = createViewModel()
        viewModel.setWriteMode(NfcWriteMode.Url)
        viewModel.setWriteText("example.com")
        viewModel.armWrite()
        assertTrue(viewModel.state.value.writeArmed)

        viewModel.onTag(tag)
        advanceUntilIdle()

        coVerify { tagIo.write(tag, any()) }
        coVerify { tagIo.read(tag) }
        verify { NdefRecord.createUri("https://example.com") }
        assertFalse(viewModel.state.value.writeArmed)
        assertEquals(NfcWriteOutcome.Written(12, formatted = false), viewModel.state.value.writeOutcome)
    }

    @Test
    fun `cancelWrite disarms without writing`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.setWriteText("hello")
        viewModel.armWrite()
        viewModel.cancelWrite()

        viewModel.onTag(mockk())
        advanceUntilIdle()

        coVerify(exactly = 0) { tagIo.write(any(), any()) }
        assertNull(viewModel.state.value.writeOutcome)
    }

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
