package io.github.lqsymichaelluo.picturesandpdf

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@OptIn(ExperimentalMaterial3Api::class)
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
            Text(
                "图片排序功能页占位符",
                modifier = Modifier.padding(padding)
            )
        }
    }
}