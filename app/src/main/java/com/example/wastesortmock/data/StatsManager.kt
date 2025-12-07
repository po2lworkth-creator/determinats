package com.example.wastesortmock.data

import android.content.Context

data class StatsSnapshot(
    val totalItems: Int,
    val byCategory: Map<String, Int>
)

object StatsManager {
    private const val PREFS = "stats_prefs"
    private const val KEY_TOTAL = "total_items"
    private const val KEY_PREFIX_CATEGORY = "cat_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun increment(context: Context, categoryId: String) {
        val p = prefs(context)
        val total = p.getInt(KEY_TOTAL, 0) + 1
        val catKey = KEY_PREFIX_CATEGORY + categoryId
        val catVal = p.getInt(catKey, 0) + 1
        p.edit()
            .putInt(KEY_TOTAL, total)
            .putInt(catKey, catVal)
            .apply()
    }

    fun getSnapshot(context: Context): StatsSnapshot {
        val p = prefs(context)
        val total = p.getInt(KEY_TOTAL, 0)
        val map = mutableMapOf<String, Int>()
        p.all.forEach { (k, v) ->
            if (k.startsWith(KEY_PREFIX_CATEGORY) && v is Int) {
                val cat = k.removePrefix(KEY_PREFIX_CATEGORY)
                map[cat] = v
            }
        }
        return StatsSnapshot(total, map)
    }
}

