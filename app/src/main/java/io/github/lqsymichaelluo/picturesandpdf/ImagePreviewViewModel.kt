package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ImagePreviewViewModel : ViewModel() {

    private val _bitmapList = mutableStateOf<List<Bitmap>?>(null)
    val bitmapList: List<Bitmap>? get() = _bitmapList.value

    private val _current = mutableIntStateOf(0)
    val currentIndex: Int get() = _current.intValue

    private val _currentPDFName = mutableStateOf("unknown.pdf")
    val currentPDFName: String get() = _currentPDFName.value
    private val _imagePreviewBackgroundColorStateMap =
        mutableMapOf<String, MutableState<ImagePreviewBackgroundColorState>>()

    var hasTriggerSort = mutableStateOf(false)
    var hasTriggerPreview = mutableStateOf(false)
    fun setBitmapList(bmp: List<Bitmap>) {
        _bitmapList.value = bmp
    }
    fun setCurrent(i: Int) {
        _current.intValue = i
    }
    fun setCurrentPDFName(name: String) {
        _currentPDFName.value = name
    }
    fun moveBitmap(from: Int, to: Int) {
        val list = _bitmapList.value ?: return
        _bitmapList.value = list.toMutableList().apply {
            add(to, removeAt(from))
        }
    }
    fun setTriggerSort(
        triggered: Boolean = false
    ){
        hasTriggerSort.value = triggered
    }
    fun setTriggerPreview(
        triggered: Boolean = false
    ){
        hasTriggerPreview.value = triggered
    }
    fun clear() {
        _bitmapList.value = null
    }
    fun imagePreviewBackgroundColorState(imageID: String): MutableState<ImagePreviewBackgroundColorState> =
        _imagePreviewBackgroundColorStateMap.getOrPut(imageID) {
            mutableStateOf(
                ImagePreviewBackgroundColorState.Black
            )
        }
}
