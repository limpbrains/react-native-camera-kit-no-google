package com.rncamerakit

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
            val grayscaleData = extractYPlane(image)
            val decoded = QRDecoder.decode(image.width, image.height, grayscaleData)

            // Throttle callback invocations based on scanThrottleDelay (ms)
            val now = System.currentTimeMillis()
            if (scanThrottleDelay > 0 && (now - lastQRDetectedTime) < scanThrottleDelay) {
                return
            }

            lastQRDetectedTime = now
            onQRCodeDetected(decoded)
        } catch (e: QRDecodingException) {
            // No QR code found or decoding error - this is expected for most frames
        } finally {
            image.close()
        }
    }

    /**
     * Extracts the Y (luminance) plane from a YUV_420_888 ImageProxy.
     * The Y plane is already grayscale data (1 byte per pixel).
     * Handles row stride padding when rowStride > width.
     */
    private fun extractYPlane(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val width = image.width
        val height = image.height
        val yBytes = ByteArray(width * height)

        if (rowStride == width) {
            // Fast path: contiguous data, no padding
            yBuffer.rewind()
            yBuffer.get(yBytes, 0, width * height)
        } else {
            // Slow path: handle row stride padding
            yBuffer.rewind()
            for (row in 0 until height) {
                yBuffer.position(row * rowStride)
                yBuffer.get(yBytes, row * width, width)
            }
        }

        return yBytes
    }
}
