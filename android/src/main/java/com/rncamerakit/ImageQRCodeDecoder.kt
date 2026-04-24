package com.rncamerakit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import qr.QRDecoder

object ImageQRCodeDecoder {

    // Center-crop retry removes a 10% border on each side. Borders often contain
    // background clutter (fingers, page edges, UI chrome) that confuses the
    // detector without contributing QR data.
    private const val CROP_FRACTION = 0.1

    // The underlying QR decoder works best on images around ~600px on the longest
    // side. Larger inputs waste CPU on pixel scanning; much smaller inputs lose
    // the finder-pattern resolution. 600 is the empirical sweet spot.
    private const val RETRY_MAX_DIM = 600

    /**
     * Decodes a QR code from a base64-encoded image.
     * Returns the decoded string, or null if no QR code could be found.
     * Throws IllegalArgumentException if the input is not a valid image.
     */
    fun decode(base64: String): String? {
        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException("Could not decode base64 image data")

        return try {
            decodeFromBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeFromBitmap(bitmap: Bitmap): String? {
        val rgba = bitmapToRgba(bitmap)
        return try {
            QRDecoder.decode(bitmap.width, bitmap.height, rgba)
        } catch (e: Exception) {
            // Retry with cropped-and-scaled image for hard-to-read QR codes
            decodeCroppedAndScaled(bitmap)
        }
    }

    private fun decodeCroppedAndScaled(original: Bitmap): String? {
        val x = (original.width * CROP_FRACTION).toInt()
        val y = (original.height * CROP_FRACTION).toInt()
        val cropW = original.width - 2 * x
        val cropH = original.height - 2 * y

        // Image is too small to retry with a center-crop — treat as "no QR found".
        if (cropW < 1 || cropH < 1) return null

        val cropped = Bitmap.createBitmap(original, x, y, cropW, cropH)
        try {
            val longest = maxOf(cropW, cropH)
            val scale = if (longest > RETRY_MAX_DIM) RETRY_MAX_DIM.toFloat() / longest else 1f

            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(cropped, (cropW * scale).toInt(), (cropH * scale).toInt(), true)
            } else {
                cropped
            }

            try {
                val rgba = bitmapToRgba(scaled)
                return try {
                    QRDecoder.decode(scaled.width, scaled.height, rgba)
                } catch (e: Exception) {
                    null
                }
            } finally {
                if (scaled !== cropped) scaled.recycle()
            }
        } finally {
            cropped.recycle()
        }
    }

    private fun bitmapToRgba(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rgba = ByteArray(width * height * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val offset = i * 4
            rgba[offset] = ((pixel shr 16) and 0xFF).toByte()     // R
            rgba[offset + 1] = ((pixel shr 8) and 0xFF).toByte()  // G
            rgba[offset + 2] = (pixel and 0xFF).toByte()          // B
            rgba[offset + 3] = ((pixel shr 24) and 0xFF).toByte() // A
        }
        return rgba
    }
}
