package dev.ranzlappen.gadget.feature.torch.strobe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide source of truth for whether [StrobeService] is currently
 * strobing.
 *
 * Replaces the old `@Volatile var StrobeService.isRunning` companion flag.
 * That flag was a heuristic the screen had to *poll* (250 ms) and could go
 * stale if the OS tore the service down without an `onDestroy`. A
 * `@Singleton` `StateFlow` is strictly better:
 * - **No polling** — the screen / widgets collect (or read `.value`) and
 *   recompose only on an actual transition.
 * - **No stale-after-kill** — the runtime lives in the same process as the
 *   service; if the process dies, this singleton is re-created at `false`
 *   (the service died with it), so a cold read is always correct. The
 *   `@Volatile` flag's "killed without notice → stuck true" failure mode is
 *   structurally impossible here.
 *
 * Writers: [StrobeService] (`setRunning(true)` when the loop starts,
 * `setRunning(false)` in `stopStrobing` / `onDestroy` / `onTimeout`).
 * Readers: `TorchViewModel` (folds [running] into the screen state) and
 * `StrobeWidgetProvider` (reads `running.value` to branch start-vs-stop on a
 * tap; reached via its Hilt `@EntryPoint`).
 */
@Singleton
class StrobeRuntime @Inject constructor() {

    private val _running = MutableStateFlow(false)

    /** Hot, conflated signal: `true` while a strobe loop is active. */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Called by [StrobeService] from its lifecycle to publish the live
     *  strobing state. Idempotent. */
    fun setRunning(running: Boolean) {
        _running.value = running
    }
}
