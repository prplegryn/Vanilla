package com.example.vanilla

import android.os.Bundle
import android.view.WindowManager
import kotlin.system.exitProcess
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import java.io.StringWriter
import java.io.PrintWriter
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installCrashLogger()
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onCreate(savedInstanceState)

        setContent {
            var crashReport by remember {
                mutableStateOf(
                    getSharedPreferences("crash_report", MODE_PRIVATE)
                        .getString("last_crash", null)
                )
            }

            if (crashReport != null) {
                CrashReportScreen(
                    report = crashReport.orEmpty(),
                    onClear = {
                        getSharedPreferences("crash_report", MODE_PRIVATE)
                            .edit()
                            .remove("last_crash")
                            .apply()
                        crashReport = null
                    }
                )
            } else {
                VanillaApp()
            }
        }
    }

    private fun installCrashLogger() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))

                val time = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                val report = buildString {
                    appendLine("Vanilla crash report")
                    appendLine("Time: $time")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(writer.toString())
                }

                getSharedPreferences("crash_report", MODE_PRIVATE)
                    .edit()
                    .putString("last_crash", report)
                    .commit()
            }

            if (oldHandler != null) {
                oldHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(2)
            }
        }
    }
}

@Composable
private fun CrashReportScreen(
    report: String,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101828))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BasicText(
            text = "捕获到上次闪退日志",
            style = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )

        BasicText(
            text = "点复制日志，然后把内容发给我。清除后会进入 app。",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CrashButton(
                text = "复制日志",
                modifier = Modifier.weight(1f)
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Vanilla crash report", report)
                )
                Toast.makeText(context, "已复制日志", Toast.LENGTH_SHORT).show()
            }

            CrashButton(
                text = "清除",
                modifier = Modifier.weight(1f),
                onClick = onClear
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.26f))
                .verticalScroll(scrollState)
                .padding(14.dp)
        ) {
            BasicText(
                text = report,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun CrashButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}


private data class ChatMessage(
    val id: Int,
    val text: String,
    val fromMe: Boolean
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

@Composable
private fun VanillaApp() {
    var glassSettings by remember { mutableStateOf(GlassSettings()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var assistantDetailOpen by remember { mutableStateOf(false) }
    var apiDetailOpen by remember { mutableStateOf(false) }
    var glassDetailOpen by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var nextMessageId by remember { mutableIntStateOf(3) }
    var inputBarHeightPx by remember { mutableIntStateOf(92) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val sessions = remember { mutableStateListOf<String>() }

    BackHandler(enabled = glassDetailOpen) { glassDetailOpen = false }
    BackHandler(enabled = apiDetailOpen) { apiDetailOpen = false }
    BackHandler(enabled = assistantDetailOpen) { assistantDetailOpen = false }
    BackHandler(enabled = settingsOpen && !assistantDetailOpen && !apiDetailOpen && !glassDetailOpen) { settingsOpen = false }
    BackHandler(enabled = drawerOpen && !settingsOpen && !assistantDetailOpen && !apiDetailOpen && !glassDetailOpen) { drawerOpen = false }

    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "drawerProgress"
    )
    val settingsProgress by animateFloatAsState(
        targetValue = if (settingsOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "settingsProgress"
    )
    val assistantProgress by animateFloatAsState(
        targetValue = if (assistantDetailOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 230),
        label = "assistantProgress"
    )
    val apiProgress by animateFloatAsState(
        targetValue = if (apiDetailOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 230),
        label = "apiProgress"
    )
    val detailProgress by animateFloatAsState(
        targetValue = if (glassDetailOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 230),
        label = "detailProgress"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val chatOffsetPx = (screenWidthPx * drawerProgress).roundToInt()
        val drawerOffsetPx = (screenWidthPx * (drawerProgress - 1f)).roundToInt()
        val settingsOffsetPx = (screenWidthPx * (1f - settingsProgress)).roundToInt()
        val assistantDetailOffsetPx = (screenWidthPx * (1f - assistantProgress)).roundToInt()
        val apiDetailOffsetPx = (screenWidthPx * (1f - apiProgress)).roundToInt()
        val detailOffsetPx = (screenWidthPx * (1f - detailProgress)).roundToInt()
        val settingsPushProgress = maxOf(assistantProgress, apiProgress, detailProgress)
        val detailUnderOffsetPx = (screenWidthPx * -0.18f * settingsPushProgress).roundToInt()
        val inputBottomPadding = with(density) { inputBarHeightPx.toDp() + 26.dp }
        val backdrop = rememberLayerBackdrop()

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            BackgroundScene()
            ChatScene(
                messages = messages,
                chatOffsetPx = chatOffsetPx,
                bottomPadding = inputBottomPadding
            )

            if (drawerProgress > 0.001f) {
                DrawerScene(
                    drawerOffsetPx = drawerOffsetPx,
                    sessions = sessions
                )
            }

            if (settingsProgress > 0.001f) {
                SettingsHomeScene(
                    offsetPx = settingsOffsetPx + detailUnderOffsetPx,
                    onOpenAssistantSettings = { assistantDetailOpen = true },
                    onOpenApiSettings = { apiDetailOpen = true },
                    onOpenGlassSettings = { glassDetailOpen = true }
                )
            }

            if (assistantProgress > 0.001f) {
                AssistantSettingsScene(
                    offsetPx = assistantDetailOffsetPx
                )
            }

            if (apiProgress > 0.001f) {
                ApiProviderSettingsScene(
                    offsetPx = apiDetailOffsetPx
                )
            }

            if (detailProgress > 0.001f) {
                GlassStyleSettingsScene(
                    settings = glassSettings,
                    offsetPx = detailOffsetPx,
                    onSettingsUpdate = { update -> glassSettings = update(glassSettings) }
                )
            }
        }

        ChatTopBar(
            backdrop = backdrop,
            settings = glassSettings,
            chatOffsetPx = chatOffsetPx,
            onMenuClick = { drawerOpen = true },
            onAddClick = {
                val firstText = messages.firstOrNull()?.text?.trim().orEmpty()
                if (firstText.isNotEmpty()) {
                    val title = firstText
                        .replace("\\n", " ")
                        .replace("\\r", " ")
                        .take(32)
                    sessions.add(title)
                    messages.clear()
                    inputText = ""
                }
            }
        )

        ChatInputBar(
            backdrop = backdrop,
            settings = glassSettings,
            chatOffsetPx = chatOffsetPx,
            value = inputText,
            onValueChange = { inputText = it },
            onHeightChanged = { inputBarHeightPx = it },
            onSend = {
                val text = inputText.trim()
                if (text.isNotEmpty()) {
                    messages += ChatMessage(nextMessageId, text, fromMe = true)
                    nextMessageId += 1
                    inputText = ""
                }
            }
        )

        if (drawerProgress > 0.001f && !settingsOpen) {
            DrawerSettingsButton(
                backdrop = backdrop,
                settings = glassSettings,
                drawerOffsetPx = drawerOffsetPx,
                alpha = drawerProgress,
                onClick = { settingsOpen = true }
            )
        }
    }
}

@Composable
private fun BackgroundScene() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF8F1),
                        Color(0xFFEAF2FF),
                        Color(0xFFFFEDF7)
                    ),
                    start = Offset.Zero,
                    end = Offset(1400f, 2200f)
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
                style = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                BasicText(
                    text = "暂无会话",
                    modifier = Modifier.padding(start = 2.dp),
                    style = TextStyle(
                        color = Color(0xFF667085),
                        fontSize = 15.sp
                    )
                )
            } else {
                sessions.forEach { title ->
                    DrawerItem(title)
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(text: String) {
    var showLongPressShape by remember { mutableStateOf(false) }

    LaunchedEffect(showLongPressShape) {
        if (showLongPressShape) {
            delay(520)
            showLongPressShape = false
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (showLongPressShape) {
                    Color.White.copy(alpha = 0.78f)
                } else {
                    Color.Transparent
                }
            )
            .pointerInput(text) {
                detectTapGestures(
                    onLongPress = { showLongPressShape = true }
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 13.dp, bottom = 13.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ChatScene(
    messages: List<ChatMessage>,
    chatOffsetPx: Int,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 122.dp, bottom = bottomPadding, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(150)) +
                    slideInVertically(
                        animationSpec = tween(170),
                        initialOffsetY = { it / 3 }
                    ) +
                    scaleIn(
                        animationSpec = tween(170),
                        initialScale = 0.96f
                    )
            ) {
                MessageBubble(message)
            }
        }
        }
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
                .padding(start = 15.dp, end = 15.dp, top = 8.dp, bottom = 10.dp)
        ) {
            BasicText(
                text = message.text,
                style = TextStyle(if (message.fromMe) Color.White else Color(0xFF222B3A), fontSize = 16.sp, lineHeight = 22.sp)
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    backdrop: Backdrop,
    settings: GlassSettings,
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
            settings = settings,
            modifier = Modifier.align(Alignment.CenterStart).size(50.dp),
            shape = Capsule(),
            onClick = onMenuClick
        ) { MenuIcon() }

        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier.align(Alignment.Center).width(108.dp).height(50.dp),
            shape = Capsule()
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 7.dp, end = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF687BFF)))
                BasicText(
                    text = "friend",
                    style = TextStyle(
                        color = Color(0xFF202838),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier.align(Alignment.CenterEnd).size(50.dp),
            shape = Capsule(),
            onClick = onAddClick
        ) { PlusIcon(fontSize = 30) }
    }
}

@Composable
private fun BoxScope.ChatInputBar(
    backdrop: Backdrop,
    settings: GlassSettings,
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
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp, max = 132.dp),
            shape = RoundedCornerShape(29.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
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
                    textStyle = TextStyle(
                        color = Color(0xFF162033),
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    ),
                    singleLine = false,
                    maxLines = 4,
                    cursorBrush = SolidColor(Color(0xFF4A7DFF)),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                BasicText(
                                    text = "Message",
                                    style = TextStyle(
                                        color = Color(0xFF697386).copy(alpha = 0.70f),
                                        fontSize = 17.sp
                                    )
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
    settings: GlassSettings,
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
            settings = settings,
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
                style = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(16.dp))

            SettingsListItem(
                title = "AI 助手设定",
                subtitle = "头像、名字、提示词和聊天背景",
                onClick = onOpenAssistantSettings
            )

            SettingsListItem(
                title = "API 提供商设置",
                subtitle = "Base URL、API Key、模型名称",
                onClick = onOpenApiSettings
            )

            SettingsListItem(
                title = "液态玻璃样式设置",
                subtitle = "模糊、折射、高光和阴影",
                onClick = onOpenGlassSettings
            )
        }
    }
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var showLongPressShape by remember { mutableStateOf(false) }

    LaunchedEffect(showLongPressShape) {
        if (showLongPressShape) {
            delay(520)
            showLongPressShape = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (showLongPressShape) {
                    Color.White.copy(alpha = 0.78f)
                } else {
                    Color.Transparent
                }
            )
            .pointerInput(onClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showLongPressShape = true }
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicText(
            text = subtitle,
            style = TextStyle(
                color = Color(0xFF667085),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun AssistantSettingsScene(offsetPx: Int) {
    var assistantName by remember { mutableStateOf("friend") }
    var prompt by remember { mutableStateOf("") }
    var backgroundName by remember { mutableStateOf("") }
    var backgroundBlur by remember { mutableStateOf(0f) }
    var backgroundDim by remember { mutableStateOf(0.15f) }

    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = "AI 助手",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                color = Color(0xFF1F2937),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(4.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF687BFF),
                                Color(0xFFFF8DB3)
                            )
                        )
                    )
            )

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicText(
                    text = "助手名字",
                    style = TextStyle(
                        color = Color(0xFF202838),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                BasicTextField(
                    value = assistantName,
                    onValueChange = { assistantName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.62f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF162033),
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF4A7DFF))
                )
            }
        }

        AssistantMultilineField(
            label = "提示词设定",
            value = prompt,
            placeholder = "例如：你是一个简洁、温柔、直接的 AI 助手。",
            onValueChange = { prompt = it }
        )

        BackgroundPickerMock(
            value = backgroundName,
            onValueChange = { backgroundName = it }
        )

        ParamSlider("聊天背景模糊度", backgroundBlur, 0f..30f, "px") {
            backgroundBlur = it
        }

        ParamSlider("聊天背景明暗度", backgroundDim, 0f..0.75f, "%") {
            backgroundDim = it
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun AssistantMultilineField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = label,
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
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
            textStyle = TextStyle(
                color = Color(0xFF162033),
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            cursorBrush = SolidColor(Color(0xFF4A7DFF)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(
                                color = Color(0xFF667085).copy(alpha = 0.62f),
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun BackgroundPickerMock(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = "聊天背景设定",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFEAF2FF),
                                Color(0xFFFFEDF7),
                                Color(0xFFEFFFF8)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "自动裁切为全屏显示",
                    style = TextStyle(
                        color = Color(0xFF202838).copy(alpha = 0.72f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF162033),
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color(0xFF4A7DFF)),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "背景图片名称 / 路径占位",
                                style = TextStyle(
                                    color = Color(0xFF667085).copy(alpha = 0.62f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}


@Composable
private fun ApiProviderSettingsScene(offsetPx: Int) {
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }

    Column(
        Modifier
            .offset { IntOffset(offsetPx, 0) }
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = "API 提供商",
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                color = Color(0xFF1F2937),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(8.dp))

        ApiTextField(
            label = "Base URL",
            value = baseUrl,
            placeholder = "https://api.example.com/v1",
            onValueChange = { baseUrl = it }
        )

        ApiTextField(
            label = "API Key",
            value = apiKey,
            placeholder = "sk-...",
            onValueChange = { apiKey = it }
        )

        ApiTextField(
            label = "模型名称",
            value = modelName,
            placeholder = "gpt-4o-mini / deepseek-chat / ...",
            onValueChange = { modelName = it }
        )
    }
}

@Composable
private fun ApiTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .padding(horizontal = 16.dp, vertical = 15.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF162033),
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(Color(0xFF4A7DFF)),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(
                                color = Color(0xFF667085).copy(alpha = 0.62f),
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
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
        BasicText(
            text = "顶部预览会实时使用下面的参数。",
            style = TextStyle(Color(0xFF667085), fontSize = 14.sp, lineHeight = 20.sp)
        )

        GlassPreviewCard(settings = settings)

        SettingsGroupTitle("液态玻璃")
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

        PlainTextButton("恢复默认") {
            onSettingsUpdate { GlassSettings() }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun GlassPreviewCard(settings: GlassSettings) {
    val previewBackdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE8ECF7))
    ) {
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(previewBackdrop)
        ) {
            Image(
                painter = painterResource(id = R.drawable.glass_preview),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        GlassSurface(
            backdrop = previewBackdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.Center)
                .width(172.dp)
                .height(52.dp),
            shape = Capsule()
        ) {
            BasicText(
                text = "Glass Preview",
                style = TextStyle(
                    color = Color(0xFF202838),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(Color(0xFF202838), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(1f) }
    val knobSize = 22.dp
    val knobPx = with(density) { knobSize.toPx() }
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    fun updateFromX(x: Float) {
        val newFraction = (x / widthPx).coerceIn(0f, 1f)
        val newValue = range.start + (range.endInclusive - range.start) * newFraction
        onValueChange(newValue)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = label,
                style = TextStyle(Color(0xFF344054), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
            BasicText(
                text = formatSettingValue(value, suffix),
                style = TextStyle(Color(0xFF667085), fontSize = 12.sp)
            )
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(range, widthPx, onValueChange) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateFromX(down.position.x)
                        drag(down.id) { change ->
                            updateFromX(change.position.x)
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(Capsule())
                    .background(Color(0xFF101828).copy(alpha = 0.12f))
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(Capsule())
                    .background(Color(0xFF101828).copy(alpha = 0.48f))
            )
            Box(
                Modifier
                    .offset { IntOffset((fraction * (widthPx - knobPx)).roundToInt(), 0) }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
            )
        }
    }
}

private fun formatSettingValue(value: Float, suffix: String): String {
    return if (suffix == "%") "${(value * 100f).roundToInt()}%" else "${value.roundToInt()}$suffix"
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
    val interactionSource = remember { MutableInteractionSource() }
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
                shadow = {
                    Shadow(
                        radius = 8.dp,
                        color = Color.Black.copy(alpha = settings.shadowAlpha)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 6.dp,
                        color = Color.Black.copy(alpha = settings.innerShadowAlpha),
                        alpha = settings.innerShadowAlpha
                    )
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = settings.glassAlpha)) }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) { content() }
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
        style = TextStyle(
            color = Color(0xFF202838),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    )
}

@Composable
private fun SendButton(size: Float, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5E7EB))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "↑",
            style = TextStyle(
                color = Color(0xFF303846),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PlainTextButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(Color(0xFF202838), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
    }
}
