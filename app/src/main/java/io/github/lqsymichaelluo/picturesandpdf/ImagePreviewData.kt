package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.compose.runtime.snapshots.SnapshotStateList

data class ImagePreviewData(
    val bitmapList: SnapshotStateList<Bitmap>,
    val currentIndex: Int = 0,
    val hasTriggeredSort: Boolean = false,
    val hasTriggeredPreview: Boolean = false
)
