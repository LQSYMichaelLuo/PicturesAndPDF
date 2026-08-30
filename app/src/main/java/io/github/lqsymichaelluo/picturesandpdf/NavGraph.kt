package io.github.lqsymichaelluo.picturesandpdf

import android.view.DragEvent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: MainViewModel,
    imagePreviewViewModel: ImagePreviewViewModel,
    rootNavController: NavHostController,
    onImportPicture: (String?) -> Unit,
    onImportPDF: () -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    isPhoneLandscape: Boolean
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Pic2PDF.route,
            modifier = modifier,
            enterTransition = {
                when (initialState.destination.route) {
                    Screen.Pic2PDF.route -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        )
                    }

                    else -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300)
                        )
                    }
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screen.PDF2Pic.route -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300)
                        )
                    }

                    else -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        )
                    }
                }
            }
        ) {
            composable(
                route = Screen.Pic2PDF.route
            ) {
                Pic2PDFScreen(
                    viewModel = viewModel,
                    rootNavController = rootNavController,
                    imagePreviewViewModel = imagePreviewViewModel,
                    onImportPicture = onImportPicture,
                    requestDragAndDropPermission = requestDragAndDropPermission,
                    releaseDragAndDropPermission = releaseDragAndDropPermission
                )
            }
            composable(
                route = Screen.PDF2Pic.route
            ) {
                PDF2PicScreen(
                    viewModel = viewModel,
                    isPhoneLandscape = isPhoneLandscape,
                    rootNavController = rootNavController,
                )
            }
        }
    }
}

@Composable
fun SideNavGraph(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: MainViewModel,
    imagePreviewViewModel: ImagePreviewViewModel,
    rootNavController: NavHostController,
    onImportPicture: (String?) -> Unit,
    onImportPDF: () -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
    isPhoneLandscape: Boolean
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Pic2PDF.route,
            modifier = modifier,
            enterTransition = {
                when (initialState.destination.route) {
                    Screen.Pic2PDF.route -> {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        )
                    }

                    else -> {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        )
                    }
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    Screen.PDF2Pic.route -> {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        )
                    }

                    else -> {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        )
                    }
                }
            }
        ) {
            composable(
                route = Screen.Pic2PDF.route
            ) {
                Pic2PDFScreen(
                    viewModel = viewModel,
                    rootNavController = rootNavController,
                    imagePreviewViewModel = imagePreviewViewModel,
                    onImportPicture = onImportPicture,
                    requestDragAndDropPermission = requestDragAndDropPermission,
                    releaseDragAndDropPermission = releaseDragAndDropPermission
                )
            }
            composable(
                route = Screen.PDF2Pic.route
            ) {
                PDF2PicScreen(
                    viewModel = viewModel,
                    isPhoneLandscape = isPhoneLandscape,
                    rootNavController = rootNavController,
                )
            }
        }
    }
}