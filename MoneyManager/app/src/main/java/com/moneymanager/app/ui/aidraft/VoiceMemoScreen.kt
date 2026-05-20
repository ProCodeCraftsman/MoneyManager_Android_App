package com.moneymanager.app.ui.aidraft

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.app.ui.util.Permissions
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMemoScreen(
    viewModel: AiDraftViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToConfirm: (TransactionDraft) -> Unit,
) {
    val context = LocalContext.current

    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val isAiAvailable by viewModel.isAiAvailable.collectAsState()
    var transcription by rememberSaveable { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var hasRecorded by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var saveAsNote by rememberSaveable { mutableStateOf(true) }
    var audioPermissionGranted by remember { mutableStateOf(false) }
    var hasRequestedPermission by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rmsBuffer = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording && elapsedSeconds < 60) {
                delay(1000L)
                elapsedSeconds++
            }
            if (elapsedSeconds >= 60) {
                speechRecognizer?.stopListening()
                isRecording = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    fun startNewRecordingSession() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        transcription = ""
        hasRecorded = false
        elapsedSeconds = 0
        rmsBuffer.replaceAll { 0f }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isRecording = true }
            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                if (rmsBuffer.size >= 7) rmsBuffer.removeAt(0)
                rmsBuffer.add(normalized)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                transcription = matches?.firstOrNull() ?: ""
                isRecording = false
                hasRecorded = true
                rmsBuffer.replaceAll { 0f }
            }
            override fun onError(error: Int) {
                isRecording = false
                hasRecorded = true
                if (transcription.isEmpty()) transcription = ""
                rmsBuffer.replaceAll { 0f }
            }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedPermission = true
        audioPermissionGranted = granted
        if (granted) {
            startNewRecordingSession()
        }
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        isRecording = false
    }

    fun micButtonTapHandler() {
        if (isRecording) {
            stopRecording()
        } else {
            if (audioPermissionGranted) {
                startNewRecordingSession()
            } else {
                audioPermissionLauncher.launch(Permissions.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToDraft -> {
                    val draftWithNote = if (saveAsNote && transcription.isNotBlank()) {
                        event.draft.copy(note = transcription.trim())
                    } else {
                        event.draft
                    }
                    onNavigateToConfirm(draftWithNote)
                }
                is NavigationEvent.NavigateToCreated -> {
                    snackbarHostState.showSnackbar(
                        "✓ Transaction added${if (event.message.isNotEmpty()) " ($event.message)" else ""}",
                        duration = SnackbarDuration.Short
                    )
                    kotlinx.coroutines.delay(1500)
                    onNavigateBack()
                }
                is NavigationEvent.NavigateBack -> onNavigateBack()
                is NavigationEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    if (!audioPermissionGranted && hasRequestedPermission) {
        LaunchedEffect(Unit) {
            delay(2000)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Draft from Voice") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable { micButtonTapHandler() },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    val barHeights = rmsBuffer.mapIndexed { index, rms ->
                        animateFloatAsState(
                            targetValue = if (isRecording) rms else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "bar_$index"
                        ).value
                    }
                    Canvas(modifier = Modifier.size(60.dp, 48.dp)) {
                        val barWidthPx = 4.dp.toPx()
                        val barGapPx = 2.dp.toPx()
                        val minHeightPx = 8.dp.toPx()
                        val maxHeightPx = 48.dp.toPx()
                        val totalWidth = 7 * barWidthPx + 6 * barGapPx
                        val startX = (size.width - totalWidth) / 2f
                        barHeights.forEachIndexed { index, normalizedHeight ->
                            val barHeight = minHeightPx + normalizedHeight * (maxHeightPx - minHeightPx)
                            val x = startX + index * (barWidthPx + barGapPx)
                            val y = (size.height - barHeight) / 2f
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidthPx, barHeight),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                } else {
                    Icon(Icons.Default.Mic, contentDescription = "Tap to record", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            Text(
                text = "%d:%02d / 1:00".format(minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
                color = if (elapsedSeconds >= 55) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                !hasRecorded && !isRecording -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MicNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No voice memo recorded", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap the microphone and speak clearly to record a transaction description.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                isRecording -> {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        placeholder = { Text("Listening...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        minLines = 3
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = transcription,
                        onValueChange = { transcription = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Transcription") },
                        placeholder = { Text("No speech detected. Tap Re-record or type manually.") },
                        enabled = !uiState.isGenerating,
                        minLines = 3
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasRecorded) {
                    OutlinedButton(
                        onClick = {
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                            transcription = ""
                            hasRecorded = false
                            elapsedSeconds = 0
                            rmsBuffer.replaceAll { 0f }
                        },
                        enabled = !uiState.isGenerating && !isRecording
                    ) { Text("Re-record") }
                }
                if (isAiAvailable) {
                    var dotCount by remember { mutableStateOf(0) }
                    LaunchedEffect(uiState.isGenerating) {
                        if (uiState.isGenerating) {
                            dotCount = 0
                            while (true) {
                                delay(500)
                                dotCount = (dotCount + 1) % 4
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.quickAddFromVoice(transcription.trim(), saveAsNote) },
                        enabled = transcription.isNotBlank() && !uiState.isGenerating && !isRecording
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("Quick Add")
                    }

                    Button(
                        onClick = { viewModel.generateDraft(transcription.trim(), "VOICE") },
                        enabled = transcription.isNotBlank() && !uiState.isGenerating && !isRecording
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("AI Fill")
                    }

                    if (uiState.isGenerating) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${uiState.generatingStep ?: "Processing"}${".".repeat(dotCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (hasRecorded && transcription.isNotBlank()) {
                    Button(
                        onClick = {
                            val draft = TransactionDraft(
                                note = if (saveAsNote) transcription.trim() else null,
                                sourceType = "VOICE"
                            )
                            onNavigateToConfirm(draft)
                        },
                        enabled = !isRecording
                    ) { Text("Use as Note") }
                }
            }

            if (!audioPermissionGranted && hasRequestedPermission) {
                Text(
                    "Microphone permission is required to record a voice memo. Please enable it in app settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save transcription as note", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Switch(checked = saveAsNote, onCheckedChange = { saveAsNote = it }, enabled = !uiState.isGenerating)
            }
        }
    }
}
