package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AirQualityData
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColorGasCyan = Color(0xFF00B4D8)
private val ColorDustAmber = Color(0xFFFF9100)

enum class ChartEngine {
    RECHARTS,
    NATIVE_COMPOSE
}

/**
 * Historical trend line graph of gas (MQ-135 ADC) and dust density (GP2Y1010 µg/m³) readings
 * stored in the Room database, featuring an interactive Recharts component with a native Compose fallback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GasDustTrendChartCard(
    history: List<AirQualityData>,
    isDarkMode: Boolean,
    onSeedSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEngine by remember { mutableStateOf(ChartEngine.RECHARTS) }
    var showGasSeries by remember { mutableStateOf(true) }
    var showDustSeries by remember { mutableStateOf(true) }
    var maxPointsLimit by remember { mutableIntStateOf(30) }

    val displayReadings = remember(history, maxPointsLimit) {
        if (history.size > maxPointsLimit) {
            history.takeLast(maxPointsLimit)
        } else {
            history
        }
    }

    // Metric aggregates
    val gasValues = remember(displayReadings) { displayReadings.map { it.gas } }
    val dustValues = remember(displayReadings) { displayReadings.map { it.dust } }

    val avgGas = if (gasValues.isNotEmpty()) gasValues.average().toInt() else 0
    val maxGas = if (gasValues.isNotEmpty()) gasValues.maxOrNull() ?: 0 else 0
    val avgDust = if (dustValues.isNotEmpty()) dustValues.average() else 0.0
    val maxDust = if (dustValues.isNotEmpty()) dustValues.maxOrNull() ?: 0.0 else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gas_dust_trend_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = "Trend Graph Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Historical Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Recharts",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Gas & Dust Density from Room Database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Storage badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${history.size} in DB",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Engine & Limit Selectors Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Engine Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        EnginePill(
                            title = "Recharts",
                            selected = selectedEngine == ChartEngine.RECHARTS,
                            onClick = { selectedEngine = ChartEngine.RECHARTS },
                            testTag = "engine_recharts_button"
                        )
                        EnginePill(
                            title = "Native M3",
                            selected = selectedEngine == ChartEngine.NATIVE_COMPOSE,
                            onClick = { selectedEngine = ChartEngine.NATIVE_COMPOSE },
                            testTag = "engine_native_button"
                        )
                    }
                }

                // Points range selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(15 to "15 pts", 30 to "30 pts", 50 to "50 pts").forEach { (limit, label) ->
                        val isSelected = maxPointsLimit == limit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .pointerInput(limit) {
                                        detectTapGestures { maxPointsLimit = limit }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Series Visibility Filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showGasSeries,
                    onClick = { if (showGasSeries && !showDustSeries) return@FilterChip else showGasSeries = !showGasSeries },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ColorGasCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MQ-135 Gas", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorGasCyan.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (showGasSeries) ColorGasCyan else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("toggle_gas_series")
                )

                FilterChip(
                    selected = showDustSeries,
                    onClick = { if (showDustSeries && !showGasSeries) return@FilterChip else showDustSeries = !showDustSeries },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ColorDustAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GP2Y1010 Dust", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorDustAmber.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (showDustSeries) ColorDustAmber else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("toggle_dust_series")
                )
            }

            // Summary Stats Cards Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricBox(
                    label = "Gas Avg / Peak",
                    value = "$avgGas / $maxGas ADC",
                    accentColor = ColorGasCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    label = "Dust Avg / Peak",
                    value = "${"%.1f".format(avgDust)} / ${"%.1f".format(maxDust)} µg",
                    accentColor = ColorDustAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            // Chart Render Area
            if (displayReadings.isEmpty()) {
                EmptyHistoricalTrendState(onSeedSampleData = onSeedSampleData)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    when (selectedEngine) {
                        ChartEngine.RECHARTS -> {
                            RechartsWebView(
                                readings = displayReadings,
                                isDark = isDarkMode,
                                showGas = showGasSeries,
                                showDust = showDustSeries,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ChartEngine.NATIVE_COMPOSE -> {
                            NativeComposeTrendChart(
                                readings = displayReadings,
                                isDark = isDarkMode,
                                showGas = showGasSeries,
                                showDust = showDustSeries,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom Axis Guide / Legend Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Left Y: Gas ADC • Right Y: Dust µg/m³",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (displayReadings.size <= 2) {
                    TextButton(onClick = onSeedSampleData) {
                        Text(
                            text = "+ Load Demo Points",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnginePill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.pointerInput(selected) {
            detectTapGestures { onClick() }
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .testTag(testTag)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 28.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoricalTrendState(
    onSeedSampleData: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DataUsage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Database Trend Data Yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Connect ESP32 or seed demo historical points to plot trend graph",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSeedSampleData,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("seed_sample_points_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Seed Demo Readings in DB", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Android WebView hosting interactive Recharts line graph with React 18, Recharts 2.x,
 * and high-fidelity fallback for offline mode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebView(
    readings: List<AirQualityData>,
    isDark: Boolean,
    showGas: Boolean,
    showDust: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dataJson = remember(readings) {
        val array = JSONArray()
        readings.forEach { item ->
            val obj = JSONObject()
            obj.put("time", timeFormat.format(Date(item.timestamp)))
            obj.put("gas", item.gas)
            obj.put("dust", String.format(Locale.US, "%.1f", item.dust).toDoubleOrNull() ?: item.dust)
            obj.put("aqi", item.aqi)
            array.put(obj)
        }
        array.toString()
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewError by remember { mutableStateOf(false) }

    val htmlDocument = remember(isDark) {
        generateRechartsHtml(isDark = isDark)
    }

    LaunchedEffect(dataJson, isDark, showGas, showDust) {
        webViewRef?.let { webView ->
            val script = "if (window.updateChartData) { window.updateChartData($dataJson, $isDark, $showGas, $showDust); }"
            webView.evaluateJavascript(script, null)
        }
    }

    if (webViewError) {
        // Fallback gracefully to native chart if WebView is unavailable (e.g., standard JVM unit test)
        NativeComposeTrendChart(
            readings = readings,
            isDark = isDark,
            showGas = showGas,
            showDust = showDust,
            modifier = modifier
        )
    } else {
        AndroidView(
            modifier = modifier.testTag("recharts_webview"),
            factory = { context ->
                try {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0) // Transparent background
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val initScript = "if (window.updateChartData) { window.updateChartData($dataJson, $isDark, $showGas, $showDust); }"
                                view?.evaluateJavascript(initScript, null)
                            }
                        }
                        loadDataWithBaseURL("https://recharts.org", htmlDocument, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                } catch (e: Throwable) {
                    webViewError = true
                    WebView(context) // dummy
                }
            },
            update = { webView ->
                try {
                    val script = "if (window.updateChartData) { window.updateChartData($dataJson, $isDark, $showGas, $showDust); }"
                    webView.evaluateJavascript(script, null)
                } catch (_: Exception) {}
            }
        )
    }
}

/**
 * Builds HTML document containing React, ReactDOM, and Recharts, with dual Y-axes, custom tooltip,
 * and high-performance SVG fallback if offline.
 */
private fun generateRechartsHtml(isDark: Boolean): String {
    val bgColor = if (isDark) "#121820" else "#FAFCFD"
    val textColor = if (isDark) "#E0E3E8" else "#202428"
    val subTextColor = if (isDark) "#8A94A0" else "#65727F"
    val gridColor = if (isDark) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.06)"
    val tooltipBg = if (isDark) "#1E2632" else "#FFFFFF"
    val tooltipBorder = if (isDark) "#334155" else "#E2E8F0"

    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      width: 100%;
      height: 100%;
      background-color: $bgColor;
      color: $textColor;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      overflow: hidden;
      user-select: none;
      -webkit-user-select: none;
    }
    #root {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      position: relative;
    }
    .custom-tooltip {
      background: $tooltipBg;
      border: 1px solid $tooltipBorder;
      border-radius: 8px;
      padding: 8px 12px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.25);
      font-size: 11px;
      color: $textColor;
    }
    .tooltip-header {
      font-weight: bold;
      margin-bottom: 4px;
      color: $subTextColor;
      border-bottom: 1px solid $tooltipBorder;
      padding-bottom: 3px;
    }
    .tooltip-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-top: 3px;
    }
    .badge-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 6px;
    }
    /* SVG Fallback styling */
    svg.fallback-chart {
      width: 100%;
      height: 100%;
    }
    .chart-label {
      font-size: 10px;
      fill: $subTextColor;
    }
  </style>
  <script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
</head>
<body>
  <div id="root"></div>

  <script>
    var currentData = [];
    var currentIsDark = $isDark;
    var currentShowGas = true;
    var currentShowDust = true;
    var rechartsLoaded = false;

    function renderApp() {
      var rootEl = document.getElementById('root');
      if (!rootEl) return;

      if (window.Recharts && window.React && window.ReactDOM) {
        rechartsLoaded = true;
        renderWithRecharts(rootEl);
      } else {
        renderSvgFallback(rootEl);
      }
    }

    function renderWithRecharts(rootEl) {
      try {
        var R = window.Recharts;
        var React = window.React;
        var ReactDOM = window.ReactDOM;

        var ResponsiveContainer = R.ResponsiveContainer;
        var LineChart = R.LineChart;
        var Line = R.Line;
        var XAxis = R.XAxis;
        var YAxis = R.YAxis;
        var CartesianGrid = R.CartesianGrid;
        var Tooltip = R.Tooltip;
        var Legend = R.Legend;

        var CustomTooltip = function(props) {
          if (!props.active || !props.payload || !props.payload.length) return null;
          var item = props.payload[0].payload;
          return React.createElement('div', { className: 'custom-tooltip' },
            React.createElement('div', { className: 'tooltip-header' }, 'Time: ' + (item.time || '')),
            currentShowGas && React.createElement('div', { className: 'tooltip-row' },
              React.createElement('span', null,
                React.createElement('span', { className: 'badge-dot', style: { background: '#00B4D8' } }),
                'MQ-135 Gas:'
              ),
              React.createElement('strong', { style: { color: '#00B4D8' } }, item.gas + ' ADC')
            ),
            currentShowDust && React.createElement('div', { className: 'tooltip-row' },
              React.createElement('span', null,
                React.createElement('span', { className: 'badge-dot', style: { background: '#FF9100' } }),
                'GP2Y1010 Dust:'
              ),
              React.createElement('strong', { style: { color: '#FF9100' } }, item.dust + ' µg/m³')
            )
          );
        };

        var chart = React.createElement(ResponsiveContainer, { width: '100%', height: '100%' },
          React.createElement(LineChart, {
            data: currentData,
            margin: { top: 15, right: 15, left: -10, bottom: 5 }
          },
            React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '$gridColor', opacity: 0.6 }),
            React.createElement(XAxis, {
              dataKey: 'time',
              stroke: '$subTextColor',
              tick: { fill: '$subTextColor', fontSize: 10 }
            }),
            currentShowGas && React.createElement(YAxis, {
              yAxisId: 'gasAxis',
              orientation: 'left',
              stroke: '#00B4D8',
              tick: { fill: '#00B4D8', fontSize: 10 },
              domain: ['auto', 'auto']
            }),
            currentShowDust && React.createElement(YAxis, {
              yAxisId: 'dustAxis',
              orientation: 'right',
              stroke: '#FF9100',
              tick: { fill: '#FF9100', fontSize: 10 },
              domain: ['auto', 'auto']
            }),
            React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
            currentShowGas && React.createElement(Line, {
              yAxisId: 'gasAxis',
              type: 'monotone',
              dataKey: 'gas',
              name: 'Gas (ADC)',
              stroke: '#00B4D8',
              strokeWidth: 2.5,
              dot: { r: 2, fill: '#00B4D8' },
              activeDot: { r: 6 }
            }),
            currentShowDust && React.createElement(Line, {
              yAxisId: 'dustAxis',
              type: 'monotone',
              dataKey: 'dust',
              name: 'Dust (µg/m³)',
              stroke: '#FF9100',
              strokeWidth: 2.5,
              dot: { r: 2, fill: '#FF9100' },
              activeDot: { r: 6 }
            })
          )
        );

        if (ReactDOM.createRoot) {
          if (!window._reactRoot) {
            window._reactRoot = ReactDOM.createRoot(rootEl);
          }
          window._reactRoot.render(chart);
        } else {
          ReactDOM.render(chart, rootEl);
        }
      } catch (err) {
        renderSvgFallback(rootEl);
      }
    }

    // High performance SVG fallback that requires zero CDN scripts
    function renderSvgFallback(rootEl) {
      if (!currentData || currentData.length === 0) {
        rootEl.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;font-size:12px;color:$subTextColor;">Awaiting Readings...</div>';
        return;
      }
      var width = rootEl.clientWidth || 340;
      var height = rootEl.clientHeight || 240;
      var padLeft = 45;
      var padRight = 45;
      var padTop = 20;
      var padBottom = 30;
      var plotW = width - padLeft - padRight;
      var plotH = height - padTop - padBottom;

      var gasMax = 100;
      var dustMax = 50;
      currentData.forEach(function(d) {
        if (d.gas > gasMax) gasMax = d.gas;
        if (d.dust > dustMax) dustMax = d.dust;
      });
      gasMax = Math.ceil(gasMax * 1.15);
      dustMax = Math.ceil(dustMax * 1.15);

      var pointsGas = [];
      var pointsDust = [];
      var n = currentData.length;
      for (var i = 0; i < n; i++) {
        var x = padLeft + (n === 1 ? plotW / 2 : (i / (n - 1)) * plotW);
        var yG = padTop + plotH - (currentData[i].gas / gasMax) * plotH;
        var yD = padTop + plotH - (currentData[i].dust / dustMax) * plotH;
        pointsGas.push(x.toFixed(1) + ',' + yG.toFixed(1));
        pointsDust.push(x.toFixed(1) + ',' + yD.toFixed(1));
      }

      var svgHtml = '<svg class="fallback-chart" viewBox="0 0 ' + width + ' ' + height + '">';
      // Grid lines
      for (var g = 0; g <= 4; g++) {
        var gy = padTop + (g / 4) * plotH;
        svgHtml += '<line x1="' + padLeft + '" y1="' + gy + '" x2="' + (width - padRight) + '" y2="' + gy + '" stroke="$gridColor" stroke-width="1" stroke-dasharray="3,3" />';
      }
      // Gas line
      if (currentShowGas) {
        svgHtml += '<polyline fill="none" stroke="#00B4D8" stroke-width="2.5" points="' + pointsGas.join(' ') + '" />';
      }
      // Dust line
      if (currentShowDust) {
        svgHtml += '<polyline fill="none" stroke="#FF9100" stroke-width="2.5" points="' + pointsDust.join(' ') + '" />';
      }
      // Y-axis Gas labels
      if (currentShowGas) {
        svgHtml += '<text x="8" y="' + (padTop + 10) + '" fill="#00B4D8" font-size="10" font-weight="bold">' + gasMax + '</text>';
        svgHtml += '<text x="8" y="' + (height - padBottom) + '" fill="#00B4D8" font-size="10">0</text>';
      }
      // Y-axis Dust labels
      if (currentShowDust) {
        svgHtml += '<text x="' + (width - 35) + '" y="' + (padTop + 10) + '" fill="#FF9100" font-size="10" font-weight="bold">' + dustMax + '</text>';
        svgHtml += '<text x="' + (width - 25) + '" y="' + (height - padBottom) + '" fill="#FF9100" font-size="10">0</text>';
      }
      // X labels
      if (currentData.length > 0) {
        svgHtml += '<text x="' + padLeft + '" y="' + (height - 8) + '" class="chart-label">' + currentData[0].time + '</text>';
        if (currentData.length > 1) {
          svgHtml += '<text x="' + (width - padRight - 35) + '" y="' + (height - 8) + '" class="chart-label">' + currentData[currentData.length - 1].time + '</text>';
        }
      }
      svgHtml += '</svg>';
      rootEl.innerHTML = svgHtml;
    }

    window.updateChartData = function(data, isDark, showGas, showDust) {
      currentData = data || [];
      currentIsDark = isDark;
      currentShowGas = showGas !== undefined ? showGas : true;
      currentShowDust = showDust !== undefined ? showDust : true;
      renderApp();
    };

    window.addEventListener('load', function() {
      renderApp();
    });
  </script>
</body>
</html>
""".trimIndent()
}

/**
 * 100% Native Jetpack Compose Canvas Trend Line Chart with Bezier smoothing,
 * dual Y-axis scaling, touch scrubber highlight, and animated rendering.
 */
@Composable
fun NativeComposeTrendChart(
    readings: List<AirQualityData>,
    isDark: Boolean,
    showGas: Boolean,
    showDust: Boolean,
    modifier: Modifier = Modifier
) {
    if (readings.isEmpty()) return

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var touchIndex by remember { mutableStateOf<Int?>(null) }

    val gasValues = readings.map { it.gas }
    val dustValues = readings.map { it.dust }

    val maxGas = remember(gasValues) { (gasValues.maxOrNull() ?: 100).coerceAtLeast(50) }
    val maxDust = remember(dustValues) { (dustValues.maxOrNull() ?: 50.0).coerceAtLeast(20.0) }

    val gridStrokeColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val axisTextColor = if (isDark) Color(0xFF90A4AE).toArgb() else Color(0xFF546E7A).toArgb()
    val indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        // Interactive touched point preview banner
        touchIndex?.let { idx ->
            if (idx in readings.indices) {
                val point = readings[idx]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time: ${timeFormat.format(Date(point.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (showGas) {
                                Text(
                                    text = "Gas: ${point.gas} ADC",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ColorGasCyan
                                )
                            }
                            if (showDust) {
                                Text(
                                    text = "Dust: ${"%.1f".format(point.dust)} µg/m³",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ColorDustAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(readings) {
                    detectTapGestures(
                        onPress = { offset ->
                            val padLeft = 45f
                            val padRight = 45f
                            val usableWidth = size.width - padLeft - padRight
                            if (usableWidth > 0 && readings.size > 1) {
                                val relX = (offset.x - padLeft).coerceIn(0f, usableWidth)
                                val fraction = relX / usableWidth
                                val calculatedIndex = (fraction * (readings.size - 1)).toInt()
                                touchIndex = calculatedIndex.coerceIn(0, readings.lastIndex)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padLeft = 45.dp.toPx()
                val padRight = 45.dp.toPx()
                val padTop = 15.dp.toPx()
                val padBottom = 25.dp.toPx()

                val chartW = size.width - padLeft - padRight
                val chartH = size.height - padTop - padBottom

                if (chartW <= 0 || chartH <= 0) return@Canvas

                // 1. Draw horizontal grid lines
                val gridLevels = 4
                for (i in 0..gridLevels) {
                    val y = padTop + (chartH / gridLevels) * i
                    drawLine(
                        color = gridStrokeColor,
                        start = Offset(padLeft, y),
                        end = Offset(size.width - padRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Draw Y-Axis labels (Text Paint)
                val textPaint = Paint().apply {
                    color = axisTextColor
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                }
                val gasPaint = Paint().apply {
                    color = ColorGasCyan.toArgb()
                    textSize = 10.sp.toPx()
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val dustPaint = Paint().apply {
                    color = ColorDustAmber.toArgb()
                    textSize = 10.sp.toPx()
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                if (showGas) {
                    drawContext.canvas.nativeCanvas.drawText("$maxGas", 6.dp.toPx(), padTop + 10.dp.toPx(), gasPaint)
                    drawContext.canvas.nativeCanvas.drawText("0", 12.dp.toPx(), size.height - padBottom, textPaint)
                }

                if (showDust) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${maxDust.toInt()}",
                        size.width - padRight + 6.dp.toPx(),
                        padTop + 10.dp.toPx(),
                        dustPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "0",
                        size.width - padRight + 6.dp.toPx(),
                        size.height - padBottom,
                        textPaint
                    )
                }

                // 3. X-Axis labels
                if (readings.isNotEmpty()) {
                    val firstTime = timeFormat.format(Date(readings.first().timestamp))
                    val lastTime = timeFormat.format(Date(readings.last().timestamp))
                    drawContext.canvas.nativeCanvas.drawText(
                        firstTime,
                        padLeft,
                        size.height - 6.dp.toPx(),
                        textPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        lastTime,
                        size.width - padRight - 40.dp.toPx(),
                        size.height - 6.dp.toPx(),
                        textPaint
                    )
                }

                val n = readings.size
                if (n == 0) return@Canvas

                val gasPath = Path()
                val dustPath = Path()
                val gasFillPath = Path()
                val dustFillPath = Path()

                val gasPoints = mutableListOf<Offset>()
                val dustPoints = mutableListOf<Offset>()

                for (i in 0 until n) {
                    val x = padLeft + if (n == 1) chartW / 2f else (i.toFloat() / (n - 1)) * chartW
                    val gasNorm = (readings[i].gas.toFloat() / maxGas).coerceIn(0f, 1f)
                    val dustNorm = (readings[i].dust.toFloat() / maxDust.toFloat()).coerceIn(0f, 1f)

                    val yGas = padTop + chartH - gasNorm * chartH
                    val yDust = padTop + chartH - dustNorm * chartH

                    gasPoints.add(Offset(x, yGas))
                    dustPoints.add(Offset(x, yDust))
                }

                // Helper to construct smooth bezier spline
                fun buildSpline(points: List<Offset>, strokePath: Path, fillPath: Path) {
                    if (points.isEmpty()) return
                    strokePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, size.height - padBottom)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        strokePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, size.height - padBottom)
                    fillPath.close()
                }

                // Render Gas Series
                if (showGas && gasPoints.isNotEmpty()) {
                    buildSpline(gasPoints, gasPath, gasFillPath)
                    drawPath(
                        path = gasFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ColorGasCyan.copy(alpha = 0.22f), ColorGasCyan.copy(alpha = 0.0f)),
                            startY = padTop,
                            endY = size.height - padBottom
                        )
                    )
                    drawPath(
                        path = gasPath,
                        color = ColorGasCyan,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    for (p in gasPoints) {
                        drawCircle(color = ColorGasCyan, radius = 2.5.dp.toPx(), center = p)
                    }
                }

                // Render Dust Series
                if (showDust && dustPoints.isNotEmpty()) {
                    buildSpline(dustPoints, dustPath, dustFillPath)
                    drawPath(
                        path = dustFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ColorDustAmber.copy(alpha = 0.18f), ColorDustAmber.copy(alpha = 0.0f)),
                            startY = padTop,
                            endY = size.height - padBottom
                        )
                    )
                    drawPath(
                        path = dustPath,
                        color = ColorDustAmber,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    for (p in dustPoints) {
                        drawCircle(color = ColorDustAmber, radius = 2.5.dp.toPx(), center = p)
                    }
                }

                // Draw touch vertical indicator line if active
                touchIndex?.let { idx ->
                    if (idx in gasPoints.indices) {
                        val touchX = gasPoints[idx].x
                        drawLine(
                            color = indicatorColor,
                            start = Offset(touchX, padTop),
                            end = Offset(touchX, size.height - padBottom),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        if (showGas) {
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = gasPoints[idx]
                            )
                            drawCircle(
                                color = ColorGasCyan,
                                radius = 4.dp.toPx(),
                                center = gasPoints[idx]
                            )
                        }
                        if (showDust) {
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = dustPoints[idx]
                            )
                            drawCircle(
                                color = ColorDustAmber,
                                radius = 4.dp.toPx(),
                                center = dustPoints[idx]
                            )
                        }
                    }
                }
            }
        }
    }
}
