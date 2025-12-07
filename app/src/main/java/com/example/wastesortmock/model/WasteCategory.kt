package com.example.wastesortmock.model

import androidx.compose.ui.graphics.Color

data class WasteCategory(
    val id: String,          // "plastic", "paper", "metal", "glass", "mixed"
    val title: String,       // "Пластик"
    val description: String, // краткое описание
    val examples: List<String>,
    val color: Color
)

