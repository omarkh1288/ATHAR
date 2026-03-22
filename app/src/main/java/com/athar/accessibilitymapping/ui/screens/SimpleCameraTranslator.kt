package com.athar.accessibilitymapping.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiInterpretEgyptianSignRequest
import com.athar.accessibilitymapping.data.ApiSignLandmark
import com.athar.accessibilitymapping.data.ApiSignObservation
import com.athar.accessibilitymapping.data.BackendApiClient
import com.athar.accessibilitymapping.ui.theme.*
import com.composables.icons.lucide.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.UUID

private const val AnalysisCooldownMs = 250L
private const val StableDetectionsRequired = 2
private const val TranscriptCooldownMs = 1800L

@Composable
fun SimpleCameraTranslator(onBack: () -> Unit) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val apiClient = remember(context) { BackendApiClient(context.applicationContext) }
  val interpretationSessionId = rememberSaveable { UUID.randomUUID().toString() }
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var isDetecting by rememberSaveable { mutableStateOf(false) }
  var statusText by remember { mutableStateOf("Allow camera access to start live recognition.") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var visibleHandCount by remember { mutableIntStateOf(0) }
  var currentDetection by remember { mutableStateOf<GestureDetectionEntry?>(null) }
  var pendingLabel by remember { mutableStateOf<String?>(null) }
  var pendingFrames by remember { mutableIntStateOf(0) }
  var lastAcceptedLabel by remember { mutableStateOf<String?>(null) }
  var lastAcceptedAt by remember { mutableLongStateOf(0L) }
  var volunteerArabicSentence by remember { mutableStateOf<String?>(null) }
  var volunteerEnglishSentence by remember { mutableStateOf<String?>(null) }
  var interpreterMode by remember { mutableStateOf<String?>(null) }
  var interpreterNotes by remember { mutableStateOf<List<String>>(emptyList()) }
  var interpreterPending by remember { mutableStateOf(false) }
  var interpreterError by remember { mutableStateOf<String?>(null) }
  val transcript = remember { mutableStateListOf<GestureDetectionEntry>() }

  val englishTranscript by remember {
    derivedStateOf { transcript.joinToString(" ") { it.translation.english } }
  }
  val arabicTranscript by remember {
    derivedStateOf { transcript.joinToString(" ") { it.translation.arabic } }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
    if (granted) {
      statusText = "Camera ready. Start detection to analyze supported gestures."
    } else {
      isDetecting = false
      statusText = "Camera permission is required for live gesture recognition."
    }
    errorText = null
  }

  fun reset(clearTranscript: Boolean) {
    if (clearTranscript) {
      transcript.clear()
      lastAcceptedLabel = null
      lastAcceptedAt = 0L
      volunteerArabicSentence = null
      volunteerEnglishSentence = null
      interpreterMode = null
      interpreterNotes = emptyList()
      interpreterError = null
    }
    currentDetection = null
    pendingLabel = null
    pendingFrames = 0
    visibleHandCount = 0
    interpreterPending = false
  }

  fun handleFrame(result: GestureFrameResult) {
    visibleHandCount = result.handCount
    if (result.errorMessage != null) {
      errorText = result.errorMessage
      statusText = result.errorMessage
      return
    }

    errorText = null
    if (result.handCount == 0) {
      currentDetection = null
      pendingLabel = null
      pendingFrames = 0
      statusText = "No hand detected. Keep one hand centered and well lit."
      return
    }

    val translation = result.translation
    if (translation == null) {
      pendingLabel = null
      pendingFrames = 0
      statusText = if (result.rawLabel != null) {
        "Detected ${result.rawLabel}, but it is outside the bilingual map."
      } else {
        "Hand detected. Try one of the supported gestures below."
      }
      return
    }

    if (result.confidencePercent < MinimumAcceptedConfidence) {
      pendingLabel = null
      pendingFrames = 0
      statusText = "Low confidence ${result.confidencePercent}%. Hold the gesture steady."
      return
    }

    if (pendingLabel == translation.modelLabel) pendingFrames += 1
    else {
      pendingLabel = translation.modelLabel
      pendingFrames = 1
    }
    if (pendingFrames < StableDetectionsRequired) {
      statusText = "Confirming ${translation.english}. Hold still for a moment."
      return
    }

    val detection = GestureDetectionEntry(
      translation = translation,
      confidencePercent = result.confidencePercent,
      rawLabel = result.rawLabel ?: translation.modelLabel,
      landmarks = result.landmarks,
      detectedAtMillis = result.analyzedAtMillis
    )
    currentDetection = detection
    statusText = "Detected ${translation.english} at ${result.confidencePercent}% confidence."

    val isNewEntry = translation.modelLabel != lastAcceptedLabel ||
      result.analyzedAtMillis - lastAcceptedAt >= TranscriptCooldownMs
    if (!isNewEntry) return

    if (transcript.size >= 40) transcript.removeAt(0)
    transcript.add(detection)
    lastAcceptedLabel = translation.modelLabel
    lastAcceptedAt = result.analyzedAtMillis
  }

  LaunchedEffect(hasCameraPermission) {
    if (!hasCameraPermission) {
      reset(clearTranscript = false)
      isDetecting = false
    }
  }

  LaunchedEffect(isDetecting) {
    if (isDetecting) {
      statusText = "Listening for a supported gesture."
      errorText = null
    } else if (hasCameraPermission) {
      reset(clearTranscript = false)
      statusText = "Camera ready. Start detection to analyze supported gestures."
      errorText = null
    }
  }

  LaunchedEffect(transcript.size, transcript.lastOrNull()?.detectedAtMillis) {
    val observations = transcript.takeLast(8)
    if (observations.isEmpty()) {
      volunteerArabicSentence = null
      volunteerEnglishSentence = null
      interpreterMode = null
      interpreterNotes = emptyList()
      interpreterError = null
      interpreterPending = false
      return@LaunchedEffect
    }

    interpreterPending = true
    interpreterError = null
    val request = ApiInterpretEgyptianSignRequest(
      sessionId = interpretationSessionId,
      observations = observations.map { detection ->
        ApiSignObservation(
          timestampMs = detection.detectedAtMillis,
          gestureLabel = detection.rawLabel,
          localEnglish = detection.translation.english,
          localArabic = detection.translation.arabic,
          confidencePercent = detection.confidencePercent,
          landmarks = detection.landmarks.map { landmark ->
            ApiSignLandmark(
              index = landmark.index,
              x = landmark.x,
              y = landmark.y,
              z = landmark.z
            )
          }
        )
      }
    )

    when (val response = apiClient.interpretEgyptianSign(request)) {
      is ApiCallResult.Success -> {
        volunteerArabicSentence = response.data.arabicSentence
        volunteerEnglishSentence = response.data.englishSentence
        interpreterMode = response.data.mode
        interpreterNotes = response.data.notes
        interpreterError = null
      }
      is ApiCallResult.Failure -> {
        volunteerArabicSentence = observations.joinToString(" ") { it.translation.arabic }
        volunteerEnglishSentence = observations.joinToString(" ") { it.translation.english }
        interpreterMode = "local_fallback"
        interpreterNotes = listOf("Backend unavailable. Showing the local transcript only.")
        interpreterError = response.message
      }
    }
    interpreterPending = false
  }

  Column(Modifier.fillMaxSize().background(BluePrimary)) {
    Box(
      Modifier.fillMaxWidth().background(NavyPrimary).statusBarsPadding().padding(16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onBack) { Icon(Lucide.ArrowLeft, "Back", tint = Color.White) }
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.Hand, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("Sign Language Translator", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }
          Text(
            "Live camera gestures with English and Arabic output",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp
          )
        }
      }
    }

    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Gray200)
      ) {
        Column {
          Box(
            modifier = Modifier.fillMaxWidth().height(420.dp).background(
              Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF172554), Color.Black))
            )
          ) {
            LiveGestureCameraPreview(
              modifier = Modifier.fillMaxSize(),
              hasCameraPermission = hasCameraPermission,
              isAnalyzing = isDetecting,
              onFrameAnalyzed = ::handleFrame
            )
            GridOverlay()

            if (!hasCameraPermission) {
              CenterOverlay(
                title = "Camera permission required",
                message = "Enable camera access to analyze live gestures.",
                buttonText = "Grant Permission",
                onButtonClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
              )
            } else if (errorText != null) {
              CenterOverlay(title = "Recognizer error", message = errorText ?: "", buttonText = null, onButtonClick = null)
            } else if (!isDetecting) {
              CenterOverlay(title = "Camera ready", message = "Tap start detection to begin live recognition.", buttonText = null, onButtonClick = null)
            } else if (currentDetection == null) {
              CenterOverlay(title = "Listening", message = "Show one supported gesture with one hand.", buttonText = null, onButtonClick = null)
            }

            if (isDetecting && currentDetection != null) {
              DetectionOverlay(currentDetection ?: return@Column)
            }

            Surface(
              modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
              shape = RoundedCornerShape(999.dp),
              color = if (isDetecting) ErrorRed.copy(alpha = 0.95f) else NavyPrimary.copy(alpha = 0.92f)
            ) {
              Text(
                if (isDetecting) "LIVE" else "READY",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }

            if (visibleHandCount > 0) {
              Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.16f)
              ) {
                Text(
                  if (visibleHandCount == 1) "1 hand" else "$visibleHandCount hands",
                  color = Color.White,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
              }
            }
          }

          Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = {
                if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                else isDetecting = !isDetecting
              },
              modifier = Modifier.weight(1f).height(56.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = when {
                  !hasCameraPermission -> NavyPrimary
                  isDetecting -> ErrorRed
                  else -> AccentGold
                }
              ),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(
                imageVector = when {
                  !hasCameraPermission -> Lucide.Camera
                  isDetecting -> Lucide.CircleStop
                  else -> Lucide.Play
                },
                contentDescription = null
              )
              Spacer(Modifier.width(8.dp))
              Text(
                when {
                  !hasCameraPermission -> "Enable Camera"
                  isDetecting -> "Stop Detection"
                  else -> "Start Detection"
                },
                fontWeight = FontWeight.Bold
              )
            }

            IconButton(
              onClick = {
                reset(clearTranscript = true)
                errorText = null
                statusText = if (hasCameraPermission) {
                  "Transcript cleared. Start detection to analyze supported gestures."
                } else {
                  "Allow camera access to start live recognition."
                }
              },
              modifier = Modifier.size(56.dp).background(BlueSecondary, RoundedCornerShape(16.dp))
            ) {
              Icon(Lucide.RotateCcw, "Clear", tint = NavyPrimary)
            }
          }

          Row(
            Modifier.fillMaxWidth().background(if (errorText != null) ErrorRed.copy(alpha = 0.08f) else BluePrimary)
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              Modifier.size(10.dp).clip(CircleShape).background(if (errorText != null) ErrorRed else SuccessGreen)
            )
            Text(statusText, color = if (errorText != null) ErrorRed else NavyPrimary)
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Gray200)
      ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.MessageSquare, null, tint = AccentGold, modifier = Modifier.size(22.dp))
            Text("Volunteer Sentence", color = NavyPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }

          if (interpreterPending && volunteerArabicSentence.isNullOrBlank()) {
            Text("Interpreting the live sign sequence...", color = TextLight)
          } else {
            DetectionBlock("Arabic sentence", volunteerArabicSentence.orEmpty()) {
              if (!volunteerArabicSentence.isNullOrBlank()) {
                clipboard.setText(AnnotatedString(volunteerArabicSentence.orEmpty()))
              }
            }
            DetectionBlock("English translation", volunteerEnglishSentence.orEmpty()) {
              if (!volunteerEnglishSentence.isNullOrBlank()) {
                clipboard.setText(AnnotatedString(volunteerEnglishSentence.orEmpty()))
              }
            }
          }

          if (!interpreterMode.isNullOrBlank()) {
            Text("Interpreter mode: ${interpreterMode.orEmpty()}", color = TextLight)
          }
          if (!interpreterError.isNullOrBlank()) {
            Text("Interpreter status: ${interpreterError.orEmpty()}", color = ErrorRed)
          } else if (interpreterPending) {
            Text("Interpreter status: waiting for backend response...", color = TextLight)
          }
          interpreterNotes.take(2).forEach { note ->
            Text(note, color = TextLight, fontSize = 13.sp)
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Gray200)
      ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.Languages, null, tint = AccentGold, modifier = Modifier.size(22.dp))
            Text("Current Detection", color = NavyPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }

          if (currentDetection == null) {
            Text("No confirmed gesture yet.", color = TextLight)
          } else {
            DetectionBlock("English", currentDetection?.translation?.english.orEmpty()) {
              clipboard.setText(AnnotatedString(currentDetection?.translation?.english.orEmpty()))
            }
            DetectionBlock("Arabic", currentDetection?.translation?.arabic.orEmpty()) {
              clipboard.setText(AnnotatedString(currentDetection?.translation?.arabic.orEmpty()))
            }
            Text("Model label: ${currentDetection?.rawLabel.orEmpty()}", color = TextLight)
            Text("Confidence: ${currentDetection?.confidencePercent ?: 0}%", color = TextLight)
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Gray200)
      ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.MessageSquare, null, tint = AccentGold, modifier = Modifier.size(22.dp))
            Text("Transcript", color = NavyPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }
          TranscriptBlock("English transcript", englishTranscript) {
            if (englishTranscript.isNotBlank()) clipboard.setText(AnnotatedString(englishTranscript))
          }
          TranscriptBlock("Arabic transcript", arabicTranscript) {
            if (arabicTranscript.isNotBlank()) clipboard.setText(AnnotatedString(arabicTranscript))
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Gray200)
      ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Supported Gestures", color = NavyPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          SupportedGestureTranslations.forEach { gesture ->
            Column(
              Modifier.fillMaxWidth().background(BluePrimary, RoundedCornerShape(16.dp))
                .border(1.dp, Gray200, RoundedCornerShape(16.dp)).padding(14.dp)
            ) {
              Text("${gesture.english} - ${gesture.arabic}", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
              Spacer(Modifier.height(4.dp))
              Text(gesture.hint, color = TextLight, fontSize = 13.sp)
            }
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)),
        border = BorderStroke(2.dp, AccentGold.copy(alpha = 0.35f))
      ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Current Scope", color = NavyPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Text(
            "This now uses real camera frames and a real MediaPipe gesture model. It does not cover full sign-language vocabulary or guarantee 100% accuracy. Full Arabic and English sign-language translation needs a custom trained model and evaluation data.",
            color = NavyPrimary
          )
        }
      }
    }
  }
}

@Composable
private fun DetectionBlock(title: String, value: String, onCopy: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(title, color = TextLight, fontWeight = FontWeight.SemiBold)
      IconButton(
        onClick = onCopy,
        enabled = value.isNotBlank(),
        modifier = Modifier.size(36.dp).background(if (value.isNotBlank()) BluePrimary else Color.Transparent, RoundedCornerShape(12.dp))
      ) {
        Icon(Lucide.Copy, "Copy $title", tint = if (value.isNotBlank()) NavyPrimary else Gray200)
      }
    }
    Box(
      Modifier.fillMaxWidth().background(BluePrimary, RoundedCornerShape(16.dp))
        .border(2.dp, Gray200, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
      Text(
        if (value.isNotBlank()) value else "Waiting for interpretation...",
        color = if (value.isNotBlank()) NavyPrimary else TextLight,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun TranscriptBlock(title: String, value: String, onCopy: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(title, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
      IconButton(
        onClick = onCopy,
        enabled = value.isNotBlank(),
        modifier = Modifier.size(36.dp).background(if (value.isNotBlank()) BluePrimary else Color.Transparent, RoundedCornerShape(12.dp))
      ) {
        Icon(Lucide.Copy, "Copy $title", tint = if (value.isNotBlank()) NavyPrimary else Gray200)
      }
    }
    Box(
      Modifier.fillMaxWidth().heightIn(min = 90.dp).background(BluePrimary, RoundedCornerShape(16.dp))
        .border(2.dp, Gray200, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
      if (value.isBlank()) {
        Text("Detected gestures will appear here.", color = TextLight, modifier = Modifier.align(Alignment.Center))
      } else {
        SelectionContainer { Text(value, color = NavyPrimary, lineHeight = 24.sp) }
      }
    }
  }
}

@Composable
private fun CenterOverlay(title: String, message: String, buttonText: String?, onButtonClick: (() -> Unit)?) {
  Column(
    Modifier.fillMaxSize().padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
        .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(18.dp))
    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Text(message, color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
    if (buttonText != null && onButtonClick != null) {
      Spacer(Modifier.height(18.dp))
      Button(onClick = onButtonClick, colors = ButtonDefaults.buttonColors(containerColor = AccentGold)) {
        Text(buttonText)
      }
    }
  }
}

@Composable
private fun GridOverlay() {
  Canvas(Modifier.fillMaxSize()) {
    val gridColor = Color.White.copy(alpha = 0.12f)
    val stroke = 1.dp.toPx()
    for (index in 1 until 3) {
      val x = size.width * index / 3f
      val y = size.height * index / 3f
      drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), stroke)
      drawLine(gridColor, Offset(0f, y), Offset(size.width, y), stroke)
    }
  }
}

@Composable
private fun DetectionOverlay(detection: GestureDetectionEntry) {
  Box(Modifier.fillMaxSize()) {
    Canvas(Modifier.fillMaxSize()) {
      val width = size.width * 0.62f
      val height = size.height * 0.48f
      val left = (size.width - width) / 2f
      val top = (size.height - height) / 2f - 18.dp.toPx()
      drawRoundRect(
        color = AccentGold,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx()),
        style = Stroke(width = 3.dp.toPx())
      )
    }
    Surface(
      modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      shape = RoundedCornerShape(18.dp),
      color = NavyPrimary.copy(alpha = 0.92f)
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(detection.translation.english, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(detection.translation.arabic, color = Color.White.copy(alpha = 0.92f), fontSize = 18.sp)
        Text("${detection.confidencePercent}% confidence", color = AccentGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun LiveGestureCameraPreview(
  modifier: Modifier,
  hasCameraPermission: Boolean,
  isAnalyzing: Boolean,
  onFrameAnalyzed: (GestureFrameResult) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val recognizer = remember(context) { MediaPipeGestureRecognizer(context.applicationContext) }
  val latestOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)
  val latestIsAnalyzing by rememberUpdatedState(isAnalyzing)
  val lastAnalyzedTimestamp = remember { AtomicLong(0L) }
  var previewView by remember { mutableStateOf<PreviewView?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      recognizer.close()
      analysisExecutor.shutdown()
    }
  }

  DisposableEffect(previewView, lifecycleOwner, hasCameraPermission) {
    val activePreviewView = previewView ?: return@DisposableEffect onDispose {}
    if (!hasCameraPermission) return@DisposableEffect onDispose {}

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    var cameraProvider: ProcessCameraProvider? = null
    val listener = Runnable {
      try {
        cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(activePreviewView.surfaceProvider) }
        val imageAnalysis = ImageAnalysis.Builder()
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
          .build()
        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
          if (!latestIsAnalyzing) {
            imageProxy.close()
            return@setAnalyzer
          }
          val frameTimestampMs = TimeUnit.NANOSECONDS.toMillis(imageProxy.imageInfo.timestamp)
          if (frameTimestampMs - lastAnalyzedTimestamp.get() < AnalysisCooldownMs) {
            imageProxy.close()
            return@setAnalyzer
          }
          lastAnalyzedTimestamp.set(frameTimestampMs)
          val result = recognizer.analyze(imageProxy, frameTimestampMs)
          mainExecutor.execute { latestOnFrameAnalyzed(result) }
          imageProxy.close()
        }
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
      } catch (exception: Exception) {
        mainExecutor.execute {
          latestOnFrameAnalyzed(
            GestureFrameResult(errorMessage = exception.message ?: "Unable to start the camera recognizer.")
          )
        }
      }
    }
    cameraProviderFuture.addListener(listener, mainExecutor)

    onDispose { runCatching { cameraProvider?.unbindAll() } }
  }

  AndroidView(
    factory = { viewContext ->
      PreviewView(viewContext).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      }.also { previewView = it }
    },
    update = { previewView = it },
    modifier = modifier
  )
}
