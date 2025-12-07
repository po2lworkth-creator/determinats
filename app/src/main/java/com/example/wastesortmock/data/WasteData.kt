package com.example.wastesortmock.data

import androidx.compose.ui.graphics.Color
import com.example.wastesortmock.model.WasteCategory

val wasteCategories = listOf(
    WasteCategory(
        id = "metal",
        title = "Металл",
        description = "Металлические предметы: банки, фольга, металлическая упаковка.",
        examples = listOf("Банка от напитка", "Консервная банка", "Металлическая фольга"),
        color = Color(0xFF90A4AE)
    ),
    WasteCategory(
        id = "paper",
        title = "Бумага и картон",
        description = "Бумага, картон, коробки, газеты, тетради.",
        examples = listOf("Газета", "Картонная коробка", "Тетрадь", "Бумажный пакет"),
        color = Color(0xFFFFCA28)
    ),
    WasteCategory(
        id = "glass",
        title = "Стекло",
        description = "Стеклянные бутылки и банки.",
        examples = listOf("Стеклянная бутылка", "Банка от варенья", "Стеклянная банка"),
        color = Color(0xFF66BB6A)
    ),
    WasteCategory(
        id = "plastic",
        title = "Пластик",
        description = "Пластиковая упаковка, бутылки, пакеты, контейнеры.",
        examples = listOf("Пластиковая бутылка", "Пакет", "Контейнер от йогурта", "Пластиковая упаковка"),
        color = Color(0xFF42A5F5)
    ),
    WasteCategory(
        id = "other",
        title = "Другое",
        description = "Смешанные и другие отходы, которые нельзя отнести к отдельной категории.",
        examples = listOf("Грязная упаковка", "Смешанные остатки", "Одноразовая посуда"),
        color = Color(0xFFAB47BC)
    )
)

fun getCategoryById(id: String): WasteCategory? {
    return wasteCategories.find { it.id == id }
}
