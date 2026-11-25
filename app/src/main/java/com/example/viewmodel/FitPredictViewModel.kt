package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FitnessLogic
import com.example.model.FitnessPredictionResult
import com.example.model.FitnessRecord
import com.example.repository.FitnessRepository
import com.example.service.HBaseService
import com.example.service.HBaseStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val totalRecords: Int,
    val averageBmi: Float,
    val averageScore: Float,
    val highCount: Int,
    val moderateCount: Int,
    val lowCount: Int
)

class FitPredictViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FitnessRepository(application)
    val hbaseService = HBaseService(application)

    val records: StateFlow<List<FitnessRecord>> = repository.recordsFlow

    private val _hbaseStatus = MutableStateFlow(
        HBaseStatus(
            isConnected = false,
            host = hbaseService.host,
            port = hbaseService.restPort,
            tableName = hbaseService.tableName,
            message = "Offline (Local Storage Active). Tap HBase Setup to connect."
        )
    )
    val hbaseStatus: StateFlow<HBaseStatus> = _hbaseStatus.asStateFlow()

    private val _isTestingHBase = MutableStateFlow(false)
    val isTestingHBase: StateFlow<Boolean> = _isTestingHBase.asStateFlow()

    // Form Input State
    val age = MutableStateFlow("25")
    val gender = MutableStateFlow("Male")
    val heightCm = MutableStateFlow("175")
    val weightKg = MutableStateFlow("70")
    val heartRate = MutableStateFlow("68")
    val sleepHours = MutableStateFlow("7.5")
    val exerciseMins = MutableStateFlow("40")
    val waterLiters = MutableStateFlow("3.0")
    val steps = MutableStateFlow("8500")
    val stressLevel = MutableStateFlow("Low") // Low, Medium, High

    val isPredicting = MutableStateFlow(false)
    val latestResult = MutableStateFlow<FitnessPredictionResult?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    // Live dynamic BMI
    val liveBmi: StateFlow<Float> = combine(weightKg, heightCm) { w, h ->
        val weight = w.toFloatOrNull() ?: 0f
        val height = h.toFloatOrNull() ?: 0f
        FitnessLogic.calculateBmi(weight, height)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 22.9f)

    val liveBmiCategory: StateFlow<String> = combine(weightKg, heightCm) { w, h ->
        val weight = w.toFloatOrNull() ?: 0f
        val height = h.toFloatOrNull() ?: 0f
        val bmi = FitnessLogic.calculateBmi(weight, height)
        FitnessLogic.getBmiCategory(bmi)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Normal Weight")

    // Dashboard aggregated KPIs
    val dashboardMetrics: StateFlow<DashboardMetrics> = records.combine(_hbaseStatus) { list, _ ->
        if (list.isEmpty()) {
            DashboardMetrics(0, 0f, 0f, 0, 0, 0)
        } else {
            val total = list.size
            val avgBmi = (Math.round(list.map { it.bmi.toDouble() }.average() * 10.0) / 10.0).toFloat()
            val avgScore = (Math.round(list.map { it.fitnessScore.toDouble() }.average() * 10.0) / 10.0).toFloat()
            val high = list.count { it.predictedLevel.equals("High", ignoreCase = true) }
            val mod = list.count { it.predictedLevel.equals("Moderate", ignoreCase = true) }
            val low = list.count { it.predictedLevel.equals("Low", ignoreCase = true) }
            DashboardMetrics(total, avgBmi, avgScore, high, mod, low)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics(6, 23.4f, 72.5f, 3, 1, 2))

    init {
        // Initial silent test of HBase
        testHBaseConnection()
    }

    fun testHBaseConnection() {
        viewModelScope.launch {
            _isTestingHBase.value = true
            val status = hbaseService.testConnection()
            _hbaseStatus.value = status
            _isTestingHBase.value = false
        }
    }

    fun updateHBaseConfig(host: String, restPort: Int) {
        hbaseService.host = host
        hbaseService.restPort = restPort
        testHBaseConnection()
    }

    fun clearResultDialog() {
        latestResult.value = null
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun predictAndSave() {
        val ageVal = age.value.toIntOrNull()
        if (ageVal == null || ageVal !in 10..100) {
            errorMessage.value = "Age must be between 10 and 100."
            return
        }

        val heightVal = heightCm.value.toFloatOrNull()
        if (heightVal == null || heightVal !in 100f..250f) {
            errorMessage.value = "Height must be between 100 and 250 cm."
            return
        }

        val weightVal = weightKg.value.toFloatOrNull()
        if (weightVal == null || weightVal !in 25f..250f) {
            errorMessage.value = "Weight must be between 25 and 250 kg."
            return
        }

        val hrVal = heartRate.value.toIntOrNull() ?: 70
        val sleepVal = sleepHours.value.toFloatOrNull() ?: 7.0f
        val exVal = exerciseMins.value.toIntOrNull() ?: 30
        val waterVal = waterLiters.value.toFloatOrNull() ?: 2.5f
        val stepsVal = steps.value.toIntOrNull() ?: 7500
        val stressVal = stressLevel.value

        isPredicting.value = true
        errorMessage.value = null

        viewModelScope.launch {
            val bmi = FitnessLogic.calculateBmi(weightVal, heightVal)
            val bmiCat = FitnessLogic.getBmiCategory(bmi)
            val score = FitnessLogic.calculateFitnessScore(stepsVal, exVal, sleepVal, waterVal, hrVal, bmi, stressVal)
            val level = FitnessLogic.predictFitnessLevel(score, stepsVal, hrVal, stressVal)
            val recs = FitnessLogic.generateRecommendations(stepsVal, exVal, sleepVal, waterVal, hrVal, bmi, stressVal, level)

            var storedInHBase = false
            var rowKey: String? = null

            // If HBase is active, attempt sync
            if (_hbaseStatus.value.isConnected) {
                val tempRecord = FitnessRecord(
                    age = ageVal,
                    gender = gender.value,
                    heightCm = heightVal,
                    weightKg = weightVal,
                    heartRate = hrVal,
                    sleepHours = sleepVal,
                    exerciseMins = exVal,
                    waterLiters = waterVal,
                    steps = stepsVal,
                    stressLevel = stressVal,
                    bmi = bmi,
                    bmiCategory = bmiCat,
                    fitnessScore = score,
                    predictedLevel = level,
                    recommendations = recs
                )
                val (success, keyOrMsg) = hbaseService.saveToHBase(tempRecord)
                if (success) {
                    storedInHBase = true
                    rowKey = keyOrMsg
                }
            }

            val finalRecord = FitnessRecord(
                age = ageVal,
                gender = gender.value,
                heightCm = heightVal,
                weightKg = weightVal,
                heartRate = hrVal,
                sleepHours = sleepVal,
                exerciseMins = exVal,
                waterLiters = waterVal,
                steps = stepsVal,
                stressLevel = stressVal,
                bmi = bmi,
                bmiCategory = bmiCat,
                fitnessScore = score,
                predictedLevel = level,
                recommendations = recs,
                storedInHBase = storedInHBase,
                hbaseRowKey = rowKey
            )

            repository.addRecord(finalRecord)

            latestResult.value = FitnessPredictionResult(
                bmi = bmi,
                bmiCategory = bmiCat,
                fitnessScore = score,
                predictedLevel = level,
                recommendations = recs,
                storedInHBase = storedInHBase,
                hbaseRowKey = rowKey
            )

            isPredicting.value = false
        }
    }

    fun deleteRecord(id: String) {
        repository.deleteRecord(id)
    }
}
