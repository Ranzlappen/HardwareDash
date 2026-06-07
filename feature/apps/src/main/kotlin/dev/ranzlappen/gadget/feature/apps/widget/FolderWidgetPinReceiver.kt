package dev.ranzlappen.gadget.feature.apps.widget

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.widgetkit.pin.BaseWidgetPinSuccessReceiver
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore

/**
 * Receives the `requestPinAppWidget` success callback from [PinFolderHelper]
 * and binds the freshly-placed widget to the chosen folder. Thin subclass of
 * the kit's [BaseWidgetPinSuccessReceiver] — the base owns the full claim →
 * save → post-save flow + `goAsync` lifecycle; this class only plugs in the
 * folder-specific action / token-extra constants, the Hilt accessors, and the
 * post-save repaint.
 *
 * The pre-pin [FolderWidgetConfig] is recovered from the [PendingWidgetConfigs]
 * bridge by the token the callback carries (set in [PinFolderHelper]); the OS
 * fills in the new `appWidgetId` (the callback is mutable). If this callback
 * never fires, [FolderWidgetProvider]'s `reconcilePendingConfig` claims the
 * sole pending entry instead — both paths are idempotent.
 */
class FolderWidgetPinReceiver : BaseWidgetPinSuccessReceiver<FolderWidgetConfig>() {

    override val expectedAction: String = ACTION_PIN_CALLBACK

    override val tokenExtraKey: String = EXTRA_PENDING_TOKEN

    override val logTag: String = PinFolderHelper.TAG

    override fun pendingConfigs(context: Context): PendingWidgetConfigs<FolderWidgetConfig> =
        entryPoint(context).folderPendingConfigs()

    override fun configStore(context: Context): WidgetConfigStore<FolderWidgetConfig> =
        entryPoint(context).folderWidgetConfigStore()

    override suspend fun afterSave(context: Context, appWidgetId: Int, config: FolderWidgetConfig) {
        ContentWidgetUpdater.requestUpdate(context, FolderWidgetProvider.PROVIDER_CLASS)
    }

    private fun entryPoint(context: Context): FolderWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FolderWidgetEntryPoint::class.java,
        )

    companion object {
        const val ACTION_PIN_CALLBACK = "dev.ranzlappen.gadget.feature.apps.FOLDER_PIN_CALLBACK"
        const val EXTRA_PENDING_TOKEN = "dev.ranzlappen.gadget.feature.apps.FOLDER_PENDING_TOKEN"
    }
}
