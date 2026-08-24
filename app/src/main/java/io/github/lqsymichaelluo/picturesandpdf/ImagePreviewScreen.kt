package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.window.PopupPositionProvider
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImagePreviewScreen(
    onBack: () -> Unit = {},
    imagePreviewViewModel: ImagePreviewViewModel,
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val imageID = "image_" + imagePreviewViewModel.currentIndex
    var colorState by imagePreviewViewModel.imagePreviewBackgroundColorState(imageID)
    val belowPositionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x =
                    anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.bottom + with(density) { 1.dp.roundToPx() }
                return IntOffset(x, y)
            }
        }
    }
    val containerColor by animateColorAsState(
        targetValue = when (colorState) {
            ImagePreviewBackgroundColorState.Gray -> Color.Gray
            ImagePreviewBackgroundColorState.White -> Color.White
            else -> Color.Black
        }
    )

    fun toggleColorState() {
        colorState = when (colorState) {
            ImagePreviewBackgroundColorState.Black -> ImagePreviewBackgroundColorState.White
            ImagePreviewBackgroundColorState.White -> ImagePreviewBackgroundColorState.Gray
            else -> ImagePreviewBackgroundColorState.Black
        }
    }
    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text("图片预览", color = Color.White) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = belowPositionProvider,
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.back)) }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = belowPositionProvider,
                        tooltip = {
                            PlainTooltip { Text("转换底色") }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                toggleColorState()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_change),
                                contentDescription = null
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                )
            )
        }
    ) { padding ->
        var paddingValues by remember { mutableStateOf(padding) }
        Box(
            modifier = Modifier
                //.padding(paddingValues)
                .fillMaxSize()
                .onSizeChanged { containerSize = it },
            contentAlignment = Alignment.Center
        ) {
            val bitmapList = imagePreviewViewModel.bitmapList
            val current: Int = imagePreviewViewModel.currentIndex
            if (bitmapList != null) {
                val pagerState = rememberPagerState(
                    initialPage = current,
                    pageCount = { bitmapList.size }
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val scale = remember { Animatable(1f) }
                    val scope = rememberCoroutineScope()
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }
                    var imageSize by remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = bitmapList[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { imageSize = it }
                                .pointerInput(containerSize, imageSize) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var totalDrag = Offset.Zero
                                            var isScaling = false

                                            do {
                                                val event = awaitPointerEvent()
                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()

                                                if (zoomChange != 1f) {
                                                    isScaling = true
                                                    scope.launch {
                                                        scale.snapTo(
                                                            (scale.value * zoomChange).fastCoerceIn(1f, 35f)
                                                        )
                                                    }
                                                }

                                                val shouldConsume = (scale.value > 1f) || isScaling

                                                if (shouldConsume) {
                                                    val limit = calculateOffsetLimit(
                                                        scale = scale.value,
                                                        container = containerSize,
                                                        image = imageSize
                                                    )
                                                    offsetX = (offsetX + panChange.x)
                                                        .fastCoerceIn(-limit.x, limit.x)
                                                    offsetY = (offsetY + panChange.y)
                                                        .fastCoerceIn(-limit.y, limit.y)

                                                    event.changes.forEach { it.consume() }
                                                } else {
                                                    totalDrag += panChange
                                                    if (panChange.y.absoluteValue > panChange.x.absoluteValue) {
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                        }
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale.value,
                                    scaleY = scale.value,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                        )
                    }
                }
            }
        }
    }
}

fun calculateOffsetLimit(
    scale: Float,
    container: IntSize,
    image: IntSize
): Offset {
    val scaledWidth = image.width * scale
    val scaledHeight = image.height * scale
    val maxX = ((scaledWidth - container.width) / 2f).coerceAtLeast(0f)
    val maxY = ((scaledHeight - container.height) / 2f).coerceAtLeast(0f)
    return Offset(maxX, maxY)
}