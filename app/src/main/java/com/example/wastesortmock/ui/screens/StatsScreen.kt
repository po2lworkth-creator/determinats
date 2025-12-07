package com.example.wastesortmock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wastesortmock.data.PointsManager
import com.example.wastesortmock.data.StatsManager
import com.example.wastesortmock.data.AchievementsManager
import androidx.compose.ui.platform.LocalContext
import com.example.wastesortmock.data.StatsSnapshot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var totalPoints by remember { mutableStateOf(0) }
    var stats by remember { mutableStateOf(StatsSnapshot(0, emptyMap())) }
    var achievements by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(Unit) {
        totalPoints = PointsManager.getPoints(context)
        stats = StatsManager.getSnapshot(context)
        achievements = AchievementsManager.getUnlocked(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Рейтинг и статистика",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Общий счет: $totalPoints",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Статистика по категориям",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Всего предметов: ${stats.totalItems}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (stats.byCategory.isEmpty()) {
                            Text(
                                text = "Пока нет данных",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            stats.byCategory.forEach { (cat, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = cat, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = "$count", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ачивки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val all = AchievementsManager.all()
                        if (all.isEmpty()) {
                            Text(
                                text = "Пока нет ачивок",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                all.forEach { ach ->
                                    val unlocked = achievements.contains(ach.name)
                                    val icon = when (ach.id) {
                                        "novice_sorter" -> "🏁"
                                        "master_split" -> "🧭"
                                        "eco_activist" -> "🌿"
                                        "trash_conqueror" -> "🏆"
                                        "enthusiast" -> "🤝"
                                        "eco_warrior" -> "⚔️"
                                        "clean_home" -> "🏠"
                                        "eco_pioneer" -> "🚀"
                                        "eco_hero" -> "🦸"
                                        "eco_guru" -> "📚"
                                        else -> "⭐"
                                    }
                                    Card(
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (unlocked) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = icon,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                            Text(
                                                text = ach.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Порог: ${ach.threshold}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            LinearProgressIndicator(
                                                progress = if (unlocked) 1f else 0f,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
        }
    }
}

