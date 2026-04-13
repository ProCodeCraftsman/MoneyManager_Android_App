package com.moneymanager.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PinEntryMode {
    Enter,
    Confirm,
    Setup
}

@Composable
fun PinLockScreen(
    isSetup: Boolean = false,
    isConfirming: Boolean = false,
    storedPinHash: String? = null,
    storedPinSalt: String? = null,
    remainingAttempts: Int = 5,
    onPinEntered: (String) -> Unit,
    onPinSetupComplete: (String) -> Unit,
    onBiometricRequested: (() -> Unit)? = null,
    showBiometricButton: Boolean = false,
    biometricStatus: String = ""
) {
    var pin by remember { mutableStateOf("") }
    val maxPinLength = 4
    
    val title = when {
        isSetup && !isConfirming -> "Create PIN"
        isSetup && isConfirming -> "Confirm PIN"
        else -> "Enter PIN"
    }
    
    val subtitle = when {
        isSetup && !isConfirming -> "Create a 4-digit PIN to secure your app"
        isSetup && isConfirming -> "Re-enter your PIN to confirm"
        remainingAttempts < 5 -> "$remainingAttempts attempts remaining"
        else -> "Enter your PIN to unlock"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(maxPinLength) { index ->
                PinDot(filled = index < pin.length)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PinKeypad(
            onNumberClick = { number ->
                if (pin.length < maxPinLength) {
                    pin += number
                    if (pin.length == maxPinLength) {
                        if (isSetup && !isConfirming) {
                            onPinSetupComplete(pin)
                        } else if (isSetup && isConfirming) {
                            onPinEntered(pin)
                        } else {
                            onPinEntered(pin)
                        }
                        pin = ""
                    }
                }
            },
            onBackspaceClick = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            },
            onBiometricClick = if (showBiometricButton && onBiometricRequested != null) {
                { onBiometricRequested.invoke() }
            } else null
        )
        
        if (showBiometricButton && biometricStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = biometricStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

@Composable
private fun PinKeypad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)?
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "back")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (onBiometricClick != null) {
                                KeypadButton(
                                    onClick = onBiometricClick,
                                    content = {
                                        Icon(
                                            Icons.Default.Fingerprint,
                                            contentDescription = "Biometric",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        "back" -> {
                            KeypadButton(
                                onClick = onBackspaceClick,
                                content = {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            KeypadButton(
                                onClick = { onNumberClick(key) },
                                content = {
                                    Text(
                                        text = key,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
