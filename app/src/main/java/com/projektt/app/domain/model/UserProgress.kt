package com.projektt.app.domain.model

data class UserProgress(
    val level: Int = 1,
    val currentXp: Int = 0,
    val totalXp: Int = 0,
    val tasksCompletedToday: Int = 0,
    val lastCompletionDate: String? = null
) {
    companion object {
        const val XP_PER_LEVEL = 100
        const val BASE_XP = 20
        const val TASK_CREATE_XP = 5
    }

    val xpForCurrentLevel: Int
        get() = currentXp

    val xpNeededForNextLevel: Int
        get() = XP_PER_LEVEL

    val progressPercent: Float
        get() = currentXp.toFloat() / XP_PER_LEVEL

    fun calculateXpForTask(): Int {
        // Logarithmischer Multiplikator: steigt schnell an, flacht dann ab
        // 1. Task: 20, 2.: 24, 3.: 27, 5.: 31, 10.: 37, 20.: 42
        val bonus = (8 * kotlin.math.ln((tasksCompletedToday + 1).toDouble())).toInt()
        return BASE_XP + bonus
    }

    fun addXp(xp: Int): Pair<UserProgress, Boolean> {
        val newTotalXp = totalXp + xp
        var newCurrentXp = currentXp + xp
        var newLevel = level
        var leveledUp = false

        while (newCurrentXp >= XP_PER_LEVEL) {
            newCurrentXp -= XP_PER_LEVEL
            newLevel++
            leveledUp = true
        }

        return Pair(
            copy(
                level = newLevel,
                currentXp = newCurrentXp,
                totalXp = newTotalXp,
                tasksCompletedToday = tasksCompletedToday + 1
            ),
            leveledUp
        )
    }

    // XP ohne Streak-Erhöhung (für Task-Erstellung)
    fun addXpWithoutStreak(xp: Int): Pair<UserProgress, Boolean> {
        val newTotalXp = totalXp + xp
        var newCurrentXp = currentXp + xp
        var newLevel = level
        var leveledUp = false

        while (newCurrentXp >= XP_PER_LEVEL) {
            newCurrentXp -= XP_PER_LEVEL
            newLevel++
            leveledUp = true
        }

        return Pair(
            copy(
                level = newLevel,
                currentXp = newCurrentXp,
                totalXp = newTotalXp
            ),
            leveledUp
        )
    }
}
