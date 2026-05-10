package com.gadget.gps.spoof

/**
 * Live state of the spoof emitter. The controller exposes a
 * `StateFlow<SpoofState>`; UI + the foreground service notification both
 * subscribe.
 */
sealed interface SpoofState {

    data object Idle : SpoofState

    data class Running(
        val activeModes: Set<SpoofMode>,
        val sourceLabel: String,
        val currentLat: Double,
        val currentLon: Double,
        val startedAtMs: Long,
        val sessionLimitMs: Long,
    ) : SpoofState

    data class Error(val reason: String, val retriable: Boolean) : SpoofState
}
