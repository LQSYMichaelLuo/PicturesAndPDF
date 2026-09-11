package io.github.lqsymichaelluo.picturesandpdf

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.VibrationEffect
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    onBack: () -> Unit,
    imagePreviewViewModel: ImagePreviewViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    navAnimatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val imageID = "image_$currentIndex"
    val scope = rememberCoroutineScope()
    var colorState by imagePreviewViewModel.imagePreviewBackgroundColorState(imageID)
    val bitmapList = imagePreviewViewModel.imagePreviewList[pdfName]?.bitmapList
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { bitmapList!!.size }
    )

    val pageScales = remember { mutableMapOf<Int, Animatable<Float, *>>() }
    val pageOffsets = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    fun getScale(page: Int) = pageScales.getOrPut(page) { Animatable(1f) }
    fun getOffset(page: Int) = pageOffsets.getOrPut(page) { 0f to 0f }

    val selectedImageId = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_id", null)
        ?.collectAsState()
    LaunchedEffect(selectedImageId?.value) {
        val id = selectedImageId?.value
        if (id != null) {
            val index = id.toInt()
            if (index >= 0) {
                pagerState.scrollToPage(index)
                pageScales.getOrPut(index) { Animatable(1f) }
            }
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<String>("selected_image_id")
        }
    }
    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val currentPage = pagerState.currentPage
                val currentScale = getScale(currentPage)
                val (currentOffsetX, currentOffsetY) = getOffset(currentPage)
                val step = 100f

                val isZoomed = currentScale.value > 1.14f

                when {
                    event.matches(
                        key = Key.B,
                        ctrl = true
                    ) -> {
                        onBack()
                        true
                    }

                    event.matches(key = Key.G, ctrl = true, alt = true) -> {
                        toggleColorState()
                        true
                    }

                    event.matches(key = Key.DirectionLeft) -> {
                        if (isZoomed) {
                            HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                            pageOffsets[currentPage] = (currentOffsetX + step) to currentOffsetY
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                        }
                        true
                    }

                    event.matches(key = Key.DirectionRight) -> {
                        if (isZoomed) {
                            HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                            pageOffsets[currentPage] = (currentOffsetX - step) to currentOffsetY
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        }
                        true
                    }

                    event.matches(key = Key.DirectionDown) -> {
                        if (isZoomed) {
                            HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                            pageOffsets[currentPage] = currentOffsetX to (currentOffsetY - step)
                        }
                        true
                    }

                    event.matches(key = Key.DirectionUp) -> {
                        if (isZoomed) {
                            HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                            pageOffsets[currentPage] = currentOffsetX to (currentOffsetY + step)
                        }
                        true
                    }

                    event.matches(key = Key.RightBracket, ctrl = true) -> {
                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                        scope.launch {
                            val target = (currentScale.value * 1.25f).fastCoerceIn(0.6f, 35f)
                            currentScale.animateTo(target)
                        }
                        true
                    }

                    event.matches(key = Key.LeftBracket, ctrl = true) -> {
                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                        scope.launch {
                            val target = (currentScale.value * 0.8f).fastCoerceIn(0.6f, 35f)
                            currentScale.animateTo(target)
                            if (currentScale.value <= 1.05f) {
                                pageOffsets[currentPage] = 0f to 0f
                            }
                        }
                        if (currentScale.value <= 0.65f) {
                            if (!imagePreviewViewModel.imagePreviewList[pdfName]!!.hasTriggeredSort) {
                                imagePreviewViewModel.setTriggerSort(pdfName, triggered = true)
                                navController.navigate("image_sorting/$pdfName/0")
                                imagePreviewViewModel.clickedImageIndex.intValue = pagerState.currentPage
                                imagePreviewViewModel.setTriggerPreview(
                                    pdfName = pdfName,
                                    triggered = false
                                )
                            }
                        }
                        true
                    }

                    else -> false
                }
            },

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
                            PlainTooltip { Text("缩放程度") }
                        },
                        state = rememberTooltipState()
                    ) {
                        Text(
                            "%.0f%%  ".format(getScale(pagerState.currentPage).value * 100),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                                navController.navigate("image_sorting/$pdfName/0")
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
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapList != null) {
                    var currentPageScale by remember { mutableFloatStateOf(1f) }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { System.identityHashCode(bitmapList[it]) },
                        userScrollEnabled = getScale(pagerState.currentPage).value <= 1.14f
                    ) { page ->
                        val scale = getScale(page)
                        val (offsetX, offsetY) = getOffset(page)

                        PreviewPage(
                            source = bitmapList[page],
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "$pdfName-$page"),
                                    animatedVisibilityScope = navAnimatedVisibilityScope
                                ),
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            onOffsetChange = { x, y -> pageOffsets[page] = x to y },
                            onScaleChanged = { newScale ->
                                HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                if (page == pagerState.currentPage) {
                                    currentPageScale = newScale
                                }
                            },
                            onPinchClosed = {
                                if (!imagePreviewViewModel.imagePreviewList[pdfName]!!.hasTriggeredSort) {
                                    imagePreviewViewModel.setTriggerSort(pdfName, triggered = true)
                                    imagePreviewViewModel.clickedImageIndex.intValue = pagerState.currentPage
                                    navController.navigate("image_sorting/$pdfName/0")
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
    scale: Animatable<Float, *>,
    offsetX: Float,
    offsetY: Float,
    onOffsetChange: (Float, Float) -> Unit,
    onScaleChanged: (Float) -> Unit = {},
    onPinchClosed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val offsetXState = rememberUpdatedState(offsetX)
    val offsetYState = rememberUpdatedState(offsetY)
    val onOffsetChangeState = rememberUpdatedState(onOffsetChange)
    val onScaleChangedState = rememberUpdatedState(onScaleChanged)
    val onPinchClosedState = rememberUpdatedState(onPinchClosed)

    LaunchedEffect(Unit) {
        snapshotFlow { scale.value }.collect { onScaleChangedState.value(it) }
    }

    val imageSize = remember(source) { IntSize(source.width, source.height) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var snapping by remember { mutableStateOf(false) }
    var isGestureScaling by remember { mutableStateOf(false) }
    var lastScale by remember { mutableFloatStateOf(scale.value) }

    LaunchedEffect(scale.value, isGestureScaling) {
        if (!isGestureScaling) {
            val currentScale = scale.value
            if (lastScale != 0f && lastScale != currentScale) {
                val scaleFactor = currentScale / lastScale
                val newX = offsetXState.value * scaleFactor
                val newY = offsetYState.value * scaleFactor
                onOffsetChangeState.value(newX, newY)
            }
        }
        lastScale = scale.value
    }
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

    LaunchedEffect(offsetX, offsetY, scale.value, containerSize) {
        if (containerSize != IntSize.Zero && scale.value > 1f) {
            val limit = calculateOffsetLimit(
                scale = scale.value,
                container = containerSize,
                image = imageSize
            )
            val clampedX = offsetX.fastCoerceIn(-limit.x, limit.x)
            val clampedY = offsetY.fastCoerceIn(-limit.y, limit.y)
            if (clampedX != offsetX || clampedY != offsetY) {
                onOffsetChange(clampedX, clampedY)
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        isGestureScaling = false
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid()

                            val container = containerState.value
                            val pivotX = container.width / 2f
                            val pivotY = container.height / 2f

                            if (zoomChange != 1f) {
                                isGestureScaling = true
                                val oldScale = scale.value
                                val target = (oldScale * zoomChange).fastCoerceIn(0.6f, 35f)
                                val scaleFactor = target / oldScale

                                var newX =
                                    offsetXState.value * scaleFactor + (centroid.x - pivotX) * (1 - scaleFactor)
                                var newY =
                                    offsetYState.value * scaleFactor + (centroid.y - pivotY) * (1 - scaleFactor)

                                val limit = calculateOffsetLimit(
                                    scale = target,
                                    container = container,
                                    image = imageSizeState.value
                                )
                                newX = (newX + panChange.x).fastCoerceIn(-limit.x, limit.x)
                                newY = (newY + panChange.y).fastCoerceIn(-limit.y, limit.y)

                                onOffsetChangeState.value(newX, newY)

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
                            } else {
                                if (scale.value > 1f) {
                                    val limit = calculateOffsetLimit(
                                        scale = scale.value,
                                        container = container,
                                        image = imageSizeState.value
                                    )
                                    val finalX = (offsetXState.value + panChange.x).fastCoerceIn(
                                        -limit.x,
                                        limit.x
                                    )
                                    val finalY = (offsetYState.value + panChange.y).fastCoerceIn(
                                        -limit.y,
                                        limit.y
                                    )
                                    onOffsetChangeState.value(finalX, finalY)
                                    event.changes.forEach { it.consume() }
                                } else {
                                    if (panChange.y.absoluteValue > panChange.x.absoluteValue) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }

                            if (scale.value <= 0.65f) onPinchClosedState.value()

                        } while (event.changes.any { it.pressed })
                        isGestureScaling = false
                        val limit = calculateOffsetLimit(
                            scale = scale.value,
                            container = containerState.value,
                            image = imageSizeState.value
                        )
                        onOffsetChangeState.value(
                            offsetXState.value.fastCoerceIn(-limit.x, limit.x),
                            offsetYState.value.fastCoerceIn(-limit.y, limit.y)
                        )
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
                            if (target == 1f) onOffsetChangeState.value(0f, 0f)
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

            else -> LoadingIndicator()
        }
    }
}