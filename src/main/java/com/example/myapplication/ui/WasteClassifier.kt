package com.example.myapplication.ui

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import android.util.Log


/**
 * Single ONNX classifier entrypoint. Expects best.onnx in assets/.
 * Preprocessing: resize 224x224, RGB -> CHW, mean/std normalization, float32, shape [1,3,224,224].
 */
class WasteClassifier(context: Context) {

    // Lazy, thread-safe init; if модель не загрузилась, bundle == null
    private val bundle: SessionBundle? = ensureSession(context)

    data class ClassificationResult(val label: String, val confidence: Float)

    fun classify(image: Bitmap): ClassificationResult {
        val bundle = bundle ?: return ClassificationResult("other", 0f) // безопасный фолбэк, чтобы не падать

        val width = 224
        val height = 224
        val resized = if (image.width == width && image.height == height) {
            image
        } else {
            Bitmap.createScaledBitmap(image, width, height, true)

        }


        val size = width * height
        val floats = FloatArray(3 * size)
        var rOff = 0
        var gOff = size
        var bOff = size * 2
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                floats[rOff++] = (r - MEAN[0]) / STD[0]
                floats[gOff++] = (g - MEAN[1]) / STD[1]
                floats[bOff++] = (b - MEAN[2]) / STD[2]
            }
        }

        val inputBuffer = FloatBuffer.wrap(floats)
        val (bestIdx, bestScore) = runCatching {
            OnnxTensor.createTensor(bundle.env, inputBuffer, longArrayOf(1, 3, width.toLong(), height.toLong()))
        }.mapCatching { tensor ->
            bundle.session.run(mapOf(bundle.inputName to tensor)).use { result ->
                val scores = (result[0].value as Array<FloatArray>)[0]
                val probs = softmax(scores)
                val idx = probs.indices.maxByOrNull { probs[it] } ?: LABELS.lastIndex
                idx to probs.getOrElse(idx) { 0f }
            }
        }.getOrElse { LABELS.lastIndex to 0f }


        Log.d("MY", "test")


        if (resized !== image) resized.recycle()

        return ClassificationResult(label = LABELS[bestIdx], confidence = bestScore)
    }

    private data class SessionBundle(
        val env: OrtEnvironment,
        val session: OrtSession,
        val inputName: String
    )

    companion object {
        // Order should match training order of the classifier model
        private val LABELS = listOf("plastic", "paper","metal","glass")
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        @Volatile
        private var shared: SessionBundle? = null

        private fun ensureSession(context: Context): SessionBundle? {
            shared?.let { return it }
            return synchronized(this) {
                shared?.let { return it }
                runCatching {
                    val env = OrtEnvironment.getEnvironment()
                    val bytes = context.assets.open("best.onnx").readBytes()
                    val session = env.createSession(bytes)
                    val inputName = session.inputNames.first()
                    SessionBundle(env, session, inputName).also { shared = it }
                }.getOrNull()
            }
        }

        private fun softmax(logits: FloatArray): FloatArray {
            if (logits.isEmpty()) return logits
            val max = logits.maxOrNull() ?: 0f
            var sum = 0.0
            val exp = DoubleArray(logits.size)
            for (i in logits.indices) {
                exp[i] = kotlin.math.exp((logits[i] - max).toDouble())
                sum += exp[i]
            }
            val out = FloatArray(logits.size)
            if (sum == 0.0) return out
            for (i in logits.indices) {
                out[i] = (exp[i] / sum).toFloat()
            }
            return out
        }
    }
}


