@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.tenstep.alarm.ui.ringing

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tenstep.alarm.BuildConfig
import com.tenstep.alarm.R
import com.tenstep.alarm.alarm.MathChallenge
import com.tenstep.alarm.alarm.StepMode
import com.tenstep.alarm.data.ChallengeType
import java.util.concurrent.Executors

@Composable
fun RingingScreen(
    viewModel: RingingViewModel,
    onClose: () -> Unit
) {
    val alarm by viewModel.alarm.collectAsStateWithLifecycle()
    val isSnooze by viewModel.isSnooze.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val stepMode by viewModel.stepMode.collectAsStateWithLifecycle()
    val stepTarget by viewModel.stepTarget.collectAsStateWithLifecycle()
    val shakes by viewModel.shakes.collectAsStateWithLifecycle()
    val shakeTarget by viewModel.shakeTarget.collectAsStateWithLifecycle()
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsStateWithLifecycle()
    val debugSimulate by viewModel.debugSimulateSteps.collectAsStateWithLifecycle()
    val mathQuestion by viewModel.mathQuestion.collectAsStateWithLifecycle()
    val mathSolved by viewModel.mathSolved.collectAsStateWithLifecycle()
    val qrScanned by viewModel.qrScanned.collectAsStateWithLifecycle()

    LaunchedEffect(alarm) {
        if (alarm == null) onClose()
    }

    val current = alarm ?: return
    val satisfied = viewModel.challengeSatisfied()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                isSnooze -> stringResource(R.string.ringing_snooze_title)
                current.label.isNotBlank() -> current.label
                else -> stringResource(R.string.ringing_title)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.time_format, current.hour, current.minute),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(24.dp))

        when (current.challengeType) {
            ChallengeType.MATH -> MathChallengePanel(
                question = mathQuestion,
                solved = mathSolved,
                onAnswer = viewModel::onMathAnswer
            )
            ChallengeType.SHAKE -> ShakeChallengePanel(
                shakes = shakes,
                target = shakeTarget,
                mode = stepMode,
                satisfied = satisfied
            )
            ChallengeType.QR -> QrChallengePanel(
                scanned = qrScanned,
                onScanned = viewModel::onQrScanned,
                steps = steps,
                target = stepTarget,
                mode = stepMode,
                satisfied = satisfied
            )
            ChallengeType.STEPS -> StepsChallengePanel(
                steps = steps,
                target = stepTarget,
                mode = stepMode,
                satisfied = satisfied
            )
        }

        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (current.snoozeEnabled) {
                OutlinedButton(
                    onClick = {
                        viewModel.snooze()
                        onClose()
                    }
                ) {
                    Text(stringResource(R.string.snooze_action_min, snoozeMinutes))
                }
                Spacer(Modifier.width(16.dp))
            }
            Button(
                onClick = {
                    viewModel.dismiss()
                    onClose()
                },
                enabled = satisfied,
                modifier = Modifier.testTag("dismiss_button")
            ) {
                Text(stringResource(R.string.dismiss_action))
            }
        }

        if (BuildConfig.DEBUG && debugSimulate) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.simulateSteps(10) }) {
                Text(stringResource(R.string.simulate_progress))
            }
        }
    }
}

@Composable
private fun StepsChallengePanel(
    steps: Int,
    target: Int,
    mode: StepMode,
    satisfied: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Prominent live counter: ticks by 1 immediately on every step.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = steps.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (satisfied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "/ $target",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        StepsContent(steps = steps, target = target, mode = mode, satisfied = satisfied)
    }
}

@Composable
private fun StepsContent(
    steps: Int,
    target: Int,
    mode: StepMode,
    satisfied: Boolean
) {
    val remaining = (target - steps).coerceAtLeast(0)
    val progress = (steps.toFloat() / target).coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            when (mode) {
                StepMode.ACCELEROMETER -> {
                    Text(
                        text = stringResource(R.string.step_mode_accelerometer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(6.dp))
                }
                StepMode.UNAVAILABLE -> {
                    Text(
                        text = stringResource(R.string.step_mode_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(6.dp))
                }
                StepMode.STEP_COUNTER -> Unit
            }
            Text(
                text = if (satisfied) {
                    stringResource(R.string.steps_done)
                } else {
                    stringResource(R.string.step_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (satisfied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (!satisfied && mode != StepMode.UNAVAILABLE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.steps_remaining, remaining),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ShakeChallengePanel(
    shakes: Int,
    target: Int,
    mode: StepMode,
    satisfied: Boolean
) {
    val remaining = (target - shakes).coerceAtLeast(0)
    val progress = (shakes.toFloat() / target).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = shakes.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (satisfied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "/ $target",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                when (mode) {
                    StepMode.UNAVAILABLE -> {
                        Text(
                            text = stringResource(R.string.shake_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    else -> Unit
                }
                Text(
                    text = if (satisfied) {
                        stringResource(R.string.steps_done)
                    } else {
                        stringResource(R.string.shake_hint, remaining)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (satisfied) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun MathChallengePanel(
    question: MathChallenge.Problem?,
    solved: Boolean,
    onAnswer: (Int) -> Boolean
) {
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (solved) {
                Text(
                    text = stringResource(R.string.steps_done),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (question != null) {
                Text(
                    text = stringResource(R.string.math_question_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${question.text()} = ?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter(Char::isDigit).take(4)
                        wrong = false
                    },
                    singleLine = true,
                    isError = wrong,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                if (wrong) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.math_wrong),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val answer = input.toIntOrNull()
                    if (answer == null) {
                        wrong = true
                    } else if (!onAnswer(answer)) {
                        wrong = true
                    } else {
                        input = ""
                        wrong = false
                    }
                }) {
                    Text(stringResource(R.string.math_submit))
                }
            } else {
                Text(
                    text = stringResource(R.string.challenge_preparing),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun QrChallengePanel(
    scanned: Boolean,
    onScanned: () -> Unit,
    steps: Int,
    target: Int,
    mode: StepMode,
    satisfied: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (scanned) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.qr_scanned),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.qr_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    QrScanner(onScanned = onScanned)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.qr_fallback_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(8.dp))
        }
        // Step counting also dismisses QR alarms (fallback when the camera is
        // unavailable or the user prefers to walk instead).
        StepsChallengePanel(steps = steps, target = target, mode = mode, satisfied = satisfied)
    }
}

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScanner(
    onScanned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!cameraGranted) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.qr_camera_permission_needed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.qr_request_camera))
            }
        }
        return
    }

    val scanner = remember { BarcodeScanning.getClient() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(cameraGranted) {
        if (!cameraGranted) return@DisposableEffect onDispose {}
        onDispose {
            scanner.close()
            cameraProvider?.unbindAll()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            var detected = false
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            if (!detected && barcodes.isNotEmpty()) {
                                detected = true
                                onScanned()
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}