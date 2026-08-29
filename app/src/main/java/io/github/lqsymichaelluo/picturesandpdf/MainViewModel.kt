package io.github.lqsymichaelluo.picturesandpdf

import android.app.Application
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app

    val colorHistoryList = mutableStateListOf<ColorHistory>()
    val pictureInputList =
        mutableStateMapOf<String, SnapshotStateList<Bitmap>>()

    //                    PDFName   Images
    val pdfInputList =
        mutableStateMapOf<String, PicturesOutputState>()
    //                   PDFName   State
    private val _deletePictureButtonShowMap = mutableMapOf<Int, MutableState<Boolean>>()
    private val _foldMap = mutableMapOf<String, MutableState<Boolean>>()
    private val _rotationMap = mutableMapOf<String, MutableState<Float>>()
    private val _newNameMap = mutableMapOf<String, MutableState<String>>()
    private val _operateModeMap = mutableMapOf<String, MutableState<OperateMode>>()
    private val _hueMap = mutableStateMapOf<String, MutableState<Float>>()
    private val _saturationMap = mutableStateMapOf<String, MutableState<Float>>()
    private val _valueMap = mutableStateMapOf<String, MutableState<Float>>()
    private val _alphaMap = mutableStateMapOf<String, MutableState<Float>>()
    private val _colorInputDialogMap = mutableStateMapOf<String, MutableState<Boolean>>()
    private val _changeNameDialogMap = mutableStateMapOf<String, MutableState<Boolean>>()
    private val _deletePicturesDialogMap = mutableStateMapOf<String, MutableState<Boolean>>()
    private val _deletePDFDialogMap = mutableStateMapOf<String, MutableState<Boolean>>()
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting
    private val _exportText = MutableStateFlow("正在输出...")
    var currentOutputPDFName: String? = null
    val exportText: StateFlow<String> = _exportText
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog
    val topColors: List<String>
        get() = colorHistoryList
            .sortedByDescending { it.time }
            .distinctBy { it.color }
            .take(3)
            .map { it.color }

    init {
        colorHistoryList.addAll(ColorHistoryStorage.load(context))
    }

    fun addColor(color: String) {
        val now = System.currentTimeMillis()
        val index = colorHistoryList.indexOfFirst { it.color == color }
        if (index != -1) {
            colorHistoryList[index] = colorHistoryList[index].copy(time = now)
        } else {
            colorHistoryList.add(ColorHistory(color, now))
        }
        save()
    }

    private fun save() {
        ColorHistoryStorage.save(context, colorHistoryList.toList())
    }
    fun deletePictureButtonShowState(imageId: Int): MutableState<Boolean> =
        _deletePictureButtonShowMap.getOrPut(imageId) { mutableStateOf(false) }

    fun foldState(pdfName: String): MutableState<Boolean> =
        _foldMap.getOrPut(pdfName) { mutableStateOf(false) }

    fun rotationState(pdfName: String): MutableState<Float> =
        _rotationMap.getOrPut(pdfName) { mutableFloatStateOf(0f) }

    fun newNameState(pdfName: String): MutableState<String> =
        _newNameMap.getOrPut(pdfName) { mutableStateOf(pdfName.dropLast(4)) }

    fun operateModeState(pdfName: String): MutableState<OperateMode> =
        _operateModeMap.getOrPut(pdfName) { mutableStateOf(OperateMode.NONE) }

    fun hueState(pdfName: String): MutableState<Float> =
        _hueMap.getOrPut(pdfName) { mutableFloatStateOf(0f) }

    fun saturationState(pdfName: String): MutableState<Float> =
        _saturationMap.getOrPut(pdfName) { mutableFloatStateOf(0.00f) }

    fun valueState(pdfName: String): MutableState<Float> =
        _valueMap.getOrPut(pdfName) { mutableFloatStateOf(0.00f) }

    fun alphaState(pdfName: String): MutableState<Float> =
        _alphaMap.getOrPut(pdfName) { mutableFloatStateOf(0.00f) }

    fun colorInputDialogShowState(pdfName: String): MutableState<Boolean> =
        _colorInputDialogMap.getOrPut(pdfName) { mutableStateOf(false) }

    fun changeNameDialogShowState(pdfName: String): MutableState<Boolean> =
        _changeNameDialogMap.getOrPut(pdfName) { mutableStateOf(false) }

    fun deletePicturesDialogShowState(pdfName: String): MutableState<Boolean> =
        _deletePicturesDialogMap.getOrPut(pdfName) { mutableStateOf(false) }

    fun deletePDFDialogShowState(pdfName: String): MutableState<Boolean> =
        _deletePDFDialogMap.getOrPut(pdfName) { mutableStateOf(false) }

    fun setScale(name: String, scale: Float) {
        pdfInputList[name] = pdfInputList[name]?.copy(scale = scale) as PicturesOutputState
    }

    fun setMultiPage(name: String, multi: Boolean) {
        pdfInputList[name] =
            pdfInputList[name]?.copy(toMultiplePictures = multi) as PicturesOutputState
    }

    fun setBackgroundColor(name: String, color: Color) {
        pdfInputList[name] = pdfInputList[name]?.copy(
            backgroundColor = color
        ) as PicturesOutputState
    }

    fun setAlignMode(name: String, alignMode: Int) {
        pdfInputList[name] = pdfInputList[name]?.copy(
            alignMode = alignMode
        ) as PicturesOutputState
    }

    fun setStretchMode(name: String, stretchMode: Int) {
        pdfInputList[name] = pdfInputList[name]?.copy(
            stretchMode = stretchMode
        ) as PicturesOutputState
    }

    fun importPictures(context: Context, uris: List<Uri>, outputPDFName: String?) {
        val outputName =
            outputPDFName ?: "Output_${(System.currentTimeMillis() / 1000).toInt()}.pdf"
        val list = mutableStateListOf<Bitmap>()

        uris.forEach { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }.onSuccess { bmp ->
                bmp?.let { list.add(it) }
            }
        }
        if (list.isNotEmpty()) {
            pictureInputList.merge(outputName, list) { old, new ->
                old.apply { addAll(new) }
            }
        }
    }

    fun overridePicturesGroup(name: String, list: List<Bitmap>?) {
        list?.let {
            pictureInputList[name] = list.toMutableStateList()
        }
    }

    fun deletePictureFromGroup(pdfName:String, index: Any){
        val list = pictureInputList[pdfName]
        list?.size?.let { if (it <= 1) return print("图片组至少应当有1张图")}
        when (index) {
            is Int -> {
                list?.removeAt(index)?.recycle()
            }
            is Bitmap -> {
                list?.remove(index)
                index.recycle()
            }
            else -> {
                error("Index must be an Int or a Bitmap.")
            }
        }
    }

    fun addPicturesFromClipData(
        context: Context,
        clipData: ClipData,
        outputPDFName: String?,
        releasePermissionUnit: () -> Unit
    ) {
        val outputName =
            outputPDFName ?: "Output_${(System.currentTimeMillis() / 1000).toInt()}.pdf"
        viewModelScope.launch(Dispatchers.IO) {
            val bitmaps = (0 until clipData.itemCount).mapNotNull { index ->
                val uri = clipData.getItemAt(index).uri ?: return@mapNotNull null
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            if (bitmaps.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                val list = pictureInputList[outputName]
                    ?: mutableStateListOf<Bitmap>().also {
                        pictureInputList[outputName] = it
                    }
                list.addAll(
                    bitmaps.map { bmp ->
                        bmp.copy(Bitmap.Config.ARGB_8888, false)
                    }
                )
                releasePermissionUnit()
            }
        }
    }

    fun addPDFFromClipData(
        context: Context,
        clipData: ClipData,
        releasePermissionUnit: () -> Unit
    ) {
        val contentResolver = context.contentResolver
        val pdfUris = (0 until clipData.itemCount)
            .map { clipData.getItemAt(it).uri }
            .filter { uri ->
                val type = contentResolver.getType(uri)
                (type == "application/pdf" || type == "application/octet-stream") || (getFileName(context, uri) ?: uri.lastPathSegment ?: ""
                    .endsWith(".pdf", ignoreCase = true)) == true
            }

        if (pdfUris.isEmpty()) return

        pdfUris.forEach {  uri ->
            try {
                val type = contentResolver.getType(uri)
                if (type != null &&
                    type != "application/pdf" &&
                    type != "application/octet-stream"
                ) {
                    return@forEach
                }
                val originalName = getFileName(context, uri) ?: "unknown.pdf"
                var finalName = originalName
                var index = 1
                while (pdfInputList.containsKey(finalName)) {
                    finalName = "${originalName.dropLast(4)} ($index).pdf"
                    index++
                }
                val file = uriToFile(
                    context = context,
                    uri = uri,
                    name = finalName
                )
                pdfInputList[finalName] = PicturesOutputState(
                    file = file
                )
                AppFlags.uploadedPDFList.add(file.name)
            } catch (_: SecurityException) {
            }
        }
        releasePermissionUnit()
    }
    private fun uriToFile(context: Context, uri: Uri, name: String): File {
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, name)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    fun importPDFs(context: Context, uris: List<Uri>) {
        uris.forEach {
            val originalName = getFileName(context, it) ?: "unknown.pdf"
            var finalName = originalName
            var index = 1
            while (pdfInputList.containsKey(finalName)) {
                finalName = "${originalName.dropLast(4)} ($index).pdf"
                index++
            }
            val file = uriToFile(
                context = context,
                uri = it,
                name = finalName
            )
            pdfInputList[finalName] = PicturesOutputState(
                file = file
            )
            AppFlags.uploadedPDFList.add(file.name)
        }
    }

    fun changeOutputPDFName(oldName: String, newName: String): String {
        if (oldName == newName) return oldName
        val list = pictureInputList[oldName] ?: return oldName
        var finalName = newName.dropLast(4)
        var index = 1
        pictureInputList.remove(oldName)
        while (pictureInputList.containsKey("$finalName.pdf")) {
            finalName = "${newName.dropLast(4)} ($index)"
            index++
        }
        pictureInputList["$finalName.pdf"] = list
        foldState("$finalName.pdf").value = foldState(oldName).value
        rotationState("$finalName.pdf").value = rotationState(oldName).value
        _foldMap.remove(oldName)
        _changeNameDialogMap.remove(oldName)
        _deletePicturesDialogMap.remove(oldName)
        return "$finalName.pdf"
    }


    fun deletePicturesGroup(pdfName: String) {
        pictureInputList.remove(pdfName)
    }

    fun deletePDF(pdfName: String) {
        pdfInputList.remove(pdfName)
        _operateModeMap[pdfName]?.value = OperateMode.NONE
        AppFlags.uploadedPDFList.removeIf { it == pdfName }
    }

    fun print(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun printLong(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard =
            ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    fun dismissExportDialog() {
        _showDialog.value = false
    }

    fun exportPicToPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            _showDialog.value = true
            _exportText.value = "正在合成 PDF..."

            val resolver = context.contentResolver

            pictureInputList.forEach { (pdfName, bitmaps) ->
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, pdfName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Pic2PDF/")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                uri?.let {
                    resolver.openOutputStream(it)?.use { os ->
                        Convertor().PicturesToPDFForApp(
                            pic = bitmaps,
                            pdf = os as FileOutputStream,
                            callBack = {i, pageCount ->
                            }
                        )
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
            }

            _exportText.value =
                "已输出到 /storage/emulated/0/Download/Pic2PDF"
            _isExporting.value = false
        }
    }


    fun exportPdfToPic(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            _showDialog.value = true
            _exportText.value = "正在导出图片..."

            val resolver = context.contentResolver

            pdfInputList.forEach { (pdfName, state) ->

                val rawBitmaps = Convertor().PDFtoPicturesForApp(
                    pdf = state.file,
                    scale = state.scale,
                    backgroundColor = state.backgroundColor,
                    callBack = { i, pageCount ->
                    }
                )

                if (state.toMultiplePictures) {
                    rawBitmaps.forEachIndexed { i, bmp ->
                        saveBitmap(
                            resolver,
                            bmp,
                            "${pdfName.dropLast(4)}/page_${i + 1}.png"
                        )
                    }
                    return@forEach
                }

                val maxWidth = rawBitmaps.maxOf { it.width }

                val processedBitmaps = rawBitmaps.map { bmp ->
                    when (state.stretchMode) {
                        1 -> {
                            createBitmap(maxWidth, bmp.height).also {
                                Canvas(it).drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, maxWidth, bmp.height),
                                    null
                                )
                            }
                        }

                        2 -> {
                            val scale = maxWidth.toFloat() / bmp.width
                            val targetHeight = (bmp.height * scale).toInt()
                            createBitmap(maxWidth, targetHeight).also {
                                Canvas(it).drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, maxWidth, targetHeight),
                                    null
                                )
                            }
                        }

                        else -> bmp
                    }
                }

                val totalHeight = processedBitmaps.sumOf { it.height }
                val longBmp = createBitmap(maxWidth, totalHeight)
                val canvas = Canvas(longBmp)
                canvas.drawColor(state.backgroundColor.toArgb())

                var y = 0f
                for (bmp in processedBitmaps) {
                    val left = when (state.alignMode) {
                        1 -> (maxWidth - bmp.width) / 2f
                        2 -> (maxWidth - bmp.width).toFloat()
                        else -> 0f
                    }
                    canvas.drawBitmap(bmp, left, y, null)
                    y += bmp.height
                }

                saveBitmap(
                    resolver,
                    longBmp,
                    "${pdfName.dropLast(4)}/longpage.png"
                )

                longBmp.recycle()
                processedBitmaps.forEach { it.recycle() }
            }

            _exportText.value =
                "已输出到 /storage/emulated/0/Download/PDF2Pic"
            _isExporting.value = false
        }
    }

    private fun saveBitmap(
        resolver: ContentResolver,
        bitmap: Bitmap,
        path: String
    ) {
        val dir = path.substringBeforeLast("/")
        val name = path.substringAfterLast("/")

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/PDF2Pic/$dir")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let {
            resolver.openOutputStream(it)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        }
    }
}