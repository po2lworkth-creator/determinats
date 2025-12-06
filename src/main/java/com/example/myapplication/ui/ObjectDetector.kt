package com.example.myapplication.ui

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * YOLOv8 detector (ONNX). Expects yolov8n.onnx in assets/.
 * Returns the best bounding box in normalized coordinates [0,1].
 * Fallback: центр кадра, если модель недоступна.
 */
class ObjectDetector(context: Context) {

    private val bundle: SessionBundle? = ensureSession(context)

    data class Detection(val box: RectF, val score: Float)

    fun detect(bitmap: Bitmap): Detection? {
        val bundle = bundle ?: return fallback()
        val size = 640
        val resized = if (bitmap.width == size && bitmap.height == size) bitmap
        else Bitmap.createScaledBitmap(bitmap, size, size, true)

        val floats = FloatArray(3 * size * size)
        var rOff = 0
        var gOff = size * size
        var bOff = 2 * size * size
        for (y in 0 until size) {
            for (x in 0 until size) {
                val p = resized.getPixel(x, y)
                val r = ((p shr 16) and 0xFF) / 255f
                val g = ((p shr 8) and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                floats[rOff++] = r
                floats[gOff++] = g
                floats[bOff++] = b
            }
        }

        val input = OnnxTensor.createTensor(bundle.env, FloatBuffer.wrap(floats), longArrayOf(1, 3, size.toLong(), size.toLong()))
        val det = bundle.session.run(mapOf(bundle.inputName to input)).use { result ->
            val output = result[0].value as Array<Array<FloatArray>>
            val predictions = output[0] // shape: N x 84 (x,y,w,h,obj + 80 classes)
            parseBest(predictions, size)
        }
        if (resized !== bitmap) resized.recycle()
        return det ?: fallback()
    }

    private fun parseBest(pred: Array<FloatArray>, imgSize: Int): Detection? {
        var bestScore = 0f
        var bestBox: RectF? = null
        for (row in pred) {
            if (row.size < 6) continue
            val x = row[0]; val y = row[1]; val w = row[2]; val h = row[3]
            val obj = row[4]
            var clsScore = 0f
            for (i in 5 until row.size) clsScore = max(clsScore, row[i])
            val score = obj * clsScore
            if (score > bestScore) {
                val normFactor = if (maxOf(w, h, x, y) > 1f) imgSize.toFloat() else 1f
                val nx = x / normFactor
                val ny = y / normFactor
                val nw = w / normFactor
                val nh = h / normFactor
                val left = (nx - nw / 2f).coerceIn(0f, 1f)
                val top = (ny - nh / 2f).coerceIn(0f, 1f)
                val right = (nx + nw / 2f).coerceIn(0f, 1f)
                val bottom = (ny + nh / 2f).coerceIn(0f, 1f)
                bestBox = RectF(left, top, right, bottom)
                bestScore = score
            }
        }
        return bestBox?.let { Detection(it, bestScore) }
    }

    private fun fallback(): Detection {
        return Detection(RectF(0.2f, 0.2f, 0.8f, 0.8f), 0f)
    }

    private data class SessionBundle(
        val env: OrtEnvironment,
        val session: OrtSession,
        val inputName: String
    )

    companion object {
        @Volatile
        private var shared: SessionBundle? = null

        private fun ensureSession(context: Context): SessionBundle? {
            shared?.let { return it }
            return synchronized(this) {
                shared?.let { return it }
                runCatching {
                    val env = OrtEnvironment.getEnvironment()
                    val bytes = context.assets.open("yolov8n.onnx").readBytes()
                    val session = env.createSession(bytes)
                    val inputName = session.inputNames.first()
                    SessionBundle(env, session, inputName).also { shared = it }
                }.getOrNull()
            }
        }
    }
}

