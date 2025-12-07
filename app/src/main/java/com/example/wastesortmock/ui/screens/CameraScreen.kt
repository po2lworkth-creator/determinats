package com.example.wastesortmock.ui.screens

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.wastesortmock.data.wasteCategories
import com.example.wastesortmock.ml.WasteClassifier
import com.example.wastesortmock.data.PointsManager
import com.example.wastesortmock.data.StatsManager
import com.example.wastesortmock.data.AchievementsManager
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.media.ToneGenerator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    useFrontCamera: Boolean = false,
    onNavigateToResult: (String) -> Unit,
    onSwitchCamera: () -> Unit = {}
) {
    val pointsPerPlastic = 10
    val pointsPerTyped = 20
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var cameraReady by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var overlayLabel by remember { mutableStateOf("Нет данных") }
    var overlayColor by remember { mutableStateOf(Color.Black.copy(alpha = 0.6f)) }
    var celebrate by remember { mutableStateOf(false) }
    var celebratePoints by remember { mutableStateOf(0) }
    var celebrateLabel by remember { mutableStateOf("Поздравляем!") }

    // Классификатор
    var classifier by remember { mutableStateOf<WasteClassifier?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                classifier = WasteClassifier(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // CameraProvider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            { cameraProvider = cameraProviderFuture.get() },
            ContextCompat.getMainExecutor(context)
        )
    }

    // Запуск камеры
    LaunchedEffect(cameraProvider, useFrontCamera) {
        val provider = cameraProvider ?: return@LaunchedEffect
        try {
            provider.unbindAll()
            val previewUseCase = Preview.Builder().build().also { it.setSurfaceProvider(null) }
            val imageCaptureUseCase = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()

            preview = previewUseCase
            imageCapture = imageCaptureUseCase

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase
            )
            cameraReady = true
        } catch (e: Exception) {
            e.printStackTrace()
            cameraReady = false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Камера",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                when {
                    !cameraPermissionState.status.isGranted -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = if (cameraPermissionState.status.shouldShowRationale) {
                                        "Нужно разрешение на использование камеры"
                                    } else {
                                        "Запрос разрешения на камеру..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                    Text("Разрешить камеру")
                                }
                            }
                        }
                    }

                    isProcessing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Text(
                                    text = "Определение типа отходов...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { previewView ->
                                    preview?.setSurfaceProvider(previewView.surfaceProvider)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(overlayColor)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = overlayLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isProcessing || imageCapture == null) return@Button

                    val imageCaptureLocal = imageCapture ?: return@Button
                    val classifierLocal = classifier ?: run {
                        val availableCategories = wasteCategories.filter { it.id != "other" }
                        val randomCategory = availableCategories.random()
                        onNavigateToResult(randomCategory.id)
                        return@Button
                    }

                    isProcessing = true
                    overlayLabel = "Обработка снимка..."
                    overlayColor = Color.Black.copy(alpha = 0.65f)

                    val outputFile = java.io.File(context.getExternalFilesDir(null), "temp_capture.jpg")
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCaptureLocal.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                try {
                                    val file = output.savedUri?.let {
                                        java.io.File(it.path ?: return@let null)
                                    } ?: outputFile

                                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    bitmap?.let { bmp ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val res = classifierLocal.classifyWithScores(bmp)
                                                val maxProb = res.probability
                                                val label =
                                                    if (maxProb < 0.3f) "Неопределено" else mapAppLabel(res.appLabel)
                                                val color = if (label == "Неопределено") {
                                                    Color.Gray.copy(alpha = 0.65f)
                                                } else {
                                                    Color(0xFF0A8754).copy(alpha = 0.65f)
                                                }
                                                val modelLabelLower = res.modelLabel.lowercase()
                                                val earned = if (
                                                    modelLabelLower.contains("hdpe") ||
                                                    modelLabelLower.contains("pp") ||
                                                    modelLabelLower.contains("ps") ||
                                                    modelLabelLower.contains("pet")
                                                ) pointsPerTyped else pointsPerPlastic

                                                val total = PointsManager.addPoints(context, earned)
                                                StatsManager.increment(context, res.appLabel)
                                                AchievementsManager.checkAndUnlock(context, total)

                                                withContext(Dispatchers.Main) {
                                                    isProcessing = false
                                                    overlayLabel =
                                                        "$label / ${res.modelLabel} (${String.format("%.2f", maxProb)}) +$earned очков"
                                                    overlayColor = color
                                                    celebratePoints = earned
                                                    celebrateLabel = "Поздравляем! +$earned очков 🎉"
                                                    celebrate = true
                                                    // Звук короткий
                                                    ToneGenerator(AudioManager.STREAM_MUSIC, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                                    onNavigateToResult(res.appLabel)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) {
                                                    isProcessing = false
                                                    onNavigateToResult("other")
                                                }
                                            }
                                        }
                                    } ?: run {
                                        isProcessing = false
                                        onNavigateToResult("other")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isProcessing = false
                                    onNavigateToResult("other")
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                                isProcessing = false
                                val availableCategories = wasteCategories.filter { it.id != "other" }
                                val randomCategory = availableCategories.random()
                                onNavigateToResult(randomCategory.id)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = cameraReady && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Сфотографировать",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Кнопка назад снизу справа
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(onClick = { /* обработает навигация хоста */ }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }

            // Анимация поздравления
            AnimatedVisibility(
                visible = celebrate,
                enter = slideInVertically(initialOffsetY = { -200 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -200 }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3C2)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🎉",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = celebrateLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF444444),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Продолжай сортировать!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }

    // Авто-сокрытие анимации
    LaunchedEffect(celebrate) {
        if (celebrate) {
            delay(1500)
            celebrate = false
        }
    }
}

private fun mapAppLabel(appLabel: String): String =
    when (appLabel) {
        "metal" -> "Металл"
        "paper" -> "Бумага и картон"
        "glass" -> "Стекло"
        "plastic" -> "Пластик"
        "other" -> "Другое"
        else -> appLabel
    }
