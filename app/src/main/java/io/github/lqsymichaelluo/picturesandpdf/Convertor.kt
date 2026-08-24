package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    fun PicturesToPDF(pic: List<Bitmap>, pdf: FileOutputStream, backgroundColor: Int = 0x00000000) : Boolean  {
        val document = PdfDocument()
        for (i in pic.indices){
            val bitmap = pic[i]
            if (bitmap.isRecycled()) continue
            val pageInfo = PdfDocument.PageInfo.Builder(
               bitmap.width,
               bitmap.height,
               i
            ).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            if (backgroundColor!=0x00000000) {
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
    @Throws (IOException::class)
    fun PDFtoPictures(pdf: File, scale: Float = 4f, backgroundColor: Int = 0x00000000) : List<Bitmap> {
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
        for (i in 0 until renderer.pageCount){
            val page = renderer.openPage(i)
            val origW = page.width
            val origH = page.height
            val targW = origW * s
            val targH = origH * s
            val bitmap = createBitmap(
                targW.roundToInt(),
                targH.roundToInt(),
                Bitmap.Config.ARGB_8888
            )
            if (bitmap.isRecycled()) {
                page.close()
                continue
            }
            val canvas = Canvas(bitmap)
            if (backgroundColor!=0x00000000) {
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
        @param backgroundColor: an int must be in the range 0x00000000 to 0xFFFFFFFF
        @param callBack: a function to receive an Int which means progress and an Int which means page count
     */
    @Throws(IOException::class)
    fun PicturesToPDFForApp(pic: List<Bitmap>, pdf: FileOutputStream?, backgroundColor: Int = 0x00000000, callBack: (Int, Int) -> Unit) : Boolean  {
        val document = PdfDocument()
        for (i in pic.indices){
            val bitmap = pic[i]
            if (bitmap.isRecycled()) continue
            val pageInfo = PdfDocument.PageInfo.Builder(
                bitmap.width,
                bitmap.height,
                i
            ).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            if (backgroundColor!=0x00000000) {
                canvas.drawColor(backgroundColor)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
            callBack(i + 1, pic.size)
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
        @param callBack: a function to receive an Int which means progress and an Int which means page count
     */
    @Throws (IOException::class)
    fun PDFtoPicturesForApp(pdf: File, scale: Float = 4f, backgroundColor: Color = Color(0x00000000), callBack: (Int, Int) -> Unit) : List<Bitmap> {
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
        for (i in 0 until pageCount){
            val page = renderer.openPage(i)
            val origW = page.width
            val origH = page.height
            val targW = origW * s
            val targH = origH * s
            val bitmap = createBitmap(
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