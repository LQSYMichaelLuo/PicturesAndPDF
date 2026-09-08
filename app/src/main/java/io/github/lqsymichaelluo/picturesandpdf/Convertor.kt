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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

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
    fun PicturesToPDFForApp(pic: List<Bitmap>, pdf: FileOutputStream?, usePreProcessing: Boolean = false, backgroundColor: Int = 0x00000000, callBack: (Int, Int) -> Unit) : Boolean  {
        if (!usePreProcessing) {
            val document = android.graphics.pdf.PdfDocument()
            for (i in pic.indices) {
                val bitmap = pic[i]
                if (bitmap.isRecycled) continue
                val pageInfo = PdfDocument.PageInfo
                    .Builder(bitmap.width, bitmap.height, i + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                if (backgroundColor != 0x00000000) canvas.drawColor(backgroundColor)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                callBack(i + 1, pic.size)
            }
            pdf?.let { document.writeTo(it) }
            document.close()
            return true
        }

        val flattenOnBg = AndroidColor.alpha(backgroundColor) == 0xFF
        val cache = HashMap<String, PDImageXObject>()

        PDDocument().use { doc ->
            for (i in pic.indices) {
                val src = pic[i]
                if (src.isRecycled) continue

                val base = if (src.config == Bitmap.Config.ARGB_8888) src
                else src.copy(Bitmap.Config.ARGB_8888, false)

                var work = createBitmap(base)


                val pageW = work.width.toFloat()
                val pageH = work.height.toFloat()
                var dx = 0f
                var dy = 0f
                var iw = pageW
                var ih = pageH

                if (flattenOnBg) {
                    val flat = createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
                    val c = Canvas(flat)
                    c.drawColor(backgroundColor)
                    c.drawBitmap(work, 0f, 0f, null)
                    flat.setHasAlpha(false)
                    work.recycle()
                    work = flat
                } else {
                    when (val a = scanAlpha(work)) {
                        null -> work.setHasAlpha(false)
                        else -> when {
                            a.fullyTransparent || a.fullyOpaque -> work.setHasAlpha(false)
                            (a.right - a.left < work.width ||
                                    a.bottom - a.top < work.height) -> {
                                val cw = (a.right - a.left).coerceAtLeast(1)
                                val ch = (a.bottom - a.top).coerceAtLeast(1)
                                val cropped = Bitmap.createBitmap(work, a.left, a.top, cw, ch)
                                cropped.setHasAlpha(true)
                                dx = a.left.toFloat()
                                dy = pageH - a.bottom
                                iw = cw.toFloat()
                                ih = ch.toFloat()
                                work.recycle()
                                work = cropped
                            }
                            else -> work.setHasAlpha(true)
                        }
                    }
                }

                val pdImage: PDImageXObject =
                    LosslessFactory.createFromImage(doc, work)

                work.recycle()

                val page = PDPage(PDRectangle(pageW, pageH))
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(pdImage, dx, dy, iw, ih)
                }
                callBack(i + 1, pic.size)
            }
            pdf?.let { doc.save(it) }
        }
        return true
    }
    private data class AlphaInfo(
        val fullyOpaque: Boolean,
        val fullyTransparent: Boolean,
        val left: Int, val top: Int, val right: Int, val bottom: Int
    )
    private fun recycleIfTmp(b: Bitmap, vararg keep: Bitmap) {
        if (keep.any { it === b }) return
        if (!b.isRecycled) b.recycle()
    }
    private fun scanAlpha(bmp: Bitmap): AlphaInfo? {
        if (bmp.config != Bitmap.Config.ARGB_8888) return null
        val w = bmp.width
        val h = bmp.height
        val row = IntArray(w)
        var minA = 255
        var maxA = 0
        var left = w
        var top = h
        var right = -1
        var bottom = -1
        for (y in 0 until h) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val a = row[x] ushr 24
                if (a < minA) minA = a
                if (a > maxA) maxA = a
                if (a != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    bottom = y
                }
            }
        }
        if (maxA == 0) return AlphaInfo(false, true, 0, 0, w, h)
        return AlphaInfo(minA == 255, false, left, top, right + 1, bottom + 1)
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