package io.github.lqsymichaelluo.picturesandpdf

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int
) {
    object Pic2PDF: Screen(
        route = "pic2pdf_screen",
        title = R.string.pic2pdf,
        icon = R.drawable.ic_pic2pdf
    )
    object PDF2Pic: Screen(
        route = "pdf2pic_screen",
        title = R.string.pdf2pic,
        icon = R.drawable.ic_pdf2pic
    )
}