package io.github.lqsymichaelluo.picturesandpdf

import android.view.DragEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun Pic2PDFScreen(
    viewModel: MainViewModel,
    rootNavController: NavHostController,
    imagePreviewViewModel: ImagePreviewViewModel,

    onImportPicture: (String?) -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit
) {
    val debuggable by AppFlags.debuggable

    val gridState = rememberLazyStaggeredGridState()

    DisposableEffect(Unit) {
        onDispose { ThumbnailCache.trimToHalf() }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (viewModel.pictureInputList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_inventory),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(
                    modifier = Modifier.height(36.dp)
                )
                Text(
                    text = "列表为空呢(╥﹏╥)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = StaggeredGridCells.Adaptive(minSize = 338.dp),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            for ((pdfName, state) in viewModel.pictureInputList) {
                item(key = pdfName) {
                    PictureGroupCard(
                        PDFName = pdfName,
                        state = state,
                        viewModel = viewModel,
                        imagePreviewViewModel = imagePreviewViewModel,
                        modifier = Modifier.animateItem(),
                        rootNavController = rootNavController,
                        gridState = gridState,
                        onPDFNameChange = { oldName, newName ->
                            viewModel.changeOutputPDFName(oldName, newName + ".pdf")
                        },
                        onImportPicture = onImportPicture,
                        requestDragAndDropPermission = requestDragAndDropPermission,
                        releaseDragAndDropPermission = releaseDragAndDropPermission
                    )
                }
            }
            if (debuggable) {
                item {
                    Text(
                        text = viewModel.pictureInputList.toString(),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PictureGroupCard(
    PDFName: String,
    state: PDFOutputState,
    viewModel: MainViewModel,
    imagePreviewViewModel: ImagePreviewViewModel,
    modifier: Modifier,
    gridState: LazyStaggeredGridState,
    onPDFNameChange: (oldName: String, newName: String) -> String,
    onImportPicture: (String?) -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    rootNavController: NavHostController
) {
    val context = LocalContext.current
    var controlMode by viewModel.controlModeState(PDFName)
    val pictureShowState = controlMode == ControlMode.SHOW
    val rotation by viewModel.rotationState(PDFName)
    var isError by remember { mutableStateOf(false) }
    var isConfigChangerShow = controlMode == ControlMode.CONFIG
    var isDeleteDialogShow = controlMode == ControlMode.DELETE
    var newPDFName by viewModel.newNameState(PDFName)
    var newPDFNameTitle by remember { mutableStateOf(newPDFName) }
    val addButtonInteractionSource = remember { MutableInteractionSource() }
    val dragPress = remember { mutableStateOf<PressInteraction.Press?>(null) }
    val bitmapList = state.bitmaps
    val errorColor by animateColorAsState(
        targetValue = if (controlMode == ControlMode.DELETE) MaterialTheme.colorScheme.error else CardDefaults.cardColors().contentColor
    )

    fun toggleMode(target: ControlMode) {
        if ((target != ControlMode.SHOW && controlMode == ControlMode.SHOW) || (target == ControlMode.SHOW && controlMode != ControlMode.SHOW) || (target == ControlMode.SHOW && controlMode == ControlMode.SHOW)) {
            viewModel.rotationState(PDFName).value += 180f
        }
        controlMode = if (controlMode == target) ControlMode.NONE else target
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                        toggleMode(ControlMode.SHOW)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$newPDFNameTitle.pdf",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f, fill = false)
                )
                Row {
                    val configTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("更改输出设置") } },
                        state = configTooltipState
                    ) {
                        IconButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(ControlMode.CONFIG)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_pencil),
                                contentDescription = null
                            )
                        }
                    }
                    val deleteTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("删除这个图片组") } },
                        state = deleteTooltipState,
                    ) {
                        IconButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(ControlMode.DELETE)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = errorColor
                            )
                        }
                    }
                    val animatedRotation by animateFloatAsState(
                        targetValue = rotation,
                        animationSpec = tween(500),
                        label = "rotation"
                    )
                    val expandTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("展开/折叠图片组") } },
                        state = expandTooltipState,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = animatedRotation
                            }
                            .clip(CircleShape)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .clip(CircleShape),
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(ControlMode.SHOW)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = controlMode != ControlMode.NONE,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AnimatedContent(
                    targetState = controlMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { target ->
                    when (target) {
                        ControlMode.SHOW -> BoxWithConstraints {
                            val picMinWidth = 92.dp
                            val picMaxWidth = 104.dp
                            val itemNum =
                                (maxWidth - 8.dp) / ((picMinWidth + picMaxWidth) / 2 + 8.dp)
                            val itemWidth = ((maxWidth - 4.dp) / itemNum.roundToInt()) - 8.dp
                            val itemWidthPx =
                                with(LocalDensity.current) { itemWidth.toPx() }.roundToInt()
                            val thumbModifier = Modifier
                                .size(itemWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 4.dp,
                                        end = 4.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                bitmapList.forEachIndexed { index, bitmap ->
                                    val code = System.identityHashCode(bitmap)
                                    var deletePictureButtonShow by viewModel.deletePictureButtonShowState(
                                        code
                                    )
                                    key(code) {
                                        Box(
                                            modifier = Modifier.size(itemWidth)
                                        ) {
                                            val thumbnail = rememberLazyThumbnail(
                                                source = bitmap,
                                                targetPx = itemWidthPx,
                                                gridState = gridState
                                            )
                                            val imageBitmap =
                                                remember(thumbnail) { thumbnail?.asImageBitmap() }

                                            val clickModifier = thumbModifier.combinedClickable(
                                                onClick = {
                                                    HapticManager.vibrate(
                                                        context,
                                                        HapticManager.EFFECT_CLICK
                                                    )
                                                    val imagePreviewData = ImagePreviewData(
                                                        bitmapList = bitmapList.toMutableStateList()
                                                    )
                                                    imagePreviewViewModel.addImagePreviewList(
                                                        pdfName = "$newPDFNameTitle.pdf",
                                                        imagePreviewData = imagePreviewData
                                                    )
                                                    rootNavController.navigate("image_preview/$newPDFNameTitle.pdf/$index")
                                                },
                                                onLongClick = {
                                                    HapticManager.vibrate(
                                                        context,
                                                        HapticManager.EFFECT_HEAVY_CLICK
                                                    )
                                                    deletePictureButtonShow =
                                                        !deletePictureButtonShow
                                                }
                                            )

                                            if (imageBitmap != null) {
                                                Image(
                                                    bitmap = imageBitmap,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = clickModifier
                                                )
                                            } else {
                                                LoadingIndicator()
                                            }

                                            CompositionLocalProvider(
                                                LocalMinimumInteractiveComponentSize provides 4.dp
                                            ) {
                                                if (bitmapList.size <= 1) {
                                                    deletePictureButtonShow = false
                                                }
                                                androidx.compose.animation.AnimatedVisibility(
                                                    modifier = Modifier.align(Alignment.TopEnd),
                                                    visible = deletePictureButtonShow && !(bitmapList.size == 1 && index == 0),
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
                                                            .padding(1.5.dp)
                                                            .size(24.dp)
                                                            .background(
                                                                color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                                                    alpha = 0.45f
                                                                ),
                                                                shape = CircleShape
                                                            ),
                                                        onClick = {
                                                            HapticManager.vibrate(
                                                                context,
                                                                HapticManager.EFFECT_CLICK
                                                            )
                                                            viewModel.deletePictureFromGroup(
                                                                pdfName = newPDFName + ".pdf",
                                                                bitmap
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
                                Button(
                                    interactionSource = addButtonInteractionSource,
                                    modifier = Modifier
                                        .size(itemWidth)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .dragAndDropTarget(
                                            shouldStartDragAndDrop = { event ->
                                                event.mimeTypes().any { it.startsWith("image/") }
                                            },
                                            target = remember {
                                                object : DragAndDropTarget {
                                                    override fun onStarted(event: DragAndDropEvent) {}

                                                    override fun onEntered(event: DragAndDropEvent) {
                                                        HapticManager.vibrate(
                                                            context,
                                                            HapticManager.EFFECT_CLICK
                                                        )
                                                        val press =
                                                            PressInteraction.Press(Offset.Zero)
                                                        dragPress.value = press
                                                        addButtonInteractionSource.tryEmit(press)
                                                    }

                                                    override fun onExited(event: DragAndDropEvent) {
                                                        dragPress.value?.let {
                                                            addButtonInteractionSource.tryEmit(
                                                                PressInteraction.Cancel(it)
                                                            )
                                                        }
                                                        dragPress.value = null
                                                    }

                                                    override fun onEnded(event: DragAndDropEvent) {
                                                        dragPress.value?.let {
                                                            addButtonInteractionSource.tryEmit(
                                                                PressInteraction.Release(it)
                                                            )
                                                        }
                                                        dragPress.value = null
                                                    }

                                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                                        requestDragAndDropPermission(event.toAndroidDragEvent())
                                                        val clipData = event
                                                            .toAndroidDragEvent()
                                                            .clipData ?: return false
                                                        viewModel.addPicturesFromClipData(
                                                            context,
                                                            clipData,
                                                            PDFName,
                                                            releaseDragAndDropPermission
                                                        )
                                                        return true
                                                    }
                                                }
                                            }),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    onClick = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                        onImportPicture(PDFName)
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_add),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(itemWidth / 3)
                                        )
                                        Text(text = stringResource(R.string.add))
                                    }
                                }
                            }
                        }

                        ControlMode.CONFIG -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "输出PDF设置",
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    bottom = 6.dp
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 4.dp,
                                        end = 4.dp,
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                        }
                                ) {
                                    Text(
                                        text = "输出PDF的文件名",
                                        modifier = Modifier
                                            .padding(
                                                top = 6.dp,
                                                start = 8.dp,
                                                end = 8.dp,
                                            )
                                    )
                                    PDFName.let {
                                        OutlinedTextField(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 8.dp,
                                                    end = 8.dp,
                                                ),
                                            value = newPDFName,
                                            onValueChange = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_TICK
                                                )
                                                newPDFName = it
                                                isError = newPDFName.isBlank()
                                            },
                                            supportingText = {
                                                if (isError) {
                                                    Text(
                                                        text = "PDF名称不能为空",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            },
                                            isError = isError,
                                            label = {
                                                Text("")
                                            },
                                            singleLine = true
                                        )
                                    }
                                    TextButton(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(
                                                start = 8.dp,
                                                end = 8.dp,
                                                bottom = 2.dp
                                            ),
                                        enabled = newPDFName.isNotBlank(),
                                        onClick = {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                            if (newPDFName.isNotBlank()) {
                                                val finalName = onPDFNameChange(PDFName, newPDFName)
                                                newPDFName = finalName.dropLast(4)
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    ) {
                                        Text(stringResource(R.string.apply))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        end = 8.dp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                            val imagePreviewData = ImagePreviewData(
                                                bitmapList = bitmapList.toMutableStateList()
                                            )
                                            imagePreviewViewModel.addImagePreviewList(
                                                pdfName = "$newPDFNameTitle.pdf",
                                                imagePreviewData = imagePreviewData
                                            )
                                            rootNavController.navigate("image_sorting/$newPDFNameTitle.pdf/1")
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            8.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_sort),
                                            contentDescription = null
                                        )
                                        Spacer(
                                            modifier = Modifier.width(12.dp)
                                        )
                                        Text(
                                            "为图片组排序"
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_back),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .rotate(180f)
                                            .padding(
                                                10.dp
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        end = 8.dp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                            viewModel.setPreProcessing(
                                                PDFName,
                                                !state.usePreProcessing
                                            )
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "是否进行预处理？",
                                            modifier = Modifier.padding(
                                                8.dp
                                            )
                                        )
                                        Switch(
                                            modifier = Modifier.padding(
                                                8.dp
                                            ),
                                            checked = state.usePreProcessing,
                                            onCheckedChange = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.setPreProcessing(PDFName, it)
                                            }
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = state.usePreProcessing
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "质量",
                                                modifier = Modifier.padding(
                                                    8.dp
                                                )
                                            )
                                            Slider(
                                                modifier = Modifier.padding(
                                                    8.dp
                                                ),
                                                value = state.compressQuality.toFloat(),
                                                onValueChange = {
                                                    HapticManager.vibrate(
                                                        context,
                                                        HapticManager.EFFECT_TICK
                                                    )
                                                    viewModel.setCompressQuality(
                                                        PDFName,
                                                        it.roundToInt()
                                                    )
                                                },
                                                valueRange = 0f..100f,
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        end = 8.dp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                        }
                                ) {
                                    Text(
                                        "图片处理模块占位" + "文本".repeat(Random.nextInt(1,19)),
                                        modifier = Modifier.padding(
                                            8.dp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        ControlMode.DELETE -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "删除此项？",
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    bottom = 12.dp
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text("此操作不可撤销。")
                                TextButton(
                                    modifier = Modifier.align(Alignment.End),
                                    onClick = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                        viewModel.deletePicturesGroup(PDFName)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    )
                                ) {
                                    Text(stringResource(R.string.ok))
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}