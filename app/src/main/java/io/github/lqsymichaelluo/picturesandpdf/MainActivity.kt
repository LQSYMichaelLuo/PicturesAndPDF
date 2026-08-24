package io.github.lqsymichaelluo.picturesandpdf

import android.os.Bundle
import android.view.DragAndDropPermissions
import android.view.DragEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import io.github.lqsymichaelluo.picturesandpdf.ui.theme.PicturesPDFTheme
import io.github.lqsymichaelluo.shared.PlatformType

class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController
    private val viewModel: MainViewModel by viewModels()
    private val imagePreviewViewModel: ImagePreviewViewModel by viewModels()
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            viewModel.importPictures(this, it, viewModel.currentOutputPDFName)
            viewModel.currentOutputPDFName = null
        }

    private val pickPDFLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            viewModel.importPDFs(this, it)
        }

    private var currentDragAndDropPermissions: DragAndDropPermissions? = null

    fun importPicture(outputPDFName: String?) {
        viewModel.currentOutputPDFName = outputPDFName
        pickImageLauncher.launch("image/*")
    }

    fun importPDF() = pickPDFLauncher.launch(arrayOf("application/pdf"))

    fun requestDragAndDropPermission(event: DragEvent?){
        currentDragAndDropPermissions = super.requestDragAndDropPermissions(event as DragEvent?)
    }
    fun releaseDragAndDropPermission(){
        currentDragAndDropPermissions?.release()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Thread {
            Glide.get(this).clearDiskCache()
        }.start()
        setContent {
            PicturesPDFTheme {
                //val navController = rememberNavController()
                RootNavGraph(
                    viewModel = viewModel,
                    imagePreviewViewModel = imagePreviewViewModel,
                    onImportPicture = { importPicture(it) },
                    onImportPDF = { importPDF() },
                    requestDragAndDropPermission = { requestDragAndDropPermission(it) },
                    releaseDragAndDropPermission = { releaseDragAndDropPermission() }
                )
            }
        }
        PlatformType(0)
    }
}
