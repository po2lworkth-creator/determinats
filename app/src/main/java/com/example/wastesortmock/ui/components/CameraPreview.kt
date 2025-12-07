package com.example.wastesortmock.ui.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = false,
    onCameraReady: (Boolean) -> Unit = {},
    onImageCaptured: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Инициализация CameraProvider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    // Запуск камеры
    LaunchedEffect(cameraProvider, useFrontCamera) {
        val provider = cameraProvider ?: return@LaunchedEffect
        
        try {
            // Отключаем предыдущий preview
            provider.unbindAll()
            
            // Создаём новый Preview
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(null) // Будет установлен в AndroidView
                }
            
            // Создаём ImageCapture для съёмки фото
            val imageCaptureUseCase = ImageCapture.Builder()
                .setTargetRotation(context.display?.rotation ?: 0)
                .build()
            
            preview = previewUseCase
            imageCapture = imageCaptureUseCase
            
            // Выбираем камеру
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
            // Привязываем use cases к lifecycle
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase
            )
            
            onCameraReady(true)
        } catch (e: Exception) {
            e.printStackTrace()
            onCameraReady(false)
        }
    }

    // Очистка при удалении
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    // PreviewView для отображения камеры
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            preview?.setSurfaceProvider(previewView.surfaceProvider)
            
            // Сохраняем ссылку на imageCapture для использования в CameraScreen
            // Функция для захвата изображения будет вызвана извне
        }
    )
    
    // Экспортируем функцию захвата через обратный вызов
    LaunchedEffect(imageCapture) {
        if (imageCapture != null) {
            // Создаём функцию захвата, которая будет использоваться в CameraScreen
            // Но лучше передать imageCapture через параметр
        }
    }
}

/**
 * Вспомогательная функция для захвата изображения
 */
fun captureImage(
    imageCapture: ImageCapture?,
    context: android.content.Context,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    val imageCapture = imageCapture ?: run {
        onError(Exception("ImageCapture не инициализирован"))
        return
    }
    
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
        File(context.getExternalFilesDir(null), "temp_capture.jpg")
    ).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val file = output.savedUri?.let { 
                        java.io.File(it.path ?: return@let null)
                    } ?: File(context.getExternalFilesDir(null), "temp_capture.jpg")
                    
                    // Читаем Bitmap из файла
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    bitmap?.let {
                        onSuccess(it)
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
