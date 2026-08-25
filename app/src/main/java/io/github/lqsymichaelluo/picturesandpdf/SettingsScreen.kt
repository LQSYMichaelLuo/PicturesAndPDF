package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    val debugState by viewModel.debuggable
    //val context = LocalContext.current
    val density = LocalDensity.current
    var clearEnabled by remember { mutableStateOf(true) }
    val belowPositionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x =
                    anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.bottom + with(density) { 1.dp.roundToPx() }
                return IntOffset(x, y)
            }
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = belowPositionProvider,
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.back)) }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "返回"
                            )
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val context = LocalContext.current
            var licenseText by remember { mutableStateOf("License") }
            var isLicenseShow by remember { mutableStateOf(false) }
            SettingsGroupTitle("存储")
            ListItem(
                headlineContent = { Text("清理缓存") },
                supportingContent = { Text("清理一些无用的缓存文件") },
                trailingContent = {
                    Button(
                        onClick = {
                            viewModel.clearFileCache(context)
                            clearEnabled = false
                        },
                        enabled = clearEnabled
                    ) {
                        if (clearEnabled) {
                            Text("清理")
                        } else {
                            Text("已清理")
                        }
                    }
                },
                modifier = Modifier.clickable(
                    enabled = clearEnabled
                ) {
                    clearEnabled = false
                    viewModel.clearFileCache(context)
                }
            )
            HorizontalDivider()
            SettingsGroupTitle("开发者选项")
            SettingsSwitchItem(
                title = "Show Debug Text",
                checked = debugState,
                onCheckedChange = {
                    viewModel.toggleDebug()
                }
            )
            HorizontalDivider()
            SettingsGroupTitle("关于")
            ListItem(
                headlineContent = { Text("在 Github 上查看源码") },
                supportingContent = { Text("Apache License 2.0") },
                modifier = Modifier.combinedClickable(
                    onClick = {
                        val intent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                        intent.launchUrl(
                            context,
                            "https://github.com/LQSYMichaelLuo/PicturesAndPDF".toUri()
                        )
                    },
                    onLongClick = {
                        isLicenseShow = true
                    }
                )
            )
            LaunchedEffect(Unit) {
                licenseText = context.readAsset("LICENSE")
            }
            val licenseScrollState = rememberScrollState()
            if (isLicenseShow) {
                AlertDialog(
                    onDismissRequest = {
                        isLicenseShow = false
                    },
                    title = {
                        Text("License")
                    },
                    text = {
                        Text(
                            text = licenseText,
                            modifier = Modifier.verticalScroll(licenseScrollState)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isLicenseShow = false
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.ok)
                            )
                        }
                    }
                )
            }
            /*/
            repeat(19) { index ->
                SettingsGroupTitle("Settings Group ${index + 1}")
                repeat(Random.nextInt(1, 5)) { indexNum ->
                    SettingsSwitchItem(
                        title = "Setting ${indexNum + 1}",
                        checked = Random.nextBoolean()
                    )
                }
                Divider()
            }

            SettingsGroupTitle("Settings Group 20")

            repeat(Random.nextInt(1, 5)) { indexNum ->
                SettingsSwitchItem(
                    title = "Setting ${indexNum + 1}",
                    checked = Random.nextBoolean()
                )
            }
            */

        }
    }
}

@Composable
fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

fun Context.readAsset(fileName: String): String {
    return assets.open(fileName).bufferedReader().use { it.readText() }
}