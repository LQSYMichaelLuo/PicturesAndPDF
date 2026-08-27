package io.github.lqsymichaelluo.picturesandpdf

import android.view.DragEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImageSortingScreen(
    pdfName: String = "unknown.pdf",
    onBack: () -> Unit = {},
    onImportPicture: (String?) -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    imagePreviewViewModel: ImagePreviewViewModel
) {
    BackHandler {
        onBack()
    }
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    val hapticFeedback = LocalHapticFeedback.current

    var receivingDrag by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (receivingDrag)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (receivingDrag)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val importButtonInteractionSource = remember { MutableInteractionSource() }
    val dragPress = remember { mutableStateOf<PressInteraction.Press?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片排序") },
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
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null
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
                            PlainTooltip { Text(stringResource(R.string.import_str)) }
                        },
                        state = rememberTooltipState()
                    ) {
                        Button(
                            shape = if (receivingDrag) RoundedCornerShape(24.dp)
                            else CircleShape,
                            contentPadding = if (receivingDrag) PaddingValues(8.dp)
                            else PaddingValues(0.dp),
                            interactionSource = importButtonInteractionSource,
                            modifier = Modifier
                                .wrapContentWidth()
                                .defaultMinSize(minWidth = 42.dp)
                                .animateContentSize()
                                .dragAndDropTarget(
                                    shouldStartDragAndDrop = { event ->
                                        event.mimeTypes()
                                            .any { it.startsWith("image/") }
                                    },
                                    target = remember {
                                        object : DragAndDropTarget {
                                            override fun onStarted(event: DragAndDropEvent) {
                                                receivingDrag = true
                                            }

                                            override fun onEntered(event: DragAndDropEvent) {
                                                val press = PressInteraction.Press(Offset.Zero)
                                                dragPress.value = press
                                                importButtonInteractionSource.tryEmit(press)
                                            }

                                            override fun onExited(event: DragAndDropEvent) {
                                                dragPress.value?.let {
                                                    importButtonInteractionSource.tryEmit(
                                                        PressInteraction.Cancel(it)
                                                    )
                                                }
                                                dragPress.value = null
                                            }

                                            override fun onEnded(event: DragAndDropEvent) {
                                                dragPress.value?.let {
                                                    importButtonInteractionSource.tryEmit(
                                                        PressInteraction.Release(it)
                                                    )
                                                }
                                                dragPress.value = null
                                                receivingDrag = false
                                            }

                                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                                val androidEvent = event.toAndroidDragEvent()
                                                requestDragAndDropPermission(androidEvent)
                                                val clipData = androidEvent
                                                    .clipData ?: return false
                                                imagePreviewViewModel.addPicturesFromClipData(
                                                    context,
                                                    clipData,
                                                    pdfName,
                                                    releaseDragAndDropPermission
                                                )
                                                return true
                                            }
                                        }
                                    }),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            onClick = {
                                onImportPicture(pdfName)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_import),
                                    contentDescription = stringResource(R.string.import_str)
                                )
                                AnimatedVisibility(
                                    visible = receivingDrag,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.dragtip))
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0)
                )
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
                                scale = (scale * zoomChange).coerceIn(0.45f, 2.5f)

                                val previewEntry = imagePreviewViewModel.imagePreviewList[pdfName]
                                if (scale >= 2f && previewEntry != null && !previewEntry.hasTriggeredPreview) {
                                    imagePreviewViewModel.setTriggerPreview(
                                        pdfName = pdfName,
                                        triggered = true
                                    )
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
                imagePreviewViewModel.moveBitmap(
                    pdfName = pdfName,
                    from.index,
                    to.index
                )
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            val minCellSize = (100.dp * scale).coerceIn(45.dp, 250.dp)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCellSize),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 3.dp,
                    top = padding.calculateTopPadding(),
                    end = 3.dp
                )
            ) {
                imagePreviewViewModel.imagePreviewList[pdfName]!!.bitmapList.let { list ->
                    items(list.size, key = {
                        System.identityHashCode(list[it])
                    }) { index ->
                        ReorderableItem(
                            reorderableState,
                            key = System.identityHashCode(list[index])
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 16.dp else 0.dp
                            )
                            val borderStrokeWidth by animateDpAsState(
                                if (isDragging) 0.dp else 0.5.dp
                            )

                            val overrideSize = (200 * scale).toInt().coerceIn(100, 800)
                            val code = System.identityHashCode(list[index])
                            var deletePictureButtonShow by imagePreviewViewModel.deletePictureButtonShowState(
                                code
                            )
                            key(code) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    GlideImage(
                                        model = list[index],
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        loading = placeholder(R.drawable.ic_pdf2pic),
                                        failure = placeholder(R.drawable.ic_error),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(3.dp)
                                            .shadow(
                                                elevation = elevation,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = borderStrokeWidth,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                }
                                            )
                                            .clickable(
                                                interactionSource = null,
                                                indication = null,
                                            ) {
                                                deletePictureButtonShow =
                                                    !deletePictureButtonShow
                                            }
                                            .animateItem()
                                    ) { requestBuilder ->
                                        requestBuilder
                                            .override(overrideSize, overrideSize)
                                            .centerCrop()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    }
                                    CompositionLocalProvider(
                                        LocalMinimumInteractiveComponentSize provides 4.dp
                                    ) {
                                        if (imagePreviewViewModel.imagePreviewList[pdfName]!!.bitmapList.size <= 1) {
                                            deletePictureButtonShow = false
                                        }
                                        AnimatedVisibility(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp),
                                            visible = deletePictureButtonShow && !(imagePreviewViewModel.imagePreviewList[pdfName]!!.bitmapList.size == 1 && index == 0),
                                            enter = fadeIn(tween(150)) + scaleIn(
                                                initialScale = 0.6f,
                                                animationSpec = tween(150)
                                            ),
                                            exit = fadeOut(tween(100)) + scaleOut(
                                                targetScale = 0.6f,
                                                animationSpec = tween(100)
                                            )
                                        ) {
                                            IconButton(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                                            alpha = 0.45f
                                                        ),
                                                        shape = CircleShape
                                                    ),
                                                onClick = {
                                                    imagePreviewViewModel.deletePictureFromGroup(
                                                        pdfName = pdfName,
                                                        imagePreviewViewModel.imagePreviewList[pdfName]!!.bitmapList[index]
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_close),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}