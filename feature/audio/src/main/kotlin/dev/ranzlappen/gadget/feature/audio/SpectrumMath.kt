package dev.ranzlappen.gadget.feature.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure audio maths for the live spectrum (v3 B10): RMS level, a Hann-windowed
 * radix-2 FFT, and log-spaced band folding. No Android types, so it is unit
 * tested on the JVM (`SpectrumMathTest`).
 */
internal object SpectrumMath {

    /** Floor of the band scale; anything quieter draws as an empty bar. */
    const val BAND_FLOOR_DB = -80f

    /**
     * Same dB scale as [DbMeterMetricSource]: dBFS shifted by +60 and clamped
     * to 0..60, so the meter and automation thresholds read alike.
     */
    fun rmsDb(samples: ShortArray, count: Int = samples.size): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        val rms = sqrt(sum / count)
        if (rms <= 0.0) return 0f
        return (20.0 * log10(rms / Short.MAX_VALUE) + 60.0).coerceIn(0.0, 60.0).toFloat()
    }

    /**
     * Magnitudes of the first `n / 2` FFT bins of `samples[0 until n]`
     * (n a power of two), Hann-windowed and normalised so a full-scale sine
     * peaks at ≈ 1.0.
     */
    fun magnitudes(samples: ShortArray, n: Int): FloatArray {
        require(n > 1 && n and (n - 1) == 0) { "n must be a power of two" }
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            val s = if (i < samples.size) samples[i].toDouble() / Short.MAX_VALUE else 0.0
            val hann = 0.5 - 0.5 * cos(2.0 * PI * i / (n - 1))
            re[i] = s * hann
        }
        fft(re, im)
        // Hann window coherent gain is 0.5, and a real sine splits its energy
        // over ±f, so |X| = A·n/4 for amplitude A.
        val scale = 4.0 / n
        return FloatArray(n / 2) { k -> (sqrt(re[k] * re[k] + im[k] * im[k]) * scale).toFloat() }
    }

    /** Frequency (Hz) of the loudest bin, skipping DC. */
    fun peakHz(magnitudes: FloatArray, sampleRate: Int): Float {
        if (magnitudes.size < 2) return 0f
        var best = 1
        for (k in 2 until magnitudes.size) if (magnitudes[k] > magnitudes[best]) best = k
        return best * sampleRate / (magnitudes.size * 2f)
    }

    /**
     * Folds FFT bins into [bandCount] log-spaced bands between [minHz] and
     * [maxHz]; each band takes its loudest bin, mapped from
     * [BAND_FLOOR_DB]..0 dBFS to 0..1.
     */
    fun bands(
        magnitudes: FloatArray,
        sampleRate: Int,
        bandCount: Int,
        minHz: Float = 40f,
        maxHz: Float = sampleRate / 2f,
    ): List<Float> {
        val binHz = sampleRate / (magnitudes.size * 2f)
        val ratio = maxHz / minHz
        return List(bandCount) { b ->
            val lo = minHz * ratio.pow(b.toFloat() / bandCount)
            val hi = minHz * ratio.pow((b + 1f) / bandCount)
            val from = (lo / binHz).toInt().coerceIn(1, magnitudes.lastIndex)
            val to = (hi / binHz).toInt().coerceIn(from, magnitudes.lastIndex)
            var peak = 0f
            for (k in from..to) if (magnitudes[k] > peak) peak = magnitudes[k]
            toUnit(peak)
        }
    }

    fun formatHz(hz: Float): String = when {
        hz >= 1000f -> "%.1f kHz".format(hz / 1000f)
        else -> "${hz.toInt()} Hz"
    }

    private fun toUnit(magnitude: Float): Float {
        if (magnitude <= 0f) return 0f
        val db = 20f * log10(magnitude)
        return ((db - BAND_FLOOR_DB) / -BAND_FLOOR_DB).coerceIn(0f, 1f)
    }

    /** In-place iterative radix-2 Cooley–Tukey FFT. */
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wRe = cos(angle)
            val wIm = sin(angle)
            var start = 0
            while (start < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until len / 2) {
                    val a = start + k
                    val b = a + len / 2
                    val tRe = re[b] * curRe - im[b] * curIm
                    val tIm = re[b] * curIm + im[b] * curRe
                    re[b] = re[a] - tRe
                    im[b] = im[a] - tIm
                    re[a] += tRe
                    im[a] += tIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                start += len
            }
            len = len shl 1
        }
    }
}
