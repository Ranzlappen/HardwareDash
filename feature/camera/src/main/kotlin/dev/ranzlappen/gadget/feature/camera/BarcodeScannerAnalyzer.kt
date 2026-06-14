package dev.ranzlappen.gadget.feature.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.UUID

class BarcodeScannerAnalyzer(
    private val onResult: (BarcodeResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build(),
    )

    private var lastRawValue: String? = null
    private var lastDetectedAt: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val now = System.currentTimeMillis()
                barcodes.firstOrNull()?.rawValue?.takeIf { it.isNotBlank() }?.let { raw ->
                    if (raw != lastRawValue || now - lastDetectedAt > DEDUP_WINDOW_MS) {
                        lastRawValue = raw
                        lastDetectedAt = now
                        val barcode = barcodes.first()
                        onResult(
                            BarcodeResult(
                                id = UUID.randomUUID().toString(),
                                rawValue = raw,
                                format = formatName(barcode.format),
                                displayType = displayType(barcode),
                                timestamp = now,
                            ),
                        )
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "UNKNOWN"
    }

    private fun displayType(barcode: Barcode): String =
        when (barcode.valueType) {
            Barcode.TYPE_URL -> "URL"
            Barcode.TYPE_WIFI -> "WiFi"
            Barcode.TYPE_EMAIL -> "Email"
            Barcode.TYPE_PHONE -> "Phone"
            Barcode.TYPE_SMS -> "SMS"
            Barcode.TYPE_GEO -> "Location"
            Barcode.TYPE_CONTACT_INFO -> "Contact"
            Barcode.TYPE_CALENDAR_EVENT -> "Calendar"
            Barcode.TYPE_DRIVER_LICENSE -> "ID"
            Barcode.TYPE_ISBN -> "ISBN"
            Barcode.TYPE_PRODUCT -> "Product"
            else -> "Text"
        }

    companion object {
        private const val DEDUP_WINDOW_MS = 2_000L
    }
}
