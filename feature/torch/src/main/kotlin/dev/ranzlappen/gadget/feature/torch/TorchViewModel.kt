package dev.ranzlappen.gadget.feature.torch

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Passthrough ViewModel over [TorchController].
 *
 * Forwards [TorchController.state] as the screen's primary state
 * source and exposes one click handler. `TorchController.toggle()`
 * is synchronous (Camera2's `setTorchMode` is a fast binder call),
 * so no coroutine wrapping is needed.
 */
@HiltViewModel
class TorchViewModel @Inject constructor(
    private val controller: TorchController,
) : ViewModel() {

    val state: StateFlow<TorchState> = controller.state

    fun onToggleClick() {
        controller.toggle()
    }
}
