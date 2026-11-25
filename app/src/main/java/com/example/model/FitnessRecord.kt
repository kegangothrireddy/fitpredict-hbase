package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FitnessRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val age: Int,
    val gender: String, // Male, Female, Other
    val heightCm: Float,
    val weightKg: Float,
    val heartRate: Int,
    val sleepHours: Float,
    val exerciseMins: Int,
    val waterLiters: Float,
    val steps: Int,
    val stressLevel: String, // Low, Medium, High
    val bmi: Float,
    val bmiCategory: String,
    val fitnessScore: Float, // 0 - 100
    val predictedLevel: String, // Low, Moderate, High
    val recommendations: List<String>,
    val storedInHBase: Boolean = false,
    val hbaseRowKey: String? = null
)

data class FitnessPredictionResult(
    val bmi: Float,
    val bmiCategory: String,
    val fitnessScore: Float,
    val predictedLevel: String,
    val recommendations: List<String>,
    val storedInHBase: Boolean,
    val hbaseRowKey: String?
)

object FitnessLogic {

    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f || weightKg <= 0f) return 22.0f
        val heightM = heightCm / 100.0f
        val bmi = weightKg / (heightM * heightM)
        return (Math.round(bmi * 10.0) / 10.0).toFloat()
    }

    fun getBmiCategory(bmi: Float): String {
        return when {
            bmi < 18.5f -> "Underweight"
            bmi < 25.0f -> "Normal Weight"
            bmi < 30.0f -> "Overweight"
            else -> "Obese"
        }
    }

    /**
     * Transparent, documented rule-based Fitness Score from 0 to 100.
     * Weights:
     * - Daily Steps (25 pts): 10,000 steps = 25 pts
     * - Exercise Duration (20 pts): 45 mins = 20 pts
     * - Sleep Hours (15 pts): 7-9 hours = 15 pts
     * - Hydration (15 pts): 3.0 Liters = 15 pts
     * - Resting Heart Rate (15 pts): 60-72 BPM = 15 pts
     * - BMI (10 pts): 18.5 - 24.9 = 10 pts
     * - Stress Level Modifier: Low (+5), Medium (0), High (-5)
     */
    fun calculateFitnessScore(
        steps: Int,
        exerciseMins: Int,
        sleepHours: Float,
        waterLiters: Float,
        heartRate: Int,
        bmi: Float,
        stressLevel: String
    ): Float {
        var score = 0.0f

        // Steps (max 25)
        val stepRatio = (steps.coerceAtLeast(0) / 10000.0f).coerceAtMost(1.0f)
        score += stepRatio * 25.0f

        // Exercise (max 20)
        val exRatio = (exerciseMins.coerceAtLeast(0) / 45.0f).coerceAtMost(1.0f)
        score += exRatio * 20.0f

        // Sleep (max 15)
        score += when {
            sleepHours in 7.0f..9.0f -> 15.0f
            sleepHours in 6.0f..<7.0f || sleepHours in 9.0f..10.0f -> 10.0f
            sleepHours in 5.0f..<6.0f -> 6.0f
            else -> 2.0f
        }

        // Hydration (max 15)
        val waterRatio = (waterLiters.coerceAtLeast(0f) / 3.0f).coerceAtMost(1.0f)
        score += waterRatio * 15.0f

        // Heart Rate (max 15)
        score += when {
            heartRate in 55..72 -> 15.0f
            heartRate in 73..80 -> 10.0f
            heartRate in 81..90 -> 6.0f
            else -> 2.0f
        }

        // BMI (max 10)
        score += when {
            bmi in 18.5f..24.9f -> 10.0f
            bmi in 25.0f..29.9f -> 6.0f
            bmi in 17.0f..<18.5f -> 5.0f
            else -> 2.0f
        }

        // Stress Modifier
        when (stressLevel.lowercase()) {
            "low" -> score += 5.0f
            "high" -> score -= 5.0f
            // medium -> no change
        }

        return (score.coerceIn(0.0f, 100.0f) * 10f).let { Math.round(it) / 10f }
    }

    /**
     * Predicts fitness category based on multi-factor decision boundaries.
     */
    fun predictFitnessLevel(fitnessScore: Float, steps: Int, heartRate: Int, stressLevel: String): String {
        return when {
            fitnessScore >= 75.0f && steps >= 8000 && heartRate <= 75 -> "High"
            fitnessScore < 48.0f || (steps < 4500 && stressLevel.equals("High", ignoreCase = true)) -> "Low"
            else -> "Moderate"
        }
    }

    /**
     * Synthesizes actionable, non-medical lifestyle tips based on biometrics.
     */
    fun generateRecommendations(
        steps: Int,
        exerciseMins: Int,
        sleepHours: Float,
        waterLiters: Float,
        heartRate: Int,
        bmi: Float,
        stressLevel: String,
        level: String
    ): List<String> {
        val recs = mutableListOf<String>()

        if (steps < 6500) {
            recs.add("Increase daily steps: Aim for a 15-min post-meal brisk walk to reach at least 7,500 daily steps.")
        } else if (steps >= 10000) {
            recs.add("Excellent step volume: Maintain this high daily movement consistency.")
        }

        if (exerciseMins < 25) {
            recs.add("Cardio & Training: Target at least 30 minutes of moderate aerobic or bodyweight exercise 4-5 days/week.")
        } else {
            recs.add("Great exercise commitment: Schedule active recovery and stretching days to avoid fatigue.")
        }

        if (sleepHours < 7.0f) {
            recs.add("Sleep Optimization: Prioritize 7 to 8 hours of restorative sleep to enhance muscle recovery and stress resilience.")
        }

        if (waterLiters < 2.5f) {
            recs.add("Hydration: Carry a water bottle to reach a daily target of 2.5 - 3.5 Liters.")
        }

        if (heartRate > 78) {
            recs.add("Cardiovascular Health: Regular zone-2 cardio and breathing exercises help lower resting heart rate.")
        }

        if (stressLevel.equals("High", ignoreCase = true)) {
            recs.add("Stress Management: Incorporate 10 minutes of mindfulness, meditation, or outdoor breaks daily.")
        }

        if (bmi >= 25.0f) {
            recs.add("Nutrition Balance: Emphasize lean protein, fiber, and portion control for steady metabolic health.")
        }

        if (recs.isEmpty()) {
            recs.add("Maintain your healthy daily routine with balanced nutrition, hydration, and regular workouts.")
        }

        return recs
    }
}
