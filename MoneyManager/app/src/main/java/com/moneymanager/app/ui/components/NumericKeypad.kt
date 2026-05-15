package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onEvaluate: () -> Unit,
    accentColor: Color = Color.Unspecified,
    accentContainer: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val resolvedAccent = if (accentColor != Color.Unspecified) accentColor
                         else MaterialTheme.colorScheme.primary
    val resolvedContainer = if (accentContainer != Color.Unspecified) accentContainer
                            else MaterialTheme.colorScheme.primaryContainer

    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", "DEL", "+")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("/", "*", "-", "+")
                    val isDelete = key == "DEL"
                    val isClear = key == "C"

                    Button(
                        onClick = {
                            when (key) {
                                "DEL" -> onDeleteClick()
                                "C"   -> onClearClick()
                                else  -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = when {
                            isOperator -> ButtonDefaults.buttonColors(
                                containerColor = resolvedContainer.copy(alpha = 0.6f),
                                contentColor = resolvedAccent
                            )
                            isDelete || isClear -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    ) {
                        if (isDelete) {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete")
                        } else {
                            Text(
                                text = if (key == "*") "×" else key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onEvaluate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = resolvedAccent,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = "=",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}
