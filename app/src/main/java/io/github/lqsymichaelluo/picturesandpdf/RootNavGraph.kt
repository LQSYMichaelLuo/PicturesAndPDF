package io.github.lqsymichaelluo.picturesandpdf

import android.view.DragEvent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.lqsymichaelluo.picturesandpdf.ui.theme.PicturesPDFTheme

@Composable
fun RootNavGraph(
    viewModel: MainViewModel,
    onImportPicture: (String?) -> Unit,
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
            composable("image_preview") {
                PicturesPDFTheme(
                    darkTheme = true
                ) {
                    ImagePreviewScreen(
                        imagePreviewViewModel = imagePreviewViewModel,
                        onBack = {
                            navController.popBackStack()
                            imagePreviewViewModel.clear()
                        },
                        sortImage = {
                            navController.navigate("image_sorting")
                            imagePreviewViewModel.setTriggerPreview(false)
                        }
                    )
                }
            }
            composable("image_sorting"){
                PicturesPDFTheme {
                    ImageSortingScreen(
                        onBack = {
                            navController.popBackStack()
                            imagePreviewViewModel.setTriggerSort(false)
                        },
                        imagePreviewViewModel = imagePreviewViewModel
                    )
                }
            }
        }
    }
}