package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PicturesOutputState(
    val scale: Float = 4.0f,
    val backgroundColor: Color = Color(0x00000000),
    val toMultiplePictures: Boolean = true,
    val alignMode: Int = 0,
    val stretchMode: Int = 0,
    val file: File
)
