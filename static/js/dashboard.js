/**
 * static/js/dashboard.js
 * Chart.js initializations and analytics visualization for FitAnalytics
 */

document.addEventListener("DOMContentLoaded", function () {
    const levelCanvas = document.getElementById("fitnessLevelChart");
    const stepsCanvas = document.getElementById("stepsBarChart");

    if (!levelCanvas && !stepsCanvas) {
        return; // Not on the dashboard page
    }

    fetch("/api/stats")
        .then(response => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then(data => {
            if (data.status !== "success") return;

            // 1. Fitness Level Distribution Doughnut Chart
            if (levelCanvas) {
                const levelData = data.levels || {};
                const labels = Object.keys(levelData);
                const values = Object.values(levelData);

                // Palette: Green (High), Yellow/Orange (Moderate), Red (Low)
                const backgroundColors = labels.map(l => {
                    const clean = l.toLowerCase();
                    if (clean.includes("high")) return "#10b981";
                    if (clean.includes("moderate")) return "#f59e0b";
                    return "#ef4444";
                });

                new Chart(levelCanvas, {
                    type: "doughnut",
                    data: {
                        labels: labels,
                        datasets: [{
                            data: values,
                            backgroundColor: backgroundColors,
                            borderWidth: 2,
                            borderColor: "#ffffff",
                            hoverOffset: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: "bottom",
                                labels: {
                                    boxWidth: 12,
                                    padding: 15,
                                    font: { family: "'Plus Jakarta Sans', sans-serif", size: 12 }
                                }
                            },
                            tooltip: {
                                callbacks: {
                                    label: function (context) {
                                        const label = context.label || "";
                                        const value = context.raw || 0;
                                        const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                        const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                                        return ` ${label}: ${value} users (${percentage}%)`;
                                    }
                                }
                            }
                        },
                        cutout: "65%"
                    }
                });
            }

            // 2. Daily Steps Distribution Bar Chart
            if (stepsCanvas) {
                const stepBins = data.steps || {};
                const labels = Object.keys(stepBins);
                const counts = Object.values(stepBins);

                new Chart(stepsCanvas, {
                    type: "bar",
                    data: {
                        labels: labels,
                        datasets: [{
                            label: "Number of Individuals",
                            data: counts,
                            backgroundColor: "#3b82f6",
                            borderRadius: 6,
                            maxBarThickness: 45
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: { precision: 0, font: { family: "'Plus Jakarta Sans', sans-serif" } },
                                grid: { color: "#f1f5f9" }
                            },
                            x: {
                                grid: { display: false },
                                ticks: { font: { family: "'Plus Jakarta Sans', sans-serif" } }
                            }
                        },
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    label: function (ctx) {
                                        return ` Count: ${ctx.raw} records`;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        })
        .catch(err => {
            console.error("Failed to load dashboard chart analytics:", err);
        });
});
