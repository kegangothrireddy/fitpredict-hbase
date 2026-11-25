package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FitBackground
import com.example.ui.theme.FitBlueDark
import com.example.ui.theme.FitTextSecondary
import com.example.viewmodel.FitPredictViewModel

@Composable
fun WebDashboardScreen(viewModel: FitPredictViewModel) {
    val records by viewModel.records.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()

    val htmlContent = remember(metrics, records) {
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>FitPredict Web Dashboard</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body {
                    background-color: #F8FAFC;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    padding: 12px;
                    color: #0F172A;
                }
                .card {
                    border: 1px solid #E2E8F0;
                    border-radius: 12px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
                    background: #FFFFFF;
                    margin-bottom: 12px;
                }
                .badge-high { background-color: #D1FAE5; color: #065F46; }
                .badge-mod { background-color: #FEF3C7; color: #92400E; }
                .badge-low { background-color: #FEE2E2; color: #991B1B; }
                .metric-val { font-size: 22px; font-weight: 700; color: #0284C7; }
                .chart-container { position: relative; height: 180px; width: 100%; }
            </style>
        </head>
        <body>
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h5 class="fw-bold mb-0" style="color: #0369A1;">FitPredict Web Dashboard</h5>
                    <small class="text-muted">Responsive HTML5, CSS3 & Chart.js Engine</small>
                </div>
                <span class="badge bg-primary">Local Web Engine</span>
            </div>

            <!-- KPI Row -->
            <div class="row g-2 mb-2">
                <div class="col-6">
                    <div class="card p-2 text-center">
                        <small class="text-muted">Total Records</small>
                        <div class="metric-val">${metrics.totalRecords}</div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="card p-2 text-center">
                        <small class="text-muted">Average BMI</small>
                        <div class="metric-val" style="color: #10B981;">${metrics.averageBmi}</div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="card p-2 text-center">
                        <small class="text-muted">Average Score</small>
                        <div class="metric-val" style="color: #F59E0B;">${metrics.averageScore}<small style="font-size:12px;">/100</small></div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="card p-2 text-center">
                        <small class="text-muted">HBase Table</small>
                        <div class="metric-val" style="font-size:15px; padding-top:6px;">fitness_data</div>
                    </div>
                </div>
            </div>

            <!-- Chart.js Visualization -->
            <div class="card p-3">
                <h6 class="fw-bold mb-2">Fitness Levels Breakdown (Chart.js)</h6>
                <div class="chart-container">
                    <canvas id="fitnessWebChart"></canvas>
                </div>
            </div>

            <!-- Recent Log Table -->
            <div class="card p-2">
                <h6 class="fw-bold p-2 mb-0">Recent Archive</h6>
                <div class="table-responsive">
                    <table class="table table-sm table-hover mb-0" style="font-size: 11px;">
                        <thead class="table-light">
                            <tr>
                                <th>Date</th>
                                <th>Steps</th>
                                <th>BMI</th>
                                <th>Score</th>
                                <th>Level</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${
                                records.take(5).joinToString("") { r ->
                                    val badgeCls = when (r.predictedLevel.lowercase()) {
                                        "high" -> "badge-high"
                                        "moderate" -> "badge-mod"
                                        else -> "badge-low"
                                    }
                                    """
                                    <tr>
                                        <td>${r.dateString.take(10)}</td>
                                        <td>${r.steps}</td>
                                        <td>${r.bmi}</td>
                                        <td>${r.fitnessScore}</td>
                                        <td><span class="badge $badgeCls">${r.predictedLevel}</span></td>
                                    </tr>
                                    """
                                }
                            }
                        </tbody>
                    </table>
                </div>
            </div>

            <script>
                const ctx = document.getElementById('fitnessWebChart').getContext('2d');
                new Chart(ctx, {
                    type: 'doughnut',
                    data: {
                        labels: ['High', 'Moderate', 'Low'],
                        datasets: [{
                            data: [${metrics.highCount}, ${metrics.moderateCount}, ${metrics.lowCount}],
                            backgroundColor: ['#10B981', '#F59E0B', '#EF4444'],
                            borderWidth: 2
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } }
                        }
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FitBackground)
            .padding(bottom = 80.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Responsive Web Dashboard Engine",
                    fontWeight = FontWeight.Bold,
                    color = FitBlueDark,
                    fontSize = 16.sp
                )
                Text(
                    text = "In-app embedded HTML5, CSS3, JavaScript and Chart.js preview",
                    fontSize = 12.sp,
                    color = FitTextSecondary
                )
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        )
    }
}
