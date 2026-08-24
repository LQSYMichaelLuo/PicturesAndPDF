package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.view.DragEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlin.math.roundToInt


@Composable
fun Pic2PDFScreen(
    viewModel: MainViewModel,
    rootNavController: NavHostController,
    imagePreviewViewModel: ImagePreviewViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onImportPicture: (String?) -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit
) {
    val context = LocalContext.current
    val debuggable by AppFlags.debuggable
    var receivingPictures by remember { mutableStateOf(false) }
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
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            for ((pdfName, bitmaps) in viewModel.pictureInputList) {
                item(key = pdfName) {
                    var newName = ""
                    PictureGroupCard(
                        PDFName = pdfName,
                        bitmapList = bitmaps,
                        viewModel = viewModel,
                        imagePreviewViewModel = imagePreviewViewModel,
                        modifier = Modifier.animateItem(),
                        rootNavController = rootNavController,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onPDFNameChange = { pdfName, newName ->
                            viewModel.changeOutputPDFName(pdfName, newName + ".pdf")
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun PictureGroupCard(
    PDFName: String,
    bitmapList: List<Bitmap>,
    viewModel: MainViewModel,
    imagePreviewViewModel: ImagePreviewViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    onPDFNameChange: (oldName: String, newName: String) -> String,
    onImportPicture: (String?) -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    rootNavController: NavHostController
) {
    val context = LocalContext.current
    val foldStatus by viewModel.foldState(PDFName)
    val rotation by viewModel.rotationState(PDFName)
    var isError by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isChangeNameDialogShow by viewModel.changeNameDialogShowState(PDFName)
    var isDeleteDialogShow by viewModel.deletePicturesDialogShowState(PDFName)
    var newPDFName by viewModel.newNameState(PDFName)
    var newPDFNameTitle by remember { mutableStateOf(newPDFName) }
    val interactionSource = remember { MutableInteractionSource() }
    val addButtonInteractionSource = remember { MutableInteractionSource() }
    val dragPress = remember { mutableStateOf<PressInteraction.Press?>(null) }

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
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        viewModel.foldState(PDFName).value = !foldStatus
                        viewModel.rotationState(PDFName).value += 180f
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(PDFName) } },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = "$newPDFNameTitle.pdf",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )
                }
                Row() {
                    val renameTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("更改输出的PDF名") } },
                        state = renameTooltipState
                    ) {
                        IconButton(
                            onClick = {
                                isChangeNameDialogShow = true
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
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("删除这个图片组") } },
                        state = deleteTooltipState,
                    ) {
                        IconButton(
                            onClick = {
                                isDeleteDialogShow = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null
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
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                    onClick = {
                                        viewModel.foldState(PDFName).value = !foldStatus
                                        viewModel.rotationState(PDFName).value += 180f
                                    }
                                )
                                .clip(CircleShape),
                            onClick = {
                                viewModel.foldState(PDFName).value = !foldStatus
                                viewModel.rotationState(PDFName).value += 180f
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
                visible = !foldStatus,
            ) {
                BoxWithConstraints {
                    val picMinWidth = 92.dp
                    val picMaxWidth = 104.dp
                    val itemNum = (maxWidth - 8.dp) / ((picMinWidth + picMaxWidth) / 2 + 8.dp)
                    val itemWidth = ((maxWidth - 4.dp) / itemNum.roundToInt()) - 8.dp
                    val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }.roundToInt()
                    val thumbModifier = Modifier
                        .size(itemWidth)
                        .clip(RoundedCornerShape(8.dp))
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
                            val code = bitmap.hashCode()
                            var deletePictureButtonShow by viewModel.deletePictureButtonShowState(
                                code
                            )
                            key(code) {
                                Box(
                                    modifier = Modifier.size(itemWidth)
                                ) {
                                    GlideImage(
                                        model = bitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        loading = placeholder(R.drawable.ic_pdf2pic),
                                        failure = placeholder(R.drawable.ic_error),
                                        modifier = thumbModifier.combinedClickable(
                                            onClick = {
                                                selectedBitmap = bitmap
                                                imagePreviewViewModel.setBitmapList(bitmapList)
                                                imagePreviewViewModel.setCurrent(index)
                                                rootNavController.navigate("image_preview")
                                            },
                                            onLongClick = {
                                                deletePictureButtonShow = !deletePictureButtonShow
                                            }
                                        )
                                    ) { requestBuilder ->
                                        requestBuilder
                                            .override(itemWidthPx, itemWidthPx)
                                            .centerCrop()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    }

                                    CompositionLocalProvider(
                                        LocalMinimumInteractiveComponentSize provides 4.dp
                                    ) {
                                        if (bitmapList.size <= 1) { deletePictureButtonShow = false }
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
                                                    //.align(Alignment.TopEnd)
                                                    .padding(1.5.dp)
                                                    .size(24.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f),
                                                        shape = CircleShape
                                                    ),
                                                onClick = {
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
                                            override fun onStarted(event: DragAndDropEvent) {
                                            }

                                            override fun onEntered(event: DragAndDropEvent) {
                                                val press = PressInteraction.Press(Offset.Zero)
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
                            onClick = { onImportPicture(PDFName) }
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
            }
            if (isDeleteDialogShow) {
                AlertDialog(
                    onDismissRequest = { isDeleteDialogShow = false },
                    title = {
                        Text(
                            text = "删除此项？"
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "此操作不可撤销。",
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeleteDialogShow = false
                                viewModel.deletePicturesGroup(PDFName)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "确定"
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                isDeleteDialogShow = false
                            }
                        ) {
                            Text("取消")
                        }
                    }
                )
            }
            if (isChangeNameDialogShow) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("请输入将要输出的PDF名") },
                    text = {
                        Column {
                            PDFName.let {
                                OutlinedTextField(
                                    value = newPDFName,
                                    onValueChange = {
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
                                    singleLine = false
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = newPDFName.isNotBlank(),
                            onClick = {
                                if (newPDFName.isNotBlank()) {
                                    isChangeNameDialogShow = false
                                    val finalName = onPDFNameChange(PDFName, newPDFName)
                                    newPDFName = finalName.dropLast(4)
                                }
                            }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                isChangeNameDialogShow = false
                            }
                        ) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}


