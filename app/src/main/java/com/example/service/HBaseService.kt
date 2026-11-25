package com.example.service

import android.content.Context
import android.util.Base64
import com.example.model.FitnessRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class HBaseStatus(
    val isConnected: Boolean,
    val host: String,
    val port: Int,
    val tableName: String,
    val message: String,
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)

class HBaseService(private val context: Context) {

    private val prefs = context.getSharedPreferences("hbase_config", Context.MODE_PRIVATE)

    var host: String
        get() = prefs.getString("hbase_host", "10.0.2.2") ?: "10.0.2.2" // 10.0.2.2 maps to host localhost in Android Emulator
        set(value) = prefs.edit().putString("hbase_host", value).apply()

    var restPort: Int
        get() = prefs.getInt("hbase_rest_port", 8080)
        set(value) = prefs.edit().putInt("hbase_rest_port", value).apply()

    var thriftPort: Int
        get() = prefs.getInt("hbase_thrift_port", 9090)
        set(value) = prefs.edit().putInt("hbase_thrift_port", value).apply()

    val tableName = "fitness_data"

    /**
     * Checks if the Apache HBase REST daemon is reachable on the configured host and port.
     * Never falsely reports connected unless an actual HTTP 200 response is received.
     */
    suspend fun testConnection(): HBaseStatus = withContext(Dispatchers.IO) {
        val targetUrl = "http://$host:$restPort/version"
        try {
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                HBaseStatus(
                    isConnected = true,
                    host = host,
                    port = restPort,
                    tableName = tableName,
                    message = "Connected to Apache HBase REST daemon at $host:$restPort (HTTP 200 OK)."
                )
            } else {
                HBaseStatus(
                    isConnected = false,
                    host = host,
                    port = restPort,
                    tableName = tableName,
                    message = "HBase server returned HTTP $responseCode at $targetUrl."
                )
            }
        } catch (e: Exception) {
            HBaseStatus(
                isConnected = false,
                host = host,
                port = restPort,
                tableName = tableName,
                message = "HBase REST daemon unreachable at $targetUrl: ${e.message ?: "Connection refused"} (Offline Local Storage Active)"
            )
        }
    }

    /**
     * Attempts to persist a FitnessRecord into Apache HBase table via the HBase REST API.
     * Uses Base64-encoded column qualifiers and values matching:
     * - profile:age, profile:gender, profile:height_cm, profile:weight_kg, profile:bmi
     * - activity:steps, activity:exercise_mins
     * - health:heart_rate, health:sleep_hours, health:water_liters, health:stress_level
     * - prediction:fitness_score, prediction:predicted_level, prediction:date
     */
    suspend fun saveToHBase(record: FitnessRecord): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val rowKey = "rec_${record.timestamp}_${record.id.take(6)}"
        val encodedRowKey = Base64.encodeToString(rowKey.toByteArray(), Base64.NO_WRAP)
        val targetUrl = "http://$host:$restPort/$tableName/$rowKey"

        try {
            fun createCell(column: String, value: String): JSONObject {
                return JSONObject().apply {
                    put("column", Base64.encodeToString(column.toByteArray(), Base64.NO_WRAP))
                    put("$", Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP))
                }
            }

            val cells = JSONArray().apply {
                put(createCell("profile:age", record.age.toString()))
                put(createCell("profile:gender", record.gender))
                put(createCell("profile:height_cm", record.heightCm.toString()))
                put(createCell("profile:weight_kg", record.weightKg.toString()))
                put(createCell("profile:bmi", record.bmi.toString()))
                put(createCell("activity:steps", record.steps.toString()))
                put(createCell("activity:exercise_mins", record.exerciseMins.toString()))
                put(createCell("health:heart_rate", record.heartRate.toString()))
                put(createCell("health:sleep_hours", record.sleepHours.toString()))
                put(createCell("health:water_liters", record.waterLiters.toString()))
                put(createCell("health:stress_level", record.stressLevel))
                put(createCell("prediction:fitness_score", record.fitnessScore.toString()))
                put(createCell("prediction:predicted_level", record.predictedLevel))
                put(createCell("prediction:date", record.dateString))
            }

            val rowObj = JSONObject().apply {
                put("key", encodedRowKey)
                put("Cell", cells)
            }

            val rootObj = JSONObject().apply {
                put("Row", JSONArray().put(rowObj))
            }

            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(rootObj.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..204) {
                Pair(true, rowKey)
            } else {
                Pair(false, "Server returned status $code")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Failed to reach HBase")
        }
    }
}
