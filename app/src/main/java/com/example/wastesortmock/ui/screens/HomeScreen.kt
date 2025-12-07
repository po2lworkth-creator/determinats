package com.example.wastesortmock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wastesortmock.ui.components.HomeActionButton
import com.example.wastesortmock.data.PointsManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size

@Composable
fun HomeScreen(
    onNavigateToFrontCamera: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToItem: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {}
) {
    val context = LocalContext.current
    var totalPoints by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        totalPoints = PointsManager.getPoints(context)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Заголовок
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Сортировка отходов",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Выберите действие · Баллы: $totalPoints",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Основные кнопки + компактный блок статистики справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Камера",
                    subtitle = "Использовать заднюю камеру",
                    icon = Icons.Default.Face,
                    onClick = onNavigateToFrontCamera
                )
                
                HomeActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Штрих-код",
                    subtitle = "Сканировать штрих-код",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = onNavigateToBarcode
                )
                
                HomeActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Предмет",
                    subtitle = "Введите название предмета для определения типа отходов",
                    icon = Icons.Default.Search,
                    onClick = onNavigateToItem
                )

                // Компактная кнопка в углу
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    HomeActionButton(
                        modifier = Modifier.size(140.dp),
                        title = "Рейтинг",
                        subtitle = "Баллы и ачивки",
                        icon = Icons.Default.Leaderboard,
                        onClick = onNavigateToStats
                    )
                }
            }
            
            // Запасное место внизу для больших экранов
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
