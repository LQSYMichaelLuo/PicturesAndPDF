package io.github.lqsymichaelluo.picturesandpdf

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagePreviewViewModel : ViewModel() {
    private val _imagePreviewBackgroundColorStateMap =
        mutableMapOf<String, MutableState<ImagePreviewBackgroundColorState>>()
    val imagePreviewList =
        mutableMapOf<String, ImagePreviewData>()
    private val _deletePictureButtonShowMap = mutableMapOf<Int, MutableState<Boolean>>()
    var currentOutputPDFName: String? = null
    fun addImagePreviewList(
        pdfName: String,
        imagePreviewData: ImagePreviewData
    ){
        imagePreviewList[pdfName] = imagePreviewData
    }
    fun deletePictureButtonShowState(imageId: Int): MutableState<Boolean> =
        _deletePictureButtonShowMap.getOrPut(imageId) { mutableStateOf(false) }
    fun moveBitmap(pdfName: String, from: Int, to: Int) {
        val list: SnapshotStateList<Bitmap> =
            imagePreviewList[pdfName]?.bitmapList ?: return

        if (from == to) return
        if (from !in list.indices || to !in list.indices) return

        val item = list.removeAt(from)
        list.add(to, item)
    }
    fun deletePictureFromGroup(pdfName:String, index: Any){
        val list = imagePreviewList[pdfName]!!.bitmapList
        when (index) {
            is Int -> {
                list.removeAt(index).recycle()
            }
            is Bitmap -> {
                list.remove(index)
                index.recycle()
            }
            else -> {
                error("Index must be an Int or a Bitmap.")
            }
        }
    }
    fun importPictures(context: Context, uris: List<Uri>, outputPDFName: String?) {
        val list = mutableStateListOf<Bitmap>()

        uris.forEach { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }.onSuccess { bmp ->
                bmp?.let { list.add(it) }
            }
        }
        if (list.isNotEmpty()) {
            imagePreviewList[outputPDFName]!!.bitmapList.addAll(list)
        }
    }
    fun addPicturesFromClipData(
        context: Context,
        clipData: ClipData,
        outputPDFName: String?,
        releasePermissionUnit: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmaps = (0 until clipData.itemCount).mapNotNull { index ->
                val uri = clipData.getItemAt(index).uri ?: return@mapNotNull null
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            if (bitmaps.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                val list = imagePreviewList[outputPDFName]!!.bitmapList
                list.addAll(
                    bitmaps.map { bmp ->
                        bmp.copy(Bitmap.Config.ARGB_8888, false)
                    }.toMutableStateList()
                )
                releasePermissionUnit()
            }
        }
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
