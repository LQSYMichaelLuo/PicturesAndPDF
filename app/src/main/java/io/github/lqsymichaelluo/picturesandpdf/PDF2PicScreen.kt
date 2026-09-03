package io.github.lqsymichaelluo.picturesandpdf

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@Composable
fun PDF2PicScreen(
    viewModel: MainViewModel,
    isPhoneLandscape: Boolean,
    rootNavController: NavController
) {
    val debuggable by AppFlags.debuggable
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (viewModel.pdfInputList.isEmpty()) {
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
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = if (isPhoneLandscape) StaggeredGridCells.Fixed(2)
            else StaggeredGridCells.Adaptive(minSize = 540.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            for ((pdfName, _) in viewModel.pdfInputList) {
                item(key = pdfName) {
                    PDFCard(
                        viewModel = viewModel,
                        pdfName = pdfName,
                        modifier = Modifier.animateItem(),
                        rootNavController = rootNavController
                    )
                }
            }
            if (debuggable) {
                item {
                    Text(
                        text = viewModel.pdfInputList.toString(),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFCard(
    viewModel: MainViewModel,
    pdfName: String,
    modifier: Modifier,
    rootNavController: NavController
) {
    val state = viewModel.pdfInputList[pdfName] ?: return
    val context = LocalContext.current
    var operateMode by viewModel.operateModeState(pdfName)
    val isOperatingAreaShow = operateMode != OperateMode.NONE

    val cardSurfaceColor = CardDefaults.cardColors().containerColor
    val dialogSurfaceColor = AlertDialogDefaults.containerColor

    var hue by viewModel.hueState(pdfName)
    var saturation by viewModel.saturationState(pdfName)
    var value by viewModel.valueState(pdfName)
    var alpha by viewModel.alphaState(pdfName)

    val editingColor = hsvaToColor(hue, saturation, value, alpha)

    var isColorInputDialogShow by viewModel.colorInputDialogShowState(pdfName)
    val interactionSource = remember { MutableInteractionSource() }
    val topColors = viewModel.topColors

    var isDeleteDialogShow by viewModel.deletePDFDialogShowState(pdfName)

    val containerColor by animateColorAsState(
        targetValue = if (operateMode == OperateMode.SCALE) MaterialTheme.colorScheme.primary else Color.Transparent
    )
    val contentColor by animateColorAsState(
        targetValue = if (operateMode == OperateMode.SCALE) MaterialTheme.colorScheme.onPrimary else CardDefaults.cardColors().contentColor
    )

    fun toggleMode(target: OperateMode) {
        operateMode = if (operateMode == target) OperateMode.NONE else target
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .combinedClickable(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                rootNavController.navigate(
                                    "pdf_preview/$pdfName"
                                )
                            }
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pdf),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text(pdfName) } },
                        state = rememberTooltipState(),
                    ) {
                        Text(
                            text = pdfName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("改变输出图片精度") } },
                        state = rememberTooltipState()
                    ) {
                        TextButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(OperateMode.SCALE)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = containerColor
                            )
                        ) {
                            Text(
                                text = "%.1f".format(state.scale),
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("改变输出图片背景颜色") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = {
                            HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                            toggleMode(OperateMode.PALETTE)
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (operateMode == OperateMode.PALETTE)
                                        R.drawable.ic_palette_filled
                                    else
                                        R.drawable.ic_palette
                                ),
                                contentDescription = null
                            )
                        }
                    }
                    if (state.toMultiplePictures) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = { PlainTooltip { Text("输出多张图") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(OperateMode.PAGE)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pictures),
                                    contentDescription = null
                                )
                            }
                        }
                    } else {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = { PlainTooltip { Text("输出一张长图") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                toggleMode(OperateMode.PAGE)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pdf2pic),
                                    contentDescription = null
                                )
                            }
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
                                isDeleteDialogShow = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isOperatingAreaShow,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AnimatedContent(
                    targetState = operateMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { target ->
                    when (target) {
                        OperateMode.SCALE -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "拖动滑动条以调节PDF转图片的渲染精度",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = state.scale,
                                onValueChange = {
                                    HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                    viewModel.setScale(pdfName, it)
                                },
                                valueRange = 1f..6f,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = "当渲染精度为1.0时，输出图片应当与PDF原画精度一致" +
                                        "\n当渲染精度为4.0时，最兼顾图片质量和文件大小",
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        OperateMode.PALETTE -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "滑动拖动条以进行调节输出图片的背景颜色",
                                modifier = Modifier.padding(
                                    top = 12.dp,
                                    start = 12.dp,
                                    end = 12.dp
                                ),
                                fontWeight = FontWeight.Bold
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HSVASlider(
                                    hue = hue,
                                    saturation = saturation,
                                    value = value,
                                    alpha = alpha,
                                    onHueChange = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                        hue = it
                                        viewModel.setBackgroundColor(
                                            pdfName,
                                            hsvaToColor(
                                                it,
                                                saturation,
                                                value,
                                                alpha
                                            )
                                        )
                                    },
                                    onSaturationChange = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                        saturation = it
                                        viewModel.setBackgroundColor(
                                            pdfName,
                                            hsvaToColor(
                                                hue,
                                                it,
                                                value,
                                                alpha
                                            )
                                        )
                                    },
                                    onValueChange = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                        value = it
                                        viewModel.setBackgroundColor(
                                            pdfName,
                                            hsvaToColor(
                                                hue,
                                                saturation,
                                                it,
                                                alpha
                                            )
                                        )
                                    },
                                    onAlphaChange = {
                                        HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                        alpha = it
                                        viewModel.setBackgroundColor(
                                            pdfName,
                                            hsvaToColor(
                                                hue,
                                                saturation,
                                                value,
                                                it
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(24.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (editingColor.alpha <= 0.1f || editingColor.isCloseTo(
                                            other = cardSurfaceColor
                                        )
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_circle),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(editingColor),
                                            modifier = Modifier
                                                .size(26.dp)
                                                .drawWithContent {
                                                    drawContent()
                                                    val strokeWidth = 2.dp.toPx()
                                                    drawCircle(
                                                        color = Color.Gray,
                                                        radius = size.minDimension / 2 - strokeWidth / 2,
                                                        style = Stroke(width = strokeWidth)
                                                    )
                                                }
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.ic_circle),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(editingColor),
                                            modifier = Modifier
                                                .size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = editingColor.toHexString(),
                                        modifier = Modifier.combinedClickable(
                                            //interactionSource = remember { MutableInteractionSource() },
                                            //indication = null,
                                            onClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.print("MEOW!!!")
                                            },
                                            onLongClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.copyToClipboard(
                                                    text = editingColor.toHexString(),
                                                    label = "?"
                                                )
                                            }
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(24.dp))

                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                            TooltipAnchorPosition.Above
                                        ),
                                        tooltip = { PlainTooltip { Text("手动输入背景颜色代码") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                            isColorInputDialogShow = true
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_pencil),
                                                contentDescription = null
                                            )
                                        }
                                    }

                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                            TooltipAnchorPosition.Above
                                        ),
                                        tooltip = { PlainTooltip { Text("保存当前颜色以便后续调用") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_CLICK
                                            )
                                            viewModel.addColor(editingColor.toHexString())
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_save),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        OperateMode.PAGE -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "对于输出图片的方式进行设置",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp
                                )
                            ) {
                                Text(
                                    text = "模式",
                                    modifier = Modifier.padding(end = 16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                val options = listOf("多图", "单张长图")
                                val toMultiple =
                                    viewModel.pdfInputList[pdfName]?.toMultiplePictures ?: false
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.weight(1f),

                                    ) {
                                    options.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            selected = when (index) {
                                                0 -> toMultiple
                                                1 -> !toMultiple
                                                else -> false
                                            },
                                            onClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.setMultiPage(pdfName, index == 0)
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = options.size
                                            ),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                                inactiveBorderColor = MaterialTheme.colorScheme.primary,
                                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                                inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp
                                )
                            ) {
                                Text(
                                    text = "对齐",
                                    modifier = Modifier.padding(end = 16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                val options = listOf("左对齐", "居中对齐", "右对齐")

                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.weight(1f),

                                    ) {
                                    options.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            enabled = viewModel.pdfInputList[pdfName]?.stretchMode == 0 &&
                                                    viewModel.pdfInputList[pdfName]?.toMultiplePictures == false,
                                            selected = index == viewModel.pdfInputList[pdfName]?.alignMode,
                                            onClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.setAlignMode(pdfName, index)
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = options.size
                                            ),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                                inactiveBorderColor = MaterialTheme.colorScheme.primary,
                                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                                inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp
                                )
                            ) {
                                Text(
                                    text = "缩放",
                                    modifier = Modifier.padding(end = 16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                val options = listOf("原尺寸", "横向等宽", "比例等宽")

                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    options.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            enabled = viewModel.pdfInputList[pdfName]?.toMultiplePictures == false,
                                            selected = index == viewModel.pdfInputList[pdfName]?.stretchMode,
                                            onClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.setStretchMode(pdfName, index)
                                                if (index != 0) viewModel.setAlignMode(pdfName, 0)
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = options.size
                                            ),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                                inactiveBorderColor = MaterialTheme.colorScheme.primary,
                                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                                inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp
                                )
                            ) {
                                Text(
                                    text = "格式",
                                    modifier = Modifier.padding(end = 16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    listOf("PNG", "JPEG", "WEBP_LOSSY", "WEBP_LOSSLESS")
                                } else {
                                    listOf("PNG", "JPEG", "WEBP")
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    options.forEachIndexed { index, label ->
                                        val selected =
                                            index == viewModel.pdfInputList[pdfName]?.format
                                        FilterChip(
                                            selected = selected,
                                            modifier = Modifier,
                                            onClick = {
                                                HapticManager.vibrate(
                                                    context,
                                                    HapticManager.EFFECT_CLICK
                                                )
                                                viewModel.setFormatMode(pdfName, index)
                                                if (index != 0) viewModel.setAlignMode(pdfName, 0)
                                            },
                                            label = { Text(label) },
                                            shape = if (selected) {
                                                CircleShape
                                            } else {
                                                RoundedCornerShape(8.dp)
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_check),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            } else null,
                                        )
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                            AnimatedVisibility(
                                visible = state.format != 0
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = 12.dp
                                    )
                                ) {
                                    Text(
                                        text = "质量",
                                        modifier = Modifier.padding(end = 16.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Slider(
                                        value = state.quality.toFloat(),
                                        onValueChange = {
                                            HapticManager.vibrate(
                                                context,
                                                HapticManager.EFFECT_TICK
                                            )
                                            viewModel.setQuality(pdfName, it.roundToInt())
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("  ${state.quality}")
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )
                        }

                        else -> {}
                    }
                }
            }

            if (isColorInputDialogShow) {
                var inputText by remember(editingColor) {
                    mutableStateOf(editingColor.toHexString())
                }

                LaunchedEffect(true) {
                    inputText = editingColor.toHexString()
                }

                val parsedColor = inputText.toArgbColor()
                val hasError = !inputText.startsWith("#") || parsedColor == null

                AlertDialog(
                    onDismissRequest = { isColorInputDialogShow = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "自定义背景色",
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                topColors.forEach {
                                    it.toArgbColor()?.let { color ->
                                        if (color.alpha <= 0.1f || color.isCloseTo(other = dialogSurfaceColor)) {
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                    TooltipAnchorPosition.Above
                                                ),
                                                tooltip = { PlainTooltip { Text(color.toHexString()) } },
                                                state = rememberTooltipState(),
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_circle),
                                                    contentDescription = null,
                                                    colorFilter = ColorFilter.tint(color),
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .drawWithContent {
                                                            drawContent()
                                                            val strokeWidth = 2.dp.toPx()
                                                            drawCircle(
                                                                color = Color.Gray,
                                                                radius = size.minDimension / 2 - strokeWidth / 2,
                                                                style = Stroke(width = strokeWidth)
                                                            )
                                                        }
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null
                                                        ) {
                                                            HapticManager.vibrate(
                                                                context,
                                                                HapticManager.EFFECT_TICK
                                                            )
                                                            inputText = color.toHexString()
                                                        }
                                                )
                                            }
                                        } else {
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                    TooltipAnchorPosition.Above
                                                ),
                                                tooltip = { PlainTooltip { Text(color.toHexString()) } },
                                                state = rememberTooltipState(),
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_circle),
                                                    contentDescription = null,
                                                    colorFilter = ColorFilter.tint(color),
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null
                                                        ) {
                                                            HapticManager.vibrate(
                                                                context,
                                                                HapticManager.EFFECT_TICK
                                                            )
                                                            inputText = color.toHexString()
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    text = {
                        OutlinedTextField(
                            value = inputText,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                                inputText = it.take(10)
                            },
                            label = { Text("十六进制颜色") },
                            placeholder = { Text("#AARRGGBB") },
                            singleLine = true,
                            isError = hasError,
                            supportingText = {
                                if (hasError) {
                                    Text("支持 #AARRGGBB | #RRGGBB")
                                }
                            },
                            trailingIcon = {
                                parsedColor?.let {
                                    if (it.alpha <= 0.1f || it.isCloseTo(other = dialogSurfaceColor)) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_circle),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(parsedColor),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .drawWithContent {
                                                    drawContent()
                                                    val strokeWidth = 2.dp.toPx()
                                                    drawCircle(
                                                        color = Color.Gray,
                                                        radius = size.minDimension / 2 - strokeWidth / 2,
                                                        style = Stroke(width = strokeWidth)
                                                    )
                                                }
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    HapticManager.vibrate(
                                                        context,
                                                        HapticManager.EFFECT_TICK
                                                    )
                                                    inputText = parsedColor.toHexString()
                                                }
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.ic_circle),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(parsedColor),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    HapticManager.vibrate(
                                                        context,
                                                        HapticManager.EFFECT_TICK
                                                    )
                                                    inputText = parsedColor.toHexString()
                                                }
                                        )
                                    }
                                }
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            enabled = parsedColor != null,
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                parsedColor?.let {
                                    viewModel.setBackgroundColor(pdfName, it)
                                    val hsv = it.toHsva()
                                    hue = hsv.h
                                    saturation = hsv.s
                                    value = hsv.v
                                    alpha = hsv.a
                                }
                                isColorInputDialogShow = false
                            }
                        ) { Text(stringResource(R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                            isColorInputDialogShow = false
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
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
                                HapticManager.vibrate(context, HapticManager.EFFECT_HEAVY_CLICK)
                                isDeleteDialogShow = false
                                viewModel.deletePDF(pdfName)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.ok)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                isDeleteDialogShow = false
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HSVASlider(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueGradientBar(
                hue = hue,
                saturation = saturation,
                value = value,
                alpha = alpha,
                valueRange = 0f..360f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H  ")
            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f)
            )
            Text("  ${hue.toInt()}")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("S  ")
            Slider(
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("  %.2f".format(saturation))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("V  ")
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("  %.2f".format(value))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A  ")
            Slider(
                value = alpha,
                onValueChange = onAlphaChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("  %.2f".format(alpha))
        }
    }
}

@Composable
private fun HueGradientBar(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        Canvas(modifier) {
            val radius = 16.dp.toPx()
            val colors = List(361) {
                hsvaToColor(it.toFloat(), saturation, value, alpha)
            }
            drawRoundRect(
                brush = Brush.horizontalGradient(colors),
                cornerRadius = CornerRadius(radius)
            )
            drawRoundRect(
                color = primaryColor,
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = 2.dp.toPx())
            )
            var x = 0f
            val spacing = 2.dp.toPx()
            val stripeWidth = 0.6.dp.toPx()
            while (x <= size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = stripeWidth
                )
                x += spacing
            }
            val progress = (hue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val lineX = progress * size.width
            val margin = 4.dp.toPx()
            val lineWidth = 3.dp.toPx()
            val rect = Rect(
                left = lineX - lineWidth / 2,
                top = margin,
                right = lineX + lineWidth / 2,
                bottom = size.height - margin
            )
            drawRoundRect(
                color = Color.Gray,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(lineWidth / 2)
            )
        }
    }
}

fun hsvaToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
    val hsv = floatArrayOf(h, s, v)
    val argb = AndroidColor.HSVToColor((alpha * 255).toInt(), hsv)
    return Color(argb)
}

fun String.toArgbColor(): Color? = runCatching {
    val s = trim().uppercase()
    when {
        s.matches(Regex("^#([0-9A-F]{6})$")) -> {
            Color("FF${s.drop(1)}".toLong(16))
        }

        s.matches(Regex("^#([0-9A-F]{8})$")) -> {
            Color(s.drop(1).toLong(16))
        }

        else -> null
    }
}.getOrNull()

fun Color.toHsva(): Hsv {
    val hsv = FloatArray(3)
    val argb = AndroidColor.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
    AndroidColor.colorToHSV(argb, hsv)
    return Hsv(hsv[0], hsv[1], hsv[2], alpha)
}

data class Hsv(val h: Float, val s: Float, val v: Float, val a: Float)

fun Color.isCloseTo(
    other: Color,
    tolerance: Float = 0.09f
): Boolean {
    val dr = red - other.red
    val dg = green - other.green
    val db = blue - other.blue
    val da = alpha - other.alpha

    val distance = dr * dr + dg * dg + db * db + da * da
    return distance < tolerance * tolerance
}

fun Color.toHexString(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    val a = (alpha * 255).toInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}