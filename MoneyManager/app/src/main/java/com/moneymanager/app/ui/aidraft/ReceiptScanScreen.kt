package com.moneymanager.app.ui.aidraft

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.app.ui.util.Permissions
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    viewModel: AiDraftViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToConfirm: (TransactionDraft) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAiAvailable by viewModel.isAiAvailable.collectAsState()
    val modelSupportsVision by viewModel.modelSupportsVision.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrFailureCount by remember { mutableStateOf(0) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun triggerOcr(uri: Uri) {
        coroutineScope.launch {
            isOcrProcessing = true
            ocrText = null
            try {
                val recognizedText = withContext(Dispatchers.IO) {
                    val inputImage = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = Tasks.await(recognizer.process(inputImage))
                    result.text
                }
                val normalized = normalizeOcrText(recognizedText)
                ocrText = normalized
                ocrFailureCount = 0
            } catch (_: Exception) {
                ocrFailureCount++
            } finally {
                isOcrProcessing = false
            }
        }
    }

    fun handleImageCaptured(uri: Uri, path: String?) {
        if (modelSupportsVision) {
            coroutineScope.launch {
                isOcrProcessing = true
                try {
                    val bytes = withContext(Dispatchers.IO) { getBitmapBytes(context, uri) }
                    if (bytes != null) {
                        viewModel.generateDraftFromImage(bytes, "RECEIPT", path)
                    } else {
                        triggerOcr(uri)
                    }
                } finally {
                    isOcrProcessing = false
                }
            }
        } else {
            triggerOcr(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            imageUri = pendingCameraUri
            val path = FileHelper.saveReceipt(context, pendingCameraUri!!)
            receiptPath = path
            handleImageCaptured(pendingCameraUri!!, path)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) {
            pendingCameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val path = FileHelper.saveReceipt(context, it)
            receiptPath = path
            handleImageCaptured(it, path)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToDraft -> onNavigateToConfirm(event.draft)
                is NavigationEvent.NavigateToCreated -> {
                    snackbarHostState.showSnackbar("Transaction created", duration = SnackbarDuration.Short)
                    delay(1000)
                    onNavigateBack()
                }
                NavigationEvent.NavigateBack -> onNavigateBack()
                is NavigationEvent.ShowError -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Draft from Receipt") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Receipt image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No receipt captured", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Take a photo of your receipt or select one from your gallery to begin.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cameraPermissionGranted || imageUri == null) {
                    OutlinedButton(
                        onClick = {
                            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            pendingCameraUri = uri
                            if (cameraPermissionGranted) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Permissions.CAMERA)
                            }
                        },
                        enabled = !isOcrProcessing && !uiState.isGenerating
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Take Photo")
                    }
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !isOcrProcessing && !uiState.isGenerating
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Gallery")
                }
            }

            if (!cameraPermissionGranted && imageUri != null) {
                Text(
                    "Camera unavailable. Use gallery instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                isOcrProcessing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Processing...")
                    }
                }
                ocrFailureCount == 1 -> {
                    Column {
                        Text(
                            "Could not read text from this image. Tap Retry to try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { imageUri?.let { triggerOcr(it) } }) {
                            Text("Retry")
                        }
                    }
                }
                ocrFailureCount >= 2 -> {
                    Text(
                        "OCR failed again. You can enter the details manually from the visible text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                ocrText != null -> {
                    Column {
                        Text("OCR Text", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .verticalScroll(rememberScrollState())
                        ) {
                            SelectionContainer {
                                Text(
                                    ocrText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

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

            if (isAiAvailable) {
                if (modelSupportsVision) {
                    // Vision path: image is sent directly to LLM — no OCR text needed.
                    // handleImageCaptured() auto-triggers on capture; this button lets the user retry.
                    Button(
                        onClick = {
                            imageUri?.let { uri ->
                                coroutineScope.launch {
                                    val bytes = withContext(Dispatchers.IO) { getBitmapBytes(context, uri) }
                                    bytes?.let { viewModel.generateDraftFromImage(it, "RECEIPT", receiptPath) }
                                }
                            }
                        },
                        enabled = imageUri != null && !uiState.isGenerating && !isOcrProcessing,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("AI Vision Fill")
                    }
                } else {
                    Button(
                        onClick = { ocrText?.let { viewModel.generateDraft(it, "RECEIPT", attachmentPath = receiptPath) } },
                        enabled = ocrText != null && !uiState.isGenerating && !isOcrProcessing,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("AI Fill")
                    }
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
            }
        }
    }
}

private fun getBitmapBytes(context: android.content.Context, uri: Uri): ByteArray? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        // Scale down to max 2048px on longest edge to keep memory and token usage reasonable
        val scaled = scaleBitmapToMaxDim(bitmap, 2048)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        baos.toByteArray()
    } catch (_: Exception) {
        null
    }
}

private fun scaleBitmapToMaxDim(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val scale = maxDim.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
}

private fun normalizeOcrText(raw: String): String {
    var result = raw
    result = result.replace(Regex("Rs\\.|Rs |INR |rs\\.|₹ |RS\\.|₹", RegexOption.IGNORE_CASE), "₹")
    result = result.replace(Regex("(?<=[0-9])[Oo](?=[0-9])"), "0")
    result = result.replace(Regex("(?<=[0-9])[lI](?=[0-9])"), "1")
    result = result.replace(Regex("(?<=[0-9])\\.(?=[0-9]{3}[^0-9]|$)"), ",")
    val lines = result.lines().filterNot { line ->
        val trimmed = line.trim()
        trimmed.equals("thank you", ignoreCase = true) ||
        trimmed.equals("thank you for shopping", ignoreCase = true) ||
        trimmed.equals("customer copy", ignoreCase = true) ||
        trimmed.equals("merchant copy", ignoreCase = true) ||
        trimmed.equals("original", ignoreCase = true) ||
        trimmed.equals("duplicate", ignoreCase = true) ||
        trimmed.matches(Regex("^[-*=\\s]+$"))
    }
    return lines.joinToString("\n").trim()
}
