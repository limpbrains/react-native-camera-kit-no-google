package com.rncamerakit

data class FacePayload(
    val id: Int,
    val yaw: Double,
    val pitch: Double,
    val roll: Double,
    val boundsX: Double,
    val boundsY: Double,
    val boundsWidth: Double,
    val boundsHeight: Double,
)

const val DEFAULT_FACE_DETECTION_THROTTLE_MS = 100L
