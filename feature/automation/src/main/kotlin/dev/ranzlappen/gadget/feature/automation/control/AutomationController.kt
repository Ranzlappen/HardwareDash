package dev.ranzlappen.gadget.feature.automation.control


/**
 * Rooted-only Automation capability surface. Standard flavor returns
 * [AutomationControllerResult.Unsupported] for every method.
 *
 * All methods route through `RootSafetyGate.check(...)` and enforce
 * hard allow / deny lists inside their helpers.
 */
interface AutomationController {

    /**
     * Fires a privileged intent via `am`. Per-call action deny-list
     * rejects REBOOT / SHUTDOWN / FACTORY_RESET / MASTER_CLEAR plus
     * system-ui internals.
     */
    suspend fun firePrivilegedIntent(config: PrivilegedIntentConfig): AutomationControllerResult

    /**
     * Writes to `Settings.System` / `Secure` / `Global` via `settings put`.
     * Hard allow-list of safe keys enforced inside the helper. Snapshot
     * + restore via `SysfsMutationLog` under the synthesized
     * `settings://<scope>/<key>` pseudo-path.
     */
    suspend fun overrideSystemSetting(config: SystemSettingsOverrideConfig): AutomationControllerResult

    /**
     * Read-only `dumpsys` snapshot for a fixed allow-list of sections.
     * 8 KB tail-cap per section.
     */
    suspend fun dumpsysSnapshot(): AutomationControllerResult

    /** Reverts every Automation-surface mutation. */
    suspend fun resetAllAutomationMutations(): AutomationControllerResult

    /**
     * Auto-revert path called on `LinkScreen` dispose. Filters by
     * `settings://` prefix only — leaves any other automation state
     * (already returned-from intent, dumpsys output) untouched since
     * those carry no mutation log entries.
     */
    suspend fun revertOnScreenExit(): AutomationControllerResult
}
