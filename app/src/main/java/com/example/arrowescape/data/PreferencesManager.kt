package com.example.arrowescape.data

import android.content.Context
import android.content.SharedPreferences
import com.example.arrowescape.model.PlayerProgress

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("arrow_escape_prefs", Context.MODE_PRIVATE)

    fun loadProgress(): PlayerProgress {
        val currentLevel = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        val gems = prefs.getInt(KEY_GEMS, 100)
        val hints = prefs.getInt(KEY_HINTS, 3)
        val refilled = prefs.getInt(KEY_LIVES_REFILLED, 0)
        val streak = prefs.getInt(KEY_DAILY_STREAK, 0)
        val lastDaily = prefs.getString(KEY_LAST_DAILY, "") ?: ""
        val sound = prefs.getBoolean(KEY_SOUND, true)
        val haptics = prefs.getBoolean(KEY_HAPTICS, true)
        val reducedMotion = prefs.getBoolean(KEY_REDUCED_MOTION, false)

        val starsMap = mutableMapOf<Int, Int>()
        val rawStars = prefs.getString(KEY_STARS_MAP, "") ?: ""
        if (rawStars.isNotEmpty()) {
            rawStars.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val lvl = parts[0].toIntOrNull()
                    val stars = parts[1].toIntOrNull()
                    if (lvl != null && stars != null) {
                        starsMap[lvl] = stars
                    }
                }
            }
        }

        return PlayerProgress(
            currentLevel = currentLevel,
            starsPerLevel = starsMap,
            gems = gems,
            hintsRemaining = hints,
            totalLivesRefilled = refilled,
            dailyChallengeStreak = streak,
            lastDailyChallengeDate = lastDaily,
            soundEnabled = sound,
            hapticsEnabled = haptics,
            reducedMotion = reducedMotion
        )
    }

    fun saveProgress(progress: PlayerProgress) {
        val starsSerialized = progress.starsPerLevel.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit()
            .putInt(KEY_CURRENT_LEVEL, progress.currentLevel)
            .putInt(KEY_GEMS, progress.gems)
            .putInt(KEY_HINTS, progress.hintsRemaining)
            .putInt(KEY_LIVES_REFILLED, progress.totalLivesRefilled)
            .putInt(KEY_DAILY_STREAK, progress.dailyChallengeStreak)
            .putString(KEY_LAST_DAILY, progress.lastDailyChallengeDate)
            .putBoolean(KEY_SOUND, progress.soundEnabled)
            .putBoolean(KEY_HAPTICS, progress.hapticsEnabled)
            .putBoolean(KEY_REDUCED_MOTION, progress.reducedMotion)
            .putString(KEY_STARS_MAP, starsSerialized)
            .apply()
    }

    fun resetProgress(): PlayerProgress {
        prefs.edit().clear().apply()
        val defaultProgress = PlayerProgress()
        saveProgress(defaultProgress)
        return defaultProgress
    }

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_GEMS = "gems"
        private const val KEY_HINTS = "hints"
        private const val KEY_LIVES_REFILLED = "lives_refilled"
        private const val KEY_DAILY_STREAK = "daily_streak"
        private const val KEY_LAST_DAILY = "last_daily"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_REDUCED_MOTION = "reduced_motion"
        private const val KEY_STARS_MAP = "stars_map"
    }
}
