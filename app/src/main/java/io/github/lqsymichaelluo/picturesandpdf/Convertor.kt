package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class Convertor {

    /*/ @param pic: the List of Bitmap to be converted into pdf
        @param pdf: the "java.io.FileOutputStream" object of the PDF file to be created
        @param backgroundColor: an int must be in the range 0x00000000 to 0xFFFFFFFF
     */
    @Throws(IOException::class)
    fun PicturesToPDF(
        pic: List<Bitmap>,
        pdf: FileOutputStream,
        backgroundColor: Int = 0x00000000
    ): Boolean {
        val document = PdfDocument()
        for (i in pic.indices) {
            val bitmap = pic[i]
            if (bitmap.isRecycled()) continue
            val pageInfo = PdfDocument.PageInfo.Builder(
                bitmap.width,
                bitmap.height,
                i
            ).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            if (backgroundColor != 0x00000000) {
                canvas.drawColor(backgroundColor)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }
        document.writeTo(pdf)
        document.close()
        return true
    }

    /*/ @param pdf: the "java.io.File" object of the PDF file to be read
        @param scale: a float must be in range 1 to 15
                       1 -> original screen resolution
                       4 -> high definition, balancing image quality and file size
                      10 -> nearly lossless
                 Actually, we could even set the scale into bigger float number, but 10 is enough.
        @param backgroundColor: an int must be in range 0x00000000 to 0xFFFFFFFF
     */
    @Throws(IOException::class)
    fun PDFtoPictures(
        pdf: File,
        scale: Float = 4f,
        backgroundColor: Int = 0x00000000
    ): List<Bitmap> {
        var s = scale
        if (s < 1f || s > 15f) s = 4f
        val pic: MutableList<Bitmap> = ArrayList()
        val parcelFileDescriptor = ParcelFileDescriptor.open(
            pdf,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
            ?: return emptyList()
        val renderer = PdfRenderer(parcelFileDescriptor)
        val matrix = Matrix()
        matrix.postScale(s, s)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val origW = page.width
            val origH = page.height
            val targW = origW * s
            val targH = origH * s
            val bitmap = Bitmap.createBitmap(
                targW.roundToInt(),
                targH.roundToInt(),
                Bitmap.Config.ARGB_8888
            )
            if (bitmap.isRecycled()) {
                page.close()
                continue
            }
            val canvas = Canvas(bitmap)
            if (backgroundColor != 0x00000000) {
                canvas.drawColor(backgroundColor)
            }
            page.render(
                bitmap,
                null,
                matrix,
                PdfRenderer.Page.RENDER_MODE_FOR_PRINT
            )
            pic.add(bitmap)
            page.close()
        }
        renderer.close()
        parcelFileDescriptor.close()
        return pic
    }

    /*/ @param pic: the List of Bitmap to be converted into pdf
        @param pdf: the "java.io.FileOutputStream" object of the PDF file to be created
        @param usePreProcessing: a boolean to decide pre-processing
        @param compressQuality: an int to decide the quality of pre-processing
        @param stretchMode: an int who decides the stretch mode of pictures
        @param backgroundColor: a color whose value must be in the range 0x00000000 to 0xFFFFFFFF
        @param callBack: a function to receive an Int which means progress and an Int which means page count
     */
    @Throws(IOException::class)
    fun PicturesToPDFForApp(
        pic: List<Bitmap>,
        pdf: FileOutputStream?,
        usePreProcessing: Boolean = false,
        compressQuality: Int = 82,
        stretchMode: Int = 0,
        backgroundColor: Color = Color(0x00000000),
        callBack: (Int, Int) -> Unit
    ): Boolean {
        if (!usePreProcessing) {
            val document = PdfDocument()
            for (i in pic.indices) {
                val bitmap = pic[i]
                if (bitmap.isRecycled) continue
                val maxWidth = pic.maxOf { it.width }
                val progressBitmap =  when (stretchMode) {
                    1 -> {
                        createBitmap(maxWidth, bitmap.height).also {
                            Canvas(it).drawBitmap(
                                bitmap,
                                null,
                                Rect(0, 0, maxWidth, bitmap.height),
                                null
                            )
                        }
                    }
                    2 -> {
                        val scale = maxWidth.toFloat() / bitmap.width
                        val targetHeight = (bitmap.height * scale).toInt()
                        createBitmap(maxWidth, targetHeight).also {
                            Canvas(it).drawBitmap(
                                bitmap,
                                null,
                                Rect(0, 0, maxWidth, targetHeight),
                                null
                            )
                        }
                    }
                    else -> bitmap
                }
                val pageInfo = PdfDocument.PageInfo
                    .Builder(progressBitmap.width, progressBitmap.height, i + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(backgroundColor.toArgb())
                canvas.drawBitmap(progressBitmap, 0f, 0f, null)
                document.finishPage(page)
                callBack(i + 1, pic.size)
            }
            pdf?.let { document.writeTo(it) }
            document.close()
            return true
        }
        val document = PdfDocument()

        for (i in pic.indices) {
            val originalBitmap = pic[i]
            if (originalBitmap.isRecycled) continue

            val maxWidth = pic.maxOf { it.width }
            val progressBitmap =  when (stretchMode) {
                1 -> {
                    createBitmap(maxWidth, originalBitmap.height).also {
                        Canvas(it).drawBitmap(
                            originalBitmap,
                            null,
                            Rect(0, 0, maxWidth, originalBitmap.height),
                            null
                        )
                    }
                }
                2 -> {
                    val scale = maxWidth.toFloat() / originalBitmap.width
                    val targetHeight = (originalBitmap.height * scale).toInt()
                    createBitmap(maxWidth, targetHeight).also {
                        Canvas(it).drawBitmap(
                            originalBitmap,
                            null,
                            Rect(0, 0, maxWidth, targetHeight),
                            null
                        )
                    }
                }
                else -> originalBitmap
            }
            val canvas = Canvas(progressBitmap)
            canvas.drawColor(backgroundColor.toArgb())
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            val compressedData = ByteArrayOutputStream().use { os ->
                progressBitmap.compress(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        Bitmap.CompressFormat.WEBP_LOSSY
                    else
                        Bitmap.CompressFormat.WEBP,
                    compressQuality, os
                )
                os.toByteArray()
            }

            val processedBitmap = BitmapFactory.decodeByteArray(
                compressedData, 0, compressedData.size
            ) ?: continue

            val pageInfo = PdfDocument.PageInfo
                .Builder(processedBitmap.width, processedBitmap.height, i + 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(processedBitmap, 0f, 0f, null)
            document.finishPage(page)
            processedBitmap.recycle()
            callBack(i + 1, pic.size)
        }
        pdf?.let { document.writeTo(it) }
        document.close()
        return true
    }

    /*/ @param pdf: the "java.io.File" object of the PDF file to be read
        @param scale: a float must be in range 1 to 15
                       1 -> original screen resolution
                       4 -> high definition, balancing image quality and file size
                      10 -> nearly lossless
                 Actually, we could even set the scale into bigger float number, but 10 is enough.
        @param backgroundColor: a color whose value must be in range 0x00000000 to 0xFFFFFFFF
        @param callBack: a function to receive an Int which means progress and an Int which means page count
     */
    @Throws(IOException::class)
    fun PDFtoPicturesForApp(
        pdf: File,
        scale: Float = 4f,
        backgroundColor: Color = Color(0x00000000),
        callBack: (Int, Int) -> Unit
    ): List<Bitmap> {
        var s = scale
        if (s < 1f || s > 15f) s = 4f
        val pic: MutableList<Bitmap> = ArrayList()
        val parcelFileDescriptor = ParcelFileDescriptor.open(
            pdf,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
            ?: return emptyList()
        val renderer = PdfRenderer(parcelFileDescriptor)
        val matrix = Matrix()
        matrix.postScale(s, s)
        val pageCount = renderer.pageCount
        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val origW = page.width
            val origH = page.height
            val targW = origW * s
            val targH = origH * s
            val bitmap = Bitmap.createBitmap(
                targW.roundToInt(),
                targH.roundToInt(),
                Bitmap.Config.ARGB_8888
            )
            if (bitmap.isRecycled()) {
                page.close()
                continue
            }
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor.toArgb())
            page.render(
                bitmap,
                null,
                matrix,
                PdfRenderer.Page.RENDER_MODE_FOR_PRINT
            )
            pic.add(bitmap)
            page.close()
            callBack(i + 1, pageCount + 1)
        }
        renderer.close()
        parcelFileDescriptor.close()
        return pic
    }
}