package com.moneymanager.app.ui.insights.status

import java.util.Locale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusPaneFigure(
    label: String,
    value: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Value with currency symbol
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = currency,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = String.format(Locale.US, "%.2f", value),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
