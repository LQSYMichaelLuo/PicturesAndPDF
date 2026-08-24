package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.runtime.mutableStateOf

object AppFlags {
    val debuggable = mutableStateOf(false)
    val uploadedPDFList = mutableListOf<String>()
}