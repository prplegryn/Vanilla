package com.example.vanilla

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur as composeBlur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onCreate(savedInstanceState)
        setContent { VanillaApp() }
    }
}

private data class ChatMessage(
    val id: Int,
    val text: String,
    val fromMe: Boolean,
    val isTyping: Boolean = false
)

private data class GlassSettings(
    val glassBlur: Float = 1f,
    val lensHeight: Float = 18f,
    val lensAmount: Float = 18f,
    val glassAlpha: Float = 0.30f,
    val highlightAlpha: Float = 0.65f,
    val shadowAlpha: Float = 0.07f,
    val innerShadowAlpha: Float = 0.06f
)

private data class AppSettings(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val assistantName: String = "friend",
    val systemPrompt: String = "",
    val assistantAvatarUri: String = "",
    val chatBackgroundUri: String = "",
    val backgroundBlur: Float = 0f,
    val backgroundDim: Float = 0.15f,
    val topScrimAlpha: Float = 0.22f,
    val topScrimHeight: Float = 58f,
    val bottomScrimAlpha: Float = 0.26f,
    val bottomScrimHeight: Float = 82f,
    val scrimSmoothness: Float = 220f,
    val glass: GlassSettings = GlassSettings()
)

@Composable
private fun VanillaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appSettings by remember { mutableStateOf(loadAppSettings(context)) }
    fun updateSettings(next: AppSettings) {
        appSettings = next
        saveAppSettings(context, next)
    }

    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var assistantDetailOpen by remember { mutableStateOf(false) }
    var apiDetailOpen by remember { mutableStateOf(false) }
    var scrimDetailOpen by remember { mutableStateOf(false) }
    var glassDetailOpen by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var nextMessageId by remember { mutableIntStateOf(1) }
    var inputBarHeightPx by remember { mutableIntStateOf(92) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val sessions = remember {
        mutableStateListOf<String>().apply { addAll(loadSessions(context)) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(sessions.toList()) {
        saveSessions(context, sessions)
    }

    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.text,
        messages.lastOrNull()?.isTyping,
        inputBarHeightPx
    ) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    BackHandler(enabled = glassDetailOpen) { glassDetailOpen = false }
    BackHandler(enabled = scrimDetailOpen) { scrimDetailOpen = false }
    BackHandler(enabled = apiDetailOpen) { apiDetailOpen = false }
    BackHandler(enabled = assistantDetailOpen) { assistantDetailOpen = false }
    BackHandler(enabled = settingsOpen && !assistantDetailOpen && !apiDetailOpen && !scrimDetailOpen && !glassDetailOpen) {
        settingsOpen = false
    }
    BackHandler(enabled = drawerOpen && !settingsOpen && !assistantDetailOpen && !apiDetailOpen && !scrimDetailOpen && !glassDetailOpen) {
        drawerOpen = false
    }

    val drawerProgress by animateFloatAsState(if (drawerOpen) 1f else 0f, tween(230), label = "drawer")
    val settingsProgress by animateFloatAsState(if (settingsOpen) 1f else 0f, tween(220), label = "settings")
    val assistantProgress by animateFloatAsState(if (assistantDetailOpen) 1f else 0f, tween(210), label = "assistant")
    val apiProgress by animateFloatAsState(if (apiDetailOpen) 1f else 0f, tween(210), label = "api")
    val scrimProgress by animateFloatAsState(if (scrimDetailOpen) 1f else 0f, tween(210), label = "scrim")
    val glassProgress by animateFloatAsState(if (glassDetailOpen) 1f else 0f, tween(210), label = "glass")

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(nextMessageId++, text, fromMe = true)
        messages += userMessage
        inputText = ""

        val history = messages.filter { !it.isTyping }

        scope.launch {
            var rawReply = ""
            var aiMessageId: Int? = null

            suspend fun ensureAiBubble(typing: Boolean) {
                if (aiMessageId == null) {
                    aiMessageId = nextMessageId++
                    messages += ChatMessage(
                        id = aiMessageId!!,
                        text = "",
                        fromMe = false,
                        isTyping = typing
                    )
                }
            }

            suspend fun updateAiBubble(text: String, typing: Boolean) {
                ensureAiBubble(typing)
                val index = messages.indexOfFirst { it.id == aiMessageId }
                if (index >= 0) {
                    messages[index] = messages[index].copy(
                        text = text,
                        isTyping = typing
                    )
                }
            }

            val finalReply = requestAiReplyStreaming(
                settings = appSettings,
                messages = history,
                onDelta = { delta ->
                    rawReply += delta
                    val cleaned = stripThinkTags(rawReply)

                    if (rawReply.contains("<think", ignoreCase = true) && cleaned.isBlank()) {
                        updateAiBubble("", typing = true)
                    } else if (cleaned.isNotBlank()) {
                        updateAiBubble(cleaned, typing = false)
                    }
                }
            )

            val cleanedFinal = stripThinkTags(finalReply).ifBlank {
                "我没有收到有效回复。"
            }
            updateAiBubble(cleanedFinal, typing = false)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val chatOffsetPx = (screenWidthPx * drawerProgress).roundToInt()
        val drawerOffsetPx = (screenWidthPx * (drawerProgress - 1f)).roundToInt()
        val settingsOffsetPx = (screenWidthPx * (1f - settingsProgress)).roundToInt()
        val assistantOffsetPx = (screenWidthPx * (1f - assistantProgress)).roundToInt()
        val apiOffsetPx = (screenWidthPx * (1f - apiProgress)).roundToInt()
        val scrimOffsetPx = (screenWidthPx * (1f - scrimProgress)).roundToInt()
        val glassOffsetPx = (screenWidthPx * (1f - glassProgress)).roundToInt()
        val pushedProgress = maxOf(assistantProgress, apiProgress, scrimProgress, glassProgress)
        val settingsUnderOffsetPx = (screenWidthPx * -0.18f * pushedProgress).roundToInt()
        val inputBottomPadding = with(density) { inputBarHeightPx.toDp() + 26.dp }
        val backdrop = rememberLayerBackdrop()

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            ChatBackgroundScene(appSettings)

            ChatScene(
                messages = messages,
                chatOffsetPx = chatOffsetPx,
                bottomPadding = inputBottomPadding,
                listState = listState
            )

            ChatEdgeScrims(
                settings = appSettings,
                chatOffsetPx = chatOffsetPx,
                inputBarHeightPx = inputBarHeightPx
            )

            if (drawerProgress > 0.001f) {
                DrawerScene(drawerOffsetPx = drawerOffsetPx, sessions = sessions)
            }

            if (settingsProgress > 0.001f) {
                SettingsHomeScene(
                    offsetPx = settingsOffsetPx + settingsUnderOffsetPx,
                    onOpenAssistantSettings = { assistantDetailOpen = true },
                    onOpenApiSettings = { apiDetailOpen = true },
                    onOpenScrimSettings = { scrimDetailOpen = true },
                    onOpenGlassSettings = { glassDetailOpen = true }
                )
            }

            if (assistantProgress > 0.001f) {
                AssistantSettingsScene(
                    settings = appSettings,
                    offsetPx = assistantOffsetPx,
                    onSettingsChange = ::updateSettings
                )
            }

            if (apiProgress > 0.001f) {
                ApiProviderSettingsScene(
                    settings = appSettings,
                    offsetPx = apiOffsetPx,
                    onSettingsChange = ::updateSettings
                )
            }

            if (scrimProgress > 0.001f) {
                ChatScrimSettingsScene(
                    settings = appSettings,
                    offsetPx = scrimOffsetPx,
                    onSettingsChange = ::updateSettings
                )
            }

            if (glassProgress > 0.001f) {
                GlassStyleSettingsScene(
                    settings = appSettings.glass,
                    offsetPx = glassOffsetPx,
                    onSettingsUpdate = { update ->
                        updateSettings(appSettings.copy(glass = update(appSettings.glass)))
                    }
                )
            }
        }

        ChatTopBar(
            backdrop = backdrop,
            settings = appSettings,
            chatOffsetPx = chatOffsetPx,
            onMenuClick = { drawerOpen = true },
            onAddClick = {
                val firstText = messages.firstOrNull { !it.isTyping }?.text?.trim().orEmpty()
                if (firstText.isNotEmpty()) {
                    val title = firstText
                        .replace(Regex("\\s+"), " ")
                        .take(32)
                    sessions.add(title)
                    messages.clear()
                    inputText = ""
                }
            }
        )

        ChatInputBar(
            backdrop = backdrop,
            glass = appSettings.glass,
            chatOffsetPx = chatOffsetPx,
            value = inputText,
            onValueChange = { inputText = it },
            onHeightChanged = { inputBarHeightPx = it },
            onSend = ::sendMessage
        )

        if (drawerProgress > 0.001f && !settingsOpen) {
            DrawerSettingsButton(
                backdrop = backdrop,
                glass = appSettings.glass,
                drawerOffsetPx = drawerOffsetPx,
                alpha = drawerProgress,
                onClick = { settingsOpen = true }
            )
        }
    }
}

@Composable
private fun ChatBackgroundScene(settings: AppSettings) {
    Box(Modifier.fillMaxSize()) {
        if (settings.chatBackgroundUri.isNotBlank()) {
            UriImage(
                uriString = settings.chatBackgroundUri,
                modifier = Modifier
                    .fillMaxSize()
                    .composeBlur(settings.backgroundBlur.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            DefaultBackground()
        }

        if (settings.backgroundDim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.backgroundDim.coerceIn(0f, 0.85f)))
            )
        }
    }
}

@Composable
private fun DefaultBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFF8F1),
                        Color(0xFFEAF2FF),
                        Color(0xFFFFEDF7)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF8EA2FF).copy(alpha = 0.22f), size.minDimension * 0.36f, Offset(size.width * 0.16f, size.height * 0.22f))
            drawCircle(Color(0xFFFF8DB3).copy(alpha = 0.18f), size.minDimension * 0.32f, Offset(size.width * 0.86f, size.height * 0.14f))
            drawCircle(Color(0xFF63D6B8).copy(alpha = 0.17f), size.minDimension * 0.34f, Offset(size.width * 0.82f, size.height * 0.82f))
            drawGlassLines()
        }
    }
}

private fun DrawScope.drawGlassLines() {
    val lineColor = Color.White.copy(alpha = 0.28f)
    val strokeWidth = 2.dp.toPx()
    repeat(7) { index ->
        val y = size.height * (0.18f + index * 0.1f)
        drawLine(lineColor, Offset(size.width * 0.08f, y), Offset(size.width * 0.92f, y + 42.dp.toPx()), strokeWidth, StrokeCap.Round)
    }
}

@Composable
private fun ChatEdgeScrims(
    settings: AppSettings,
    chatOffsetPx: Int,
    inputBarHeightPx: Int
) {
    val density = LocalDensity.current
    val inputBarHeightDp = with(density) { inputBarHeightPx.toDp() }

    val smoothness = settings.scrimSmoothness
        .roundToInt()
        .coerceIn(60, 1200)

    val bottomHeight by animateDpAsState(
        targetValue = inputBarHeightDp + settings.bottomScrimHeight.dp,
        animationSpec = tween(durationMillis = smoothness),
        label = "bottomScrimHeight"
    )

    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxSize()
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(settings.topScrimHeight.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = settings.topScrimAlpha.coerceIn(0f, 0.85f)),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = settings.bottomScrimAlpha.coerceIn(0f, 0.85f))
                        )
                    )
                )
        )
    }
}

@Composable
private fun DrawerScene(
    drawerOffsetPx: Int,
    sessions: List<String>
) {
    Box(
        Modifier
            .offset { IntOffset(drawerOffsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .padding(horizontal = 28.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 34.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            BasicText(
                text = "会话",
                modifier = Modifier.padding(start = 2.dp),
                style = TextStyle(Color(0xFF1F2937), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                BasicText(
                    text = "暂无会话",
                    modifier = Modifier.padding(start = 2.dp),
                    style = TextStyle(Color(0xFF667085), fontSize = 15.sp)
                )
            } else {
                sessions.forEach { DrawerItem(it) }
            }
        }
    }
}

@Composable
private fun DrawerItem(text: String) {
    var showShape by remember { mutableStateOf(false) }

    LaunchedEffect(showShape) {
        if (showShape) {
            delay(520)
            showShape = false
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (showShape) Color.White.copy(alpha = 0.78f) else Color.Transparent)
            .pointerInput(text) {
                detectTapGestures(onLongPress = { showShape = true })
            }
            .padding(start = 8.dp, end = 8.dp, top = 13.dp, bottom = 13.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(Color(0xFF202838), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ChatScene(
    messages: List<ChatMessage>,
    chatOffsetPx: Int,
    bottomPadding: Dp,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = 122.dp,
            bottom = bottomPadding,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubbleEntry(message)
        }
    }
}

@Composable
private fun MessageBubbleEntry(message: ChatMessage) {
    var visible by remember(message.id) { mutableStateOf(false) }

    LaunchedEffect(message.id) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) +
            slideInVertically(tween(170), initialOffsetY = { it / 3 }) +
            scaleIn(tween(170), initialScale = 0.96f)
    ) {
        MessageBubble(message)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 286.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (message.fromMe) 22.dp else 8.dp,
                        bottomEnd = if (message.fromMe) 8.dp else 22.dp
                    )
                )
                .background(if (message.fromMe) Color(0xFF4A7DFF) else Color.White)
                .padding(start = 15.dp, end = 15.dp, top = 9.dp, bottom = 9.dp)
        ) {
            if (message.isTyping) {
                TypingText()
            } else if (message.fromMe) {
                BasicText(
                    text = message.text,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                )
            } else {
                FormattedAiMessage(message.text)
            }
        }
    }
}

@Composable
private fun TypingText() {
    var dots by remember { mutableIntStateOf(1) }
    val transition = rememberInfiniteTransition(label = "typingAlpha")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(980),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingAlphaValue"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(520)
            dots = if (dots >= 3) 1 else dots + 1
        }
    }

    BasicText(
        text = "正在输入${".".repeat(dots)}",
        style = TextStyle(
            color = Color(0xFF222B3A).copy(alpha = alpha),
            fontSize = 16.sp,
            lineHeight = 22.sp
        )
    )
}

private sealed class MessagePart {
    data class Text(val value: String) : MessagePart()
    data class Code(val language: String, val value: String) : MessagePart()
    data class Formula(val value: String) : MessagePart()
}

@Composable
private fun FormattedAiMessage(text: String) {
    val parts = remember(text) { parseMessageParts(text) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> {
                    if (part.value.isNotBlank()) {
                        BasicText(
                            text = part.value.trim(),
                            style = TextStyle(
                                color = Color(0xFF222B3A),
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
                is MessagePart.Code -> {
                    CodeBlock(part.language, part.value)
                }
                is MessagePart.Formula -> {
                    FormulaBlock(part.value)
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101828))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (language.isNotBlank()) {
            BasicText(
                text = language,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        BasicText(
            text = code.trimEnd(),
            style = TextStyle(
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
private fun FormulaBlock(value: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF2F4F7))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = prettifyFormula(value),
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

private fun parseMessageParts(text: String): List<MessagePart> {
    val result = mutableListOf<MessagePart>()
    var cursor = 0

    while (cursor < text.length) {
        val codeStart = text.indexOf("```", cursor)

        if (codeStart < 0) {
            result += parseTextAndFormula(text.substring(cursor))
            break
        }

        if (codeStart > cursor) {
            result += parseTextAndFormula(text.substring(cursor, codeStart))
        }

        val afterFence = codeStart + 3
        val lineEnd = text.indexOf('\n', afterFence)
        val codeEndSearchFrom = if (lineEnd >= 0) lineEnd + 1 else afterFence
        val codeEnd = text.indexOf("```", codeEndSearchFrom)

        if (codeEnd < 0) {
            result += parseTextAndFormula(text.substring(codeStart))
            break
        }

        val language = if (lineEnd >= 0 && lineEnd < codeEnd) {
            text.substring(afterFence, lineEnd).trim()
        } else {
            ""
        }

        val codeBodyStart = if (lineEnd >= 0 && lineEnd < codeEnd) {
            lineEnd + 1
        } else {
            afterFence
        }

        result += MessagePart.Code(
            language = language,
            value = text.substring(codeBodyStart, codeEnd)
        )

        cursor = codeEnd + 3
    }

    return result.filter {
        when (it) {
            is MessagePart.Text -> it.value.isNotBlank()
            is MessagePart.Code -> it.value.isNotBlank()
            is MessagePart.Formula -> it.value.isNotBlank()
        }
    }
}

private fun parseTextAndFormula(text: String): List<MessagePart> {
    val result = mutableListOf<MessagePart>()
    var cursor = 0

    fun nextMarkerIndex(from: Int): Pair<Int, String>? {
        val markers = listOf(
            Pair(text.indexOf("\$\$", from), "\$\$"),
            Pair(text.indexOf("\\[", from), "\\["),
            Pair(text.indexOf("\\(", from), "\\(")
        ).filter { it.first >= 0 }

        return markers.minByOrNull { it.first }
    }

    while (cursor < text.length) {
        val next = nextMarkerIndex(cursor)

        if (next == null) {
            val remain = text.substring(cursor)
            if (remain.isNotBlank()) result += MessagePart.Text(remain)
            break
        }

        val startIndex = next.first
        val startMarker = next.second

        if (startIndex > cursor) {
            val before = text.substring(cursor, startIndex)
            if (before.isNotBlank()) result += MessagePart.Text(before)
        }

        val endMarker = when (startMarker) {
            "\$\$" -> "\$\$"
            "\\[" -> "\\]"
            "\\(" -> "\\)"
            else -> ""
        }

        val contentStart = startIndex + startMarker.length
        val endIndex = text.indexOf(endMarker, contentStart)

        if (endIndex < 0) {
            val remain = text.substring(startIndex)
            if (remain.isNotBlank()) result += MessagePart.Text(remain)
            break
        }

        val formula = text.substring(contentStart, endIndex).trim()
        if (formula.isNotBlank()) {
            result += MessagePart.Formula(formula)
        }

        cursor = endIndex + endMarker.length
    }

    return result
}

private fun prettifyFormula(value: String): String {
    return value
        .replace("\\times", "×")
        .replace("\\cdot", "·")
        .replace("\\leq", "≤")
        .replace("\\geq", "≥")
        .replace("\\neq", "≠")
        .replace("\\approx", "≈")
        .replace("\\infty", "∞")
        .replace("\\sqrt", "√")
        .replace("\\pi", "π")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\theta", "θ")
        .replace("\\lambda", "λ")
        .replace("\\mu", "μ")
}
@Composable
private fun ChatTopBar(
    backdrop: Backdrop,
    settings: AppSettings,
    chatOffsetPx: Int,
    onMenuClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = settings.glass,
            modifier = Modifier.align(Alignment.CenterStart).size(50.dp),
            shape = Capsule(),
            onClick = onMenuClick
        ) { MenuIcon() }

        GlassSurface(
            backdrop = backdrop,
            settings = settings.glass,
            modifier = Modifier.align(Alignment.Center).width(108.dp).height(50.dp),
            shape = Capsule()
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 7.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(settings.assistantAvatarUri, Modifier.size(34.dp))
                BasicText(
                    text = settings.assistantName.ifBlank { "friend" },
                    style = TextStyle(Color(0xFF202838), fontSize = 17.sp, fontWeight = FontWeight.Normal)
                )
            }
        }

        GlassSurface(
            backdrop = backdrop,
            settings = settings.glass,
            modifier = Modifier.align(Alignment.CenterEnd).size(50.dp),
            shape = Capsule(),
            onClick = onAddClick
        ) { PlusIcon(fontSize = 30) }
    }
}

@Composable
private fun BoxScope.ChatInputBar(
    backdrop: Backdrop,
    glass: GlassSettings,
    chatOffsetPx: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onSend: () -> Unit
) {
    val textScroll = rememberScrollState()

    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .heightIn(min = 58.dp, max = 148.dp)
            .onSizeChanged { onHeightChanged(it.height) }
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = glass,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp, max = 132.dp),
            shape = RoundedCornerShape(29.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 96.dp)
                        .verticalScroll(textScroll)
                        .padding(top = 8.dp, bottom = 7.dp),
                    textStyle = TextStyle(Color(0xFF162033), fontSize = 17.sp, lineHeight = 24.sp),
                    singleLine = false,
                    maxLines = 4,
                    cursorBrush = SolidColor(Color(0xFF4A7DFF)),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                BasicText(
                                    text = "Message",
                                    style = TextStyle(Color(0xFF697386).copy(alpha = 0.70f), fontSize = 17.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                SendButton(size = 42f, onClick = onSend)
            }
        }
    }
}

@Composable
private fun DrawerSettingsButton(
    backdrop: Backdrop,
    glass: GlassSettings,
    drawerOffsetPx: Int,
    alpha: Float,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .offset { IntOffset(drawerOffsetPx, 0) }
            .fillMaxSize()
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = glass,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 22.dp, bottom = 26.dp)
                .size(58.dp),
            shape = Capsule(),
            onClick = onClick
        ) {
            BasicText(
                text = "⚙",
                style = TextStyle(Color(0xFF202838).copy(alpha = alpha), fontSize = 25.sp, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun SettingsHomeScene(
    offsetPx: Int,
    onOpenAssistantSettings: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenScrimSettings: () -> Unit,
    onOpenGlassSettings: () -> Unit
) {
    Box(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .padding(horizontal = 28.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 34.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            BasicText(
                text = "设置",
                modifier = Modifier.padding(start = 2.dp),
                style = TextStyle(Color(0xFF1F2937), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(16.dp))

            SettingsListItem("AI 助手设定", "头像、名字、提示词和聊天背景", onOpenAssistantSettings)
            SettingsListItem("API 提供商设置", "Base URL、API Key、模型名称", onOpenApiSettings)
            SettingsListItem("聊天界面渐变遮罩", "顶部、底部暗色渐变和过渡", onOpenScrimSettings)
            SettingsListItem("液态玻璃样式设置", "模糊、折射、高光和阴影", onOpenGlassSettings)
        }
    }
}

@Composable
private fun SettingsListItem(title: String, subtitle: String, onClick: () -> Unit) {
    var showShape by remember { mutableStateOf(false) }

    LaunchedEffect(showShape) {
        if (showShape) {
            delay(520)
            showShape = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (showShape) Color.White.copy(alpha = 0.78f) else Color.Transparent)
            .pointerInput(onClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showShape = true }
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        BasicText(
            text = title,
            style = TextStyle(Color(0xFF202838), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
        BasicText(
            text = subtitle,
            style = TextStyle(Color(0xFF667085), fontSize = 13.sp, lineHeight = 18.sp)
        )
    }
}

@Composable
private fun AssistantSettingsScene(
    settings: AppSettings,
    offsetPx: Int,
    onSettingsChange: (AppSettings) -> Unit
) {
    val context = LocalContext.current

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            onSettingsChange(settings.copy(assistantAvatarUri = uri.toString()))
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            onSettingsChange(settings.copy(chatBackgroundUri = uri.toString()))
        }
    }

    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BasicText(
            text = "AI 助手",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(Color(0xFF1F2937), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        )

        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(settings.assistantAvatarUri, Modifier.size(58.dp).clickableNoRipple {
                avatarLauncher.launch(arrayOf("image/*"))
            })

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "助手名字",
                    style = TextStyle(Color(0xFF202838), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                )
                SettingTextField(
                    value = settings.assistantName,
                    placeholder = "friend",
                    singleLine = true,
                    onValueChange = { onSettingsChange(settings.copy(assistantName = it)) }
                )
            }
        }

        SettingTextArea(
            label = "提示词设定",
            value = settings.systemPrompt,
            placeholder = "例如：你是一个简洁、温柔、直接的 AI 助手。",
            onValueChange = { onSettingsChange(settings.copy(systemPrompt = it)) }
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                text = "聊天背景设定",
                modifier = Modifier.padding(start = 2.dp),
                style = TextStyle(Color(0xFF202838), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.62f))
                    .clickableNoRipple { backgroundLauncher.launch(arrayOf("image/*")) },
                contentAlignment = Alignment.Center
            ) {
                if (settings.chatBackgroundUri.isNotBlank()) {
                    UriImage(settings.chatBackgroundUri, Modifier.fillMaxSize(), ContentScale.Crop)
                } else {
                    BasicText(
                        text = "点击选择本地图片\n自动裁切为全屏显示",
                        style = TextStyle(Color(0xFF667085), fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                    )
                }
            }
        }

        ParamSlider("聊天背景模糊度", settings.backgroundBlur, 0f..30f, "px") {
            onSettingsChange(settings.copy(backgroundBlur = it))
        }

        ParamSlider("聊天背景明暗度", settings.backgroundDim, 0f..0.75f, "%") {
            onSettingsChange(settings.copy(backgroundDim = it))
        }
    }
}

@Composable
private fun ApiProviderSettingsScene(
    settings: AppSettings,
    offsetPx: Int,
    onSettingsChange: (AppSettings) -> Unit
) {
    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BasicText(
            text = "API 提供商",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(Color(0xFF1F2937), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        )

        ApiTextField("Base URL", settings.apiBaseUrl, "https://api.openai.com/v1") {
            onSettingsChange(settings.copy(apiBaseUrl = it))
        }
        ApiTextField("API Key", settings.apiKey, "sk-...") {
            onSettingsChange(settings.copy(apiKey = it))
        }
        ApiTextField("模型名称", settings.modelName, "gpt-4o-mini / deepseek-chat / ...") {
            onSettingsChange(settings.copy(modelName = it))
        }
    }
}

@Composable
private fun ChatScrimSettingsScene(
    settings: AppSettings,
    offsetPx: Int,
    onSettingsChange: (AppSettings) -> Unit
) {
    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BasicText(
            text = "聊天渐变遮罩",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                color = Color(0xFF1F2937),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        ParamSlider("顶部暗色透明度", settings.topScrimAlpha, 0f..0.75f, "%") {
            onSettingsChange(settings.copy(topScrimAlpha = it))
        }

        ParamSlider("顶部渐变高度", settings.topScrimHeight, 24f..120f, "px") {
            onSettingsChange(settings.copy(topScrimHeight = it))
        }

        ParamSlider("底部暗色透明度", settings.bottomScrimAlpha, 0f..0.85f, "%") {
            onSettingsChange(settings.copy(bottomScrimAlpha = it))
        }

        ParamSlider("底部渐变高度", settings.bottomScrimHeight, 32f..180f, "px") {
            onSettingsChange(settings.copy(bottomScrimHeight = it))
        }

        ParamSlider("过渡平滑度", settings.scrimSmoothness, 60f..800f, "ms") {
            onSettingsChange(settings.copy(scrimSmoothness = it))
        }
    }
}

@Composable
private fun GlassStyleSettingsScene(
    settings: GlassSettings,
    offsetPx: Int,
    onSettingsUpdate: ((GlassSettings) -> GlassSettings) -> Unit
) {
    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BasicText(
            text = "液态玻璃样式",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(Color(0xFF1F2937), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        )

        SafeGlassPreviewCard(settings)

        ParamSlider("背景模糊", settings.glassBlur, 0f..24f, "px") {
            onSettingsUpdate { current -> current.copy(glassBlur = it) }
        }
        ParamSlider("折射高度", settings.lensHeight, 0f..36f, "px") {
            onSettingsUpdate { current -> current.copy(lensHeight = it) }
        }
        ParamSlider("折射强度", settings.lensAmount, 0f..48f, "px") {
            onSettingsUpdate { current -> current.copy(lensAmount = it) }
        }
        ParamSlider("玻璃白色层", settings.glassAlpha, 0f..0.75f, "%") {
            onSettingsUpdate { current -> current.copy(glassAlpha = it) }
        }
        ParamSlider("高光", settings.highlightAlpha, 0f..1f, "%") {
            onSettingsUpdate { current -> current.copy(highlightAlpha = it) }
        }
        ParamSlider("外阴影", settings.shadowAlpha, 0f..0.35f, "%") {
            onSettingsUpdate { current -> current.copy(shadowAlpha = it) }
        }
        ParamSlider("内阴影", settings.innerShadowAlpha, 0f..0.45f, "%") {
            onSettingsUpdate { current -> current.copy(innerShadowAlpha = it) }
        }
    }
}

@Composable
private fun SafeGlassPreviewCard(settings: GlassSettings) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEAF2FF), Color(0xFFFFEDF7), Color(0xFFEFFFF8))))
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .width(176.dp)
                .height(54.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = settings.glassAlpha.coerceIn(0.12f, 0.75f))),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "Glass Preview",
                style = TextStyle(Color(0xFF202838), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun ApiTextField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = label,
            style = TextStyle(Color(0xFF202838), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        )
        SettingTextField(value, placeholder, true, onValueChange)
    }
}

@Composable
private fun SettingTextField(
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        singleLine = singleLine,
        textStyle = TextStyle(Color(0xFF162033), fontSize = 15.sp),
        cursorBrush = SolidColor(Color(0xFF4A7DFF)),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(Color(0xFF667085).copy(alpha = 0.62f), fontSize = 15.sp)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SettingTextArea(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = label,
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(Color(0xFF202838), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp, max = 220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            textStyle = TextStyle(Color(0xFF162033), fontSize = 15.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(Color(0xFF4A7DFF)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(Color(0xFF667085).copy(alpha = 0.62f), fontSize = 15.sp, lineHeight = 22.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit
) {
    val step = if (suffix == "%") 0.05f else 1f
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    fun setValue(newValue: Float) {
        onValueChange(newValue.coerceIn(range.start, range.endInclusive))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(text = label, style = TextStyle(Color(0xFF344054), fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            BasicText(text = formatSettingValue(value, suffix), style = TextStyle(Color(0xFF667085), fontSize = 13.sp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallAdjustButton("−") { setValue(value - step) }
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(Capsule())
                    .background(Color(0xFF101828).copy(alpha = 0.12f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(Capsule())
                        .background(Color(0xFF101828).copy(alpha = 0.48f))
                )
            }
            SmallAdjustButton("+") { setValue(value + step) }
        }
    }
}

@Composable
private fun SmallAdjustButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF2F4F7))
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text = text, style = TextStyle(Color(0xFF202838), fontSize = 22.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun GlassSurface(
    backdrop: Backdrop,
    settings: GlassSettings,
    modifier: Modifier = Modifier,
    shape: Shape = Capsule(),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(settings.glassBlur)
                    if (settings.lensHeight > 0f && settings.lensAmount > 0f) {
                        lens(
                            refractionHeight = settings.lensHeight,
                            refractionAmount = settings.lensAmount,
                            chromaticAberration = false
                        )
                    }
                },
                highlight = { Highlight.Ambient.copy(alpha = settings.highlightAlpha) },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = settings.shadowAlpha)) },
                innerShadow = {
                    InnerShadow(
                        radius = 6.dp,
                        color = Color.Black.copy(alpha = settings.innerShadowAlpha),
                        alpha = settings.innerShadowAlpha
                    )
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = settings.glassAlpha)) }
            )
            .then(if (onClick != null) Modifier.clickableNoRipple(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun AvatarImage(uriString: String, modifier: Modifier) {
    if (uriString.isNotBlank()) {
        UriImage(uriString, modifier.clip(CircleShape), ContentScale.Crop)
    } else {
        Box(
            modifier
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF687BFF), Color(0xFFFF8DB3))))
        )
    }
}

@Composable
private fun UriImage(uriString: String, modifier: Modifier, contentScale: ContentScale) {
    val context = LocalContext.current
    val bitmap = remember(uriString) { loadBitmapFromUri(context, uriString) }

    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(Color(0xFFE8ECF7)))
    }
}

private fun loadBitmapFromUri(context: Context, uriString: String) = runCatching {
    val uri = Uri.parse(uriString)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    val target = 1600
    var sample = 1
    while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample.coerceAtLeast(1)
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }

    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun MenuIcon() {
    Canvas(Modifier.size(23.dp, 18.dp)) {
        val strokeWidth = 2.3.dp.toPx()
        val color = Color(0xFF202838)
        drawLine(color, Offset(1.dp.toPx(), 2.dp.toPx()), Offset(size.width - 1.dp.toPx(), 2.dp.toPx()), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(1.dp.toPx(), size.height / 2f), Offset(size.width - 1.dp.toPx(), size.height / 2f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(1.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 1.dp.toPx(), size.height - 2.dp.toPx()), strokeWidth, StrokeCap.Round)
    }
}

@Composable
private fun PlusIcon(fontSize: Int) {
    BasicText(
        text = "+",
        style = TextStyle(Color(0xFF202838), fontSize = fontSize.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    )
}

@Composable
private fun SendButton(size: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5E7EB))
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "↑",
            style = TextStyle(Color(0xFF303846), fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
    }
}

private fun formatSettingValue(value: Float, suffix: String): String {
    return if (suffix == "%") "${(value * 100f).roundToInt()}%" else "${value.roundToInt()}$suffix"
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun loadAppSettings(context: Context): AppSettings {
    val p = context.getSharedPreferences("vanilla_settings", Context.MODE_PRIVATE)
    val glass = GlassSettings(
        glassBlur = p.getFloat("glassBlur", 1f),
        lensHeight = p.getFloat("lensHeight", 18f),
        lensAmount = p.getFloat("lensAmount", 18f),
        glassAlpha = p.getFloat("glassAlpha", 0.30f),
        highlightAlpha = p.getFloat("highlightAlpha", 0.65f),
        shadowAlpha = p.getFloat("shadowAlpha", 0.07f),
        innerShadowAlpha = p.getFloat("innerShadowAlpha", 0.06f)
    )
    return AppSettings(
        apiBaseUrl = p.getString("apiBaseUrl", "") ?: "",
        apiKey = p.getString("apiKey", "") ?: "",
        modelName = p.getString("modelName", "") ?: "",
        assistantName = p.getString("assistantName", "friend") ?: "friend",
        systemPrompt = p.getString("systemPrompt", "") ?: "",
        assistantAvatarUri = p.getString("assistantAvatarUri", "") ?: "",
        chatBackgroundUri = p.getString("chatBackgroundUri", "") ?: "",
        backgroundBlur = p.getFloat("backgroundBlur", 0f),
        backgroundDim = p.getFloat("backgroundDim", 0.15f),
        topScrimAlpha = p.getFloat("topScrimAlpha", 0.22f),
        topScrimHeight = p.getFloat("topScrimHeight", 58f),
        bottomScrimAlpha = p.getFloat("bottomScrimAlpha", 0.26f),
        bottomScrimHeight = p.getFloat("bottomScrimHeight", 82f),
        scrimSmoothness = p.getFloat("scrimSmoothness", 220f),
        glass = glass
    )
}

private fun saveAppSettings(context: Context, settings: AppSettings) {
    context.getSharedPreferences("vanilla_settings", Context.MODE_PRIVATE)
        .edit()
        .putString("apiBaseUrl", settings.apiBaseUrl)
        .putString("apiKey", settings.apiKey)
        .putString("modelName", settings.modelName)
        .putString("assistantName", settings.assistantName)
        .putString("systemPrompt", settings.systemPrompt)
        .putString("assistantAvatarUri", settings.assistantAvatarUri)
        .putString("chatBackgroundUri", settings.chatBackgroundUri)
        .putFloat("backgroundBlur", settings.backgroundBlur)
        .putFloat("backgroundDim", settings.backgroundDim)
        .putFloat("topScrimAlpha", settings.topScrimAlpha)
        .putFloat("topScrimHeight", settings.topScrimHeight)
        .putFloat("bottomScrimAlpha", settings.bottomScrimAlpha)
        .putFloat("bottomScrimHeight", settings.bottomScrimHeight)
        .putFloat("scrimSmoothness", settings.scrimSmoothness)
        .putFloat("glassBlur", settings.glass.glassBlur)
        .putFloat("lensHeight", settings.glass.lensHeight)
        .putFloat("lensAmount", settings.glass.lensAmount)
        .putFloat("glassAlpha", settings.glass.glassAlpha)
        .putFloat("highlightAlpha", settings.glass.highlightAlpha)
        .putFloat("shadowAlpha", settings.glass.shadowAlpha)
        .putFloat("innerShadowAlpha", settings.glass.innerShadowAlpha)
        .apply()
}

private fun loadSessions(context: Context): List<String> {
    val raw = context.getSharedPreferences("vanilla_sessions", Context.MODE_PRIVATE)
        .getString("titles", "[]") ?: "[]"

    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val title = array.optString(i).trim()
                if (title.isNotEmpty()) add(title)
            }
        }
    }.getOrDefault(emptyList())
}

private fun saveSessions(context: Context, sessions: List<String>) {
    val array = JSONArray()
    sessions.forEach { array.put(it) }
    context.getSharedPreferences("vanilla_sessions", Context.MODE_PRIVATE)
        .edit()
        .putString("titles", array.toString())
        .apply()
}

private suspend fun requestAiReplyStreaming(
    settings: AppSettings,
    messages: List<ChatMessage>,
    onDelta: suspend (String) -> Unit
): String {
    val baseUrl = settings.apiBaseUrl.trim()
    val apiKey = settings.apiKey.trim()
    val model = settings.modelName.trim()

    if (baseUrl.isEmpty()) return "请先在设置里填写 Base URL。"
    if (apiKey.isEmpty()) return "请先在设置里填写 API Key。"
    if (model.isEmpty()) return "请先在设置里填写模型名称。"

    return withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                baseUrl.trimEnd('/') + "/chat/completions"
            }

            val body = JSONObject()
                .put("model", model)
                .put("stream", true)

            val messageArray = JSONArray()

            if (settings.systemPrompt.trim().isNotEmpty()) {
                messageArray.put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", settings.systemPrompt.trim())
                )
            }

            messages
                .filter { !it.isTyping && it.text.isNotBlank() }
                .takeLast(24)
                .forEach { message ->
                    messageArray.put(
                        JSONObject()
                            .put("role", if (message.fromMe) "user" else "assistant")
                            .put("content", message.text)
                    )
                }

            body.put("messages", messageArray)

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                val responseText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val errorMessage = runCatching {
                    JSONObject(responseText)
                        .optJSONObject("error")
                        ?.optString("message")
                        .orEmpty()
                }.getOrDefault("")

                return@runCatching if (errorMessage.isNotBlank()) {
                    "请求失败：$errorMessage"
                } else {
                    "请求失败：HTTP $status"
                }
            }

            val builder = StringBuilder()

            connection.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("data:")) return@forEach

                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") return@useLines
                    if (data.isBlank()) return@forEach

                    val delta = runCatching {
                        val choice = JSONObject(data)
                            .getJSONArray("choices")
                            .getJSONObject(0)

                        val deltaObject = choice.optJSONObject("delta")
                        val messageObject = choice.optJSONObject("message")

                        deltaObject?.optString("content")
                            ?: messageObject?.optString("content")
                            ?: ""
                    }.getOrDefault("")

                    if (delta.isNotEmpty()) {
                        builder.append(delta)
                        withContext(Dispatchers.Main) {
                            onDelta(delta)
                        }
                    }
                }
            }

            builder.toString()
        }.getOrElse { e ->
            "请求出错：${e.message ?: e::class.java.simpleName}"
        }
    }
}

private fun stripThinkTags(text: String): String {
    var output = text

    while (true) {
        val start = output.indexOf("<think", ignoreCase = true)
        if (start < 0) break

        val tagEnd = output.indexOf(">", start)
        if (tagEnd < 0) {
            output = output.substring(0, start)
            break
        }

        val end = output.indexOf("</think>", tagEnd + 1, ignoreCase = true)

        output = if (end >= 0) {
            output.removeRange(start, end + "</think>".length)
        } else {
            output.substring(0, start)
        }
    }

    return output.trim()
}
