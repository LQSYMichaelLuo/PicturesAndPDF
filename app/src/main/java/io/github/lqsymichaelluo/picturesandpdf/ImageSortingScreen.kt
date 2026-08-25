package io.github.lqsymichaelluo.picturesandpdf

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImageSortingScreen(
    onBack: () -> Unit = {},
    imagePreviewViewModel: ImagePreviewViewModel
) {
    BackHandler {
        onBack()
    }
    val density = LocalDensity.current
    var scale = remember { 1f }
    val hapticFeedback = LocalHapticFeedback.current
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片排序") },
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
                                contentDescription = null
                            )
                        }
                    }
                },
                actions = {
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                scale = (scale * zoomChange).coerceIn(0.5f, 5f)

                                if (scale >= 1.2f && !imagePreviewViewModel.hasTriggerPreview.value) {
                                    imagePreviewViewModel.setTriggerPreview(true)
                                    onBack()
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            val gridState = rememberLazyGridState()
            val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
                imagePreviewViewModel.moveBitmap(from.index, to.index)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            var itemWidth by remember { mutableStateOf(92.dp) }
            val itemWidthPx: Int = with(density) {
                itemWidth.toPx().toInt()
            }
            val density = LocalDensity.current
            LazyVerticalGrid(
                columns = GridCells.Adaptive(114.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues = PaddingValues(
                    start = 3.dp,
                    top = padding.calculateTopPadding(),
                    end = 3.dp
                ))
            ) {
                imagePreviewViewModel.bitmapList?.let { list ->
                    items(list.size, key = {
                        System.identityHashCode(list[it])
                    }) { index ->
                        ReorderableItem(
                            reorderableState,
                            key = System.identityHashCode(list[index])
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 8.dp else 0.dp
                            )
                            GlideImage(
                                model = list[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                loading = placeholder(R.drawable.ic_pdf2pic),
                                failure = placeholder(R.drawable.ic_error),
                                modifier = Modifier.fillMaxWidth()
                                    .padding(3.dp)
                                    .onSizeChanged { size ->
                                        itemWidth = with(density) {
                                            size.width.toDp()
                                        }
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        }
                                    )
                                    .animateItem()
                                    .shadow(elevation)

                            ) { requestBuilder ->
                                requestBuilder
                                    .override(itemWidthPx, itemWidthPx)
                                    .centerCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                            }
                        }
                    }
                }
            }
        }
    }
}