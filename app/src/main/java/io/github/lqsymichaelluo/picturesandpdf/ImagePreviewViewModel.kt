package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class ImagePreviewViewModel : ViewModel() {
    private val _imagePreviewBackgroundColorStateMap =
        mutableMapOf<String, MutableState<ImagePreviewBackgroundColorState>>()
    val imagePreviewList =
        mutableMapOf<String, ImagePreviewData>()

    fun addImagePreviewList(
        pdfName: String,
        imagePreviewData: ImagePreviewData
    ){
        imagePreviewList[pdfName] = imagePreviewData
    }
    fun moveBitmap(pdfName: String, from: Int, to: Int) {
        val list: SnapshotStateList<Bitmap> =
            imagePreviewList[pdfName]?.bitmapList ?: return

        if (from == to) return
        if (from !in list.indices || to !in list.indices) return

        val item = list.removeAt(from)
        list.add(to, item)
    }
    fun setTriggerSort(
        pdfName: String = "unknown.pdf",
        triggered: Boolean = false
    ){
        imagePreviewList[pdfName]?.let {
            imagePreviewList[pdfName] = it.copy(
                hasTriggeredSort = triggered
            )
        }
    }
    fun setTriggerPreview(
        pdfName: String = "unknown.pdf",
        triggered: Boolean = false
    ){
        imagePreviewList[pdfName]?.let {
            imagePreviewList[pdfName] = it.copy(
                hasTriggeredPreview = triggered
            )
        }
    }
    fun imagePreviewBackgroundColorState(imageID: String): MutableState<ImagePreviewBackgroundColorState> =
        _imagePreviewBackgroundColorStateMap.getOrPut(imageID) {
            mutableStateOf(
                ImagePreviewBackgroundColorState.Black
            )
        }
}
