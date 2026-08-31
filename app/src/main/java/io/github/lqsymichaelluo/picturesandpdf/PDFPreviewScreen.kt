package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFPreviewScreen(
    pdfName: String,
    pdfPreviewViewModel: PdfPreviewViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(pdfName) { File(context.cacheDir, pdfName) }
    val fileExists = remember(file) { file.exists() && file.length() > 0L }

    LaunchedEffect(fileExists) {
        if (!fileExists) onBack()
    }
    if (!fileExists) return

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var colorState by pdfPreviewViewModel.pdfPreviewBackgroundColorState(pdfName)
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
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pdfName,
                        modifier = Modifier.basicMarquee()
                    )
                },
                scrollBehavior = scrollBehavior,
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
                                contentDescription = null,
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
                }
            )
        }
    ){
        paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
        ){
            PdfViewer(
                file = file,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}