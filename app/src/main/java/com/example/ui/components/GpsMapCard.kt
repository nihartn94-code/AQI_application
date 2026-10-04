package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiSensitive
import com.example.ui.theme.GpsTeal
import java.util.Locale

@Composable
fun GpsMapCard(
    latitude: Double,
    longitude: Double,
    altitude: Double,
    speed: Double,
    satellites: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasGpsFix = satellites > 0 && (latitude != 0.0 || longitude != 0.0)
    val noSatColor = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gps_map_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GpsTeal.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = GpsTeal,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GPS TELEMETRY & MAP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (hasGpsFix) "Station Location Fixed" else "Waiting for GPS Fix...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasGpsFix) AqiGood else AqiSensitive
                        )
                    }
                }

                // Satellites lock chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (satellites > 0) AqiGood.copy(alpha = 0.15f) else noSatColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SatelliteAlt,
                            contentDescription = null,
                            tint = if (satellites > 0) AqiGood else noSatColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$satellites Satellites",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (satellites > 0) AqiGood else noSatColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Map Visual Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F1E28))
                    .testTag("interactive_map_canvas")
            ) {
                // Procedural terrain grid & radar canvas
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines (lat/long grid simulation)
                    val gridSpacing = 32.dp.toPx()
                    var x = 0f
                    while (x < w) {
                        drawLine(
                            color = Color(0x1F4DD8EC),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = Color(0x1F4DD8EC),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }

                    // Stylized topographical road / trail vector curves
                    val roadPath = Path().apply {
                        moveTo(0f, h * 0.7f)
                        cubicTo(w * 0.3f, h * 0.6f, w * 0.5f, h * 0.85f, w, h * 0.4f)
                    }
                    drawPath(
                        path = roadPath,
                        color = Color(0x334DD8EC),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Secondary intersecting road
                    val secondaryRoad = Path().apply {
                        moveTo(w * 0.2f, 0f)
                        quadraticBezierTo(w * 0.45f, h * 0.5f, w * 0.6f, h)
                    }
                    drawPath(
                        path = secondaryRoad,
                        color = Color(0x2280DEEA),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val centerX = w / 2f
                    val centerY = h / 2f

                    if (hasGpsFix) {
                        // Dynamic Radar Pulse Rings
                        val maxPulseRadius = 60.dp.toPx()
                        val currentPulseRadius = maxPulseRadius * pulseRadiusFraction
                        val alpha = (1f - pulseRadiusFraction).coerceIn(0f, 1f)

                        drawCircle(
                            color = GpsTeal.copy(alpha = alpha * 0.45f),
                            radius = currentPulseRadius,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = GpsTeal.copy(alpha = alpha * 0.8f),
                            radius = currentPulseRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Pin core
                    drawCircle(
                        color = if (hasGpsFix) GpsTeal else noSatColor,
                        radius = 7.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                }

                // Overlay Map Details
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color(0xCC0A151C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = GpsTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasGpsFix) {
                            "${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}"
                        } else {
                            "Coordinates unavailable"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Coordinates & Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GpsMetricChip(
                    label = "Latitude",
                    value = if (hasGpsFix) String.format(Locale.US, "%.6f°", latitude) else "--"
                )
                GpsMetricChip(
                    label = "Longitude",
                    value = if (hasGpsFix) String.format(Locale.US, "%.6f°", longitude) else "--"
                )
                GpsMetricChip(
                    label = "Altitude",
                    value = if (hasGpsFix) "${String.format(Locale.US, "%.1f", altitude)} m" else "--"
                )
                GpsMetricChip(
                    label = "Speed",
                    value = if (hasGpsFix) "${String.format(Locale.US, "%.1f", speed)} km/h" else "--"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map launcher action
            ElevatedButton(
                onClick = {
                    openMapIntent(context, latitude, longitude)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_in_maps_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Station Location in Maps",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun GpsMetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
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

private fun openMapIntent(context: Context, lat: Double, lng: Double) {
    if (lat == 0.0 && lng == 0.0) {
        Toast.makeText(context, "Waiting for valid GPS coordinates fix from ESP32", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(ESP32+Air+Quality+Monitor)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        // Fallback to web browser maps
        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open map: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
