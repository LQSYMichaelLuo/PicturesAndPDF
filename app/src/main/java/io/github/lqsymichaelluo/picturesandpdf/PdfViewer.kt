package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

//Thanks AI. --TiSu
private const val MAX_CACHED_PAGES = 10
private const val BASE_WIDTH = 1080
private const val MIN_RENDER_WIDTH = 480
private const val MAX_RENDER_WIDTH = 2400

private val PAGE_SPACING = 8.dp
private val CONTENT_PADDING_V = 8.dp
private val SCROLLBAR_WIDTH = 6.dp
private val SCROLLBAR_MIN_THUMB = 56.dp

private class OpenResult(
    val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer,
    val heights: List<Int>,
    val count: Int
)

class PdfRenderState(private val file: File) {

    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null

    var cacheWidth by mutableIntStateOf(0)
        private set

    private val cache = object : LinkedHashMap<Int, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>): Boolean {
            if (size > MAX_CACHED_PAGES) {
                eldest.value.recycle()
                return true
            }
            return false
        }
    }

    var pageCount by mutableIntStateOf(0)
        private set

    var error by mutableStateOf<Throwable?>(null)
        private set

    var pageHeights by mutableStateOf(emptyList<Int>())
        private set

    fun peek(index: Int): Bitmap? = synchronized(cache) { cache[index] }

    suspend fun open() {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val d = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val r = PdfRenderer(d)
                val heights = (0 until r.pageCount).map { i ->
                    r.openPage(i).use { page ->
                        (page.height * (BASE_WIDTH.toFloat() / page.width)).roundToInt()
                    }
                }
                OpenResult(d, r, heights, r.pageCount)
            }
        }

        withContext(Dispatchers.Main) {
            result.onSuccess { r ->
                descriptor = r.descriptor
                renderer = r.renderer
                pageHeights = r.heights
                pageCount = r.count
            }.onFailure { error = it }
        }
    }

    suspend fun renderPage(index: Int, width: Int): Bitmap? = mutex.withLock {
        if (width != cacheWidth) {
            synchronized(cache) {
                cache.values.forEach { if (!it.isRecycled) it.recycle() }
                cache.clear()
            }
            cacheWidth = width
        }
        synchronized(cache) { cache[index] }?.let { return@withLock it }

        withContext(Dispatchers.IO) {
            runCatching {
                val r = renderer ?: return@runCatching null
                r.openPage(index).use { page ->
                    val s = width.toFloat() / page.width
                    val h = (page.height * s).roundToInt()
                    createBitmap(width, h).also { bmp ->
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        synchronized(cache) { cache[index] = bmp }
                    }
                }
            }.getOrNull()
        }
    }

    fun close() {
        synchronized(cache) {
            cache.values.forEach { if (!it.isRecycled) it.recycle() }
            cache.clear()
        }
        renderer?.close()
        descriptor?.close()
        renderer = null
        descriptor = null
    }
}

@Composable
fun rememberPdfRenderState(file: File): PdfRenderState {
    val state = remember(file) { PdfRenderState(file) }
    DisposableEffect(state) {
        onDispose { state.close() }
    }
    LaunchedEffect(state) { state.open() }
    return state
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PdfViewer(
    file: File,
    modifier: Modifier = Modifier,
    indicatorDismissDelay: Long = 1500L
) {
    val state = rememberPdfRenderState(file)
    val density = LocalDensity.current

    if (state.error != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reading PDF file failed T_T：${state.error?.message}")
        }
        return
    }

    if (state.pageCount == 0 || state.pageHeights.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }

    val spacingPx = with(density) { PAGE_SPACING.roundToPx() }
    val paddingPx = with(density) { CONTENT_PADDING_V.roundToPx() }

    val renderWidthPx by remember(viewportWidthPx) {
        derivedStateOf {
            if (viewportWidthPx <= 0) 0
            else viewportWidthPx.coerceIn(MIN_RENDER_WIDTH, MAX_RENDER_WIDTH)
        }
    }

    val displayHeights by remember(state.pageHeights, viewportWidthPx) {
        derivedStateOf {
            val w = viewportWidthPx.takeIf { it > 0 } ?: BASE_WIDTH
            state.pageHeights.map { h -> h * w / BASE_WIDTH }
        }
    }

    val totalContentPx by remember(displayHeights, spacingPx, paddingPx) {
        derivedStateOf {
            if (displayHeights.isEmpty()) 0
            else displayHeights.sum() + spacingPx * (displayHeights.size - 1) + paddingPx * 2
        }
    }

    fun offsetBefore(index: Int): Int {
        val hs = displayHeights
        if (hs.isEmpty()) return paddingPx
        var sum = paddingPx
        for (i in 0 until index.coerceAtMost(hs.size)) sum += hs[i] + spacingPx
        return sum
    }

    val scrollProgress by remember(listState, totalContentPx, viewportHeightPx) {
        derivedStateOf {
            val maxScroll = totalContentPx - viewportHeightPx
            if (maxScroll <= 0) return@derivedStateOf 0f
            val scrolled =
                offsetBefore(listState.firstVisibleItemIndex) +
                        listState.firstVisibleItemScrollOffset - paddingPx
            (scrolled.toFloat() / maxScroll).coerceIn(0f, 1f)
        }
    }

    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    var draggingScrollbar by remember { mutableStateOf(false) }
    var idle by remember { mutableStateOf(true) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            idle = false
        } else {
            delay(indicatorDismissDelay.milliseconds)
            idle = true
        }
    }
    val overlayVisible = !idle || draggingScrollbar

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewportWidthPx = it.width
                viewportHeightPx = it.height
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = CONTENT_PADDING_V),
            verticalArrangement = Arrangement.spacedBy(PAGE_SPACING)
        ) {
            items(state.pageCount, key = { it }) { index ->
                PdfPage(
                    state = state,
                    index = index,
                    renderWidthPx = renderWidthPx,
                    displayHeightPx = displayHeights.getOrNull(index) ?: 0
                )
            }
        }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "$currentPage / ${state.pageCount}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        PdfFastScrollbar(
            visible = overlayVisible,
            progress = scrollProgress,
            viewportHeightPx = viewportHeightPx,
            totalContentPx = totalContentPx,
            onDragStateChange = { draggingScrollbar = it },
            onScrollBy = { delta -> scope.launch { listState.scrollBy(delta) } },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp, top = 20.dp, bottom = 20.dp)
        )
    }
}

@Composable
private fun PdfFastScrollbar(
    visible: Boolean,
    progress: Float,
    viewportHeightPx: Int,
    totalContentPx: Int,
    onDragStateChange: (Boolean) -> Unit,
    onScrollBy: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var trackHeightPx by remember { mutableIntStateOf(0) }

    val thumbHeightPx = remember(trackHeightPx, viewportHeightPx, totalContentPx) {
        if (totalContentPx <= 0 || trackHeightPx == 0) 0
        else {
            val raw = trackHeightPx.toFloat() * viewportHeightPx / totalContentPx
            maxOf(raw.roundToInt(), with(density) { SCROLLBAR_MIN_THUMB.roundToPx() })
                .coerceAtMost(trackHeightPx)
        }
    }
    val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0)

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "scrollbarAlpha"
    )

    val thumbColor = MaterialTheme.colorScheme.onSurface
    val trackColor = MaterialTheme.colorScheme.surface
    val pillShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .width(SCROLLBAR_WIDTH * 2)
            .fillMaxHeight()
            .graphicsLayer { this.alpha = alpha }
            .background(trackColor, pillShape)
            .onSizeChanged {
                HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                trackHeightPx = it.height
            }
            .pointerInput(travelPx, totalContentPx, viewportHeightPx) {
                if (travelPx <= 0 || totalContentPx <= 0) return@pointerInput
                val maxScroll = (totalContentPx - viewportHeightPx).coerceAtLeast(0)
                detectVerticalDragGestures(
                    onDragStart = { onDragStateChange(true) },
                    onDragEnd = { onDragStateChange(false) },
                    onDragCancel = { onDragStateChange(false) },
                    onVerticalDrag = { change, dragAmount ->
                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                        change.consume()
                        onScrollBy(dragAmount / travelPx.toFloat() * maxScroll)
                    }
                )
            }
    ) {
        if (thumbHeightPx > 0) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { thumbHeightPx.toDp() })
                    .offset { IntOffset(0, (progress * travelPx).roundToInt()) }
                    .background(thumbColor, pillShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PdfPage(
    state: PdfRenderState,
    index: Int,
    renderWidthPx: Int,
    displayHeightPx: Int
) {
    val density = LocalDensity.current
    val heightDp = with(density) { displayHeightPx.toDp() }

    var bitmap by remember(index, renderWidthPx) {
        mutableStateOf(if (state.cacheWidth == renderWidthPx) state.peek(index) else null)
    }

    LaunchedEffect(index, renderWidthPx) {
        if (bitmap == null && renderWidthPx > 0) {
            bitmap = state.renderPage(index, renderWidthPx)
        }
    }

    val bmp = bitmap
    if (bmp != null && !bmp.isRecycled) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth().height(heightDp)
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(heightDp),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(modifier = Modifier.padding(48.dp))
        }
    }
}