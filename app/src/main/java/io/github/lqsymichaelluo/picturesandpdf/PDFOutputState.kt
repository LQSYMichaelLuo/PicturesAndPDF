package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable

@Serializable
data class PDFOutputState(
    val bitmaps: SnapshotStateList<Bitmap>,
    val usePreProcessing: Boolean = false
)
