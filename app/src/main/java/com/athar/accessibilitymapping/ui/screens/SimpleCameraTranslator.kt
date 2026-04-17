package com.athar.accessibilitymapping.ui.screens
import com.athar.accessibilitymapping.ui.theme.ssp

import com.athar.accessibilitymapping.ui.theme.sdp

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.athar.accessibilitymapping.data.signlanguage.SignTranslationPipeline
import com.athar.accessibilitymapping.ui.theme.*
import com.composables.icons.lucide.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.UUID

private const val AnalysisCooldownMs = 100L
private const val StableDetectionsRequired = 2
private const val TranscriptCooldownMs = 1800L

@Composable
fun SimpleCameraTranslator(onBack: () -> Unit) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val apiClient = remember(context) { BackendApiClient(context.applicationContext) }
  val translationPipeline = remember(context) { SignTranslationPipeline(context.applicationContext) }
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
  val instantGestureCount = SupportedGestureTranslations.size
  val eslSequenceCount = translationPipeline.eslClassCount
  val totalRecognizedSignCount = instantGestureCount + eslSequenceCount
  val lexiconEntryCount = translationPipeline.lexicon.allEntries.size
  val phraseRuleCount = translationPipeline.lexicon.allPhraseRules.size

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
    if (granted) {
      statusText = "Camera ready. Start detection to recognize $totalRecognizedSignCount camera signs."
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
      translationPipeline.clear()
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

    // Feed every frame into the translation pipeline (including no-hand frames)
    val pipelineUpdated = translationPipeline.feedFrame(
      label = result.rawLabel.orEmpty(),
      confidence = result.confidencePercent / 100f,
      timestampMs = result.analyzedAtMillis,
      handCount = result.handCount
    )

    // Feed landmarks into the ESL LSTM model for Egyptian Sign Language recognition
    val eslUpdated = translationPipeline.feedLandmarks(
      rightHandLandmarks = result.rightHandCoords,
      leftHandLandmarks = result.leftHandCoords,
      timestampMs = result.analyzedAtMillis,
      handCount = result.handCount
    )

    if (pipelineUpdated || eslUpdated) {
      val pipelineResult = translationPipeline.currentTranslation
      if (pipelineResult.arabicText.isNotBlank()) {
        volunteerArabicSentence = pipelineResult.arabicText
        volunteerEnglishSentence = pipelineResult.englishText
        interpreterMode = if (eslUpdated) "esl_lstm" else "local_pipeline"
        interpreterNotes = buildList {
          add("Translated from ${pipelineResult.signSequence.size} recognized signs.")
          if (eslUpdated) add("ESL LSTM model active (66 Egyptian signs).")
          if (pipelineResult.isPartial) add("Partial — still signing.")
          pipelineResult.debugInfo?.phraseMatches?.forEach { add("Phrase: $it") }
        }
        interpreterError = null
        interpreterPending = false
      }
    }

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
        "Hand detected. Analyzing sign language..."
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

  DisposableEffect(Unit) {
    onDispose { translationPipeline.close() }
  }

  LaunchedEffect(hasCameraPermission) {
    if (!hasCameraPermission) {
      reset(clearTranscript = false)
      isDetecting = false
    }
  }

  LaunchedEffect(isDetecting) {
    if (isDetecting) {
      statusText = "Listening for signs ($totalRecognizedSignCount camera signs supported)."
      errorText = null
    } else if (hasCameraPermission) {
      reset(clearTranscript = false)
      statusText = "Camera ready. Start detection to recognize $totalRecognizedSignCount camera signs."
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

    // The local translation pipeline already provides Arabic/English output
    // via handleFrame → translationPipeline.feedFrame. The pipeline output
    // is set immediately on each committed sign. Here we optionally enhance
    // with the backend API for richer interpretation.
    val pipelineResult = translationPipeline.currentTranslation
    val hasPipelineOutput = pipelineResult.arabicText.isNotBlank()

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
        val backendLooksLikeFallback = response.data.mode.contains("fallback", ignoreCase = true)
        val backendLooksShorterThanPipeline = hasPipelineOutput &&
          response.data.arabicSentence.trim().length < pipelineResult.arabicText.trim().length
        val shouldPreferPipeline = hasPipelineOutput &&
          (backendLooksLikeFallback || (pipelineResult.signSequence.size > 1 && backendLooksShorterThanPipeline))

        if (shouldPreferPipeline) {
          volunteerArabicSentence = pipelineResult.arabicText
          volunteerEnglishSentence = pipelineResult.englishText
          interpreterMode = "local_pipeline"
          interpreterNotes = buildList {
            add("Using the local translation pipeline because it preserved more of the signed sequence.")
            addAll(response.data.notes)
            if (pipelineResult.isPartial) add("Partial — still signing.")
          }
        } else {
          volunteerArabicSentence = response.data.arabicSentence
          volunteerEnglishSentence = response.data.englishSentence
          interpreterMode = response.data.mode
          interpreterNotes = response.data.notes
        }
        interpreterError = null
      }
      is ApiCallResult.Failure -> {
        // Fall back to pipeline output (phrase-matched, cleaned Arabic text)
        if (hasPipelineOutput) {
          volunteerArabicSentence = pipelineResult.arabicText
          volunteerEnglishSentence = pipelineResult.englishText
          interpreterMode = "local_pipeline"
          interpreterNotes = buildList {
            add("Using local translation pipeline with phrase matching.")
            if (pipelineResult.isPartial) add("Partial — still signing.")
          }
        } else {
          volunteerArabicSentence = observations.joinToString(" ") { it.translation.arabic }
          volunteerEnglishSentence = observations.joinToString(" ") { it.translation.english }
          interpreterMode = "local_fallback"
          interpreterNotes = listOf("Backend unavailable. Showing the local transcript only.")
        }
        interpreterError = response.message
      }
    }
    interpreterPending = false
  }

  Column(Modifier.fillMaxSize().background(BluePrimary)) {
    Box(
      Modifier.fillMaxWidth().background(NavyPrimary).statusBarsPadding()
        .padding(horizontal = 16.sdp, vertical = 18.sdp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
        Surface(
          modifier = Modifier.size(40.sdp).clip(CircleShape).clickable(onClick = onBack),
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.12f)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Lucide.ArrowLeft, "Back", tint = Color.White, modifier = Modifier.size(20.sdp))
          }
        }
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
            Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(22.sdp))
            Text("Sign Language", color = Color.White, fontSize = 22.ssp, fontWeight = FontWeight.Bold)
          }
          Text(
            "Live camera · English & Arabic",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.ssp
          )
        }
      }
    }

    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.sdp),
      verticalArrangement = Arrangement.spacedBy(16.sdp)
    ) {
      // Camera preview card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.sdp)
      ) {
        Box(
          modifier = Modifier.fillMaxWidth().height(400.sdp).clip(RoundedCornerShape(24.sdp)).background(
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
            CenterOverlay(
              title = "Listening",
              message = "Sign with one hand. $totalRecognizedSignCount camera signs are supported by the current model.",
              buttonText = null,
              onButtonClick = null
            )
          }

          if (isDetecting && currentDetection != null) {
            DetectionOverlay(currentDetection ?: return@Card)
          }

          Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.sdp),
            shape = RoundedCornerShape(999.sdp),
            color = if (isDetecting) ErrorRed.copy(alpha = 0.95f) else NavyPrimary.copy(alpha = 0.92f)
          ) {
            Text(
              if (isDetecting) "LIVE" else "READY",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 11.ssp,
              modifier = Modifier.padding(horizontal = 10.sdp, vertical = 6.sdp)
            )
          }

          if (visibleHandCount > 0) {
            Surface(
              modifier = Modifier.align(Alignment.TopEnd).padding(12.sdp),
              shape = RoundedCornerShape(999.sdp),
              color = Color.White.copy(alpha = 0.16f)
            ) {
              Text(
                if (visibleHandCount == 1) "1 hand" else "$visibleHandCount hands",
                color = Color.White,
                fontSize = 11.ssp,
                modifier = Modifier.padding(horizontal = 10.sdp, vertical = 6.sdp)
              )
            }
          }
        }
      }

      // Status bar
      Row(
        Modifier.fillMaxWidth().background(
          if (errorText != null) ErrorRed.copy(alpha = 0.08f) else Color.White,
          RoundedCornerShape(14.sdp)
        ).border(1.sdp, if (errorText != null) ErrorRed.copy(alpha = 0.2f) else Gray200, RoundedCornerShape(14.sdp))
          .padding(horizontal = 14.sdp, vertical = 12.sdp),
        horizontalArrangement = Arrangement.spacedBy(10.sdp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          Modifier.size(8.sdp).clip(CircleShape).background(if (errorText != null) ErrorRed else if (isDetecting) SuccessGreen else AccentGold)
        )
        Text(statusText, color = if (errorText != null) ErrorRed else NavyPrimary, fontSize = 13.ssp)
      }

      // Controls row - below the camera, not overlapping
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.sdp)
      ) {
        Button(
          onClick = {
            if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
            else isDetecting = !isDetecting
          },
          modifier = Modifier.weight(1f).height(54.sdp),
          colors = ButtonDefaults.buttonColors(
            containerColor = when {
              !hasCameraPermission -> NavyPrimary
              isDetecting -> ErrorRed
              else -> AccentGold
            }
          ),
          shape = RoundedCornerShape(16.sdp),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.sdp)
        ) {
          Icon(
            imageVector = when {
              !hasCameraPermission -> Lucide.Camera
              isDetecting -> Lucide.CircleStop
              else -> Lucide.Play
            },
            contentDescription = null,
            modifier = Modifier.size(20.sdp)
          )
          Spacer(Modifier.width(8.sdp))
          Text(
            when {
              !hasCameraPermission -> "Enable Camera"
              isDetecting -> "Stop Detection"
              else -> "Start Detection"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.ssp
          )
        }

        Surface(
          modifier = Modifier.size(54.sdp).clip(RoundedCornerShape(16.sdp)).clickable {
            reset(clearTranscript = true)
            errorText = null
            statusText = if (hasCameraPermission) {
              "Transcript cleared. Start detection to analyze supported gestures."
            } else {
              "Allow camera access to start live recognition."
            }
          },
          shape = RoundedCornerShape(16.sdp),
          color = Color.White,
          border = BorderStroke(1.5.dp, Gray200)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Lucide.RotateCcw, "Clear", tint = NavyPrimary, modifier = Modifier.size(22.sdp))
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.sdp, Gray200)
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(14.sdp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.sdp)) {
            Icon(Lucide.MessageSquare, null, tint = AccentGold, modifier = Modifier.size(22.sdp))
            Text("Volunteer Sentence", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
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
            Text(note, color = TextLight, fontSize = 13.ssp)
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.sdp, Gray200)
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(14.sdp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.sdp)) {
            Icon(Lucide.Languages, null, tint = AccentGold, modifier = Modifier.size(22.sdp))
            Text("Current Detection", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.sdp, Gray200)
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(14.sdp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.sdp)) {
            Icon(Lucide.MessageSquare, null, tint = AccentGold, modifier = Modifier.size(22.sdp))
            Text("Transcript", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
          }
          TranscriptBlock("English transcript", englishTranscript) {
            if (englishTranscript.isNotBlank()) clipboard.setText(AnnotatedString(englishTranscript))
          }
          TranscriptBlock("Arabic transcript", arabicTranscript) {
            if (arabicTranscript.isNotBlank()) clipboard.setText(AnnotatedString(arabicTranscript))
          }
        }
      }

      // ── Instant Gestures (7 MediaPipe) ───────────────────────────────
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.sdp, Gray200)
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(12.sdp)) {
          Text("Instant Gestures ($instantGestureCount)", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
          Text("Recognized instantly from a single frame.", color = TextLight, fontSize = 13.ssp)
          SupportedGestureTranslations.forEach { gesture ->
            Column(
              Modifier.fillMaxWidth().background(BluePrimary, RoundedCornerShape(16.sdp))
                .border(1.sdp, Gray200, RoundedCornerShape(16.sdp)).padding(14.sdp)
            ) {
              Text("${gesture.english} - ${gesture.arabic}", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
              Spacer(Modifier.height(4.sdp))
              Text(gesture.hint, color = TextLight, fontSize = 13.ssp)
            }
          }
        }
      }

      // ── Egyptian Sign Language (66 ESL signs) ────────────────────────
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.sdp, Gray200)
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(12.sdp)) {
          Text("Egyptian Sign Language ($eslSequenceCount signs)", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
          Text("Recognized from hand movement sequences using the ESL LSTM model.", color = TextLight, fontSize = 13.ssp)

          EslSignCategory("Question Words", listOf(
            "ازاى - How" to "ازاى",  "ايه - What" to "ايه",  "فين - Where" to "فين",
            "ليه - Why" to "ليه",  "مين - Who" to "مين",  "امتى - When" to "امتى"
          ))
          EslSignCategory("Days", listOf(
            "الأحد - Sunday" to "الاحد",  "الثلاثاء - Tuesday" to "الثلاثاء",
            "الأربعاء - Wednesday" to "الاربعاء",  "الخميس - Thursday" to "الخميس",
            "الجمعة - Friday" to "الجمعة",  "السبت - Saturday" to "السبت"
          ))
          EslSignCategory("Colors", listOf(
            "أبيض - White" to "ابيض",  "أحمر - Red" to "احمر",  "أزرق - Blue" to "ازرق",
            "أسود - Black" to "اسود",  "برتقالي - Orange" to "برتقالي",
            "بنفسجي - Purple" to "بنفسجى",  "ألوان - Colors" to "الوان"
          ))
          EslSignCategory("Jobs", listOf(
            "دكتور - Doctor" to "دكتور",  "معلم - Teacher" to "معلم",
            "مهندس - Engineer" to "مهندس",  "طيار - Pilot" to "طيار",
            "عامل - Worker" to "عامل"
          ))
          EslSignCategory("Expressions", listOf(
            "شكرا - Thank you" to "شكرا",  "آسف - Sorry" to "اسف",
            "بحبك - I love you" to "بحبك",  "مع السلامة - Goodbye" to "مع السلامة",
            "أكيد - Sure" to "اكيد",  "ممكن - Maybe" to "ممكن",
            "كويس - Good" to "جيد - كويس"
          ))
          EslSignCategory("Descriptions", listOf(
            "جميل - Beautiful" to "جميل",  "طويل - Tall" to "طويل",
            "قصير - Short" to "قصير",  "خفيف - Light" to "خفيف",
            "مشغول - Busy" to "مشغول",  "فاضي - Free" to "فاضي",
            "نشيط - Active" to "نشيط",  "متفائل - Optimistic" to "متفائل",
            "قبيح - Ugly" to "قبيح",  "فقير - Poor" to "فقير"
          ))
          EslSignCategory("Common Words", listOf(
            "احنا - We" to "احنا",  "بتاعي - Mine" to "بتاعى",
            "بتاعه - His/Hers" to "بتاعه",  "في - In" to "فى",
            "فوق - Up" to "فوق",  "جنب - Next to" to "جمب",
            "بعد - After" to "بعد",  "غير - Other" to "غير",  "و - And" to "و"
          ))
          EslSignCategory("More", listOf(
            "الأسرة - Family" to "الاسرة",  "جد - Grandfather" to "جد",
            "خطوبة - Engagement" to "خطوبة",  "مطلق - Divorced" to "مطلق",
            "شغل - Work" to "شغل",  "فلوس - Money" to "فلوس",
            "كلية - College" to "كلية",  "لغة - Language" to "لغة",
            "اسم - Name" to "اسم",  "رقم - Number" to "رقم",
            "انترنت - Internet" to "انترنت",  "سماعة - Headphones" to "سماعة",
            "الجو - Weather" to "الجو",  "مشكلة - Problem" to "مشكلة",
            "كام - How much" to "كام كمية"
          ))
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)),
        border = BorderStroke(2.sdp, AccentGold.copy(alpha = 0.35f))
      ) {
        Column(Modifier.padding(20.sdp), verticalArrangement = Arrangement.spacedBy(10.sdp)) {
          Text("How It Works", color = NavyPrimary, fontSize = 18.ssp, fontWeight = FontWeight.Bold)
          Text(
            "This translator uses two AI models working together:\n\n" +
              "1. MediaPipe Gesture Recognizer - instantly detects $instantGestureCount common hand gestures from a single camera frame.\n\n" +
              "2. ESL LSTM Model - recognizes $eslSequenceCount Egyptian Sign Language signs by analyzing hand movement sequences (30 frames).\n\n" +
              "Together they provide $totalRecognizedSignCount camera-recognizable signs today. " +
              "The app also ships with a bilingual lexicon of $lexiconEntryCount entries and $phraseRuleCount sentence rules, " +
              "so translation can grow without hardcoded limits.\n\n" +
              "To truly read 100+ new gestures directly from the camera, the next step is replacing the current ESL model and labels file with a larger trained Egyptian Sign Language model.",
            color = NavyPrimary
          )
        }
      }
    }
  }
}

@Composable
private fun DetectionBlock(title: String, value: String, onCopy: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.sdp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(title, color = TextLight, fontWeight = FontWeight.SemiBold)
      IconButton(
        onClick = onCopy,
        enabled = value.isNotBlank(),
        modifier = Modifier.size(36.sdp).background(if (value.isNotBlank()) BluePrimary else Color.Transparent, RoundedCornerShape(12.sdp))
      ) {
        Icon(Lucide.Copy, "Copy $title", tint = if (value.isNotBlank()) NavyPrimary else Gray200)
      }
    }
    Box(
      Modifier.fillMaxWidth().background(BluePrimary, RoundedCornerShape(16.sdp))
        .border(2.sdp, Gray200, RoundedCornerShape(16.sdp)).padding(16.sdp)
    ) {
      Text(
        if (value.isNotBlank()) value else "Waiting for interpretation...",
        color = if (value.isNotBlank()) NavyPrimary else TextLight,
        fontSize = 16.ssp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun TranscriptBlock(title: String, value: String, onCopy: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.sdp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(title, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
      IconButton(
        onClick = onCopy,
        enabled = value.isNotBlank(),
        modifier = Modifier.size(36.sdp).background(if (value.isNotBlank()) BluePrimary else Color.Transparent, RoundedCornerShape(12.sdp))
      ) {
        Icon(Lucide.Copy, "Copy $title", tint = if (value.isNotBlank()) NavyPrimary else Gray200)
      }
    }
    Box(
      Modifier.fillMaxWidth().heightIn(min = 90.sdp).background(BluePrimary, RoundedCornerShape(16.sdp))
        .border(2.sdp, Gray200, RoundedCornerShape(16.sdp)).padding(16.sdp)
    ) {
      if (value.isBlank()) {
        Text("Detected gestures will appear here.", color = TextLight, modifier = Modifier.align(Alignment.Center))
      } else {
        SelectionContainer { Text(value, color = NavyPrimary, lineHeight = 24.ssp) }
      }
    }
  }
}

@Composable
private fun CenterOverlay(title: String, message: String, buttonText: String?, onButtonClick: (() -> Unit)?) {
  Column(
    Modifier.fillMaxSize().padding(28.sdp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      Modifier.size(96.sdp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
        .border(2.sdp, Color.White.copy(alpha = 0.18f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(44.sdp))
    }
    Spacer(Modifier.height(18.sdp))
    Text(title, color = Color.White, fontSize = 18.ssp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.sdp))
    Text(message, color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
    if (buttonText != null && onButtonClick != null) {
      Spacer(Modifier.height(18.sdp))
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
      modifier = Modifier.align(Alignment.BottomCenter).padding(16.sdp),
      shape = RoundedCornerShape(18.sdp),
      color = NavyPrimary.copy(alpha = 0.92f)
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 18.sdp, vertical = 14.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(detection.translation.english, color = Color.White, fontSize = 20.ssp, fontWeight = FontWeight.Bold)
        Text(detection.translation.arabic, color = Color.White.copy(alpha = 0.92f), fontSize = 18.ssp)
        Text("${detection.confidencePercent}% confidence", color = AccentGold, fontSize = 13.ssp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun EslSignCategory(title: String, signs: List<Pair<String, String>>) {
  Column(verticalArrangement = Arrangement.spacedBy(6.sdp)) {
    Text(title, color = NavyPrimary, fontSize = 15.ssp, fontWeight = FontWeight.SemiBold)
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.sdp),
      verticalArrangement = Arrangement.spacedBy(6.sdp)
    ) {
      signs.forEach { (label, _) ->
        Text(
          label,
          modifier = Modifier
            .background(BluePrimary, RoundedCornerShape(8.sdp))
            .border(1.sdp, Gray200, RoundedCornerShape(8.sdp))
            .padding(horizontal = 10.sdp, vertical = 6.sdp),
          color = NavyPrimary,
          fontSize = 13.ssp
        )
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
  val handLandmarker = remember(context) { MediaPipeHandLandmarker(context.applicationContext) }
  val latestOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)
  val latestIsAnalyzing by rememberUpdatedState(isAnalyzing)
  val lastAnalyzedTimestamp = remember { AtomicLong(0L) }
  var previewView by remember { mutableStateOf<PreviewView?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      recognizer.close()
      handLandmarker.close()
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
          // Run gesture recognizer (7 instant gestures)
          val gestureResult = recognizer.analyze(imageProxy, frameTimestampMs)
          // Run hand landmarker (2-hand landmarks for ESL LSTM)
          val landmarkResult = if (handLandmarker.isReady) {
            handLandmarker.extractLandmarks(imageProxy, frameTimestampMs)
          } else null
          // Merge both results
          val mergedResult = if (landmarkResult != null && landmarkResult.handCount > 0) {
            gestureResult.copy(
              rightHandCoords = landmarkResult.rightHandCoords,
              leftHandCoords = landmarkResult.leftHandCoords,
              handCount = maxOf(gestureResult.handCount, landmarkResult.handCount)
            )
          } else gestureResult
          mainExecutor.execute { latestOnFrameAnalyzed(mergedResult) }
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
