package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.FitPredictViewModel

@Composable
fun HBaseScreen(viewModel: FitPredictViewModel) {
    val hbaseStatus by viewModel.hbaseStatus.collectAsState()
    val isTesting by viewModel.isTestingHBase.collectAsState()

    var hostInput by remember { mutableStateOf(viewModel.hbaseService.host) }
    var portInput by remember { mutableStateOf(viewModel.hbaseService.restPort.toString()) }

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
                    text = "Apache HBase Integration",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = FitTextPrimary)
                )
                Text(
                    text = "NoSQL Big Data storage configuration and setup guide",
                    style = MaterialTheme.typography.bodySmall.copy(color = FitTextSecondary)
                )
            }
        }

        // Live Connection Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (hbaseStatus.isConnected) FitGreenSecondary else Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "HBase Gateway Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FitTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (hbaseStatus.isConnected) FitGreenContainer else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (hbaseStatus.isConnected) "CONNECTED" else "DISCONNECTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hbaseStatus.isConnected) FitOnGreenContainer else Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = hbaseStatus.message,
                        fontSize = 13.sp,
                        color = FitTextSecondary
                    )

                    Divider(color = Color(0xFFE2E8F0))

                    // Configuration Inputs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("HBase Host IP") },
                            placeholder = { Text("10.0.2.2") },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("REST Port") },
                            placeholder = { Text("8080") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Text(
                        text = "Tip: '10.0.2.2' is the Android emulator's loopback address that connects to 'localhost' on your host PC/WSL.",
                        fontSize = 11.sp,
                        color = FitTextSecondary
                    )

                    Button(
                        onClick = {
                            val portInt = portInput.toIntOrNull() ?: 8080
                            viewModel.updateHBaseConfig(hostInput, portInt)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FitBluePrimary),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinging HBase Gateway...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test & Save Connection", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Schema Card: Column Families
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "HBase Table Schema: 'fitness_data'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FitBlueDark
                    )
                    Text(
                        text = "Records are stored in HBase partitioned across 4 Column Families:",
                        fontSize = 12.sp,
                        color = FitTextSecondary
                    )

                    val families = listOf(
                        "profile" to "age, gender, height_cm, weight_kg, bmi",
                        "activity" to "steps, exercise_mins, calories",
                        "health" to "heart_rate, sleep_hours, water_liters, stress_level",
                        "prediction" to "fitness_score, predicted_level, date, row_key"
                    )

                    families.forEach { (cf, cols) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FitSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FitBlueContainer,
                                modifier = Modifier.width(90.dp)
                            ) {
                                Text(
                                    text = cf,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FitOnBlueContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = cols, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = FitTextPrimary)
                        }
                    }
                }
            }
        }

        // Step-by-Step Setup Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FitSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Step-by-Step HBase Setup Guide (WSL/Linux)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FitTextPrimary
                    )

                    StepCodeBlock(
                        step = "1. Start Apache HBase Daemon",
                        cmd = "cd ~/hbase-2.4.17\nbin/start-hbase.sh"
                    )

                    StepCodeBlock(
                        step = "2. Start HBase REST Server (Port 8080)",
                        cmd = "bin/hbase-daemon.sh start rest -p 8080"
                    )

                    StepCodeBlock(
                        step = "3. Start HBase Thrift Gateway (Port 9090 for Python)",
                        cmd = "bin/hbase-daemon.sh start thrift -p 9090"
                    )

                    StepCodeBlock(
                        step = "4. Create Table in HBase Shell",
                        cmd = "bin/hbase shell\ncreate 'fitness_data', 'profile', 'activity', 'health', 'prediction'\nlist"
                    )
                }
            }
        }
    }
}

@Composable
fun StepCodeBlock(step: String, cmd: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = step, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FitBlueDark)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A)
        ) {
            Text(
                text = cmd,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
