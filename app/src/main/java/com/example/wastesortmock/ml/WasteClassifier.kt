package com.example.wastesortmock.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * Классификатор отходов на основе TensorFlow Lite модели
 */
class WasteClassifier(context: Context) {
    
    private val TAG = "WasteClassifier"
    private var interpreter: Interpreter? = null
    
    // Размер входного изображения для модели ResNet101v2 (использовался 224x224 в обучении)
    private val INPUT_SIZE = 224
    private val PIXEL_SIZE = 3 // RGB
    private val IMAGE_MEAN = 127.5f // preprocess_input для ResNetV2: (x / 127.5) - 1
    private val IMAGE_STD = 127.5f
    
    // Классы обученной модели (garbage-dataset): cardboard, glass, metal, paper, plastic, trash
    private val MODEL_CLASSES = listOf("cardboard", "glass", "metal", "paper", "plastic", "trash")
    
    // Классы приложения (металл, бумага/картон, стекло, пластик, другое)
    private val OUTPUT_CLASSES = listOf("metal", "paper", "glass", "plastic", "other")
    
    init {
        try {
            interpreter = Interpreter(loadModelFile(context, "waste_classifier.tflite"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Загружает модель из assets
     */
    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Классифицирует изображение и возвращает категорию отходов
     * @param bitmap изображение для классификации
     * @return ID категории ("metal", "paper", "glass", "plastic", "other")
     */
    fun classify(bitmap: Bitmap): String? {
        val interpreter = interpreter ?: return "other"
        
        try {
            // Подготовка входных данных
            val inputBuffer = preprocessImage(bitmap)
            inputBuffer.rewind()
            
            // Узнаём реальный размер выхода модели
            val outputShape = interpreter.getOutputTensor(0).shape() // [1, numClasses]
            val numClasses = outputShape.last()
            if (numClasses <= 0) return "other"

            // Подготовка выходных данных (1 x numClasses)
            val outputArray = Array(1) { FloatArray(numClasses) }
            
            // Запуск инференса
            interpreter.run(inputBuffer, outputArray)
            
            val logits = outputArray[0]
            val probabilities = softmax(logits)
            
            // Находим класс с максимальной вероятностью среди фактических классов модели
            var maxIndex = 0
            var maxProb = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            // Маппим класс модели в класс приложения
            val modelClass = MODEL_CLASSES.getOrNull(maxIndex)
            
            // Логируем top-3 для диагностики
            logTopK(probabilities, 3)

            return mapModelClassToWaste(modelClass)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return "other"
        }
    }

    /**
     * Подробный результат с вероятностями
     */
    fun classifyWithScores(bitmap: Bitmap): ClassificationResult {
        val interpreter = interpreter ?: return ClassificationResult("other", "other", 0f, emptyList())
        return try {
            val inputBuffer = preprocessImage(bitmap)
            inputBuffer.rewind()

            val outputShape = interpreter.getOutputTensor(0).shape() // [1, numClasses]
            val numClasses = outputShape.last()
            if (numClasses <= 0) return ClassificationResult("other", "other", 0f, emptyList())

            val outputArray = Array(1) { FloatArray(numClasses) }
            interpreter.run(inputBuffer, outputArray)

            val logits = outputArray[0]
            val probabilities = softmax(logits)

            // top-1
            var maxIndex = 0
            var maxProb = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }
            val modelClass = MODEL_CLASSES.getOrNull(maxIndex) ?: "unknown"
            val appClass = mapModelClassToWaste(modelClass)

            val probs = probabilities.mapIndexed { idx, p ->
                (MODEL_CLASSES.getOrNull(idx) ?: "cls$idx") to p
            }

            ClassificationResult(appClass, modelClass, maxProb, probs)
        } catch (e: Exception) {
            e.printStackTrace()
            ClassificationResult("other", "error", 0f, emptyList())
        }
    }
    
    /**
     * Предобработка изображения для модели
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Изменяем размер изображения
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // Создаём ByteBuffer для входных данных
        val inputBuffer = ByteBuffer.allocateDirect(
            4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE
        ).apply {
            order(ByteOrder.nativeOrder())
        }
        
        // Конвертируем изображение в массив пикселей
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
        
        // Заполняем буфер данными пикселей (нормализованными)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                
                // Извлекаем RGB и нормализуем
                inputBuffer.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        
        return inputBuffer
    }
    
    private fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return logits
        val maxLogit = logits.maxOrNull() ?: 0f
        var sum = 0.0
        val exps = DoubleArray(logits.size)
        for (i in logits.indices) {
            val v = kotlin.math.exp((logits[i] - maxLogit).toDouble())
            exps[i] = v
            sum += v
        }
        return FloatArray(logits.size) { i ->
            (exps[i] / sum).toFloat()
        }
    }
    
    private fun logTopK(probabilities: FloatArray, k: Int) {
        val pairs = probabilities.mapIndexed { index, prob ->
            val cls = MODEL_CLASSES.getOrNull(index) ?: "cls$index"
            cls to prob
        }.sortedByDescending { it.second }
        val top = pairs.take(k).joinToString { "${it.first}: ${"%.3f".format(it.second)}" }
        Log.d(TAG, "Top-$k: $top")
    }
    
    private fun mapModelClassToWaste(modelClass: String?): String {
        return when (modelClass) {
            "cardboard" -> "paper"    // картон → бумага/картон
            "paper" -> "paper"
            "glass" -> "glass"
            "metal" -> "metal"
            "plastic" -> "plastic"
            "trash" -> "other"        // прочий мусор → другое
            else -> "other"
        }
    }
    
    /**
     * Освобождает ресурсы
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

data class ClassificationResult(
    val appLabel: String,
    val modelLabel: String,
    val probability: Float,
    val probabilities: List<Pair<String, Float>>
)

