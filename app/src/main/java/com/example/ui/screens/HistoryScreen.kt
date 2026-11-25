package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FitnessRecord
import com.example.ui.theme.*
import com.example.viewmodel.FitPredictViewModel

@Composable
fun HistoryScreen(
    viewModel: FitPredictViewModel,
    onNavigateToPredict: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = remember(records, selectedFilter, searchQuery) {
        records.filter { r ->
            val matchesFilter = when (selectedFilter) {
                "High" -> r.predictedLevel.equals("High", ignoreCase = true)
                "Moderate" -> r.predictedLevel.equals("Moderate", ignoreCase = true)
                "Low" -> r.predictedLevel.equals("Low", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                r.dateString.contains(searchQuery, ignoreCase = true) ||
                        r.gender.contains(searchQuery, ignoreCase = true) ||
                        r.stressLevel.contains(searchQuery, ignoreCase = true) ||
                        r.predictedLevel.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FitBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Prediction History & Archive",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                )
                Text(
                    text = "Browse records stored in local database and synced to Apache HBase",
                    style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                )
            }
        }

        // Search Bar & Filter Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by date, level, stress...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FitTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "High", "Moderate", "Low").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val chipBg = when {
                                isSelected && filter == "High" -> LevelHighGreen
                                isSelected && filter == "Moderate" -> LevelModerateAmber
                                isSelected && filter == "Low" -> LevelLowRose
                                isSelected -> FitBluePrimary
                                else -> Color(0xFFE2E8F0)
                            }
                            val chipText = if (isSelected) Color.White else FitTextSecondary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Total count bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredRecords.size} record${if (filteredRecords.size == 1) "" else "s"}",
                    fontSize = 13.sp,
                    color = FitTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FitSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = FitTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No records found", fontWeight = FontWeight.Bold, color = FitTextPrimary)
                        Text(text = "Try adjusting your search filter or add a new assessment", fontSize = 12.sp, color = FitTextSecondary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onNavigateToPredict, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FitBluePrimary)) {
                            Text("New Assessment")
                        }
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                HistoryRecordCard(
                    record = record,
                    onDelete = { viewModel.deleteRecord(record.id) }
                )
            }
        }
    }
}

@Composable
fun HistoryRecordCard(
    record: FitnessRecord,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val badgeColor = when (record.predictedLevel.lowercase()) {
        "high" -> LevelHighGreen
        "moderate" -> LevelModerateAmber
        else -> LevelLowRose
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Date & Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = record.dateString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FitTextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (record.storedInHBase) FitGreenContainer else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = if (record.storedInHBase) "HBase: Synced" else "Local",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (record.storedInHBase) FitOnGreenContainer else FitTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${record.predictedLevel} Fitness",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score and BMI summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FitSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Fitness Score", fontSize = 11.sp, color = FitTextSecondary)
                    Text("${record.fitnessScore}/100", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitBlueDark)
                }
                Column {
                    Text("BMI", fontSize = 11.sp, color = FitTextSecondary)
                    Text("${record.bmi} kg/m²", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitTextPrimary)
                }
                Column {
                    Text("Category", fontSize = 11.sp, color = FitTextSecondary)
                    Text(record.bmiCategory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FitTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact metrics chips
            Text(
                text = "${record.age}y &bull; ${record.gender} &bull; ${record.steps} steps &bull; ${record.exerciseMins}m exercise &bull; ${record.heartRate} bpm &bull; ${record.sleepHours}h sleep &bull; Stress: ${record.stressLevel}",
                fontSize = 12.sp,
                color = FitTextSecondary
            )

            // Expandable recommendations & details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Personalized Guidance:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FitTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    record.recommendations.forEach { tip ->
                        Text("• $tip", fontSize = 12.sp, color = FitTextSecondary, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    if (record.hbaseRowKey != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("HBase Key: ${record.hbaseRowKey}", fontSize = 10.sp, color = FitGreenDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Hide Guidance ▲" else "View Guidance ▼", fontSize = 12.sp, color = FitBluePrimary)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
