package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AqiCategory
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiHazardous
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiSensitive
import com.example.ui.theme.AqiUnhealthy
import com.example.ui.theme.AqiVeryUnhealthy
import kotlin.math.cos
import kotlin.math.sin

fun getAqiColor(category: AqiCategory): Color {
    return when (category) {
        AqiCategory.GOOD -> AqiGood
        AqiCategory.MODERATE -> AqiModerate
        AqiCategory.UNHEALTHY_SENSITIVE -> AqiSensitive
        AqiCategory.UNHEALTHY -> AqiUnhealthy
        AqiCategory.VERY_UNHEALTHY -> AqiVeryUnhealthy
        AqiCategory.HAZARDOUS -> AqiHazardous
    }
}

@Composable
fun AqiGaugeCard(
    aqi: Int,
    category: AqiCategory,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val aqiColor = getAqiColor(category)
    val animatedColor by animateColorAsState(
        targetValue = aqiColor,
        animationSpec = tween(600),
        label = "aqi_color_anim"
    )

    // Sweep angle from 0 to 500 mapped to 240 degrees (start angle 150 to 390)
    val targetFraction = (aqi.toFloat() / 500f).coerceIn(0.02f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "aqi_gauge_fraction"
    )

    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val knobBorderColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("aqi_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(animatedColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AIR QUALITY INDEX",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = animatedColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "EPA Standard",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Circular Arc Gauge
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val startAngle = 145f
                    val sweepRange = 250f

                    // Background Track
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepRange,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Sweep with Gradient
                    val activeSweep = sweepRange * animatedFraction
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to AqiGood,
                            0.2f to AqiModerate,
                            0.4f to AqiSensitive,
                            0.6f to AqiUnhealthy,
                            0.8f to AqiVeryUnhealthy,
                            1.0f to AqiHazardous
                        ),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Needle Knob Indicator at arc end
                    val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val radius = (size.width - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    val knobX = center.x + (radius * cos(currentAngleRad)).toFloat()
                    val knobY = center.y + (radius * sin(currentAngleRad)).toFloat()

                    drawCircle(
                        color = knobBorderColor,
                        radius = 8.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                    drawCircle(
                        color = animatedColor,
                        radius = 5.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                }

                // Centered AQI Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (aqi > 0) aqi.toString() else "--",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = animatedColor,
                        modifier = Modifier.testTag("aqi_numeric_value")
                    )
                    Text(
                        text = "AQI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Banner Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = animatedColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = animatedColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (if (statusText.isNotBlank() && statusText != "UNKNOWN") statusText else category.displayName).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = animatedColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.healthAdvice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
