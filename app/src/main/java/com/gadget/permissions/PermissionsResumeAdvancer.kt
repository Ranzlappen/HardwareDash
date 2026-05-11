package com.gadget.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Lifecycle helper for the "Grant all" walkthrough: after launching a
 * Settings activity, set the returned state to `true`. When the host
 * Composable receives `ON_RESUME` (i.e. the user came back from
 * Settings), [onResume] is invoked once and the flag is cleared.
 *
 * Without this, the iteration through pending special-permission steps
 * never advances past the first one — there is no other way for the
 * Composable to learn that the user finished interacting with the
 * Settings activity.
 */
@Composable
fun rememberPermissionsResumeAdvancer(
    onResume: () -> Unit,
): MutableState<Boolean> {
    val awaitingResume = rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingResume.value) {
                awaitingResume.value = false
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return awaitingResume
}
