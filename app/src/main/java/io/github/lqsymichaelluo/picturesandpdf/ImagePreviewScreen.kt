package io.github.lqsymichaelluo.picturesandpdf

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.VibrationEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceIn
import androidx.navigation.NavController
import androidx.wear.compose.material3.MaterialTheme
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    pdfName: String = "unknown.pdf",
    currentIndex: Int = 0,
    onBack: () -> Unit = {},
    imagePreviewViewModel: ImagePreviewViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val imageID = "image_$currentIndex"
    var colorState by imagePreviewViewModel.imagePreviewBackgroundColorState(imageID)
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

    DisposableEffect(Unit) {
        onDispose { PreviewBitmapCache.trimToHalf() }
    }

    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text("图片预览", color = Color.White) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below
                        ),
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.back)) }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = {
                            HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                            onBack()
                        }) {
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
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below
                        ),
                        tooltip = {
                            PlainTooltip { Text("给图片组排序") }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                imagePreviewViewModel.setTriggerSort(
                                    pdfName = pdfName,
                                    triggered = true
                                )
                                navController.navigate("image_sorting/$pdfName")
                                imagePreviewViewModel.setTriggerPreview(
                                    pdfName = pdfName,
                                    triggered = false
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sort),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below
                        ),
                        tooltip = {
                            PlainTooltip { Text("转换底色") }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = {
                            HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                            toggleColorState()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_change),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                )
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val bitmapList = imagePreviewViewModel.imagePreviewList[pdfName]?.bitmapList
            if (bitmapList != null) {
                var currentPageScale by remember { mutableFloatStateOf(1f) }

                val pagerState = rememberPagerState(
                    initialPage = currentIndex,
                    pageCount = { bitmapList.size }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { System.identityHashCode(bitmapList[it]) },
                    userScrollEnabled = currentPageScale <= 1.05f
                ) { page ->
                    PreviewPage(
                        source = bitmapList[page],
                        modifier = Modifier.fillMaxSize(),
                        onScaleChanged = { newScale ->
                            HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                            if (page == pagerState.currentPage) {
                                currentPageScale = newScale
                            }
                        },
                        onPinchClosed = {
                            if (!imagePreviewViewModel.imagePreviewList[pdfName]!!.hasTriggeredSort) {
                                imagePreviewViewModel.setTriggerSort(
                                    pdfName,
                                    triggered = true
                                )
                                navController.navigate("image_sorting/$pdfName")
                                imagePreviewViewModel.setTriggerPreview(
                                    pdfName = pdfName,
                                    triggered = false
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun calculateDisplaySize(container: IntSize, image: IntSize): IntSize {
    if (image.width <= 0 || image.height <= 0 ||
        container.width <= 0 || container.height <= 0
    ) return IntSize.Zero

    val factor = ContentScale.Fit.computeScaleFactor(
        srcSize = Size(image.width.toFloat(), image.height.toFloat()),
        dstSize = Size(container.width.toFloat(), container.height.toFloat())
    )
    return IntSize(
        (image.width * factor.scaleX).toInt(),
        (image.height * factor.scaleY).toInt()
    )
}

fun calculateOffsetLimit(
    scale: Float,
    container: IntSize,
    image: IntSize
): Offset {
    val shown = calculateDisplaySize(container, image)
    val scaledWidth = shown.width * scale
    val scaledHeight = shown.height * scale
    val maxX = ((scaledWidth - container.width) / 2f).coerceAtLeast(0f)
    val maxY = ((scaledHeight - container.height) / 2f).coerceAtLeast(0f)
    return Offset(maxX, maxY)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewPage(
    source: Bitmap,
    modifier: Modifier = Modifier,
    onScaleChanged: (Float) -> Unit = {},
    onPinchClosed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var snapping by remember { mutableStateOf(false) }

    val onScaleChangedState = rememberUpdatedState(onScaleChanged)
    val onPinchClosedState = rememberUpdatedState(onPinchClosed)

    LaunchedEffect(Unit) {
        snapshotFlow { scale.value }.collect { onScaleChangedState.value(it) }
    }

    val imageSize = remember(source) { IntSize(source.width, source.height) }

    var ready by remember(source) {
        mutableStateOf(
            if (PreviewBitmapCache.needsFitting(source)) PreviewBitmapCache.peek(source)
            else source
        )
    }
    var failed by remember(source) { mutableStateOf(false) }

    LaunchedEffect(source) {
        if (ready != null) return@LaunchedEffect
        val result = PreviewBitmapCache.prepare(source)
        if (result != null) ready = result else failed = true
    }

    val imageBitmap = remember(ready) {
        ready?.takeIf { BitmapSafeLimits.isSafe(it) }?.asImageBitmap()
    }

    val containerState = rememberUpdatedState(containerSize)
    val imageSizeState = rememberUpdatedState(imageSize)

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        var isScaling = false

                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f) {
                                isScaling = true
                                val target =
                                    (scale.value * zoomChange).fastCoerceIn(0.6f, 35f)
                                if (!snapping) {
                                    snapping = true
                                    scope.launch {
                                        try {
                                            scale.snapTo(target)
                                        } finally {
                                            snapping = false
                                        }
                                    }
                                }
                            }

                            if (scale.value <= 0.65f) {
                                onPinchClosedState.value()
                            }

                            if (scale.value > 1f || isScaling) {
                                val limit = calculateOffsetLimit(
                                    scale = scale.value,
                                    container = containerState.value,
                                    image = imageSizeState.value
                                )
                                offsetX =
                                    (offsetX + panChange.x).fastCoerceIn(-limit.x, limit.x)
                                offsetY =
                                    (offsetY + panChange.y).fastCoerceIn(-limit.y, limit.y)
                                event.changes.forEach { it.consume() }
                            } else {
                                if (panChange.y.absoluteValue > panChange.x.absoluteValue) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        val limit = calculateOffsetLimit(
                            scale = scale.value,
                            container = containerState.value,
                            image = imageSizeState.value
                        )
                        offsetX = offsetX.fastCoerceIn(-limit.x, limit.x)
                        offsetY = offsetY.fastCoerceIn(-limit.y, limit.y)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        HapticManager.vibrate(context, VibrationEffect.EFFECT_DOUBLE_CLICK)
                        scope.launch {
                            val target = if (scale.value != 1f) 1f else 2.5f
                            scale.animateTo(target)
                            if (target == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            failed -> {
                Image(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                LoadingIndicator()
            }
        }
    }
}