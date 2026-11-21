package dev.ranzlappen.gadget.feature.camera

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the camera screen is doing with the preview (v3 B9). */
enum class CameraMode { Photo, Scan }

/** Which way a lens faces — CameraX's `CameraSelector.LENS_FACING_*`, decoupled for tests. */
enum class LensFacing { Back, Front, External }

/** Role of a lens among the ones facing the same way, from its focal length. */
enum class LensRole { UltraWide, Main, Tele, Front, External }

/** One selectable camera, as CameraX exposes it (logical or physical). */
@Immutable
data class LensOption(
    val cameraId: String,
    val facing: LensFacing,
    /** Shortest focal length reported by the lens, mm; null if unknown. */
    val focalLengthMm: Float?,
    val role: LensRole,
)

/**
 * Assign roles to the cameras CameraX lists. Front / external cameras are
 * just that. Back cameras: with one, it's Main; with several, the one whose
 * focal length is closest to the median is Main, shorter ones (< 0.75×)
 * Ultra-wide, longer ones (> 1.5×) Tele. Many phones expose a single
 * logical back camera — then ultra-wide / tele are reached through zoom
 * ([zoomPresets]) instead.
 */
internal fun assignLensRoles(cameras: List<Triple<String, LensFacing, Float?>>): List<LensOption> {
    val backFocals = cameras.filter { it.second == LensFacing.Back }.mapNotNull { it.third }.sorted()
    val main = backFocals.getOrNull(backFocals.size / 2)
    return cameras.map { (id, facing, focal) ->
        val role = when (facing) {
            LensFacing.Front -> LensRole.Front
            LensFacing.External -> LensRole.External
            LensFacing.Back -> when {
                main == null || focal == null || backFocals.size < 2 -> LensRole.Main
                focal < main * ULTRA_WIDE_RATIO -> LensRole.UltraWide
                focal > main * TELE_RATIO -> LensRole.Tele
                else -> LensRole.Main
            }
        }
        LensOption(id, facing, focal, role)
    }.sortedWith(compareBy({ it.facing.ordinal }, { it.focalLengthMm ?: Float.MAX_VALUE }))
}

private const val ULTRA_WIDE_RATIO = 0.75f
private const val TELE_RATIO = 1.5f

/**
 * Quick zoom buttons inside the camera's supported range: the minimum when
 * it is below 1× (a logical camera's ultra-wide), then 1×, 2×, 5× and 10×
 * where supported.
 */
internal fun zoomPresets(minRatio: Float, maxRatio: Float): List<Float> {
    if (maxRatio <= 0f || minRatio <= 0f || maxRatio < minRatio) return emptyList()
    val presets = mutableListOf<Float>()
    if (minRatio < 1f - ZOOM_EPSILON) presets += (minRatio * 10f).roundToInt() / 10f
    listOf(1f, 2f, 5f, 10f).filterTo(presets) { it >= minRatio - ZOOM_EPSILON && it <= maxRatio + ZOOM_EPSILON }
    return presets.distinct()
}

private const val ZOOM_EPSILON = 0.01f

/** "0.6×", "1×", "2.5×". */
internal fun formatZoom(ratio: Float): String {
    val rounded = (ratio * 10f).roundToInt() / 10f
    return if (abs(rounded - rounded.roundToInt()) < ZOOM_EPSILON) "${rounded.roundToInt()}×" else "$rounded×"
}

/** Exposure compensation as EV, e.g. "+0.7 EV", "0 EV", "−1.3 EV". */
internal fun formatEv(index: Int, stepEv: Float): String {
    val ev = (index * stepEv * 10f).roundToInt() / 10f
    return when {
        ev > 0f -> "+$ev EV"
        ev < 0f -> "−${-ev} EV"
        else -> "0 EV"
    }
}

/** MediaStore display name for a capture: `Gadget_20261121_024015`. */
internal fun photoFileName(timeMillis: Long): String =
    "Gadget_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(timeMillis))

/** Where captures go (`MediaStore.MediaColumns.RELATIVE_PATH`). */
internal const val PHOTO_RELATIVE_PATH = "Pictures/Gadget"
