package com.rncamerakit

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import qr.QRDecoder
import qr.QRDecodingException

class QRCodeAnalyzer(
    private val onQRCodeDetected: (decodedValue: String) -> Unit,
    private val scanThrottleDelay: Long = 0L
) : ImageAnalysis.Analyzer {
    // Time in milliseconds of the last time we dispatched detected QR code
    private var lastQRDetectedTime: Long = 0L

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        try {
            Log.d("LLL", "11111")
            val rgbData = imageProxyToRgb(image)
            val decoded = QRDecoder.decode(image.width, image.height, rgbData)

            // Throttle callback invocations based on scanThrottleDelay (ms)
            val now = System.currentTimeMillis()
            if (scanThrottleDelay > 0 && (now - lastQRDetectedTime) < scanThrottleDelay) {
                return
            }

            lastQRDetectedTime = now
            onQRCodeDetected(decoded)
        } catch (e: QRDecodingException) {
            Log.d("LLL", "QR decode error: ${e.message}", e)
            // No QR code found or decoding error - this is expected for most frames
        } finally {
            image.close()
        }
    }

    private fun imageProxyToRgb(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val yBytes = ByteArray(ySize)
        val uBytes = ByteArray(uSize)
        val vBytes = ByteArray(vSize)

        yBuffer.get(yBytes)
        uBuffer.get(uBytes)
        vBuffer.get(vBytes)

        val width = image.width
        val height = image.height
        val rgbBytes = ByteArray(width * height * 3)

        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        var rgbIndex = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                val yIndex = row * yRowStride + col
                val uvRow = row / 2
                val uvCol = col / 2
                val uvIndex = uvRow * uvRowStride + uvCol * uvPixelStride

                val y = yBytes[yIndex].toInt() and 0xFF
                val u = (if (uvIndex < uBytes.size) uBytes[uvIndex].toInt() else 128) and 0xFF
                val v = (if (uvIndex < vBytes.size) vBytes[uvIndex].toInt() else 128) and 0xFF

                // YUV to RGB conversion
                val yVal = y - 16
                val uVal = u - 128
                val vVal = v - 128

                var r = (1.164 * yVal + 1.596 * vVal).toInt()
                var g = (1.164 * yVal - 0.392 * uVal - 0.813 * vVal).toInt()
                var b = (1.164 * yVal + 2.017 * uVal).toInt()

                // Clamp values to 0-255
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                rgbBytes[rgbIndex++] = r.toByte()
                rgbBytes[rgbIndex++] = g.toByte()
                rgbBytes[rgbIndex++] = b.toByte()
            }
        }

        return rgbBytes
    }
}
