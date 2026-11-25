package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FitnessPredictionResult
import com.example.ui.theme.*
import com.example.viewmodel.FitPredictViewModel

@Composable
fun PredictScreen(
    viewModel: FitPredictViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToHBase: () -> Unit
) {
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()
    val sleepHours by viewModel.sleepHours.collectAsState()
    val exerciseMins by viewModel.exerciseMins.collectAsState()
    val waterLiters by viewModel.waterLiters.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val stressLevel by viewModel.stressLevel.collectAsState()

    val liveBmi by viewModel.liveBmi.collectAsState()
    val liveBmiCat by viewModel.liveBmiCategory.collectAsState()

    val isPredicting by viewModel.isPredicting.collectAsState()
    val latestResult by viewModel.latestResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hbaseStatus by viewModel.hbaseStatus.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FitBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Predict Fitness Level",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                )
                Text(
                    text = "Enter biometric, activity, and recovery metrics for AI assessment",
                    style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                )
            }
        }

        // Error message banner if any
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = LevelLowRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage ?: "", color = Color(0xFF991B1B), fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 1: Demographics & Body Dimensions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = FitBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "1. Personal Profile & Body Dimensions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FitBlueDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { viewModel.age.value = it },
                            label = { Text("Age (yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Gender Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gender", fontSize = 12.sp, color = FitTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Male", "Female").forEach { g ->
                                    val isSelected = gender == g
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) FitBluePrimary else Color.Transparent)
                                            .clickable { viewModel.gender.value = g },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = g,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else FitTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = heightCm,
                            onValueChange = { viewModel.heightCm.value = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = weightKg,
                            onValueChange = { viewModel.weightKg.value = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Live Calculated BMI Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = FitBlueContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = FitBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Live Calculated BMI:", fontSize = 13.sp, color = FitOnBlueContainer)
                            }
                            Text(
                                text = "$liveBmi kg/m² ($liveBmiCat)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = FitBlueDark
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Physical Activity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = FitGreenSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "2. Daily Activity & Movement", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FitGreenDark)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = steps,
                            onValueChange = { viewModel.steps.value = it },
                            label = { Text("Daily Steps") },
                            placeholder = { Text("e.g. 8500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = exerciseMins,
                            onValueChange = { viewModel.exerciseMins.value = it },
                            label = { Text("Exercise (mins)") },
                            placeholder = { Text("e.g. 40") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Health, Sleep, Water & Stress Level
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "3. Biometrics, Recovery & Stress Level", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FitTextPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = heartRate,
                            onValueChange = { viewModel.heartRate.value = it },
                            label = { Text("Heart Rate (BPM)") },
                            placeholder = { Text("e.g. 68") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = sleepHours,
                            onValueChange = { viewModel.sleepHours.value = it },
                            label = { Text("Sleep (hours)") },
                            placeholder = { Text("e.g. 7.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = waterLiters,
                        onValueChange = { viewModel.waterLiters.value = it },
                        label = { Text("Water Intake (Liters)") },
                        placeholder = { Text("e.g. 3.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Stress Level Segmented Control
                    Column {
                        Text(
                            text = "Daily Stress Level",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FitTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Low", "Medium", "High").forEach { lvl ->
                                val isSelected = stressLevel.equals(lvl, ignoreCase = true)
                                val bg = when {
                                    isSelected && lvl == "Low" -> FitGreenSecondary
                                    isSelected && lvl == "Medium" -> Color(0xFFF59E0B)
                                    isSelected && lvl == "High" -> Color(0xFFEF4444)
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { viewModel.stressLevel.value = lvl },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lvl,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else FitTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: HBase Persistence Info & Action Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (hbaseStatus.isConnected) FitGreenContainer else Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (hbaseStatus.isConnected) FitGreenDark else FitTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hbaseStatus.isConnected) "HBase Storage: Active" else "HBase Storage: Offline (Local Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (hbaseStatus.isConnected) FitOnGreenContainer else FitTextPrimary
                        )
                        Text(
                            text = if (hbaseStatus.isConnected) "Record will sync to '${hbaseStatus.tableName}' via REST" else "Records are safely preserved in local device storage.",
                            fontSize = 11.sp,
                            color = FitTextSecondary
                        )
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = { viewModel.predictAndSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FitBluePrimary),
                enabled = !isPredicting
            ) {
                if (isPredicting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analyzing Metrics...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoGraph, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Predict Fitness Level", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Results Dialog
    latestResult?.let { result ->
        ResultDialog(
            result = result,
            onDismiss = { viewModel.clearResultDialog() },
            onViewHistory = {
                viewModel.clearResultDialog()
                onNavigateToHistory()
            }
        )
    }
}

@Composable
fun ResultDialog(
    result: FitnessPredictionResult,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FitSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fitness Assessment Result",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Predicted Level Big Badge
                val badgeColor = when (result.predictedLevel.lowercase()) {
                    "high" -> LevelHighGreen
                    "moderate" -> LevelModerateAmber
                    else -> LevelLowRose
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${result.predictedLevel} Fitness",
                        color = badgeColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Progress
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Fitness Score", fontSize = 13.sp, color = FitTextSecondary)
                        Text(text = "${result.fitnessScore} / 100", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FitBlueDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (result.fitnessScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = badgeColor,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculated BMI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FitSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calculated BMI", fontSize = 13.sp, color = FitTextSecondary)
                    Text("${result.bmi} kg/m² (${result.bmiCategory})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FitTextPrimary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recommendations
                Text(
                    text = "Personalized Recommendations:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FitTextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.recommendations.take(3).forEach { rec ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text("• ", color = FitBluePrimary, fontWeight = FontWeight.Bold)
                            Text(text = rec, fontSize = 12.sp, color = FitTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // HBase Persistence Status Note
                Text(
                    text = if (result.storedInHBase) "✓ Persisted to Apache HBase (Row: ${result.hbaseRowKey})" else "ⓘ Saved to Local Database (HBase is offline)",
                    fontSize = 11.sp,
                    color = if (result.storedInHBase) FitGreenDark else FitTextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onViewHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FitBluePrimary)
                    ) {
                        Text("History")
                    }
                }
            }
        }
    }
}
