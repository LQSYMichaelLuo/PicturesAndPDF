package io.github.lqsymichaelluo.picturesandpdf

import android.content.Intent
import android.view.DragEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.CircularProgressIndicator


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController,
    viewModel: MainViewModel,
    imagePreviewViewModel: ImagePreviewViewModel,
    onImportPicture: (String?) -> Unit,
    onImportPDF: () -> Unit,
    requestDragAndDropPermission: (DragEvent) -> Unit,
    releaseDragAndDropPermission: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when (currentRoute) {
        Screen.Pic2PDF.route -> stringResource(Screen.Pic2PDF.title)
        Screen.PDF2Pic.route -> stringResource(Screen.PDF2Pic.title)
        else -> stringResource(R.string.title_name)
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val width = adaptiveInfo.windowSizeClass.widthSizeClass
    val height = adaptiveInfo.windowSizeClass.heightSizeClass

    val isPhoneLandscape = height == WindowHeightSizeClass.Compact

    val isExporting by viewModel.isExporting.collectAsState()
    val exportText by viewModel.exportText.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    var titleHeight by remember { mutableStateOf(64.dp) }

    var receivingDrag by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (receivingDrag)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (receivingDrag)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val importButtonInteractionSource = remember { MutableInteractionSource() }
    val dragPress = remember { mutableStateOf<PressInteraction.Press?>(null) }

    if (isPhoneLandscape)
        Scaffold(
            topBar = {
                TopAppBar(
                    expandedHeight = titleHeight + 12.dp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                                    titleHeight =
                                        with(density) { layoutCoordinates.size.height.toDp() }
                                },
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = stringResource(R.string.app_name))
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (AppFlags.debuggable.value)
                                TextButton(
                                    onClick = { viewModel.printLong("Using layout of dwarf: ${isPhoneLandscape}") },
                                ) {
                                    Text("info")
                                }
                        }
                    },
                    //scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        //scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    ),
                    actions = {
                        val density = LocalDensity.current
                        val context = LocalContext.current

                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.import_str)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            Button(
                                shape = if (receivingDrag) RoundedCornerShape(24.dp)
                                else CircleShape,
                                contentPadding = if (receivingDrag) PaddingValues(8.dp)
                                else PaddingValues(0.dp),
                                interactionSource = importButtonInteractionSource,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .defaultMinSize(minWidth = 42.dp)
                                    .animateContentSize()
                                    .dragAndDropTarget(
                                        shouldStartDragAndDrop = { event ->
                                            when (currentRoute) {
                                                Screen.Pic2PDF.route -> {
                                                    event.mimeTypes()
                                                        .any { it.startsWith("image/") }
                                                }

                                                Screen.PDF2Pic.route -> {
                                                    event.mimeTypes().any {
                                                        it.equals(
                                                            "application/pdf",
                                                            ignoreCase = true
                                                        ) || it.equals(
                                                            "application/octet-stream",
                                                            ignoreCase = true
                                                        )
                                                    }
                                                }

                                                else -> false
                                            }
                                        },
                                        target = remember {
                                            object : DragAndDropTarget {
                                                override fun onStarted(event: DragAndDropEvent) {
                                                    receivingDrag = true
                                                }

                                                override fun onEntered(event: DragAndDropEvent) {
                                                    val press = PressInteraction.Press(Offset.Zero)
                                                    dragPress.value = press
                                                    importButtonInteractionSource.tryEmit(press)
                                                }

                                                override fun onExited(event: DragAndDropEvent) {
                                                    dragPress.value?.let {
                                                        importButtonInteractionSource.tryEmit(
                                                            PressInteraction.Cancel(it)
                                                        )
                                                    }
                                                    dragPress.value = null
                                                }

                                                override fun onEnded(event: DragAndDropEvent) {
                                                    dragPress.value?.let {
                                                        importButtonInteractionSource.tryEmit(
                                                            PressInteraction.Release(it)
                                                        )
                                                    }
                                                    dragPress.value = null
                                                    receivingDrag = false
                                                }

                                                override fun onDrop(event: DragAndDropEvent): Boolean {
                                                    val androidEvent = event.toAndroidDragEvent()
                                                    requestDragAndDropPermission(androidEvent)
                                                    val clipData = androidEvent
                                                        .clipData ?: return false
                                                    viewModel.addPicturesFromClipData(
                                                        context,
                                                        clipData,
                                                        null,
                                                        releaseDragAndDropPermission
                                                    )
                                                    viewModel.addPDFFromClipData(
                                                        context,
                                                        clipData,
                                                        releaseDragAndDropPermission
                                                    )
                                                    return true
                                                }
                                            }
                                        }),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    contentColor = contentColor
                                ),
                                onClick = {
                                    when (currentRoute) {
                                        Screen.Pic2PDF.route -> {
                                            onImportPicture(null)
                                        }

                                        Screen.PDF2Pic.route -> {
                                            onImportPDF()
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_import),
                                        contentDescription = stringResource(R.string.import_str)
                                    )
                                    AnimatedVisibility(
                                        visible = receivingDrag,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = stringResource(R.string.dragtip))
                                        }
                                    }
                                }
                            }
                        }
                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.export_str)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            var showProgressDialog by remember { mutableStateOf(false) }
                            var isOutputing by remember { mutableStateOf(true) }
                            var outputText by remember { mutableStateOf("正在输出...") }
                            if (showDialog) {
                                AlertDialog(
                                    onDismissRequest = {},
                                    title = { Text("正在输出文件") },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isExporting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                            }
                                            Text(exportText)
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            enabled = !isExporting,
                                            onClick = viewModel::dismissExportDialog
                                        ) {
                                            Text(stringResource(R.string.ok))
                                        }
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    when (currentRoute) {
                                        Screen.Pic2PDF.route -> {
                                            if (!viewModel.pictureInputList.isEmpty()) {
                                                viewModel.exportPicToPdf(context)
                                            }
                                        }

                                        Screen.PDF2Pic.route -> {
                                            if (!viewModel.pdfInputList.isEmpty()) {
                                                viewModel.exportPdfToPic(context)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_export),
                                    contentDescription = stringResource(R.string.export_str)
                                )
                            }
                        }
                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.settings)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            SettingsActivity::class.java
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = stringResource(R.string.settings)
                                )
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SideBar(navController)
                SideNavGraph(
                    navController = navController,
                    modifier = Modifier,
                    viewModel = viewModel,
                    rootNavController = rootNavController,
                    imagePreviewViewModel = imagePreviewViewModel,
                    onImportPicture = onImportPicture,
                    onImportPDF = onImportPDF,
                    requestDragAndDropPermission = requestDragAndDropPermission,
                    releaseDragAndDropPermission = releaseDragAndDropPermission,
                    isPhoneLandscape = true
                )
            }
        }
    else
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.app_name))
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    //scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        //scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    ),
                    actions = {
                        val density = LocalDensity.current
                        val context = LocalContext.current

                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.import_str)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            Button(
                                shape = if (receivingDrag) RoundedCornerShape(24.dp)
                                else CircleShape,
                                contentPadding = if (receivingDrag) PaddingValues(8.dp)
                                else PaddingValues(0.dp),
                                interactionSource = importButtonInteractionSource,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .defaultMinSize(minWidth = 42.dp)
                                    .animateContentSize()
                                    .dragAndDropTarget(
                                        shouldStartDragAndDrop = { event ->
                                            when (currentRoute) {
                                                Screen.Pic2PDF.route -> {
                                                    event.mimeTypes()
                                                        .any { it.startsWith("image/") }
                                                }

                                                Screen.PDF2Pic.route -> {
                                                    event.mimeTypes().any {
                                                        it.equals(
                                                            "application/pdf",
                                                            ignoreCase = true
                                                        ) || it.equals(
                                                            "application/octet-stream",
                                                            ignoreCase = true
                                                        )
                                                    }
                                                }

                                                else -> false
                                            }
                                        },
                                        target = remember {
                                            object : DragAndDropTarget {
                                                override fun onStarted(event: DragAndDropEvent) {
                                                    receivingDrag = true
                                                }

                                                override fun onEntered(event: DragAndDropEvent) {
                                                    val press = PressInteraction.Press(Offset.Zero)
                                                    dragPress.value = press
                                                    importButtonInteractionSource.tryEmit(press)
                                                }

                                                override fun onExited(event: DragAndDropEvent) {
                                                    dragPress.value?.let {
                                                        importButtonInteractionSource.tryEmit(
                                                            PressInteraction.Cancel(it)
                                                        )
                                                    }
                                                    dragPress.value = null
                                                }

                                                override fun onEnded(event: DragAndDropEvent) {
                                                    dragPress.value?.let {
                                                        importButtonInteractionSource.tryEmit(
                                                            PressInteraction.Release(it)
                                                        )
                                                    }
                                                    dragPress.value = null
                                                    receivingDrag = false
                                                }

                                                override fun onDrop(event: DragAndDropEvent): Boolean {
                                                    val androidEvent = event.toAndroidDragEvent()
                                                    requestDragAndDropPermission(androidEvent)
                                                    val clipData = androidEvent
                                                        .clipData ?: return false
                                                    viewModel.addPicturesFromClipData(
                                                        context,
                                                        clipData,
                                                        null,
                                                        releaseDragAndDropPermission
                                                    )
                                                    viewModel.addPDFFromClipData(
                                                        context,
                                                        clipData,
                                                        releaseDragAndDropPermission
                                                    )
                                                    return true
                                                }
                                            }
                                        }),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    contentColor = contentColor
                                ),
                                onClick = {
                                    when (currentRoute) {
                                        Screen.Pic2PDF.route -> {
                                            onImportPicture(null)
                                        }

                                        Screen.PDF2Pic.route -> {
                                            onImportPDF()
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_import),
                                        contentDescription = stringResource(R.string.import_str)
                                    )
                                    AnimatedVisibility(
                                        visible = receivingDrag,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = stringResource(R.string.dragtip))
                                        }
                                    }
                                }
                            }
                        }
                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.export_str)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            var showProgressDialog by remember { mutableStateOf(false) }
                            var isOutputing by remember { mutableStateOf(true) }
                            var outputText by remember { mutableStateOf("正在输出...") }
                            if (showDialog) {
                                AlertDialog(
                                    onDismissRequest = {},
                                    title = { Text("正在输出文件") },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isExporting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                            }
                                            Text(exportText)
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            enabled = !isExporting,
                                            onClick = viewModel::dismissExportDialog
                                        ) {
                                            Text(stringResource(R.string.ok))
                                        }
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    when (currentRoute) {
                                        Screen.Pic2PDF.route -> {
                                            if (!viewModel.pictureInputList.isEmpty()) {
                                                viewModel.exportPicToPdf(context)
                                            }
                                        }

                                        Screen.PDF2Pic.route -> {
                                            if (!viewModel.pdfInputList.isEmpty()) {
                                                viewModel.exportPdfToPic(context)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_export),
                                    contentDescription = stringResource(R.string.export_str)
                                )
                            }
                        }
                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip { Text(stringResource(R.string.settings)) }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            SettingsActivity::class.java
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = stringResource(R.string.settings)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                BottomBar(navController = navController)
            }
        ) { innerPadding ->
            BottomNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                rootNavController = rootNavController,
                imagePreviewViewModel = imagePreviewViewModel,
                onImportPicture = onImportPicture,
                onImportPDF = onImportPDF,
                requestDragAndDropPermission = requestDragAndDropPermission,
                releaseDragAndDropPermission = releaseDragAndDropPermission,
                isPhoneLandscape = false
            )
        }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Pic2PDF,
        Screen.PDF2Pic
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        screens.forEach { screen ->
            AddItem(
                screen = screen,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

@Composable
fun SideBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Pic2PDF,
        Screen.PDF2Pic
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationRail(
        windowInsets = WindowInsets(
            left = 0,
            right = 0
        ),
        modifier = Modifier
            .fillMaxHeight()
            .width(96.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        screens.forEach { screen ->
            AddRailItem(
                screen = screen,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.AddItem(
    screen: Screen,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

    NavigationBarItem(
        selected = isSelected,
        onClick = {
            navController.navigate(screen.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        },
        icon = {
            val tooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip {
                        Text(text = stringResource(id = screen.title))
                    }
                },
                state = tooltipState
            ) {
                Icon(
                    painter = painterResource(id = screen.icon),
                    contentDescription = stringResource(id = screen.title)
                )
            }
        },
        label = {
            Text(
                text = stringResource(id = screen.title)
            )

        },

        alwaysShowLabel = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.AddRailItem(
    screen: Screen,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val isSelected =
        currentDestination?.hierarchy?.any { it.route == screen.route } == true

    NavigationRailItem(
        selected = isSelected,
        onClick = {
            navController.navigate(screen.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        },
        icon = {
            val tooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip {
                        Text(text = stringResource(id = screen.title))
                    }
                },
                state = tooltipState
            ) {
                Icon(
                    painter = painterResource(id = screen.icon),
                    contentDescription = stringResource(id = screen.title)
                )
            }
        },
        label = {
            Text(
                text = stringResource(id = screen.title),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    lineBreak = LineBreak.Simple
                ),
                softWrap = true
            )
        },
        alwaysShowLabel = true
    )
}

