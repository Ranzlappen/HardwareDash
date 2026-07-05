package dev.ranzlappen.gadget.root.ui

import dev.ranzlappen.gadget.core.root.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.root.launch.FatalReason

/**
 * Hard-fail screen rendered by [dev.ranzlappen.gadget.MainActivity] when
 * [dev.ranzlappen.gadget.core.root.launch.LaunchGate] denies launch on the rooted flavor. The
 * standard flavor's no-op gate never returns a denial, so this screen never
 * appears in the standard APK at runtime.
 *
 * The CTA wiring is intentionally minimal — Batch 3 expands this with a
 * "download companion module" path; for now, the UI just explains what's
 * wrong and offers a way to exit or open instructions.
 */
@Composable
fun FatalLaunchScreen(
    reason: FatalReason,
    onExit: () -> Unit,
    onOpenInstructions: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Root access required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = describe(reason),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            if (onOpenInstructions != null) {
                Button(onClick = onOpenInstructions) {
                    Text("Open install instructions")
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(onClick = onExit) {
                Text("Close app")
            }
        }
    }
}

private fun describe(reason: FatalReason): String = when (reason) {
    FatalReason.NoRootDetected ->
        "This is the rooted build of Gadget, but no supported root manager " +
            "(Magisk, KernelSU, or APatch) was detected on this device. " +
            "Install the standard build instead, or set up a root manager " +
            "and try again."

    is FatalReason.RootRequestDenied ->
        "${reason.providerName} is installed but denied root access to Gadget. " +
            "Open ${reason.providerName} and grant Gadget permission, then relaunch."

    is FatalReason.IncompatibleProvider ->
        "${reason.providerName} is installed but is not compatible with this " +
            "build. Update your root manager or use the standard build."
}
