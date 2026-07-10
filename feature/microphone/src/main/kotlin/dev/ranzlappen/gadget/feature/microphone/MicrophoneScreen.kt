package dev.ranzlappen.gadget.feature.microphone

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    )

    SnackbarHost(hostState = snackbarHostState)
}
