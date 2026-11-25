package com.example.repository

import android.content.Context
import com.example.model.FitnessLogic
import com.example.model.FitnessRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class FitnessRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("fitness_records_db", Context.MODE_PRIVATE)
    private val _recordsFlow = MutableStateFlow<List<FitnessRecord>>(emptyList())
    val recordsFlow: StateFlow<List<FitnessRecord>> = _recordsFlow.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        val rawJson = prefs.getString("records_json", null)
        if (rawJson.isNullOrBlank()) {
            // Seed initial cohort dataset
            val seedList = getInitialSeedData()
            saveRecordsInternal(seedList)
            _recordsFlow.value = seedList
        } else {
            try {
                val jsonArray = JSONArray(rawJson)
                val list = mutableListOf<FitnessRecord>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val recsArray = obj.optJSONArray("recommendations") ?: JSONArray()
                    val recsList = mutableListOf<String>()
                    for (j in 0 until recsArray.length()) {
                        recsList.add(recsArray.getString(j))
                    }

                    list.add(
                        FitnessRecord(
                            id = obj.getString("id"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            dateString = obj.optString("dateString", "2026-10-13 10:00"),
                            age = obj.optInt("age", 25),
                            gender = obj.optString("gender", "Male"),
                            heightCm = obj.optDouble("heightCm", 175.0).toFloat(),
                            weightKg = obj.optDouble("weightKg", 70.0).toFloat(),
                            heartRate = obj.optInt("heartRate", 68),
                            sleepHours = obj.optDouble("sleepHours", 7.5).toFloat(),
                            exerciseMins = obj.optInt("exerciseMins", 40),
                            waterLiters = obj.optDouble("waterLiters", 3.0).toFloat(),
                            steps = obj.optInt("steps", 8500),
                            stressLevel = obj.optString("stressLevel", "Low"),
                            bmi = obj.optDouble("bmi", 22.9).toFloat(),
                            bmiCategory = obj.optString("bmiCategory", "Normal Weight"),
                            fitnessScore = obj.optDouble("fitnessScore", 80.5).toFloat(),
                            predictedLevel = obj.optString("predictedLevel", "High"),
                            recommendations = recsList,
                            storedInHBase = obj.optBoolean("storedInHBase", false),
                            hbaseRowKey = obj.optString("hbaseRowKey", null)
                        )
                    )
                }
                _recordsFlow.value = list
            } catch (e: Exception) {
                val fallback = getInitialSeedData()
                _recordsFlow.value = fallback
            }
        }
    }

    fun addRecord(record: FitnessRecord) {
        val current = _recordsFlow.value.toMutableList()
        current.add(0, record) // Insert at beginning
        _recordsFlow.value = current
        saveRecordsInternal(current)
    }

    fun deleteRecord(id: String) {
        val filtered = _recordsFlow.value.filter { it.id != id }
        _recordsFlow.value = filtered
        saveRecordsInternal(filtered)
    }

    private fun saveRecordsInternal(list: List<FitnessRecord>) {
        val jsonArray = JSONArray()
        list.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("timestamp", r.timestamp)
                put("dateString", r.dateString)
                put("age", r.age)
                put("gender", r.gender)
                put("heightCm", r.heightCm)
                put("weightKg", r.weightKg)
                put("heartRate", r.heartRate)
                put("sleepHours", r.sleepHours)
                put("exerciseMins", r.exerciseMins)
                put("waterLiters", r.waterLiters)
                put("steps", r.steps)
                put("stressLevel", r.stressLevel)
                put("bmi", r.bmi)
                put("bmiCategory", r.bmiCategory)
                put("fitnessScore", r.fitnessScore)
                put("predictedLevel", r.predictedLevel)
                put("storedInHBase", r.storedInHBase)
                put("hbaseRowKey", r.hbaseRowKey)
                val recsArray = JSONArray()
                r.recommendations.forEach { recsArray.put(it) }
                put("recommendations", recsArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("records_json", jsonArray.toString()).apply()
    }

    private fun getInitialSeedData(): List<FitnessRecord> {
        val sampleConfigs = listOf(
            Triple(25, "Male", Pair(178f, 72f)) to Quad(11200, 50, 7.5f, 3.2f) to Pair(62, "Low"),
            Triple(34, "Female", Pair(165f, 60f)) to Quad(9800, 45, 7.0f, 2.8f) to Pair(68, "Low"),
            Triple(45, "Male", Pair(172f, 88f)) to Quad(4300, 15, 5.5f, 1.5f) to Pair(82, "High"),
            Triple(29, "Female", Pair(160f, 54f)) to Quad(12400, 60, 8.0f, 3.5f) to Pair(60, "Low"),
            Triple(52, "Male", Pair(175f, 95f)) to Quad(3500, 10, 5.0f, 1.2f) to Pair(86, "High"),
            Triple(38, "Female", Pair(168f, 74f)) to Quad(6800, 25, 6.5f, 2.0f) to Pair(74, "Medium")
        )

        val list = mutableListOf<FitnessRecord>()
        sampleConfigs.forEachIndexed { idx, item ->
            val profile = item.first.first
            val activity = item.first.second
            val health = item.second

            val age = profile.first
            val gender = profile.second
            val (h, w) = profile.third

            val steps = activity.first
            val exMins = activity.second
            val sleep = activity.third
            val water = activity.fourth

            val hr = health.first
            val stress = health.second

            val bmi = FitnessLogic.calculateBmi(w, h)
            val bmiCat = FitnessLogic.getBmiCategory(bmi)
            val score = FitnessLogic.calculateFitnessScore(steps, exMins, sleep, water, hr, bmi, stress)
            val level = FitnessLogic.predictFitnessLevel(score, steps, hr, stress)
            val recs = FitnessLogic.generateRecommendations(steps, exMins, sleep, water, hr, bmi, stress, level)

            list.add(
                FitnessRecord(
                    age = age,
                    gender = gender,
                    heightCm = h,
                    weightKg = w,
                    steps = steps,
                    exerciseMins = exMins,
                    sleepHours = sleep,
                    waterLiters = water,
                    heartRate = hr,
                    stressLevel = stress,
                    bmi = bmi,
                    bmiCategory = bmiCat,
                    fitnessScore = score,
                    predictedLevel = level,
                    recommendations = recs,
                    dateString = "2026-10-1${idx + 1} 09:30",
                    storedInHBase = false,
                    hbaseRowKey = "rec_seed_00$idx"
                )
            )
        }
        return list
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
