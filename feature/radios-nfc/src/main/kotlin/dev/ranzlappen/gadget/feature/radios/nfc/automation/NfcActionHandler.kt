package dev.ranzlappen.gadget.feature.radios.nfc.automation

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcActionHandler @Inject constructor(
    private val hceState: NfcHceState,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_EMULATE_TEXT,
            label = "Emulate NFC text tag",
            params = listOf(ActionParam(PARAM_TEXT, ActionParamType.Text, "")),
        ),
        ModuleAction(
            key = ACTION_EMULATE_URL,
            label = "Emulate NFC URL tag",
            params = listOf(ActionParam(PARAM_URL, ActionParamType.Text, "")),
        ),
        ModuleAction(
            key = ACTION_CLEAR_HCE,
            label = "Clear NFC HCE emulation",
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        return when (actionKey) {
            ACTION_EMULATE_TEXT -> {
                val text = params[PARAM_TEXT]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("text param required")
                val record = NdefRecord.createTextRecord("en", text)
                hceState.setPayload(NdefMessage(record).toByteArray())
                ActionResult.Success
            }
            ACTION_EMULATE_URL -> {
                val url = params[PARAM_URL]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("url param required")
                val record = NdefRecord.createUri(Uri.parse(url))
                hceState.setPayload(NdefMessage(record).toByteArray())
                ActionResult.Success
            }
            ACTION_CLEAR_HCE -> {
                hceState.setPayload(null)
                ActionResult.Success
            }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "nfc"
        const val ACTION_EMULATE_TEXT = "nfc_emulate_text"
        const val ACTION_EMULATE_URL = "nfc_emulate_url"
        const val ACTION_CLEAR_HCE = "nfc_clear_hce"
        const val PARAM_TEXT = "text"
        const val PARAM_URL = "url"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(NfcActionHandler.FEATURE_ID)
    abstract fun bindNfcActionHandler(impl: NfcActionHandler): ActionHandler
}
