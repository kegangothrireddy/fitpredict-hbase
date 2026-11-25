package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.FitPredictViewModel

enum class AppScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PREDICT("Predict", Icons.Default.AddCircle),
    HISTORY("History", Icons.Default.History),
    WEB("Web UI", Icons.Default.Language),
    HBASE("HBase", Icons.Default.Storage)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FitPredictViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                val hbaseStatus by viewModel.hbaseStatus.collectAsState()

                // Handle back button to return to Dashboard if on another screen
                if (currentScreen != AppScreen.DASHBOARD) {
                    BackHandler {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "FitPredict",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = FitBlueDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = FitBlueContainer
                                    ) {
                                        Text(
                                            text = "HBase",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FitOnBlueContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                Surface(
                                    onClick = { currentScreen = AppScreen.HBASE },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (hbaseStatus.isConnected) FitGreenContainer else Color(0xFFFEF3C7),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                                            text = if (hbaseStatus.isConnected) "Online" else "Offline",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hbaseStatus.isConnected) FitOnGreenContainer else Color(0xFF92400E)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = FitSurface,
                                titleContentColor = FitTextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = FitSurface,
                            tonalElevation = 4.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            AppScreen.values().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            screen.icon,
                                            contentDescription = screen.title,
                                            tint = if (selected) FitBluePrimary else FitTextSecondary
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) FitBluePrimary else FitTextSecondary
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = FitBlueContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToPredict = { currentScreen = AppScreen.PREDICT },
                                onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                                onNavigateToHBase = { currentScreen = AppScreen.HBASE }
                            )
                            AppScreen.PREDICT -> PredictScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                                onNavigateToHBase = { currentScreen = AppScreen.HBASE }
                            )
                            AppScreen.HISTORY -> HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToPredict = { currentScreen = AppScreen.PREDICT }
                            )
                            AppScreen.WEB -> WebDashboardScreen(
                                viewModel = viewModel
                            )
                            AppScreen.HBASE -> HBaseScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
