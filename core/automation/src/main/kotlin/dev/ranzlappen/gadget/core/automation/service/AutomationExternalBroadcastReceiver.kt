package dev.ranzlappen.gadget.core.automation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Landing point for the app-wide **external-automation** broadcast — the
 * Tasker/MacroDroid-style hook for [Trigger.ExternalBroadcast]. Another app
 * runs
 * `am broadcast -a dev.ranzlappen.gadget.feature.automation.EXTERNAL_TRIGGER
 *  --es tag "<tag>"` and every enabled rule whose
 * [Trigger.ExternalBroadcast.tag] equals the extra fires.
 *
 * **Security model.** The receiver listens for **one fixed, app-namespaced
 * action** and matches the `tag` extra against the user's own rules inside the
 * app — there is no dynamic action string a sender could name. It is
 * `exported="true"` (external automation apps must be able to reach it), but a
 * stray broadcast can at worst fire a rule the user already authored, whose
 * actions are themselves gated; it cannot inject arbitrary behaviour. A blank
 * or absent tag is ignored.
 *
 * **Not** `@AndroidEntryPoint` — like [AutomationSystemEventReceiver], the
 * whole `:core:automation` module is Kotlin-only; the Hilt graph is reached
 * via [EntryPointAccessors] and firing goes through [RuleFireExecutor].
 */
class AutomationExternalBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExternalBroadcastEntryPoint {
        fun ruleRepository(): RuleRepository
        fun fireExecutor(): RuleFireExecutor
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXTERNAL_TRIGGER) return
        val tag = intent.getStringExtra(EXTRA_TAG)?.takeIf { it.isNotBlank() } ?: return

        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ExternalBroadcastEntryPoint::class.java,
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                entry.ruleRepository().observeRules().first()
                    .filter { it.enabled && it.trigger == Trigger.ExternalBroadcast(tag) }
                    .forEach { entry.fireExecutor().fire(it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** The single fixed, app-namespaced action external apps broadcast. */
        const val ACTION_EXTERNAL_TRIGGER =
            "dev.ranzlappen.gadget.feature.automation.EXTERNAL_TRIGGER"

        /** String extra: the user tag matched against `Trigger.ExternalBroadcast`. */
        const val EXTRA_TAG = "tag"
    }
}
