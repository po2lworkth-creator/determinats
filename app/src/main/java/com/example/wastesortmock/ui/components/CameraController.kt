package com.example.wastesortmock.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Контроллер для управления камерой и захвата изображений
 */
class CameraController(
    private val context: Context,
    private val onImageCaptured: (Bitmap) -> Unit,
    private val onError: (Exception) -> Unit
) {
    var imageCapture: ImageCapture? = null
        set(value) {
            field = value
        }
    
    fun takePicture() {
        val imageCapture = imageCapture ?: run {
            onError(Exception("Камера не готова"))
            return
        }
        
        val outputFile = File(context.getExternalFilesDir(null), "temp_capture.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val file = output.savedUri?.let { 
                            File(it.path ?: return@let null)
                        } ?: outputFile
                        
                        // Читаем Bitmap из файла
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        bitmap?.let {
                            onImageCaptured(it)
                        } ?: onError(Exception("Не удалось создать Bitmap"))
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}

