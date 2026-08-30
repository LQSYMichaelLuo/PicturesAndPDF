package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class PdfPreviewViewModel: ViewModel() {
    private val _pdfPreviewBackgroundColorStateMap =
        mutableMapOf<String, MutableState<ImagePreviewBackgroundColorState>>()

    fun pdfPreviewBackgroundColorState(pdfName: String): MutableState<ImagePreviewBackgroundColorState> =
        _pdfPreviewBackgroundColorStateMap.getOrPut(pdfName) {
            mutableStateOf(
                ImagePreviewBackgroundColorState.White
            )
        }
}