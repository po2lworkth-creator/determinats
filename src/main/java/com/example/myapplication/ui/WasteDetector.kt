package com.example.myapplication.ui

import android.graphics.Bitmap
import android.graphics.RectF

data class DetectionResult(
    val box: RectF?,
    val confidence: Float
)

interface WasteDetector {
    suspend fun detect(bitmap: Bitmap): DetectionResult
}

/**
 * Fake detector: always returns центр экрана с уверенность 0.8.
 * Замените на реальный детектор (MobileNet-SSD / YOLO-Nano / MediaPipe) при появлении модели.
 */
class FakeWasteDetector : WasteDetector {
    override suspend fun detect(bitmap: Bitmap): DetectionResult {
        // box в относительных координатах [0..1]
        val box = RectF(0.25f, 0.25f, 0.75f, 0.75f)
        return DetectionResult(box = box, confidence = 0.8f)
    }
}

