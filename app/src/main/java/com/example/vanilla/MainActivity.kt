package com.example.vanilla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VanillaApp()
        }
    }
}

private data class ChatMessage(
    val id: Int,
    val text: String,
    val fromMe: Boolean
)

private data class GlassSettings(
    val topButtonSize: Float = 52f,
    val topPillWidth: Float = 164f,
    val topPillHeight: Float = 52f,
    val avatarSize: Float = 34f,
    val inputHeight: Float = 60f,
    val drawerWidth: Float = 304f,
    val settingsSheetWidth: Float = 340f,
    val topSidePadding: Float = 16f,
    val glassBlur: Float = 10f,
    val lensHeight: Float = 12f,
    val lensAmount: Float = 24f,
    val glassAlpha: Float = 0.38f,
    val highlightAlpha: Float = 0.95f,
    val shadowAlpha: Float = 0.12f,
    val innerShadowAlpha: Float = 0.18f
)

@Composable
private fun VanillaApp() {
    var glassSettings by remember { mutableStateOf(GlassSettings()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var nextMessageId by remember { mutableStateOf(3) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(1, "你好，我是 friend。", fromMe = false),
            ChatMessage(2, "这个界面已经接入液态玻璃组件。", fromMe = false)
        )
    }

    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "drawerProgress"
    )
    val settingsProgress by animateFloatAsState(
        targetValue = if (settingsOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "settingsProgress"
    )

    val density = LocalDensity.current
    val chatOffsetPx = with(density) {
        (glassSettings.drawerWidth.dp.toPx() * drawerProgress).roundToInt()
    }
    val drawerOffsetPx = with(density) {
        (-(1f - drawerProgress) * glassSettings.drawerWidth.dp.toPx()).roundToInt()
    }

    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            BackgroundScene()
            DrawerScene(
                settings = glassSettings,
                drawerOffsetPx = drawerOffsetPx
            )
            ChatScene(
                messages = messages,
                settings = glassSettings,
                chatOffsetPx = chatOffsetPx
            )
        }

        DrawerSettingsButton(
            backdrop = backdrop,
            settings = glassSettings,
            drawerProgress = drawerProgress,
            drawerOffsetPx = drawerOffsetPx,
            onClick = { settingsOpen = true }
        )

        ChatTopBar(
            backdrop = backdrop,
            settings = glassSettings,
            chatOffsetPx = chatOffsetPx,
            onMenuClick = { drawerOpen = !drawerOpen },
            onAddClick = { }
        )

        ChatInputBar(
            backdrop = backdrop,
            settings = glassSettings,
            chatOffsetPx = chatOffsetPx,
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                val text = inputText.trim()
                if (text.isNotEmpty()) {
                    messages += ChatMessage(nextMessageId, text, fromMe = true)
                    nextMessageId += 1
                    inputText = ""
                }
            }
        )

        SettingsSheetOverlay(
            backdrop = backdrop,
            settings = glassSettings,
            progress = settingsProgress,
            visible = settingsOpen || settingsProgress > 0.01f,
            onDismiss = { settingsOpen = false },
            onSettingsChange = { glassSettings = it }
        )
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
            drawCircle(
                color = Color(0xFF8EA2FF).copy(alpha = 0.22f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.16f, size.height * 0.22f)
            )
            drawCircle(
                color = Color(0xFFFF8DB3).copy(alpha = 0.18f),
                radius = size.minDimension * 0.32f,
                center = Offset(size.width * 0.86f, size.height * 0.14f)
            )
            drawCircle(
                color = Color(0xFF63D6B8).copy(alpha = 0.17f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.82f, size.height * 0.82f)
            )
            drawGlassLines()
        }
    }
}

private fun DrawScope.drawGlassLines() {
    val lineColor = Color.White.copy(alpha = 0.34f)
    val strokeWidth = 2.dp.toPx()
    repeat(8) { index ->
        val y = size.height * (0.16f + index * 0.09f)
        drawLine(
            color = lineColor,
            start = Offset(size.width * 0.08f, y),
            end = Offset(size.width * 0.92f, y + 48.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun DrawerScene(settings: GlassSettings, drawerOffsetPx: Int) {
    Box(
        Modifier
            .offset { IntOffset(drawerOffsetPx, 0) }
            .width(settings.drawerWidth.dp)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.30f))
            .padding(horizontal = 22.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(
                text = "A界面",
                style = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                text = "这里是从左侧推拉出的页面。底部设置按钮会推出样式参数页面。",
                style = TextStyle(
                    color = Color(0xFF344054).copy(alpha = 0.78f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )
            DrawerItem("聊天历史")
            DrawerItem("置顶联系人")
            DrawerItem("收藏气泡")
            DrawerItem("本地草稿")
        }
    }
}

@Composable
private fun DrawerItem(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.28f))
            .padding(horizontal = 18.dp, vertical = 15.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFF263344),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ChatScene(
    messages: List<ChatMessage>,
    settings: GlassSettings,
    chatOffsetPx: Int
) {
    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 122.dp,
                bottom = (settings.inputHeight + 42f).dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
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
                .background(if (message.fromMe) Color(0xFF4A7DFF) else Color.White.copy(alpha = 0.78f))
                .padding(horizontal = 15.dp, vertical = 11.dp)
        ) {
            BasicText(
                text = message.text,
                style = TextStyle(
                    color = if (message.fromMe) Color.White else Color(0xFF222B3A),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
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
            .padding(horizontal = settings.topSidePadding.dp, vertical = 12.dp)
            .height(settings.topButtonSize.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(settings.topButtonSize.dp),
            shape = Capsule(),
            onClick = onMenuClick
        ) {
            MenuIcon()
        }

        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.Center)
                .width(settings.topPillWidth.dp)
                .height(settings.topPillHeight.dp),
            shape = Capsule()
        ) {
            Row(
                Modifier.padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(settings.avatarSize.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF687BFF))
                )
                BasicText(
                    text = "friend",
                    style = TextStyle(
                        color = Color(0xFF202838),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(settings.topButtonSize.dp),
            shape = Capsule(),
            onClick = onAddClick
        ) {
            PlusIcon(fontSize = 30)
        }
    }
}

@Composable
private fun BoxScope.ChatInputBar(
    backdrop: Backdrop,
    settings: GlassSettings,
    chatOffsetPx: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Box(
        Modifier
            .offset { IntOffset(chatOffsetPx, 0) }
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(settings.inputHeight.dp)
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier.fillMaxSize(),
            shape = Capsule()
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlusIcon(fontSize = 32)
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color(0xFF162033),
                        fontSize = 17.sp
                    ),
                    singleLine = true,
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

                SendButton(
                    size = (settings.inputHeight - 14f).coerceAtLeast(40f),
                    onClick = onSend
                )
            }
        }
    }
}

@Composable
private fun DrawerSettingsButton(
    backdrop: Backdrop,
    settings: GlassSettings,
    drawerProgress: Float,
    drawerOffsetPx: Int,
    onClick: () -> Unit
) {
    if (drawerProgress <= 0.01f) return

    Box(
        Modifier
            .offset { IntOffset(drawerOffsetPx, 0) }
            .fillMaxSize()
            .graphicsLayer { alpha = drawerProgress }
    ) {
        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 26.dp)
                .size(58.dp),
            shape = Capsule(),
            onClick = onClick
        ) {
            BasicText(
                text = "⚙",
                style = TextStyle(
                    color = Color(0xFF202838),
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun SettingsSheetOverlay(
    backdrop: Backdrop,
    settings: GlassSettings,
    progress: Float,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSettingsChange: (GlassSettings) -> Unit
) {
    if (!visible) return

    val density = LocalDensity.current
    val sheetOffsetPx = with(density) {
        ((1f - progress) * settings.settingsSheetWidth.dp.toPx()).roundToInt()
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f * progress))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
        )

        GlassSurface(
            backdrop = backdrop,
            settings = settings,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(sheetOffsetPx, 0) }
                .width(settings.settingsSheetWidth.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 34.dp, bottomStart = 34.dp)
        ) {
            SettingsPanel(
                settings = settings,
                onDismiss = onDismiss,
                onSettingsChange = onSettingsChange
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: GlassSettings,
    onDismiss: () -> Unit,
    onSettingsChange: (GlassSettings) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "设置",
                    style = TextStyle(
                        color = Color(0xFF1E2430),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BasicText(
                    text = "UI尺寸 / Liquid Glass 参数",
                    style = TextStyle(
                        color = Color(0xFF5C667A),
                        fontSize = 13.sp
                    )
                )
            }
            PlainRoundButton(text = "×", onClick = onDismiss)
        }

        SettingsGroupTitle("界面尺寸")
        ParamSlider("顶部圆按钮", settings.topButtonSize, 44f..72f, "dp") {
            onSettingsChange(settings.copy(topButtonSize = it))
        }
        ParamSlider("顶部药丸宽度", settings.topPillWidth, 132f..220f, "dp") {
            onSettingsChange(settings.copy(topPillWidth = it))
        }
        ParamSlider("顶部药丸高度", settings.topPillHeight, 44f..70f, "dp") {
            onSettingsChange(settings.copy(topPillHeight = it))
        }
        ParamSlider("头像直径", settings.avatarSize, 24f..48f, "dp") {
            onSettingsChange(settings.copy(avatarSize = it))
        }
        ParamSlider("输入框高度", settings.inputHeight, 52f..82f, "dp") {
            onSettingsChange(settings.copy(inputHeight = it))
        }
        ParamSlider("A界面宽度", settings.drawerWidth, 260f..380f, "dp") {
            onSettingsChange(settings.copy(drawerWidth = it))
        }
        ParamSlider("设置页宽度", settings.settingsSheetWidth, 300f..420f, "dp") {
            onSettingsChange(settings.copy(settingsSheetWidth = it))
        }

        SettingsGroupTitle("液态玻璃")
        ParamSlider("背景模糊", settings.glassBlur, 0f..24f, "px") {
            onSettingsChange(settings.copy(glassBlur = it))
        }
        ParamSlider("折射高度", settings.lensHeight, 0f..28f, "px") {
            onSettingsChange(settings.copy(lensHeight = it))
        }
        ParamSlider("折射强度", settings.lensAmount, 0f..48f, "px") {
            onSettingsChange(settings.copy(lensAmount = it))
        }
        ParamSlider("玻璃白色层", settings.glassAlpha, 0f..0.75f, "%") {
            onSettingsChange(settings.copy(glassAlpha = it))
        }
        ParamSlider("高光", settings.highlightAlpha, 0f..1f, "%") {
            onSettingsChange(settings.copy(highlightAlpha = it))
        }
        ParamSlider("外阴影", settings.shadowAlpha, 0f..0.35f, "%") {
            onSettingsChange(settings.copy(shadowAlpha = it))
        }
        ParamSlider("内阴影", settings.innerShadowAlpha, 0f..0.45f, "%") {
            onSettingsChange(settings.copy(innerShadowAlpha = it))
        }

        PlainTextButton("恢复默认") {
            onSettingsChange(GlassSettings())
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(
            color = Color(0xFF202838),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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
    val fraction = ((value - range.start) / (range.endInclusive - range.start))
        .coerceIn(0f, 1f)

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
                style = TextStyle(
                    color = Color(0xFF344054),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = formatSettingValue(value, suffix),
                style = TextStyle(
                    color = Color(0xFF667085),
                    fontSize = 12.sp
                )
            )
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(range, widthPx) {
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
                    .offset {
                        IntOffset(
                            x = (fraction * (widthPx - knobPx)).roundToInt(),
                            y = 0
                        )
                    }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
            )
        }
    }
}

private fun formatSettingValue(value: Float, suffix: String): String {
    return if (suffix == "%") {
        "${(value * 100f).roundToInt()}%"
    } else {
        "${value.roundToInt()}$suffix"
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(settings.glassBlur.dp.toPx())
                    lens(
                        refractionHeight = settings.lensHeight.dp.toPx(),
                        refractionAmount = settings.lensAmount.dp.toPx(),
                        chromaticAberration = true
                    )
                },
                highlight = {
                    Highlight.Ambient.copy(alpha = settings.highlightAlpha)
                },
                shadow = {
                    Shadow(
                        radius = 18.dp,
                        color = Color.Black.copy(alpha = settings.shadowAlpha)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 18.dp,
                        color = Color.Black.copy(alpha = settings.innerShadowAlpha),
                        alpha = settings.innerShadowAlpha
                    )
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = settings.glassAlpha))
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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
private fun PlainRoundButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.44f))
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
                color = Color(0xFF202838),
                fontSize = 25.sp,
                fontWeight = FontWeight.Medium,
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
            .background(Color.White.copy(alpha = 0.38f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFF202838),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
