package io.github.lqsymichaelluo.picturesandpdf

import android.view.DragEvent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.lqsymichaelluo.picturesandpdf.ui.theme.PicturesPDFTheme

const val image_preview_id = "image_preview/{pdfName}/{index}"
const val image_sorting_id = "image_sorting/{pdfName}"

@Composable
fun RootNavGraph(
    viewModel: MainViewModel,
    onImportPicture: (String?) -> Unit,
    onAddPicture: (String?) -> Unit,
    onImportPDF: () -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    imagePreviewViewModel: ImagePreviewViewModel
) {
    val navController = rememberNavController()
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onImportPicture = onImportPicture,
                    onImportPDF = onImportPDF,
                    requestDragAndDropPermission = requestDragAndDropPermission,
                    releaseDragAndDropPermission = releaseDragAndDropPermission,
                    rootNavController = navController,
                    imagePreviewViewModel = imagePreviewViewModel
                )
            }
            composable(image_preview_id) {
                val pdfName = it.arguments?.getString("pdfName")!!
                val index = it.arguments?.getString("index")!!.toInt()
                PicturesPDFTheme(
                    darkTheme = true
                ) {
                    ImagePreviewScreen(
                        pdfName = pdfName,
                        currentIndex = index,
                        imagePreviewViewModel = imagePreviewViewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        navController = navController
                    )
                }
            }
            composable(image_sorting_id) {
                val pdfName = it.arguments?.getString("pdfName")
                PicturesPDFTheme {
                    pdfName?.let {
                        ImageSortingScreen(
                            pdfName = pdfName,
                            onBack = {
                                navController.popBackStack()
                                imagePreviewViewModel.setTriggerSort(
                                    pdfName = pdfName,
                                    triggered = false
                                )
                                viewModel.overridePicturesGroup(
                                    name = pdfName,
                                    list = imagePreviewViewModel.imagePreviewList[pdfName]?.bitmapList
                                )
                            },
                            imagePreviewViewModel = imagePreviewViewModel,
                            onImportPicture = onAddPicture,
                            requestDragAndDropPermission = requestDragAndDropPermission,
                            releaseDragAndDropPermission = releaseDragAndDropPermission,
                        )
                    }
                }
            }
        }
    }
}