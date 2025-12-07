package com.example.wastesortmock.data

import android.content.Context

object PointsManager {
    private const val PREFS_NAME = "points_prefs"
    private const val KEY_TOTAL = "total_points"

    fun getPoints(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TOTAL, 0)
    }

    fun addPoints(context: Context, points: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getInt(KEY_TOTAL, 0) + points
        prefs.edit().putInt(KEY_TOTAL, total).apply()
        return total
    }
}

