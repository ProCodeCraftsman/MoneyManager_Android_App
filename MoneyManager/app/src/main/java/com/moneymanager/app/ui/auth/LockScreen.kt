package com.moneymanager.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.security.BiometricAuthManager
import com.moneymanager.data.security.BiometricResult
import com.moneymanager.data.security.BiometricStatus
import com.moneymanager.data.security.SecurityManager

@Composable
fun LockScreen(
    preferencesManager: PreferencesManager,
    securityManager: SecurityManager,
    biometricAuthManager: BiometricAuthManager,
    activity: FragmentActivity,
    onUnlocked: @Composable () -> Unit,
    onSetupRequired: @Composable () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var pinHash by remember { mutableStateOf<String?>(null) }
    var pinSalt by remember { mutableStateOf<String?>(null) }
    var isPinSetup by remember { mutableStateOf(false) }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var firstPinEntry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        preferencesManager.pinEnabled.collect { enabled ->
            if (!enabled) {
                isAuthenticated = true
                isLoading = false
            } else {
                pinHash = preferencesManager.pinHash.first()
                pinSalt = preferencesManager.pinSalt.first()
                biometricEnabled = preferencesManager.biometricEnabled.first()
                isPinSetup = pinHash != null && pinSalt != null
                isLoading = false
                
                if (!isPinSetup) {
                    isAuthenticated = false
                } else if (biometricEnabled && biometricAuthManager.checkAvailability()) {
                    triggerBiometric()
                }
            }
        }
    }

    val biometricAvailable = biometricAuthManager.isAvailable.collectAsState()
    val showBiometric = biometricEnabled && biometricAvailable.value

    val biometricStatus = when (biometricAuthManager.getBiometricStatus()) {
        BiometricStatus.Available -> "Biometric available"
        BiometricStatus.NotEnrolled -> "No biometrics enrolled"
        BiometricStatus.NoHardware -> "No biometric hardware"
        BiometricStatus.HardwareUnavailable -> "Biometric unavailable"
        BiometricStatus.Unknown -> ""
    }

    fun triggerBiometric() {
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock MoneyManager",
            subtitle = "Use your fingerprint",
            negativeButtonText = "Use PIN"
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    securityManager.resetWrongAttempts()
                    isAuthenticated = true
                    errorMessage = null
                }
                is BiometricResult.Error -> {
                    errorMessage = result.message
                }
                is BiometricResult.Cancelled -> {
                    // User wants to use PIN
                }
                is BiometricResult.NotAvailable -> {
                    // Biometric not available
                }
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (isAuthenticated) {
        onUnlocked()
    } else if (!isPinSetup) {
        onSetupRequired()
    } else {
        PinLockScreen(
            isSetup = true,
            isConfirming = isConfirmingPin,
            storedPinHash = pinHash,
            storedPinSalt = pinSalt,
            remainingAttempts = securityManager.getRemainingAttempts(),
            onPinEntered = { enteredPin ->
                val storedHash = pinHash ?: return@PinLockScreen
                val storedSalt = pinSalt ?: return@PinLockScreen
                
                if (securityManager.verifyPin(enteredPin, storedHash, storedSalt)) {
                    securityManager.resetWrongAttempts()
                    isAuthenticated = true
                    errorMessage = null
                } else {
                    val remaining = securityManager.recordWrongAttempt()
                    errorMessage = if (remaining > 0) {
                        "Wrong PIN. $remaining attempts remaining."
                    } else {
                        "Too many attempts. Reset required."
                    }
                }
            },
            onPinSetupComplete = { pin ->
                if (!isConfirmingPin) {
                    firstPinEntry = pin
                    isConfirmingPin = true
                } else {
                    if (pin == firstPinEntry) {
                        val (hash, salt) = securityManager.hashPin(pin)
                        scope.launch(Dispatchers.IO) {
                            preferencesManager.setPinHash(hash)
                            preferencesManager.setPinSalt(salt)
                            preferencesManager.setPinEnabled(true)
                        }
                        isAuthenticated = true
                        isConfirmingPin = false
                        firstPinEntry = ""
                    } else {
                        errorMessage = "PINs don't match. Try again."
                        isConfirmingPin = false
                        firstPinEntry = ""
                    }
                }
            },
            onBiometricRequested = if (showBiometric) {
                { triggerBiometric() }
            } else null,
            showBiometricButton = showBiometric,
            biometricStatus = biometricStatus
        )
    }
}
