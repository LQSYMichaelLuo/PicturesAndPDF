package io.github.lqsymichaelluo.picturesandpdf

import kotlinx.serialization.Serializable

@Serializable
data class ColorHistory(
    val color: String,
    val time: Long
)