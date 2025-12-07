package com.example.wastesortmock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wastesortmock.data.wasteCategories
import com.example.wastesortmock.data.getCategoryById
import com.example.wastesortmock.data.WasteCategoryClassifier
import com.example.wastesortmock.data.PointsManager
import com.example.wastesortmock.data.StatsManager
import com.example.wastesortmock.data.AchievementsManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun ProductSearchScreen(
    onNavigateToResult: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pointsPerManual = 5
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Определение типа отходов",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Введите название предмета, который у вас в руках, и мы определим его тип отходов",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название предмета") },
                placeholder = { Text("Например: пластиковая бутылка, алюминиевая банка, газета, стеклянная банка...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск"
                    )
                },
                singleLine = true
            )
            
            Button(
                onClick = {
                    // Используем расширенный классификатор для определения категории
                    val category = WasteCategoryClassifier.classify(searchText)
                    searchResult = category
                    // Начисляем очки за ручной ввод
                    val total = PointsManager.addPoints(context, pointsPerManual)
                    StatsManager.increment(context, category)
                    AchievementsManager.checkAndUnlock(context, total)
                    scope.launch {
                        snackbarHostState.showSnackbar("+" + pointsPerManual + " очков за сортировку")
                    }
                    // Переходим на экран результата
                    onNavigateToResult(category)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchText.isNotBlank()
            ) {
                Text(
                    text = "Определить категорию",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            if (searchResult != null) {
                val category = getCategoryById(searchResult!!)
                if (category != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = category.color.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Категория: ${category.title}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = category.color,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                }
            }
        }
    }
}

