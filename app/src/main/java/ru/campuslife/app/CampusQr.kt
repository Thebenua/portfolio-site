package ru.campuslife.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.EnumMap
import java.util.concurrent.Executors

sealed interface ParsedQrPayload {
    data class EventInvite(
        val eventId: Int,
        val embeddedEvent: Event?
    ) : ParsedQrPayload

    data class ParticipantTicket(
        val eventId: Int,
        val attendeeName: String,
        val attendeeGroup: String,
        val embeddedEvent: Event?
    ) : ParsedQrPayload
}

internal object CampusQrCodec {

    fun buildEventRegistrationPayload(event: Event): String {
        val compact = JSONObject()
            .put("id", event.id)
            .put("t", event.title.take(80))
            .put("c", event.category)
            .put("d", event.date.toString())
            .put("p", event.place.take(60))
            .put("s", event.subtitle.take(70))
            .put("desc", event.description.take(220))
            .put("o", event.organizer.take(50))
            .put("cap", event.capacity)
            .toString()
        val encoded = Base64.encodeToString(
            compact.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return Uri.Builder()
            .scheme("campuslife")
            .authority("register")
            .appendQueryParameter("id", event.id.toString())
            .appendQueryParameter("code", eventQrCodeToken(event.id))
            .appendQueryParameter("data", encoded)
            .build()
            .toString()
    }

    fun buildParticipantTicketPayload(
        event: Event,
        studentName: String,
        studentGroup: String
    ): String {
        return Uri.Builder()
            .scheme("campuslife")
            .authority("ticket")
            .appendQueryParameter("id", event.id.toString())
            .appendQueryParameter("name", studentName.take(50))
            .appendQueryParameter("group", studentGroup.take(30))
            .appendQueryParameter("code", eventQrCodeToken(event.id))
            .build()
            .toString()
    }

    fun parsePayloadOrShortCode(raw: String, existingEvents: List<Event>): ParsedQrPayload? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // 1. URI вида campuslife://register?... или campuslife://ticket?...
        if (trimmed.startsWith("campuslife://", ignoreCase = true)) {
            return try {
                val uri = Uri.parse(trimmed)
                val id = uri.getQueryParameter("id")?.toIntOrNull() ?: return null
                val host = uri.host?.lowercase().orEmpty()
                val embedded = uri.getQueryParameter("data")?.let { decodeEmbeddedEvent(it, id) }
                if (host == "ticket") {
                    ParsedQrPayload.ParticipantTicket(
                        eventId = id,
                        attendeeName = uri.getQueryParameter("name").orEmpty().ifBlank { "Участник по QR" },
                        attendeeGroup = uri.getQueryParameter("group").orEmpty().ifBlank { "Школьное сообщество" },
                        embeddedEvent = embedded
                    )
                } else {
                    ParsedQrPayload.EventInvite(
                        eventId = id,
                        embeddedEvent = embedded
                    )
                }
            } catch (_: Exception) {
                null
            }
        }

        // 2. Короткий код события CL-XXXX или числовой ID события
        val normalized = trimmed.uppercase()
        val matchedByToken = existingEvents.firstOrNull { ev ->
            eventQrCodeToken(ev.id).equals(normalized, ignoreCase = true) ||
                ev.id.toString() == normalized.removePrefix("CL-").trimStart('0').ifEmpty { "0" } ||
                ev.id.toString() == trimmed
        }
        if (matchedByToken != null) {
            return ParsedQrPayload.EventInvite(
                eventId = matchedByToken.id,
                embeddedEvent = matchedByToken
            )
        }

        return null
    }

    private fun decodeEmbeddedEvent(encoded: String, fallbackId: Int): Event? = try {
        val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val obj = JSONObject(String(bytes, StandardCharsets.UTF_8))
        val cat = obj.optString("c", "Наука")
        Event(
            id = obj.optInt("id", fallbackId),
            title = obj.getString("t"),
            category = cat,
            date = LocalDateTime.parse(obj.getString("d")),
            place = obj.optString("p", "Школьный кампус"),
            description = obj.optString("desc", "Мероприятие добавлено по QR-приглашению организатора."),
            subtitle = obj.optString("s", "Приглашение по QR-коду"),
            tint = tintForCategory(cat),
            organizer = obj.optString("o", "Организатор кампуса"),
            capacity = obj.optInt("cap", 40).coerceAtLeast(1),
            baseParticipants = 1,
            isUserCreated = true
        )
    } catch (_: Exception) {
        null
    }

    suspend fun generateQrBitmap(
        content: String,
        sizePx: Int = 720,
        darkColorArgb: Int = 0xFF111317.toInt(),
        lightColorArgb: Int = 0xFFF7F4EE.toInt()
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 2)
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                val offset = y * sizePx
                for (x in 0 until sizePx) {
                    pixels[offset + x] = if (matrix.get(x, y)) darkColorArgb else lightColorArgb
                }
            }
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeQrFromImageUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null

            decodeQrFromBitmap(bmp)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))
            val reader = createQrReader()
            reader.decodeWithState(binary).text
        } catch (_: Exception) {
            null
        }
    }

    fun createQrReader(): MultiFormatReader {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
            put(DecodeHintType.TRY_HARDER, true)
        }
        return MultiFormatReader().apply { setHints(hints) }
    }

    suspend fun shareQrCard(
        context: Context,
        qrBitmap: Bitmap,
        shareTitle: String,
        shareText: String
    ) {
        val uri = withContext(Dispatchers.IO) {
            try {
                val qrDir = File(context.cacheDir, "qr").apply { mkdirs() }
                val file = File(qrDir, "campuslife_qr_${System.currentTimeMillis()}.png")
                file.outputStream().use { out ->
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                null
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            if (uri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться QR-кодом"))
    }
}

private class QrFrameAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = CampusQrCodec.createQrReader()
    @Volatile
    private var handled = false

    override fun analyze(image: ImageProxy) {
        if (handled) {
            image.close()
            return
        }
        try {
            val plane = image.planes.firstOrNull()
            if (plane != null) {
                val buffer = plane.buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val width = image.width
                val height = image.height
                val rowStride = plane.rowStride
                val luminance = if (rowStride == width) {
                    bytes
                } else {
                    val packed = ByteArray(width * height)
                    for (y in 0 until height) {
                        val srcOffset = y * rowStride
                        if (srcOffset + width <= bytes.size) {
                            System.arraycopy(bytes, srcOffset, packed, y * width, width)
                        }
                    }
                    packed
                }

                val source = PlanarYUVLuminanceSource(
                    luminance,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binary = BinaryBitmap(HybridBinarizer(source))
                val result = try {
                    reader.decodeWithState(binary)
                } catch (_: Exception) {
                    null
                } finally {
                    reader.reset()
                }
                val text = result?.text
                if (!text.isNullOrBlank() && !handled) {
                    handled = true
                    onQrDetected(text)
                }
            }
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }
}

/**
 * Модальное окно с живым сгенерированным QR-кодом (приглашение организатора или личный QR-билет участника)
 */
@Composable
fun EventQrModal(
    event: Event,
    isParticipantTicket: Boolean,
    studentName: String,
    studentGroup: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val payload = remember(event, isParticipantTicket, studentName, studentGroup) {
        if (isParticipantTicket) {
            CampusQrCodec.buildParticipantTicketPayload(event, studentName, studentGroup)
        } else {
            CampusQrCodec.buildEventRegistrationPayload(event)
        }
    }
    val shortCode = remember(event.id) { eventQrCodeToken(event.id) }

    var qrBitmap by remember(payload) { mutableStateOf<Bitmap?>(null) }
    var qrImageBitmap by remember(payload) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(payload) {
        val bmp = CampusQrCodec.generateQrBitmap(payload, sizePx = 760)
        qrBitmap = bmp
        qrImageBitmap = bmp?.asImageBitmap()
    }

    CampusModalFrame(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrCodeGlyph(color = CampusColors.WarmSand, size = 19.dp)
                    Text(
                        text = if (isParticipantTicket) "Мой QR-билет" else "QR для регистрации",
                        style = MaterialTheme.typography.titleMedium,
                        color = CampusColors.Milk
                    )
                }

                GlassIconButton(
                    onClick = onDismiss,
                    size = 34.dp,
                    debounce = false,
                    contentDescriptionLabel = "Закрыть QR"
                ) {
                    CloseGlyph(color = CampusColors.Milk, size = 14.dp)
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.5.sp),
                color = CampusColors.Milk,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isParticipantTicket) {
                    "$studentName · $studentGroup"
                } else {
                    "${event.date.format(ShortDateFmt)}, ${event.date.format(TimeFmt)} · ${event.place}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = CampusColors.SlateMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(228.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF7F4EE))
                    .border(1.dp, CampusColors.WarmSand.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val img = qrImageBitmap
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "QR-код мероприятия ${event.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Генерация QR...",
                        style = MaterialTheme.typography.labelMedium,
                        color = CampusColors.DeepObsidian
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Короткий резервный код события для быстрого ввода и копирования
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(CampusColors.Graphite)
                    .border(1.dp, CampusColors.Hairline, RoundedCornerShape(percent = 50))
                    .pressableScale(
                        pressedScale = 0.95f,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Код Campus Life", payload))
                            Toast.makeText(context, "Ссылка и код QR скопированы", Toast.LENGTH_SHORT).show()
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Код: $shortCode",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.WarmSand
                )
                Text(
                    text = "· копировать",
                    style = MaterialTheme.typography.labelMedium,
                    color = CampusColors.SlateMuted
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isParticipantTicket) {
                    "Покажи этот QR-код организатору на входе для мгновенной отметки присутствия."
                } else {
                    "Участники сканируют этот QR-код через кнопку сканера в шапке Campus Life и сразу записываются на событие."
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = CampusColors.SlateMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val bmp = qrBitmap
                        if (bmp != null) {
                            scope.launch {
                                CampusQrCodec.shareQrCard(
                                    context = context,
                                    qrBitmap = bmp,
                                    shareTitle = event.title,
                                    shareText = "Приглашение на событие «${event.title}» (${event.date.format(ShortDateFmt)} в ${event.date.format(TimeFmt)}, ${event.place}). Код быстрой записи в Campus Life: $shortCode\n$payload"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    ShareGlyph(color = CampusColors.DeepObsidian, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "Поделиться QR",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

/**
 * Полноэкранный сканер QR-кодов (живая камера CameraX + распознавание из галереи + ввод кода или ссылки)
 */
@Composable
fun QrScannerScreen(
    allEvents: List<Event>,
    targetCheckInEvent: Event? = null,
    onBack: () -> Unit,
    onDecodedPayload: (ParsedQrPayload) -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraError by remember { mutableStateOf("") }
    var manualInput by rememberSaveable { mutableStateOf("") }
    var feedbackError by rememberSaveable { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            cameraError = "Доступ к камере не выдан. Можно выбрать скриншот QR из галереи или ввести код события ниже."
        } else {
            cameraError = ""
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun handleRawScannedString(raw: String) {
        val parsed = CampusQrCodec.parsePayloadOrShortCode(raw, allEvents)
        if (parsed != null) {
            feedbackError = ""
            onDecodedPayload(parsed)
        } else {
            feedbackError = "QR-код не относится к событиям Campus Life. Проверь код или выбери другой снимок."
        }
    }

    val qrImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = CampusQrCodec.decodeQrFromImageUri(context, uri)
                if (decoded != null) {
                    handleRawScannedString(decoded)
                } else {
                    feedbackError = "На выбранном изображении не удалось распознать QR-код."
                }
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CampusColors.SkyMuted.copy(alpha = 0.6f),
        unfocusedBorderColor = CampusColors.Hairline,
        focusedContainerColor = CampusColors.Graphite,
        unfocusedContainerColor = CampusColors.Graphite,
        focusedLabelColor = CampusColors.SkyMuted,
        unfocusedLabelColor = CampusColors.SlateMuted,
        cursorColor = CampusColors.WarmSand
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CampusColors.DeepObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val horizontalPad = when {
            maxWidth < 360.dp -> 14.dp
            maxWidth >= 600.dp -> 32.dp
            else -> 20.dp
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CampusColors.Graphite)
                            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                            .pressableScale(pressedScale = 0.94f, onClick = onBack)
                            .padding(start = 10.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BackArrowGlyph(color = CampusColors.Milk, size = 18.dp)
                        Text(
                            text = "Назад",
                            style = MaterialTheme.typography.labelLarge,
                            color = CampusColors.Milk
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CampusColors.GraphiteElevated)
                            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                            .pressableScale(
                                pressedScale = 0.94f,
                                onClick = { qrImagePicker.launch(arrayOf("image/*")) }
                            )
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        PhotoGlyph(color = CampusColors.WarmSand, size = 17.dp)
                        Text(
                            text = "Скан из фото",
                            style = MaterialTheme.typography.labelLarge,
                            color = CampusColors.Milk
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = if (targetCheckInEvent != null) {
                        "Проверка QR-билетов"
                    } else {
                        "QR-регистрация на событие"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = CampusColors.Milk
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (targetCheckInEvent != null) {
                        "Наведи камеру на QR-билет участника события «${targetCheckInEvent.title}» или загрузи скриншот."
                    } else {
                        "Наведи камеру на QR-код плаката или приглашения организатора — запись оформится мгновенно."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampusColors.SlateMuted
                )

                Spacer(Modifier.height(18.dp))

                // Видоискатель камеры CameraX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(268.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(CampusColors.GraphiteSubtle)
                        .border(1.dp, CampusColors.HairlineStrong, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && cameraError.isEmpty()) {
                        CameraQrPreview(
                            onQrDetected = { raw -> handleRawScannedString(raw) },
                            onCameraUnavailable = {
                                cameraError = "Камера недоступна на этом устройстве. Используй кнопку «Скан из фото» или код события."
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ScanFrameGlyph(color = CampusColors.WarmSand, size = 34.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = cameraError.ifEmpty { "Для живого сканирования разреши доступ к камере" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CampusColors.SlateMuted,
                                textAlign = TextAlign.Center
                            )
                            if (!hasCameraPermission) {
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CampusColors.GraphiteElevated,
                                        contentColor = CampusColors.Milk
                                    )
                                ) {
                                    Text("Разрешить камеру", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    // Рамка сканирования поверх видеопотока
                    Canvas(modifier = Modifier.size(184.dp)) {
                        val stroke = Stroke(width = 3.dp.toPx())
                        drawRoundRect(
                            color = CampusColors.WarmSand.copy(alpha = 0.72f),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            style = stroke
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = feedbackError.isNotEmpty(),
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(170)) + expandVertically(tween(170)),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(130)) + shrinkVertically(tween(130))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CampusColors.ErrorSoft.copy(alpha = 0.14f))
                                .border(1.dp, CampusColors.ErrorSoft.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = feedbackError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CampusColors.ErrorSoft
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }

                // Резервный ввод кода события / вставка QR-ссылки из буфера
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Код с плаката или ссылка QR",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = CampusColors.Milk
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Например: ${eventQrCodeToken(allEvents.firstOrNull()?.id ?: 1)} или ссылка campuslife://...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = CampusColors.SlateMuted
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = {
                            manualInput = it
                            if (feedbackError.isNotEmpty()) feedbackError = ""
                        },
                        placeholder = {
                            Text(
                                text = "Введи код (например CL-0001) или вставь ссылку",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CampusColors.SlateMuted
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                        shape = RoundedCornerShape(15.dp),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { handleRawScannedString(manualInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CampusColors.WarmSand,
                            contentColor = CampusColors.DeepObsidian
                        )
                    ) {
                        Text(
                            text = if (targetCheckInEvent != null) {
                                "Проверить и отметить участника"
                            } else {
                                "Найти и зарегистрироваться по коду"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun CameraQrPreview(
    onQrDetected: (String) -> Unit,
    onCameraUnavailable: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(
                            analysisExecutor,
                            QrFrameAnalyzer { raw ->
                                mainExecutor.execute { onQrDetected(raw) }
                            }
                        )
                    }

                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (selector == null) {
                    onCameraUnavailable()
                } else {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                }
            } catch (_: Exception) {
                onCameraUnavailable()
            }
        }, mainExecutor)

        onDispose {
            try {
                providerFuture.get().unbindAll()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
