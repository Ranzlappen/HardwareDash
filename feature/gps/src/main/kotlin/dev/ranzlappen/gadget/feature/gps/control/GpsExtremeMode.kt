package dev.ranzlappen.gadget.feature.gps.control

/**
 * Read-only NMEA-stream tap. Bounded by [durationMillis] (hard 30 s
 * ceiling enforced inside the helper) so even a runaway stream can't
 * starve the rooted shell.
 */
data class NmeaTapConfig(
    val durationMillis: Long,
)
