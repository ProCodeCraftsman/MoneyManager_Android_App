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
import androidx.fragment.app.FragmentActivity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.security.BiometricAuthManager
import com.moneymanager.data.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_PIN_LENGTH = 4
private const val MAX_ATTEMPTS = 5
private const val LOCKOUT_DURATION_MS = 30_000L

private sealed class LockState {
    data object Loading : LockState()
    data object SetupPin : LockState()
    data object ConfirmPin : LockState()
    data object UnlockPin : LockState()
    data object BiometricUnlock : LockState()
    data object Locked : LockState()
}

@Composable
fun AppLockScreen(
    preferencesManager: PreferencesManager,
    securityManager: SecurityManager,
    biometricAuthManager: BiometricAuthManager,
    activity: FragmentActivity,
    onUnlocked: () -> Unit
) {
    var state by remember { mutableStateOf<LockState>(LockState.Loading) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remainingAttempts by remember { mutableIntStateOf(MAX_ATTEMPTS) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var storedHash by remember { mutableStateOf<String?>(null) }
    var storedSalt by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember { biometricAuthManager.isAvailable() }

    fun unlockApp() {
        scope.launch {
            preferencesManager.resetWrongAttempts()
            onUnlocked()
        }
    }

    fun verifyPinInput(enteredPin: String) {
        val hash = storedHash ?: return
        val salt = storedSalt ?: return
        if (securityManager.verifyPin(enteredPin, hash, salt)) {
            unlockApp()
        } else {
            scope.launch {
                val attempts = (preferencesManager.wrongAttempts.first()) + 1
                preferencesManager.setWrongAttempts(attempts)
                remainingAttempts = MAX_ATTEMPTS - attempts
                if (remainingAttempts <= 0) {
                    lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                    state = LockState.Locked
                    errorMessage = "Too many attempts. Try again later."
                } else {
                    errorMessage = "Wrong PIN. $remainingAttempts attempts remaining."
                    pin = ""
                }
            }
        }
    }

    fun savePin(enteredPin: String) {
        scope.launch(Dispatchers.IO) {
            val (hash, salt) = securityManager.hashPin(enteredPin)
            preferencesManager.setPinHash(hash)
            preferencesManager.setPinSalt(salt)
            preferencesManager.setPinEnabled(true)
            preferencesManager.resetWrongAttempts()
            preferencesManager.setLastUnlockTime(System.currentTimeMillis())
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) {
        val pinEnabled = preferencesManager.pinEnabled.first()
        val biometricPrefValue = preferencesManager.biometricEnabled.first()
        biometricEnabled = biometricPrefValue

        if (!pinEnabled) {
            if (biometricPrefValue && biometricAvailable) {
                state = LockState.BiometricUnlock
            } else {
                onUnlocked()
            }
            return@LaunchedEffect
        }
        storedHash = preferencesManager.pinHash.first()
        storedSalt = preferencesManager.pinSalt.first()
        remainingAttempts = MAX_ATTEMPTS - (preferencesManager.wrongAttempts.first())

        if (storedHash == null || storedSalt == null) {
            state = LockState.SetupPin
        } else if (remainingAttempts <= 0) {
            lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            state = LockState.Locked
            errorMessage = "Too many attempts. Try again later."
        } else {
            state = LockState.UnlockPin
        }
    }

    LaunchedEffect(state) {
        if (biometricAvailable && (state == LockState.UnlockPin || state == LockState.BiometricUnlock)) {
            val bioEnabled = preferencesManager.biometricEnabled.first()
            if (bioEnabled) {
                biometricAuthManager.authenticate(
                    activity = activity,
                    onSuccess = { unlockApp() },
                    onFallback = { }
                )
            }
        }
    }

    LaunchedEffect(lockedUntil) {
        if (lockedUntil > 0L) {
            while (System.currentTimeMillis() < lockedUntil) {
                val remaining = lockedUntil - System.currentTimeMillis()
                errorMessage = "Locked. Try again in ${(remaining / 1000) + 1}s"
                delay(1000)
            }
            preferencesManager.resetWrongAttempts()
            remainingAttempts = MAX_ATTEMPTS
            lockedUntil = 0L
            errorMessage = null
            state = LockState.UnlockPin
        }
    }

    fun onDigitClick(digit: String) {
        if (pin.length >= MAX_PIN_LENGTH) return
        errorMessage = null
        pin += digit
        if (pin.length == MAX_PIN_LENGTH) {
            when (state) {
                LockState.SetupPin -> {
                    firstPin = pin
                    pin = ""
                    state = LockState.ConfirmPin
                }
                LockState.ConfirmPin -> {
                    if (pin == firstPin) {
                        savePin(pin)
                    } else {
                        errorMessage = "PINs don't match. Try again."
                        pin = ""
                        state = LockState.SetupPin
                    }
                }
                LockState.UnlockPin -> verifyPinInput(pin)
                else -> {}
            }
        }
    }

    fun onBackspaceClick() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    fun triggerBiometric() {
        if (isAuthenticating) return
        isAuthenticating = true
        biometricAuthManager.authenticate(
            activity = activity,
            onSuccess = {
                isAuthenticating = false
                unlockApp()
            },
            onFallback = {
                isAuthenticating = false
            }
        )
    }

    if (state == LockState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state == LockState.Locked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "App Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "Too many wrong attempts",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                if (biometricEnabled && biometricAvailable) {
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(onClick = ::triggerBiometric) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Biometrics")
                    }
                }
            }
        }
        return
    }

    if (state == LockState.BiometricUnlock) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unlock App",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use biometric to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            FilledTonalButton(
                onClick = ::triggerBiometric,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", modifier = Modifier.size(40.dp))
            }
        }
        return
    }

    val title = when (state) {
        LockState.SetupPin -> "Create PIN"
        LockState.ConfirmPin -> "Confirm PIN"
        LockState.UnlockPin -> "Enter PIN"
        LockState.BiometricUnlock -> "Unlock App"
        else -> ""
    }

    val subtitle = when (state) {
        LockState.SetupPin -> "Create a 4-digit PIN to secure your app"
        LockState.ConfirmPin -> "Re-enter your PIN to confirm"
        LockState.UnlockPin -> if (remainingAttempts < MAX_ATTEMPTS) {
            "$remainingAttempts attempts remaining"
        } else {
            "Enter your PIN to unlock"
        }
        LockState.BiometricUnlock -> "Use biometric to unlock"
        else -> ""
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

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(MAX_PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        PinKeypad(
            onDigitClick = ::onDigitClick,
            onBackspaceClick = ::onBackspaceClick,
            showBiometric = state == LockState.UnlockPin && biometricEnabled && biometricAvailable,
            onBiometricClick = ::triggerBiometric
        )
    }
}

@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    showBiometric: Boolean,
    onBiometricClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "back")
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (showBiometric) {
                                KeypadButton(onClick = onBiometricClick) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", modifier = Modifier.size(28.dp))
                                }
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        "back" -> KeypadButton(onClick = onBackspaceClick) {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(24.dp))
                        }
                        else -> KeypadButton(onClick = { onDigitClick(key) }) {
                            Text(text = key, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(onClick: () -> Unit, content: @Composable () -> Unit) {
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
