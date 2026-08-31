package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

@Composable
fun rememberLazyThumbnail(
    source: Bitmap,
    targetPx: Int,
    gridState: LazyGridState
): Bitmap? = rememberLazyThumbnail(source, targetPx) { gridState.isScrollInProgress }

@Composable
fun rememberLazyThumbnail(
    source: Bitmap,
    targetPx: Int,
    gridState: LazyStaggeredGridState
): Bitmap? = rememberLazyThumbnail(source, targetPx) { gridState.isScrollInProgress }

@Composable
fun rememberLazyThumbnail(
    source: Bitmap,
    targetPx: Int,
    isScrolling: () -> Boolean
): Bitmap? {
    val bucket = ThumbnailCache.sizeBucket(targetPx)
    var bitmap by remember(source, bucket) {
        mutableStateOf(ThumbnailCache[ThumbnailCache.keyOf(source, targetPx)])
    }

    LaunchedEffect(source, bucket) {
        if (bitmap != null) return@LaunchedEffect
        snapshotFlow { isScrolling() }.first { !it }
        bitmap = ThumbnailLoader.load(source, targetPx)
    }

    return bitmap
}