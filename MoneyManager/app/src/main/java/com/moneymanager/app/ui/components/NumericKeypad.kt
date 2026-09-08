package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    saveButtonText: String? = null,
    saveButtonEnabled: Boolean = true,
    onSaveClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedAccent = if (accentColor != Color.Unspecified) accentColor
                         else MaterialTheme.colorScheme.primary

    val darkRedOperatorContainer = resolvedAccent.copy(alpha = 0.35f)
    val numberButtonBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "DEL", "+")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Grid Rows 1 - 4
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("/", "*", "-", "+")
                    val isDelete = key == "DEL"

                    Button(
                        onClick = {
                            when (key) {
                                "DEL" -> onDeleteClick()
                                else  -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = when {
                            isOperator -> ButtonDefaults.buttonColors(
                                containerColor = darkRedOperatorContainer,
                                contentColor = Color.White
                            )
                            isDelete -> ButtonDefaults.buttonColors(
                                containerColor = numberButtonBg,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            else -> ButtonDefaults.buttonColors(
                                containerColor = numberButtonBg,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    ) {
                        if (isDelete) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (key == "*") "x" else key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }

        // Row 5: Clear (C) and Evaluate (=)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onClearClick,
                modifier = Modifier
                    .weight(3f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = numberButtonBg,
                    contentColor = resolvedAccent
                )
            ) {
                Text(
                    text = "C",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = resolvedAccent
                )
            }

            Button(
                onClick = onEvaluate,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = darkRedOperatorContainer,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "=",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }

        // Row 6: Cancel and Save Action Buttons
        if (onSaveClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCancelClick != null) {
                    Button(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }

                Button(
                    onClick = onSaveClick,
                    enabled = saveButtonEnabled,
                    modifier = Modifier
                        .weight(if (onCancelClick != null) 1.5f else 1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = resolvedAccent,
                        contentColor = Color.White,
                        disabledContainerColor = resolvedAccent.copy(alpha = 0.38f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = saveButtonText ?: "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
