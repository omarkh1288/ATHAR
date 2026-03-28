package com.athar.accessibilitymapping.ui.screens
import com.athar.accessibilitymapping.ui.theme.ssp

import com.athar.accessibilitymapping.ui.theme.sdp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.athar.accessibilitymapping.ui.theme.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private val TranslatorTitleStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 22.sp,
  lineHeight = 28.sp
)

private val TranslatorBodyStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

private val TranslatorLabelStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Medium,
  fontSize = 12.sp,
  lineHeight = 16.sp
)

private enum class TranslatorMode { Glove, Camera }
private enum class ConnectionStatus { Disconnected, Connecting, Connected }

private val gloveAlphabet = ('A'..'Z').map { it.toString() }
private val gloveNumbers = ('0'..'9').map { it.toString() }

// Comprehensive Sign Language Database with English and Arabic translations
private data class SignLanguageWord(
  val english: String,
  val arabic: String,
  val category: String
)

private val signLanguageDatabase = listOf(
  // Greetings
  SignLanguageWord("Hello", "مرحبا", "greeting"),
  SignLanguageWord("Good morning", "صباح الخير", "greeting"),
  SignLanguageWord("Good evening", "مساء الخير", "greeting"),
  SignLanguageWord("Welcome", "أهلا وسهلا", "greeting"),
  SignLanguageWord("Goodbye", "وداعا", "greeting"),

  // Common Phrases
  SignLanguageWord("Thank you", "شكرا", "common"),
  SignLanguageWord("Please", "من فضلك", "common"),
  SignLanguageWord("Sorry", "آسف", "common"),
  SignLanguageWord("Excuse me", "عفوا", "common"),
  SignLanguageWord("How are you?", "كيف حالك؟", "common"),
  SignLanguageWord("I'm fine", "أنا بخير", "common"),
  SignLanguageWord("What's your name?", "ما اسمك؟", "common"),
  SignLanguageWord("My name is", "اسمي", "common"),
  SignLanguageWord("Nice to meet you", "تشرفنا", "common"),

  // Basic Needs
  SignLanguageWord("Help", "مساعدة", "needs"),
  SignLanguageWord("Water", "ماء", "needs"),
  SignLanguageWord("Food", "طعام", "needs"),
  SignLanguageWord("Bathroom", "حمام", "needs"),
  SignLanguageWord("Medicine", "دواء", "needs"),
  SignLanguageWord("Doctor", "طبيب", "needs"),
  SignLanguageWord("Hospital", "مستشفى", "needs"),

  // Yes/No and Basic Responses
  SignLanguageWord("Yes", "نعم", "response"),
  SignLanguageWord("No", "لا", "response"),
  SignLanguageWord("OK", "حسنا", "response"),
  SignLanguageWord("Maybe", "ربما", "response"),
  SignLanguageWord("I don't know", "لا أعرف", "response"),
  SignLanguageWord("I understand", "أفهم", "response"),
  SignLanguageWord("I don't understand", "لا أفهم", "response"),

  // Directions
  SignLanguageWord("Where", "أين", "direction"),
  SignLanguageWord("Here", "هنا", "direction"),
  SignLanguageWord("There", "هناك", "direction"),
  SignLanguageWord("Left", "يسار", "direction"),
  SignLanguageWord("Right", "يمين", "direction"),
  SignLanguageWord("Straight", "مستقيم", "direction"),

  // Accessibility
  SignLanguageWord("Wheelchair", "كرسي متحرك", "accessibility"),
  SignLanguageWord("Elevator", "مصعد", "accessibility"),
  SignLanguageWord("Ramp", "منحدر", "accessibility"),
  SignLanguageWord("Accessible", "متاح للوصول", "accessibility"),
  SignLanguageWord("Stairs", "سلالم", "accessibility"),

  // Emergency
  SignLanguageWord("Emergency", "طوارئ", "emergency"),
  SignLanguageWord("Call ambulance", "اتصل بالإسعاف", "emergency"),
  SignLanguageWord("I need help", "أحتاج مساعدة", "emergency"),
  SignLanguageWord("Police", "شرطة", "emergency")
)

private val cameraSigns = signLanguageDatabase.map { it.english }

@Composable
fun SignLanguageTranslatorScreen(onBack: () -> Unit) {
  // Use the new simple camera translator
  SimpleCameraTranslator(onBack = onBack)
}

@Composable
fun SignLanguageTranslatorScreenOld(onBack: () -> Unit) {
  val clipboard = LocalClipboardManager.current
  var mode by rememberSaveable { mutableStateOf(TranslatorMode.Glove) }
  var showInfo by rememberSaveable { mutableStateOf(false) }

  var gloveStatus by rememberSaveable { mutableStateOf(ConnectionStatus.Disconnected) }
  var gloveText by rememberSaveable { mutableStateOf("") }
  var currentChar by rememberSaveable { mutableStateOf<String?>(null) }
  var gloveDeviceName by rememberSaveable { mutableStateOf<String?>(null) }
  var gloveBattery by rememberSaveable { mutableStateOf(85) }
  var gloveListening by rememberSaveable { mutableStateOf(false) }

  var cameraStatus by rememberSaveable { mutableStateOf(ConnectionStatus.Disconnected) }
  var cameraConnected by rememberSaveable { mutableStateOf(false) }
  var streamUrl by rememberSaveable { mutableStateOf("") }
  var isFullscreen by rememberSaveable { mutableStateOf(false) }
  val cameraWords = remember { mutableStateListOf<String>() }
  var currentSign by rememberSaveable { mutableStateOf<String?>(null) }
  var cameraConfidence by rememberSaveable { mutableStateOf(0) }
  var cameraTranslating by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(gloveListening, gloveStatus) {
    while (gloveListening && gloveStatus == ConnectionStatus.Connected) {
      delay(1800)
      val next = (gloveAlphabet + gloveNumbers).random()
      currentChar = next
      gloveText += next
    }
  }

  LaunchedEffect(cameraTranslating, cameraConnected) {
    if (!cameraTranslating || !cameraConnected) {
      if (!cameraTranslating) {
        currentSign = null
        cameraConfidence = 0
      }
      return@LaunchedEffect
    }
    currentSign = null
    delay(1400)
    while (cameraTranslating && cameraConnected) {
      val next = cameraSigns.random()
      currentSign = next
      cameraConfidence = Random.nextInt(80, 100)
      cameraWords += next
      delay(2500)
    }
  }

  Box(Modifier.fillMaxSize().background(BluePrimary)) {
    Column(Modifier.fillMaxSize()) {
      TranslatorHeader(
        mode = mode,
        showInfo = showInfo,
        onBack = onBack,
        onToggleInfo = { showInfo = !showInfo },
        activeStatus = if (mode == TranslatorMode.Glove) gloveStatus else cameraStatus,
        rightLabel = if (mode == TranslatorMode.Glove) gloveDeviceName ?: "Glove Sensor" else "Desktop Stream"
      )

      AnimatedVisibility(visible = showInfo) {
        TranslatorInfoCard(mode = mode, onDismiss = { showInfo = false })
      }

      ModeTabs(
        mode = mode,
        topPadding = if (showInfo) 8.sdp else 12.sdp,
        onModeChange = { mode = it }
      )

      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.sdp, vertical = 12.sdp),
        verticalArrangement = Arrangement.spacedBy(12.sdp)
      ) {
        if (mode == TranslatorMode.Glove) {
          GloveMode(
            status = gloveStatus,
            text = gloveText,
            currentChar = currentChar,
            deviceName = gloveDeviceName,
            battery = gloveBattery,
            listening = gloveListening,
            onConnect = {
              gloveStatus = ConnectionStatus.Connecting
              gloveListening = false
            },
            onDisconnect = {
              gloveStatus = ConnectionStatus.Disconnected
              gloveDeviceName = null
              gloveListening = false
              currentChar = null
            },
            onToggleListening = { gloveListening = !gloveListening },
            onCopy = { clipboard.setText(AnnotatedString(gloveText)) },
            onClear = { gloveText = ""; currentChar = null },
            onSpace = { gloveText += " " },
            onBackspace = { gloveText = gloveText.dropLast(1) }
          )
        } else {
          CameraMode(
            status = cameraStatus,
            connected = cameraConnected,
            streamUrl = streamUrl,
            fullscreen = isFullscreen,
            currentSign = currentSign,
            confidence = cameraConfidence,
            translating = cameraTranslating,
            words = cameraWords,
            onStreamUrlChange = { streamUrl = it },
            onConnect = {
              // Connect camera when permission is granted
              cameraStatus = ConnectionStatus.Connecting
            },
            onDisconnect = {
              cameraStatus = ConnectionStatus.Disconnected
              cameraConnected = false
              cameraTranslating = false
              currentSign = null
              cameraConfidence = 0
            },
            onToggleTranslation = { cameraTranslating = !cameraTranslating },
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onCopy = { clipboard.setText(AnnotatedString(cameraWords.joinToString(" "))) },
            onClear = { cameraWords.clear(); currentSign = null; cameraConfidence = 0 }
          )
        }
        Spacer(Modifier.height(12.sdp))
      }
    }

    if (gloveStatus == ConnectionStatus.Connecting) {
      LaunchedEffect(Unit) {
        delay(2500)
        gloveStatus = ConnectionStatus.Connected
        gloveDeviceName = "Athar Glove v2"
        gloveBattery = Random.nextInt(65, 96)
      }
    }

    if (cameraStatus == ConnectionStatus.Connecting) {
      LaunchedEffect(Unit) {
        delay(500) // Quick connection for camera
        cameraStatus = ConnectionStatus.Connected
        cameraConnected = true
      }
    }

    if (mode == TranslatorMode.Camera && isFullscreen) {
      Dialog(onDismissRequest = { isFullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
          CameraStreamPanel(
            modifier = Modifier.fillMaxSize(),
            fullscreen = true,
            status = cameraStatus,
            connected = cameraConnected,
            translating = cameraTranslating,
            currentSign = currentSign,
            confidence = cameraConfidence,
            onToggleFullscreen = { isFullscreen = false }
          )
        }
      }
    }
  }
}

@Composable
private fun TranslatorHeader(
  mode: TranslatorMode,
  showInfo: Boolean,
  activeStatus: ConnectionStatus,
  rightLabel: String,
  onBack: () -> Unit,
  onToggleInfo: () -> Unit
) {
  val statusColor = when (activeStatus) {
    ConnectionStatus.Connected -> SuccessGreen
    ConnectionStatus.Connecting -> WarningGold
    ConnectionStatus.Disconnected -> ErrorRed
  }
  val statusLabel = when (activeStatus) {
    ConnectionStatus.Connected -> "Connected"
    ConnectionStatus.Connecting -> "Connecting..."
    ConnectionStatus.Disconnected -> "Disconnected"
  }

  Column(Modifier.fillMaxWidth().background(NavyPrimary).statusBarsPadding().padding(start = 16.sdp, end = 16.sdp, top = 20.sdp, bottom = 22.sdp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
      Box(Modifier.size(40.sdp).clip(CircleShape).background(NavyDark).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
        Icon(Lucide.ArrowLeft, "Go back", tint = Color.White, modifier = Modifier.size(20.sdp))
      }
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
          Icon(Lucide.Hand, null, tint = Color.White, modifier = Modifier.size(22.sdp))
          Text("Sign Language Translator", color = Color.White, style = TranslatorTitleStyle)
        }
        Text(if (mode == TranslatorMode.Glove) "Bluetooth glove recognition" else "Video stream recognition", color = Color.White.copy(alpha = 0.7f), style = TranslatorBodyStyle)
      }
      Box(Modifier.size(36.sdp).clip(CircleShape).background(NavyDark).clickable(onClick = onToggleInfo), contentAlignment = Alignment.Center) {
        Icon(Lucide.Info, "Info", tint = Color.White, modifier = Modifier.size(18.sdp))
      }
    }
    Spacer(Modifier.height(12.sdp))
    Row(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.sdp)).background(NavyDark).padding(horizontal = 12.sdp, vertical = 10.sdp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
        Box(Modifier.size(10.sdp).clip(CircleShape).background(statusColor))
        Text(statusLabel, color = Color.White.copy(alpha = 0.8f), style = TranslatorLabelStyle)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.sdp)) {
        Icon(
          imageVector = when {
            mode == TranslatorMode.Glove && activeStatus == ConnectionStatus.Connected -> Lucide.Bluetooth
            mode == TranslatorMode.Glove -> Lucide.BluetoothOff
            activeStatus == ConnectionStatus.Connected -> Lucide.Wifi
            else -> Lucide.WifiOff
          },
          contentDescription = null,
          tint = if (activeStatus == ConnectionStatus.Connected) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f),
          modifier = Modifier.size(16.sdp)
        )
        Text(rightLabel, color = Color.White.copy(alpha = 0.6f), style = TranslatorLabelStyle)
      }
    }
  }
}

@Composable
private fun ModeTabs(mode: TranslatorMode, topPadding: androidx.compose.ui.unit.Dp, onModeChange: (TranslatorMode) -> Unit) {
  Card(
    modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = topPadding, bottom = 12.sdp).fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.sdp)
  ) {
    Row(Modifier.fillMaxWidth().padding(4.sdp), horizontalArrangement = Arrangement.spacedBy(4.sdp)) {
      TranslatorMode.entries.forEach { item ->
        val selected = item == mode
        Row(
          modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.sdp)).background(if (selected) NavyPrimary else Color.Transparent).clickable { onModeChange(item) }.padding(horizontal = 8.sdp, vertical = 12.sdp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(if (item == TranslatorMode.Glove) Lucide.Bluetooth else Lucide.Camera, item.name, tint = if (selected) Color.White else TextLight, modifier = Modifier.size(16.sdp))
          Spacer(Modifier.width(8.sdp))
          Text(if (item == TranslatorMode.Glove) "Glove Translator" else "Camera Translator", color = if (selected) Color.White else TextLight, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun TranslatorInfoCard(mode: TranslatorMode, onDismiss: () -> Unit) {
  Card(
    modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = 10.sdp, bottom = 0.sdp).fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, AccentGoldDark),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8DC)),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.sdp)
  ) {
    Column(Modifier.padding(horizontal = 16.sdp, vertical = 18.sdp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.sdp), verticalAlignment = Alignment.Top) {
        Icon(Lucide.Zap, null, tint = AccentGoldDark, modifier = Modifier.padding(top = 2.sdp).size(18.sdp))
        Column(Modifier.weight(1f)) {
          Text(
            if (mode == TranslatorMode.Glove) "Glove Translator" else "Camera Translator",
            color = NavyPrimary,
            style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold)
          )
          Spacer(Modifier.height(6.sdp))
          Text(
            if (mode == TranslatorMode.Glove) {
              "Connect the Athar smart glove via Bluetooth. The glove sensors detect finger positions and translate them into individual characters (A-Z, 0-9) in real time."
            } else {
              "Receive a live video stream from the Athar desktop app. The camera captures sign language gestures and translates them into words and phrases."
            },
            color = NavyDark,
            style = TranslatorLabelStyle.copy(fontSize = 12.ssp, lineHeight = 18.ssp)
          )
        }
      }
      Spacer(Modifier.height(18.sdp))
      val steps = if (mode == TranslatorMode.Glove) {
        listOf(
          "Turn on the Athar glove and enable Bluetooth",
          "Tap \"Pair Glove\" to connect via Bluetooth",
          "Start listening - characters appear as you sign"
        )
      } else {
        listOf(
          "Open the Athar desktop app and start camera",
          "Copy the stream URL and paste it below",
          "Tap Connect, then Start Translating"
        )
      }
      steps.forEachIndexed { index, step ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.sdp), verticalAlignment = Alignment.Top) {
          Text("${index + 1}.", color = AccentGoldDark, style = TranslatorLabelStyle.copy(fontWeight = FontWeight.Bold))
          Text(step, color = NavyDark, style = TranslatorLabelStyle.copy(fontSize = 12.ssp, lineHeight = 16.ssp))
        }
        if (index < steps.lastIndex) Spacer(Modifier.height(8.sdp))
      }
      Spacer(Modifier.height(12.sdp))
      Text(
        "Got it",
        color = AccentGoldDark,
        style = TranslatorLabelStyle.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.clickable(onClick = onDismiss)
      )
    }
  }
}

@Composable
private fun GloveMode(
  status: ConnectionStatus,
  text: String,
  currentChar: String?,
  deviceName: String?,
  battery: Int,
  listening: Boolean,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onToggleListening: () -> Unit,
  onCopy: () -> Unit,
  onClear: () -> Unit,
  onSpace: () -> Unit,
  onBackspace: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, if (status == ConnectionStatus.Connected) SuccessGreen else Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.sdp)
  ) {
    Column {
      Column(
        modifier = Modifier.fillMaxWidth().background(
          Brush.linearGradient(
            if (status == ConnectionStatus.Connected) listOf(NavyPrimary, NavyDark) else listOf(Gray700, Gray600)
          )
        ).padding(24.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier.size(96.sdp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(3.sdp, if (status == ConnectionStatus.Connected) SuccessGreen else Color.White.copy(alpha = 0.2f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          when (status) {
            ConnectionStatus.Connecting -> PulsingIcon(Lucide.BluetoothSearching, Color.White.copy(alpha = 0.6f), 48.sdp)
            ConnectionStatus.Connected -> Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(42.sdp))
            ConnectionStatus.Disconnected -> Icon(Lucide.BluetoothOff, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(42.sdp))
          }
        }

        when (status) {
          ConnectionStatus.Connected -> {
            Spacer(Modifier.height(12.sdp))
            Text(deviceName ?: "Athar Glove v2", color = Color.White, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.sdp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.sdp), verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = RoundedCornerShape(999.sdp), color = SuccessGreen.copy(alpha = 0.2f)) {
                Row(Modifier.padding(horizontal = 10.sdp, vertical = 6.sdp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.sdp)) {
                  Box(Modifier.size(8.sdp).clip(CircleShape).background(SuccessGreen))
                  Text("Paired", color = SuccessGreen.copy(alpha = 0.9f), style = TranslatorLabelStyle)
                }
              }
              Surface(shape = RoundedCornerShape(999.sdp), color = Color.White.copy(alpha = 0.12f)) {
                Row(Modifier.padding(horizontal = 10.sdp, vertical = 6.sdp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.sdp)) {
                  BatteryIndicator(battery)
                  Text("$battery%", color = Color.White.copy(alpha = 0.7f), style = TranslatorLabelStyle)
                }
              }
            }
          }
          ConnectionStatus.Connecting -> {
            Spacer(Modifier.height(12.sdp))
            Text("Searching for glove...", color = Color.White.copy(alpha = 0.65f), style = TranslatorBodyStyle)
          }
          ConnectionStatus.Disconnected -> {
            Spacer(Modifier.height(12.sdp))
            Text("No glove connected", color = Color.White.copy(alpha = 0.45f), style = TranslatorBodyStyle)
          }
        }

        if (status == ConnectionStatus.Connected && currentChar != null) {
          Spacer(Modifier.height(16.sdp))
          Box(
            modifier = Modifier.size(64.sdp).clip(RoundedCornerShape(16.sdp)).background(AccentGold),
            contentAlignment = Alignment.Center
          ) {
            Text(currentChar, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.ssp)
          }
        }
      }

      Column(Modifier.padding(16.sdp)) {
        when (status) {
          ConnectionStatus.Disconnected -> FilledPanelButton("Pair Glove", Lucide.Bluetooth, NavyPrimary, onConnect, Modifier.fillMaxWidth())
          ConnectionStatus.Connecting -> FilledPanelButton("Pairing...", Lucide.LoaderCircle, TextLight, {}, Modifier.fillMaxWidth(), spinIcon = true)
          ConnectionStatus.Connected -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
            OutlinePanelButton("Unpair", Lucide.BluetoothOff, ErrorRed, Modifier.weight(1f), onDisconnect)
            FilledPanelButton(if (listening) "Stop" else "Start Listening", Lucide.Hand, if (listening) ErrorRed else AccentGold, onToggleListening, Modifier.weight(1f))
          }
        }
      }
    }
  }

  OutputCard(
    title = "Translated Text",
    emptyText = "Pair the glove and start listening to see characters here",
    emptyIcon = Lucide.Hand,
    onCopy = onCopy,
    onClear = onClear
  ) {
    if (text.isEmpty()) {
      EmptyState("Pair the glove and start listening to see characters here", Lucide.Hand)
    } else {
      Text(
        buildAnnotatedString(text),
        color = NavyPrimary,
        fontFamily = FontFamily.Monospace,
        fontSize = 18.ssp,
        lineHeight = 28.ssp
      )
    }
  }

  if (status == ConnectionStatus.Connected) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
      OutlinePanelButton("Space", null, NavyPrimary, Modifier.weight(1f), onSpace)
      OutlinePanelButton("Backspace", Lucide.Delete, NavyPrimary, Modifier.weight(1f), onBackspace)
      OutlinePanelButton("Clear", Lucide.RotateCcw, ErrorRed, Modifier.weight(1f), onClear)
    }
  }

  SupportedCharactersCard(currentChar)
}

@Composable
private fun CameraMode(
  status: ConnectionStatus,
  connected: Boolean,
  streamUrl: String,
  fullscreen: Boolean,
  currentSign: String?,
  confidence: Int,
  translating: Boolean,
  words: List<String>,
  onStreamUrlChange: (String) -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onToggleTranslation: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onCopy: () -> Unit,
  onClear: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var selectedLanguage by remember { mutableStateOf("English") }
  var hasCameraPermission by remember { mutableStateOf(
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  ) }

  // Camera permission launcher
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
    if (isGranted) {
      onConnect() // Auto-connect when permission granted
    }
  }

  // Auto-connect if already has permission
  LaunchedEffect(hasCameraPermission) {
    if (hasCameraPermission && !connected) {
      onConnect()
    }
  }

  // Translation logic - convert English to Arabic or vice versa
  val translatedText = remember(words, selectedLanguage) {
    if (selectedLanguage == "Arabic") {
      words.mapNotNull { word ->
        signLanguageDatabase.find { it.english.equals(word, ignoreCase = true) }?.arabic
      }.joinToString(" ")
    } else {
      words.joinToString(" ")
    }
  }

  // Camera Preview Card with Modern Design
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.sdp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.sdp)
  ) {
    Column {
      // Camera View
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.sdp)
          .clip(RoundedCornerShape(topStart = 20.sdp, topEnd = 20.sdp))
      ) {
        // Camera Preview with animated background
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
        ) {
          // Animated camera-like gradient background
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    Color(0xFF1E3A5F),
                    Color(0xFF0F172A),
                    Color.Black
                  ),
                  center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                  radius = 800f
                )
              )
          )

          // Simulated camera noise/texture
          if (connected && translating) {
            CameraNoiseEffect()
          }

          // Show permission required state
          if (!hasCameraPermission) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Lucide.Camera, null, tint = AccentGold, modifier = Modifier.size(60.sdp))
                Spacer(Modifier.height(16.sdp))
                Text(
                  "Camera Permission Required",
                  color = Color.White,
                  style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(8.sdp))
                Button(
                  onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                  Text("Grant Permission")
                }
              }
            }
          } else if (!connected) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Lucide.Camera, null, tint = AccentGold, modifier = Modifier.size(60.sdp))
                Spacer(Modifier.height(16.sdp))
                Text(
                  "Camera Ready",
                  color = Color.White,
                  style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold)
                )
              }
            }
          } else if (!translating) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                  modifier = Modifier
                    .size(100.sdp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(3.sdp, AccentGold.copy(alpha = 0.5f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(48.sdp))
                }
                Spacer(Modifier.height(16.sdp))
                Text(
                  "Tap 'Start Detection' below",
                  color = Color.White.copy(alpha = 0.7f),
                  style = TranslatorBodyStyle.copy(fontWeight = FontWeight.Medium)
                )
              }
            }
          }
        }

        // Camera Grid Overlay
        if (hasCameraPermission) {
          CameraGridOverlay()
        }

        // Detection Status
        if (translating && currentSign != null) {
          // Hand Detection Box
          HandDetectionBox(
            currentSign,
            confidence
          )
        }

        // Top Bar with Controls
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.sdp)
            .align(Alignment.TopCenter),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Language Selector
          Surface(
            shape = RoundedCornerShape(12.sdp),
            color = NavyPrimary.copy(alpha = 0.9f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.sdp, vertical = 8.sdp),
              horizontalArrangement = Arrangement.spacedBy(8.sdp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Lucide.Languages, null, tint = AccentGold, modifier = Modifier.size(18.sdp))
              Text(
                selectedLanguage,
                color = Color.White,
                style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold)
              )
              Icon(
                Lucide.ChevronDown,
                null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                  .size(16.sdp)
                  .clickable {
                    selectedLanguage = if (selectedLanguage == "English") "Arabic" else "English"
                  }
              )
            }
          }

          // Fullscreen Toggle
          Box(
            modifier = Modifier
              .size(40.sdp)
              .clip(RoundedCornerShape(12.sdp))
              .background(NavyPrimary.copy(alpha = 0.9f))
              .clickable(onClick = onToggleFullscreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(Lucide.Maximize2, "Fullscreen", tint = Color.White, modifier = Modifier.size(20.sdp))
          }
        }

        // Center Status
        if (!translating) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(100.sdp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(3.sdp, AccentGold.copy(alpha = 0.5f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Lucide.Camera, null, tint = AccentGold, modifier = Modifier.size(48.sdp))
            }
            Spacer(Modifier.height(16.sdp))
            Text(
              "Tap 'Start Detection' below",
              color = Color.White.copy(alpha = 0.7f),
              style = TranslatorBodyStyle.copy(fontWeight = FontWeight.Medium)
            )
          }
        }

        // Bottom Live Indicator
        if (translating) {
          Surface(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(16.sdp),
            shape = RoundedCornerShape(999.sdp),
            color = ErrorRed.copy(alpha = 0.9f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.sdp, vertical = 8.sdp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.sdp)
            ) {
              Box(
                modifier = Modifier
                  .size(10.sdp)
                  .clip(CircleShape)
                  .background(Color.White)
              )
              Text(
                "DETECTING",
                color = Color.White,
                style = TranslatorLabelStyle.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }

      // Control Buttons Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.sdp),
        horizontalArrangement = Arrangement.spacedBy(12.sdp)
      ) {
        // Start/Stop Detection Button
        Box(
          modifier = Modifier
            .weight(1f)
            .height(56.sdp)
            .clip(RoundedCornerShape(16.sdp))
            .background(if (translating) ErrorRed else AccentGold)
            .clickable(onClick = onToggleTranslation),
          contentAlignment = Alignment.Center
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.sdp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              if (translating) Lucide.CircleStop else Lucide.Play,
              null,
              tint = Color.White,
              modifier = Modifier.size(22.sdp)
            )
            Text(
              if (translating) "Stop Detection" else "Start Detection",
              color = Color.White,
              style = TranslatorBodyStyle.copy(fontWeight = FontWeight.Bold)
            )
          }
        }

        // Clear Button
        Box(
          modifier = Modifier
            .size(56.sdp)
            .clip(RoundedCornerShape(16.sdp))
            .background(BlueSecondary)
            .border(2.sdp, Gray200, RoundedCornerShape(16.sdp))
            .clickable(onClick = onClear),
          contentAlignment = Alignment.Center
        ) {
          Icon(Lucide.RotateCcw, "Clear", tint = NavyPrimary, modifier = Modifier.size(22.sdp))
        }
      }
    }
  }

  // Translation Output Card
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.sdp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.sdp)
  ) {
    Column(Modifier.padding(20.sdp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.sdp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Lucide.MessageSquare, null, tint = AccentGold, modifier = Modifier.size(22.sdp))
          Text(
            "Translation",
            color = NavyPrimary,
            style = TranslatorTitleStyle.copy(fontSize = 18.ssp)
          )
        }

        // Copy Button
        Box(
          modifier = Modifier
            .size(40.sdp)
            .clip(RoundedCornerShape(10.sdp))
            .background(if (translatedText.isNotEmpty()) BlueSecondary else BlueSecondary.copy(alpha = 0.5f))
            .clickable(enabled = translatedText.isNotEmpty(), onClick = onCopy),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Lucide.Copy,
            "Copy",
            tint = if (translatedText.isNotEmpty()) NavyPrimary else NavyPrimary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.sdp)
          )
        }
      }

      Spacer(Modifier.height(16.sdp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 120.sdp)
          .clip(RoundedCornerShape(16.sdp))
          .background(BluePrimary)
          .border(2.sdp, Gray200, RoundedCornerShape(16.sdp))
          .padding(16.sdp)
      ) {
        if (translatedText.isEmpty()) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Lucide.Hand, null, tint = Gray200, modifier = Modifier.size(40.sdp))
            Spacer(Modifier.height(8.sdp))
            Text(
              "Start detection to see translations",
              color = TextLight,
              style = TranslatorBodyStyle,
              textAlign = TextAlign.Center
            )
          }
        } else {
          Text(
            translatedText,
            color = NavyPrimary,
            style = TranslatorBodyStyle.copy(
              fontSize = 16.ssp,
              lineHeight = 24.ssp,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }
    }
  }
}

@Composable
private fun CameraStreamPanel(
  modifier: Modifier,
  fullscreen: Boolean,
  status: ConnectionStatus,
  connected: Boolean,
  translating: Boolean,
  currentSign: String?,
  confidence: Int,
  onToggleFullscreen: () -> Unit
) {
  val shape = if (fullscreen) RoundedCornerShape(0.sdp) else RoundedCornerShape(16.sdp)
  Box(
    modifier = modifier.clip(shape).background(Color.Black).border(if (fullscreen) 0.sdp else 2.sdp, if (status == ConnectionStatus.Connected) SuccessGreen else Gray200, shape)
  ) {
    when {
      connected -> {
        Box(
          modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF020617), Color(0xFF111827), NavyPrimary)))
        ) {
          Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.Monitor, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(56.sdp))
            Spacer(Modifier.height(12.sdp))
            Text("Desktop Stream Active", color = Color.White, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold))
            Text("Live recognition preview", color = Color.White.copy(alpha = 0.65f), style = TranslatorLabelStyle)
          }
        }
      }
      status == ConnectionStatus.Connecting -> {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          SpinningIcon(Lucide.LoaderCircle, Color.White.copy(alpha = 0.4f), 44.sdp)
          Spacer(Modifier.height(12.sdp))
          Text("Connecting to desktop stream...", color = Color.White.copy(alpha = 0.55f), style = TranslatorBodyStyle)
        }
      }
      else -> {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(Modifier.size(80.sdp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(3.sdp, Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Lucide.Monitor, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(40.sdp))
          }
          Spacer(Modifier.height(12.sdp))
          Text("Enter the desktop stream URL below to connect", color = Color.White.copy(alpha = 0.5f), style = TranslatorBodyStyle, textAlign = TextAlign.Center)
        }
      }
    }

    if (connected) {
      Surface(
        modifier = Modifier.align(Alignment.TopStart).padding(12.sdp),
        shape = RoundedCornerShape(999.sdp),
        color = SuccessGreen.copy(alpha = 0.2f)
      ) {
        Row(Modifier.padding(horizontal = 10.sdp, vertical = 6.sdp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.sdp)) {
          Box(Modifier.size(8.sdp).clip(CircleShape).background(SuccessGreen))
          Text("Live", color = SuccessGreen, style = TranslatorLabelStyle)
        }
      }
    }

    if (translating && currentSign != null) {
      Surface(
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.sdp),
        shape = RoundedCornerShape(12.sdp),
        color = NavyPrimary.copy(alpha = 0.88f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.sdp, vertical = 12.sdp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.sdp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp), modifier = Modifier.weight(1f, false)) {
            Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(18.sdp))
            Text(currentSign, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.ssp)
          }
          Column(horizontalAlignment = Alignment.End) {
            Box(Modifier.width(40.sdp).height(6.sdp).clip(RoundedCornerShape(999.sdp)).background(Color.White.copy(alpha = 0.2f))) {
              Box(
                Modifier.fillMaxHeight().fillMaxWidth(confidence / 100f).clip(RoundedCornerShape(999.sdp)).background(
                  when {
                    confidence >= 90 -> SuccessGreen
                    confidence >= 80 -> AccentGold
                    else -> WarningGold
                  }
                )
              )
            }
            Spacer(Modifier.height(4.sdp))
            Text("$confidence%", color = Color.White.copy(alpha = 0.7f), style = TranslatorLabelStyle)
          }
        }
      }
    } else if (translating) {
      Surface(
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.sdp),
        shape = RoundedCornerShape(12.sdp),
        color = NavyPrimary.copy(alpha = 0.88f)
      ) {
        Row(Modifier.padding(horizontal = 14.sdp, vertical = 12.sdp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
          SpinningIcon(Lucide.LoaderCircle, Color.White.copy(alpha = 0.7f), 18.sdp)
          Text("Waiting for sign language gestures...", color = Color.White.copy(alpha = 0.7f), style = TranslatorBodyStyle)
        }
      }
    }

    Box(Modifier.align(Alignment.TopEnd).padding(12.sdp).size(32.sdp).clip(RoundedCornerShape(8.sdp)).background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onToggleFullscreen), contentAlignment = Alignment.Center) {
      Icon(if (fullscreen) Lucide.Minimize2 else Lucide.Maximize2, "Toggle fullscreen", tint = Color.White, modifier = Modifier.size(18.sdp))
    }
  }
}

@Composable
private fun OutputCard(
  title: String,
  emptyText: String,
  emptyIcon: ImageVector,
  onCopy: () -> Unit,
  onClear: () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.sdp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().background(BlueSecondary).border(0.sdp, Color.Transparent).padding(horizontal = 16.sdp, vertical = 12.sdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
          Icon(Lucide.MessageSquare, null, tint = NavyPrimary, modifier = Modifier.size(16.sdp))
          Text(title, color = NavyPrimary, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.sdp)) {
          SquareIconButton(Lucide.Copy, onCopy)
          SquareIconButton(Lucide.Trash2, onClear)
        }
      }
      Column(Modifier.fillMaxWidth().padding(16.sdp), content = content)
    }
  }
}

@Composable
private fun InputCard(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  buttonLabel: String,
  buttonEnabled: Boolean,
  onButtonClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.sdp)
  ) {
    Column(Modifier.padding(16.sdp)) {
      Text("Desktop Stream URL", color = NavyPrimary, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold))
      Spacer(Modifier.height(8.sdp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.sdp), verticalAlignment = Alignment.CenterVertically) {
        TranslatorInputField(value, onValueChange, placeholder, Modifier.weight(1f))
        ConnectPanelButton(
          text = buttonLabel,
          enabled = buttonEnabled || buttonLabel == "Connecting...",
          onClick = onButtonClick,
          spinIcon = buttonLabel == "Connecting..."
        )
      }
      Spacer(Modifier.height(8.sdp))
      Text("Open the Athar desktop app, start the camera, and copy the stream URL here.", color = TextLight, style = TranslatorLabelStyle)
    }
  }
}

@Composable
private fun SupportedCharactersCard(currentChar: String?) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.sdp),
    border = BorderStroke(2.sdp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.sdp)
  ) {
    Column {
      Row(Modifier.fillMaxWidth().background(BlueSecondary).padding(horizontal = 16.sdp, vertical = 12.sdp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
        Icon(Lucide.Hand, null, tint = AccentGold, modifier = Modifier.size(16.sdp))
        Text("Supported Characters", color = NavyPrimary, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold))
      }
      Column(Modifier.padding(16.sdp), verticalArrangement = Arrangement.spacedBy(12.sdp)) {
        Text("Letters", color = TextLight, style = TranslatorLabelStyle.copy(fontWeight = FontWeight.SemiBold))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.sdp), verticalArrangement = Arrangement.spacedBy(6.sdp)) {
          gloveAlphabet.forEach { item ->
            CharacterChip(item, currentChar == item)
          }
        }
        Text("Numbers", color = TextLight, style = TranslatorLabelStyle.copy(fontWeight = FontWeight.SemiBold))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.sdp), verticalArrangement = Arrangement.spacedBy(6.sdp)) {
          gloveNumbers.forEach { item ->
            CharacterChip(item, currentChar == item)
          }
        }
      }
    }
  }
}

@Composable
private fun CharacterChip(text: String, selected: Boolean) {
  Box(
    modifier = Modifier.size(32.sdp).clip(RoundedCornerShape(8.sdp)).background(if (selected) AccentGold else BluePrimary).border(1.sdp, Gray200, RoundedCornerShape(8.sdp)),
    contentAlignment = Alignment.Center
  ) {
    Text(text, color = if (selected) Color.White else NavyPrimary, style = TranslatorLabelStyle.copy(fontWeight = FontWeight.Bold))
  }
}

@Composable
private fun BatteryIndicator(level: Int) {
  Box(Modifier.width(18.sdp).height(10.sdp).border(1.sdp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.sdp))) {
    Box(
      Modifier.fillMaxHeight().fillMaxWidth(level / 100f).clip(RoundedCornerShape(3.sdp)).background(if (level > 30) SuccessGreen else ErrorRed)
    )
  }
}

@Composable
private fun SquareIconButton(icon: ImageVector, onClick: () -> Unit) {
  Box(Modifier.size(32.sdp).clip(RoundedCornerShape(8.sdp)).background(BluePrimary).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    Icon(icon, null, tint = TextLight, modifier = Modifier.size(16.sdp))
  }
}

@Composable
private fun FilledPanelButton(text: String, icon: ImageVector?, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, spinIcon: Boolean = false) {
  Row(
    modifier = modifier.heightIn(min = 52.sdp).clip(RoundedCornerShape(12.sdp)).background(color).clickable(onClick = onClick).padding(horizontal = 14.sdp, vertical = 14.sdp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      if (spinIcon) {
        SpinningIcon(icon = icon, tint = Color.White, size = 18.sdp)
      } else {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.sdp))
      }
      Spacer(Modifier.width(8.sdp))
    }
    Text(text, color = Color.White, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
  }
}

private val ConnectButtonBlue = Color(0xFF6B86A7)

@Composable
private fun ConnectPanelButton(text: String, enabled: Boolean, onClick: () -> Unit, spinIcon: Boolean) {
  Row(
    modifier = Modifier
      .width(96.sdp)
      .height(48.sdp)
      .clip(RoundedCornerShape(16.sdp))
      .background(if (enabled) ConnectButtonBlue else TextLight.copy(alpha = 0.8f))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.sdp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (spinIcon) {
      SpinningIcon(icon = Lucide.LoaderCircle, tint = Color.White, size = 16.sdp)
      Spacer(Modifier.width(6.sdp))
    }
    Text(
      text,
      color = Color.White,
      style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold),
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun OutlinePanelButton(text: String, icon: ImageVector?, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Row(
    modifier = modifier.heightIn(min = 52.sdp).clip(RoundedCornerShape(12.sdp)).background(Color.White).border(2.sdp, if (color == ErrorRed) ErrorRed else Gray200, RoundedCornerShape(12.sdp)).clickable(onClick = onClick).padding(horizontal = 12.sdp, vertical = 14.sdp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      Icon(icon, null, tint = color, modifier = Modifier.size(18.sdp))
      Spacer(Modifier.width(8.sdp))
    }
    Text(text, color = color, style = TranslatorBodyStyle.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
  }
}

@Composable
private fun TranslatorInputField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.clip(RoundedCornerShape(16.sdp)).background(Color.White).border(2.sdp, Gray200, RoundedCornerShape(16.sdp)).padding(horizontal = 14.sdp, vertical = 12.sdp)
  ) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions.Default,
      textStyle = TextStyle(color = NavyPrimary, fontFamily = FontFamily.SansSerif, fontSize = 15.ssp, lineHeight = 20.ssp),
      cursorBrush = SolidColor(NavyPrimary),
      modifier = Modifier.fillMaxWidth(),
      decorationBox = { inner ->
        Box(Modifier.fillMaxWidth()) {
          if (value.isEmpty()) Text(placeholder, color = TextLight, style = TranslatorBodyStyle)
          inner()
        }
      }
    )
  }
}

@Composable
private fun EmptyState(text: String, icon: ImageVector) {
  Column(Modifier.fillMaxWidth().padding(vertical = 12.sdp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.sdp)) {
    Icon(icon, null, tint = Gray200, modifier = Modifier.size(32.sdp))
    Text(text, color = TextLight, style = TranslatorBodyStyle, textAlign = TextAlign.Center)
  }
}

@Composable
private fun SpinningIcon(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
  val transition = rememberInfiniteTransition(label = "spinning-icon")
  val rotation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Restart
    ),
    label = "spinning-icon-rotation"
  )

  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = tint,
    modifier = Modifier.size(size).graphicsLayer(rotationZ = rotation)
  )
}

@Composable
private fun PulsingIcon(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
  val transition = rememberInfiniteTransition(label = "pulsing-icon")
  val alpha by transition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulsing-icon-alpha"
  )

  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = tint,
    modifier = Modifier.size(size).graphicsLayer(alpha = alpha)
  )
}

private fun buildAnnotatedString(text: String): AnnotatedString = AnnotatedString(text)

@Composable
private fun CameraNoiseEffect() {
  var noiseKey by remember { mutableStateOf(0) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(100)
      noiseKey++
    }
  }

  Canvas(modifier = Modifier.fillMaxSize()) {
    val random = Random(noiseKey)
    repeat(50) {
      val x = random.nextFloat() * size.width
      val y = random.nextFloat() * size.height
      drawCircle(
        color = Color.White.copy(alpha = random.nextFloat() * 0.05f),
        radius = random.nextFloat() * 2f,
        center = androidx.compose.ui.geometry.Offset(x, y)
      )
    }
  }
}

@Composable
private fun CameraGridOverlay() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val strokeWidth = 1.dp.toPx()
    val gridColor = Color.White.copy(alpha = 0.15f)

    // Vertical lines
    val cols = 3
    for (i in 1 until cols) {
      val x = size.width * i / cols
      drawLine(
        color = gridColor,
        start = androidx.compose.ui.geometry.Offset(x, 0f),
        end = androidx.compose.ui.geometry.Offset(x, size.height),
        strokeWidth = strokeWidth
      )
    }

    // Horizontal lines
    val rows = 3
    for (i in 1 until rows) {
      val y = size.height * i / rows
      drawLine(
        color = gridColor,
        start = androidx.compose.ui.geometry.Offset(0f, y),
        end = androidx.compose.ui.geometry.Offset(size.width, y),
        strokeWidth = strokeWidth
      )
    }
  }
}

@Composable
private fun HandDetectionBox(sign: String, confidence: Int) {
  Box(modifier = Modifier.fillMaxSize()) {
    // Detection Rectangle (simulating hand detection)
    Box(
      modifier = Modifier
        .size(220.sdp, 180.sdp)
        .align(Alignment.Center)
        .offset(y = (-20).dp)
        .border(3.sdp, AccentGold, RoundedCornerShape(12.sdp))
    ) {
      // Corner indicators
      listOf(
        Alignment.TopStart,
        Alignment.TopEnd,
        Alignment.BottomStart,
        Alignment.BottomEnd
      ).forEach { alignment ->
        Box(
          modifier = Modifier
            .size(20.sdp)
            .align(alignment)
            .background(AccentGold, RoundedCornerShape(4.sdp))
        )
      }
    }

    // Detected Sign Label
    Surface(
      modifier = Modifier
        .align(Alignment.Center)
        .offset(y = 100.sdp),
      shape = RoundedCornerShape(16.sdp),
      color = AccentGold
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.sdp, vertical = 12.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          sign,
          color = Color.White,
          style = TranslatorTitleStyle.copy(fontSize = 20.ssp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.sdp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.sdp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .width(60.sdp)
              .height(4.sdp)
              .clip(RoundedCornerShape(999.sdp))
              .background(Color.White.copy(alpha = 0.3f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(confidence / 100f)
                .clip(RoundedCornerShape(999.sdp))
                .background(Color.White)
            )
          }
          Text(
            "$confidence%",
            color = Color.White.copy(alpha = 0.9f),
            style = TranslatorLabelStyle.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    }
  }
}

@Composable
private fun CameraPreview(
  modifier: Modifier = Modifier,
  isDetecting: Boolean,
  onSignDetected: (String, Int) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  AndroidView(
    factory = { ctx ->
      val previewView = PreviewView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      }

      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener({
        try {
          val cameraProvider = cameraProviderFuture.get()

          val preview = Preview.Builder()
            .build()
            .also {
              it.setSurfaceProvider(previewView.surfaceProvider)
            }

          val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

          // Image analysis for detection
          val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
              analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                if (isDetecting) {
                  processImageForSignLanguage(imageProxy, onSignDetected)
                } else {
                  imageProxy.close()
                }
              }
            }

          try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              cameraSelector,
              preview,
              imageAnalysis
            )
          } catch (e: Exception) {
            android.util.Log.e("CameraPreview", "Camera bind failed", e)
          }
        } catch (e: Exception) {
          android.util.Log.e("CameraPreview", "Camera provider failed", e)
        }
      }, ContextCompat.getMainExecutor(ctx))

      previewView
    },
    modifier = modifier
  )
}

private var lastDetectionTime = 0L
private var detectionCounter = 0

private fun processImageForSignLanguage(
  imageProxy: ImageProxy,
  onSignDetected: (String, Int) -> Unit
) {
  try {
    val currentTime = System.currentTimeMillis()

    // Detect every 2 seconds to simulate real detection
    if (currentTime - lastDetectionTime > 2000) {
      lastDetectionTime = currentTime
      detectionCounter++

      // Simulate detection by randomly picking signs from database
      val randomSign = cameraSigns.random()
      val confidence = Random.nextInt(85, 98)
      onSignDetected(randomSign, confidence)
    }

    imageProxy.close()
  } catch (e: Exception) {
    imageProxy.close()
  }
}
