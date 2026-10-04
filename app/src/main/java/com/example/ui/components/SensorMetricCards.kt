package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DustAmber
import com.example.ui.theme.GasPurple
import com.example.ui.theme.HumidityBlue
import com.example.ui.theme.TempWarm
import java.util.Locale

@Composable
fun SensorMetricsGrid(
    temperature: Double,
    humidity: Double,
    gasValue: Int,
    dustDensity: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Temperature Card
            MetricCard(
                title = "Temperature",
                value = String.format(Locale.US, "%.1f", temperature),
                unit = "°C",
                icon = Icons.Default.Thermostat,
                accentColor = TempWarm,
                subtitle = when {
                    temperature < 18.0 -> "Cool"
                    temperature > 30.0 -> "Warm"
                    else -> "Comfortable"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("temp_metric_card")
            )

            // Humidity Card
            MetricCard(
                title = "Humidity",
                value = String.format(Locale.US, "%.1f", humidity),
                unit = "%",
                icon = Icons.Default.WaterDrop,
                accentColor = HumidityBlue,
                subtitle = when {
                    humidity < 35.0 -> "Dry air"
                    humidity > 70.0 -> "Humid"
                    else -> "Optimal"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("humidity_metric_card")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MQ135 Gas Value
            MetricCard(
                title = "MQ135 Gas",
                value = gasValue.toString(),
                unit = "ADC",
                icon = Icons.Default.Co2,
                accentColor = GasPurple,
                subtitle = when {
                    gasValue < 300 -> "Clean air"
                    gasValue < 600 -> "Moderate gas"
                    else -> "High volatile gas"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gas_metric_card")
            )

            // GP2Y1010 Dust Density
            MetricCard(
                title = "Dust Density",
                value = String.format(Locale.US, "%.1f", dustDensity),
                unit = "µg/m³",
                icon = Icons.Default.Air,
                accentColor = DustAmber,
                subtitle = when {
                    dustDensity < 30.0 -> "Low dust"
                    dustDensity < 75.0 -> "Moderate dust"
                    else -> "Dense particulates"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dust_metric_card")
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}
