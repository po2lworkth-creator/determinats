package com.example.wastesortmock.data

import android.content.Context

data class Achievement(
    val id: String,
    val name: String,
    val threshold: Int
)

object AchievementsManager {
    private const val PREFS = "achievements_prefs"
    private const val KEY_UNLOCKED = "unlocked_set"

    private val achievements = listOf(
        Achievement(
            id = "novice_sorter",
            name = "Сортировщик новичок",
            threshold = 100
        ),
        Achievement(
            id = "master_split",
            name = "Мастер раздельного сбора",
            threshold = 200
        ),
        Achievement(
            id = "eco_activist",
            name = "Эко-активист",
            threshold = 300
        ),
        Achievement(
            id = "trash_conqueror",
            name = "Покоритель мусора",
            threshold = 400
        ),
        Achievement(
            id = "enthusiast",
            name = "Сортировщик-энтузиаст",
            threshold = 500
        ),
        Achievement(
            id = "eco_warrior",
            name = "Эко-воин",
            threshold = 600
        ),
        Achievement(
            id = "clean_home",
            name = "Чистый дом",
            threshold = 700
        ),
        Achievement(
            id = "eco_pioneer",
            name = "Пионер экологии",
            threshold = 800
        ),
        Achievement(
            id = "eco_hero",
            name = "Герой экологии",
            threshold = 900
        ),
        Achievement(
            id = "eco_guru",
            name = "Эко-гуру",
            threshold = 1000
        )
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getUnlocked(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()

    /**
     * Проверяет достижения при текущем количестве очков.
     * Возвращает список только что разблокированных.
     */
    fun checkAndUnlock(context: Context, totalPoints: Int): List<String> {
        val p = prefs(context)
        val unlocked = getUnlocked(context).toMutableSet()
        val newly = mutableListOf<String>()
        achievements.forEach { ach ->
            if (totalPoints >= ach.threshold && !unlocked.contains(ach.name)) {
                unlocked.add(ach.name)
                newly.add(ach.name)
            }
        }
        if (newly.isNotEmpty()) {
            p.edit().putStringSet(KEY_UNLOCKED, unlocked).apply()
        }
        return newly
    }

    fun all(): List<Achievement> = achievements
}

