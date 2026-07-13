package dev.ranzlappen.gadget.feature.microphone

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.ui.module.RootConfirmActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult

/**
 * Microphone screen — Hilt-wrapped stateful entry point. Thin shell over the
 * stateless [MicrophoneScreenContent]: injects [MicrophoneViewModel] and owns
 * the snackbar host that surfaces each [MicrophoneControllerResult]. Mirror
 * of `VibrationScreen` / `TorchScreen`.
 */
@Composable
fun MicrophoneScreen(
    modifier: Modifier = Modifier,
    viewModel: MicrophoneViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val resultOk = stringResource(R.string.microphone_result_ok)
    val resultUnsupported = stringResource(R.string.microphone_result_unsupported)
    val resultOptedOut = stringResource(R.string.microphone_result_opted_out)
    val resultRateLimited = stringResource(R.string.microphone_result_rate_limited)
    val resultErrorFmt = stringResource(R.string.microphone_result_error)

    LaunchedEffect(Unit) {
        viewModel.resultEvents.collect { result ->
            val message = when (result) {
                MicrophoneControllerResult.Ok -> resultOk
                MicrophoneControllerResult.Unsupported -> resultUnsupported
                MicrophoneControllerResult.OptedOut -> resultOptedOut
                is MicrophoneControllerResult.RateLimited -> resultRateLimited
                is MicrophoneControllerResult.HardwareError -> resultErrorFmt.format(result.message)
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    MicrophoneScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.microphone_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.microphone_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootConfirmActionRow(
                    label = stringResource(R.string.microphone_root_disable_effects_label),
                    description = stringResource(R.string.microphone_root_disable_effects_detail),
                    runLabel = stringResource(R.string.microphone_root_run),
                    confirmTitle = stringResource(R.string.microphone_root_disable_effects_confirm_title),
                    confirmMessage = stringResource(R.string.microphone_root_disable_effects_confirm_message),
                    confirmLabel = stringResource(R.string.microphone_root_disable_effects_confirm_label),
                    cancelLabel = stringResource(R.string.microphone_root_cancel),
                    onConfirm = viewModel::onDisableEffects,
                    enabled = !rootTools.disableEffects.running,
                    statusMessage = rootTools.disableEffects.message,
                    statusKind = rootTools.disableEffects.statusKind,
                )
            }
        },
    )

    SnackbarHost(hostState = snackbarHostState)
}
