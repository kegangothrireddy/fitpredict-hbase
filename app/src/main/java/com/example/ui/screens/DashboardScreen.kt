package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FitnessRecord
import com.example.ui.theme.*
import com.example.viewmodel.FitPredictViewModel

@Composable
fun DashboardScreen(
    viewModel: FitPredictViewModel,
    onNavigateToPredict: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHBase: () -> Unit
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val hbaseStatus by viewModel.hbaseStatus.collectAsState()
    val records by viewModel.records.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FitBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Hero Welcome & Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FitPredict Analytics",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FitBlueDark
                                )
                            )
                            Text(
                                text = "Predictive Intelligence & HBase Big Data",
                                style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                            )
                        }

                        // HBase Live Status Chip
                        Surface(
                            onClick = onNavigateToHBase,
                            shape = RoundedCornerShape(20.dp),
                            color = if (hbaseStatus.isConnected) FitGreenContainer else Color(0xFFFEF3C7),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (hbaseStatus.isConnected) FitGreenSecondary else Color(0xFFD97706))
                                )
                                Text(
                                    text = if (hbaseStatus.isConnected) "HBase: Online" else "HBase: Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hbaseStatus.isConnected) FitOnGreenContainer else Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToPredict,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FitBluePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Assessment", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToHBase,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FitBluePrimary)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HBase Setup", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Summary KPI Metric Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Assessments",
                        value = "${metrics.totalRecords}",
                        subtext = "Saved records",
                        icon = Icons.Default.Assessment,
                        accentColor = FitBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Average BMI",
                        value = "${metrics.averageBmi}",
                        subtext = "kg/m² cohort",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = FitGreenSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Avg Fitness Score",
                        value = "${metrics.averageScore}/100",
                        subtext = "Rule-based score",
                        icon = Icons.Default.Stars,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "HBase Integration",
                        value = if (hbaseStatus.isConnected) "Active" else "Offline",
                        subtext = "Table: fitness_data",
                        icon = Icons.Default.CloudQueue,
                        accentColor = if (hbaseStatus.isConnected) FitGreenSecondary else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Chart 1: Fitness Level Distribution (Custom Donut Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Fitness Level Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                    )
                    Text(
                        text = "Proportion of High, Moderate, and Low predictions",
                        style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Canvas Donut Chart
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                            DonutChart(
                                high = metrics.highCount,
                                moderate = metrics.moderateCount,
                                low = metrics.lowCount
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${metrics.totalRecords}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(text = "Total", fontSize = 11.sp, color = FitTextSecondary)
                            }
                        }

                        // Chart Legend
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem(label = "High Fitness", count = metrics.highCount, color = LevelHighGreen)
                            LegendItem(label = "Moderate", count = metrics.moderateCount, color = LevelModerateAmber)
                            LegendItem(label = "Low Fitness", count = metrics.lowCount, color = LevelLowRose)
                        }
                    }
                }
            }
        }

        // Chart 2: Step Activity Overview Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Daily Step Volume Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                    )
                    Text(
                        text = "Recent assessment volume by step milestones",
                        style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val stepBins = listOf(
                        "< 5k" to records.count { it.steps < 5000 },
                        "5k-8k" to records.count { it.steps in 5000..7999 },
                        "8k-10k" to records.count { it.steps in 8000..9999 },
                        "> 10k" to records.count { it.steps >= 10000 }
                    )

                    StepBarChart(stepBins)
                }
            }
        }

        // Recent Predictions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Assessments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text("View Full History (${records.size})", color = FitBluePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Recent Predictions Items (up to 4)
        items(records.take(4), key = { it.id }) { record ->
            RecentRecordItem(record = record)
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = FitTextSecondary, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FitTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 11.sp, color = FitTextSecondary)
        }
    }
}

@Composable
fun DonutChart(high: Int, moderate: Int, low: Int) {
    val total = (high + moderate + low).coerceAtLeast(1).toFloat()
    val highAngle = (high / total) * 360f
    val modAngle = (moderate / total) * 360f
    val lowAngle = (low / total) * 360f

    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 24.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f

        if (high > 0) {
            drawArc(
                color = LevelHighGreen,
                startAngle = startAngle,
                sweepAngle = highAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += highAngle
        }

        if (moderate > 0) {
            drawArc(
                color = LevelModerateAmber,
                startAngle = startAngle,
                sweepAngle = modAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += modAngle
        }

        if (low > 0) {
            drawArc(
                color = LevelLowRose,
                startAngle = startAngle,
                sweepAngle = lowAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = "$label: ", fontSize = 13.sp, color = FitTextSecondary)
        Text(text = "$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FitTextPrimary)
    }
}

@Composable
fun StepBarChart(stepBins: List<Pair<String, Int>>) {
    val maxCount = (stepBins.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        stepBins.forEach { (binName, count) ->
            val ratio = (count / maxCount).coerceIn(0.1f, 1.0f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(text = "$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FitBluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight(ratio * 0.75f)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(FitBluePrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = binName, fontSize = 11.sp, color = FitTextSecondary)
            }
        }
    }
}

@Composable
fun RecentRecordItem(record: FitnessRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = record.dateString,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = FitTextPrimary
                    )
                    if (record.storedInHBase) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FitGreenContainer
                        ) {
                            Text("HBase", fontSize = 9.sp, color = FitOnGreenContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.age}y &bull; ${record.gender} &bull; ${record.steps} steps &bull; ${record.heartRate} bpm &bull; Stress: ${record.stressLevel}",
                    fontSize = 12.sp,
                    color = FitTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val badgeColor = when (record.predictedLevel.lowercase()) {
                    "high" -> LevelHighGreen
                    "moderate" -> LevelModerateAmber
                    else -> LevelLowRose
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = record.predictedLevel,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Score: ${record.fitnessScore}/100",
                    fontSize = 11.sp,
                    color = FitTextSecondary
                )
            }
        }
    }
}
