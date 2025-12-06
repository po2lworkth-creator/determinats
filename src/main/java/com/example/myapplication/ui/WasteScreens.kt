package com.example.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication.ui.theme.Glass
import com.example.myapplication.ui.theme.Mixed
import com.example.myapplication.ui.theme.Metal
import com.example.myapplication.ui.theme.Organic
import com.example.myapplication.ui.theme.Paper
import com.example.myapplication.ui.theme.PhotoAction
import com.example.myapplication.ui.theme.Plastic
import com.example.myapplication.ui.theme.Success
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import android.graphics.YuvImage
import android.graphics.ImageFormat
import android.graphics.Rect
import android.Manifest
import android.content.pm.PackageManager

data class WasteCategory(
    val id: String,
    val title: String,
    val color: Color,
    val instructions: List<String>,
    val confidence: Float? = null
)

sealed interface Screen {
    data object Start : Screen
    data object Camera : Screen
    data class Result(val category: WasteCategory) : Screen
    data object Gamification : Screen
    data object Stats : Screen
}

private val categories = listOf(
    WasteCategory(
        id = "plastic",
        title = "Пластик",
        color = Plastic,
        instructions = listOf("Снимите крышку", "Промойте", "Уберите этикетку")
    ),
    WasteCategory(
        id = "paper",
        title = "Бумага",
        color = Paper,
        instructions = listOf("Уберите скобы", "Сложите аккуратно")
    ),
    WasteCategory(
        id = "glass",
        title = "Стекло",
        color = Glass,
        instructions = listOf("Снимите крышку", "Ополосните")
    ),
    WasteCategory(
        id = "metal",
        title = "Металл",
        color = Metal,
        instructions = listOf("Снимите этикетку", "Промойте")
    ),
    WasteCategory(
        id = "organic",
        title = "Органика",
        color = Organic,
        instructions = listOf("Без упаковки", "Удалите лишнюю жидкость")
    ),
    WasteCategory(
        id = "mixed",
        title = "Смешанное",
        color = Mixed,
        instructions = listOf("Если не уверены", "Используйте общий контейнер")
    ),
)

@Composable
fun WasteSortingApp(modifier: Modifier = Modifier) {
    var screen: Screen by remember { mutableStateOf(Screen.Start) }
    var selectedCategory by remember { mutableStateOf<WasteCategory?>(null) }
    var lastConfidence by remember { mutableStateOf<Float?>(null) }
    var dailyProgress by remember { mutableStateOf(0.4f) }
    val context = LocalContext.current
    val classifier = remember { WasteClassifier(context) }
    val detector = remember { ObjectDetector(context) }

    fun openCategory(category: WasteCategory) {
        selectedCategory = category
        lastConfidence = category.confidence
        screen = Screen.Result(category)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val current = screen) {
            Screen.Start -> StartScreen(
                onPhoto = { screen = Screen.Camera },
                onBarcode = {
                    // демо: штрих-код отнёс предмет к пластику
                    openCategory(categories.first { cat -> cat.id == "plastic" })
                },
                onManualSelect = { openCategory(it) }
            )

            Screen.Camera -> CameraScreen(
                onCancel = { screen = Screen.Start },
                onAnalyzed = { result ->
                    selectedCategory = result
                    lastConfidence = result.confidence
                    screen = Screen.Result(result)
                },
                classifier = classifier,
                detector = detector
            )

            is Screen.Result -> ResultScreen(
                category = current.category,
                onBack = { screen = Screen.Start },
                onDone = {
                    dailyProgress = (dailyProgress + 0.1f).coerceAtMost(1f)
                    screen = Screen.Gamification
                }
            )

            Screen.Gamification -> GamificationScreen(
                progress = dailyProgress,
                onContinue = { screen = Screen.Start },
                onOpenStats = { screen = Screen.Stats }
            )

            Screen.Stats -> StatsScreen(
                onBack = { screen = Screen.Start }
            )
        }
    }
}

@Composable
fun StartScreen(
    onPhoto: () -> Unit,
    onBarcode: () -> Unit,
    onManualSelect: (WasteCategory) -> Unit
) {
    var showManual by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Что вы выбрасываете?",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionTile(
                    title = "Фото",
                    icon = Icons.Outlined.CameraAlt,
                    description = "Откройте камеру",
                    onClick = onPhoto,
                    modifier = Modifier.weight(1f)
                )
                ActionTile(
                    title = "Штрих-код",
                    icon = Icons.Outlined.QrCodeScanner,
                    description = "Сканировать код",
                    onClick = onBarcode,
                    modifier = Modifier.weight(1f)
                )
                ActionTile(
                    title = "Вручную",
                    icon = Icons.Outlined.ManageSearch,
                    description = "Выбрать категорию",
                    onClick = { showManual = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (showManual) {
                Text(
                    text = "Выберите категорию",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        CategoryCard(category = category, onClick = { onManualSelect(category) })
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Выберите способ: фото, штрих-код или вручную",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}
private fun imageProxyToBitmapProper(image: ImageProxy): Bitmap? = try {
    val y = image.planes[0].buffer
    val u = image.planes[1].buffer
    val v = image.planes[2].buffer

    val ySize = y.remaining()
    val uSize = u.remaining()
    val vSize = v.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    y.get(nv21, 0, ySize)
    v.get(nv21, ySize, vSize)
    u.get(nv21, ySize + vSize, uSize)

    val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 95, out)
    BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
} catch (_: Exception) { null }

@Composable
fun CameraScreen(
    onCancel: () -> Unit,
    onAnalyzed: (WasteCategory) -> Unit,
    classifier: WasteClassifier,
    detector: ObjectDetector,
    detectionThreshold: Float = 0.35f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var detectionBox by remember { mutableStateOf<RectF?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // ====== ЗАПРОС ПРАВ ======
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
        } else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ====== ПОДКЛЮЧЕНИЕ КАМЕРЫ ======
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        //------------------ HEADER ------------------
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Камера", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = TextPrimary)
            }
        }

        //------------------ ПРЕВЬЮ КАМЕРЫ ------------------
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission && cameraProvider != null) {
                AndroidView(factory = { ctx ->
                    val preview = Preview.Builder().build()
                    val view = PreviewView(ctx)
                    preview.setSurfaceProvider(view.surfaceProvider)

                    cameraProvider!!.unbindAll()
                    cameraProvider!!.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    view
                }, modifier = Modifier.fillMaxSize())

                // Рисуем детектированный бокс
                Canvas(modifier = Modifier.matchParentSize()) {
                    detectionBox?.let { box ->
                        drawRect(
                            color = PhotoAction.copy(alpha = 0.6f),
                            topLeft = Offset(box.left * size.width, box.top * size.height),
                            size = Size(
                                (box.right - box.left) * size.width,
                                (box.bottom - box.top) * size.height
                            ),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }
            } else Text("Нет доступа к камере", color = TextSecondary)
        }

        //------------------ КНОПКА СЪЁМКИ ------------------
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val executor = ContextCompat.getMainExecutor(context)

                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {

                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp = imageProxyToBitmapProper(image)
                        image.close()

                        if (bmp == null) {
                            statusMessage = "Ошибка фото"
                            onAnalyzed(WasteCategory("mixed", "Смешанное", Mixed, listOf(), 0f))
                            return
                        }

                        scope.launch {
                            // === OBJECT DETECT ===
                            val detection = detector.detect(bmp)
                            val box = detection?.box?.takeIf { detection.score > detectionThreshold }
                                ?: RectF(.25f, .25f, .75f, .75f)

                            detectionBox = box

                            val crop = cropBitmapToBox(bmp, box) ?: bmp
                            val resized = Bitmap.createScaledBitmap(crop, 224, 224, true)

                            // === CLASSIFY ===
                            val cls = classifier.classify(resized)
                            statusMessage = "→ ${cls.label} ${"%.0f".format(cls.confidence * 100)}%"

                            onAnalyzed(mapLabelToCategory(cls.label, cls.confidence))
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        statusMessage = "Ошибка снимка"
                        onAnalyzed(WasteCategory("mixed", "Смешанное", Mixed, listOf(), 0f))
                    }
                })
            }
        ) {
            Text("Снять и распознать")
        }

        statusMessage?.let { Text(it, color = TextSecondary, modifier = Modifier.padding(top = 12.dp)) }
    }
}



@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: WasteCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1.1f),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = categoryIcon(category.id),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .fillMaxWidth(0.35f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(category.color)
                )
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun cropBitmapToBox(bmp: Bitmap, box: RectF): Bitmap? {
    val left = (box.left * bmp.width).toInt().coerceIn(0, bmp.width - 1)
    val top = (box.top * bmp.height).toInt().coerceIn(0, bmp.height - 1)
    val right = (box.right * bmp.width).toInt().coerceIn(left + 1, bmp.width)
    val bottom = (box.bottom * bmp.height).toInt().coerceIn(top + 1, bmp.height)
    val width = right - left
    val height = bottom - top
    return if (width > 0 && height > 0) {
        Bitmap.createBitmap(bmp, left, top, width, height)
    } else null
}

private fun mapLabelToCategory(label: String, confidence: Float? = null): WasteCategory =
    when (label.lowercase()) {
        "glass" -> WasteCategory(
            id = "glass",
            title = "Стекло",
            color = Glass,
            instructions = listOf("Снимите крышку", "Ополосните"),
            confidence = confidence
        )

        "metal" -> WasteCategory(
            id = "metal",
            title = "Металл",
            color = Metal,
            instructions = listOf("Снимите этикетку", "Промойте"),
            confidence = confidence
        )

        "plastic" -> WasteCategory(
            id = "plastic",
            title = "Пластик",
            color = Plastic,
            instructions = listOf("Снимите крышку", "Промойте", "Уберите этикетку"),
            confidence = confidence
        )

        "paper", "paper_carton", "cardboard" -> WasteCategory(
            id = "paper",
            title = "Бумага",
            color = Paper,
            instructions = listOf("Уберите скобы", "Сложите аккуратно"),
            confidence = confidence
        )

        else -> WasteCategory(
            id = "mixed",
            title = "Смешанное",
            color = Mixed,
            instructions = listOf("Если не уверены", "Используйте общий контейнер"),
            confidence = confidence
        )
    }

@Composable
fun ResultScreen(
    category: WasteCategory,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            category.confidence?.let {
                Text(
                    text = "Уверенность: ${(it * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
            ContainerHighlight(color = category.color, label = "Бросьте в этот контейнер")
            InstructionList(items = category.instructions)
            FilledTonalButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TextPrimary
                )
            ) {
                Text("Выбрать другую категорию", style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Назад", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = category.color,
                    contentColor = TextPrimary
                )
            ) {
                Text("Готово", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ContainerHighlight(color: Color, label: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = "Цвет контейнера подсвечен",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp, 88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun InstructionList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun GamificationScreen(
    progress: Float,
    onContinue: () -> Unit,
    onOpenStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(68.dp)
            )
            Text(
                text = "+1 к вашей статистике",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "Спасибо, сортировка выполнена",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProgressTrack(progress = progress)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenStats,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Статистика", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhotoAction,
                    contentColor = TextPrimary
                )
            ) {
                Text("Дальше", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Сегодня: ${(progress * 10).toInt()} сортировок",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Success)
            )
        }
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            FilledTonalButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("На главную", style = MaterialTheme.typography.titleMedium)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Чаще всего",
                value = "Бумага",
                color = Paper,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Сложные",
                value = "Смешанные",
                color = Mixed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Успех",
                value = "92%",
                color = Success,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Распределение",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DonutStat(
                label = "Сегодня",
                values = listOf(
                    Paper to 0.35f,
                    Plastic to 0.2f,
                    Glass to 0.2f,
                    Organic to 0.15f,
                    Mixed to 0.1f
                )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatBar(label = "Пластик", percent = 0.72f, color = Plastic)
                StatBar(label = "Бумага", percent = 0.86f, color = Paper)
                StatBar(label = "Стекло", percent = 0.64f, color = Glass)
                StatBar(label = "Органика", percent = 0.58f, color = Organic)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .fillMaxWidth(0.4f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatBar(label: String, percent: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${(percent * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun DonutStat(label: String, values: List<Pair<Color, Float>>, size: Dp = 220.dp) {
    Card(
        modifier = Modifier
            .width(size)
            .height(size),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            var start = -90f
            Canvas(modifier = Modifier.fillMaxSize()) {
                values.forEach { (color, percent) ->
                    val sweep = 360 * percent
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 22.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Text("Успех 92%", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        }
    }
}

private fun categoryIcon(id: String) = when (id) {
    "plastic" -> Icons.Outlined.Inventory2
    "paper" -> Icons.Outlined.Recycling
    "glass" -> Icons.Outlined.WaterDrop
    "metal" -> Icons.Outlined.Layers
    "organic" -> Icons.Outlined.Eco
    else -> Icons.Outlined.Label
}

