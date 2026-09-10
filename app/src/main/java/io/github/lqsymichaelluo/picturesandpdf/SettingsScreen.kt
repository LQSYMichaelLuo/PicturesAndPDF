package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

private val KEYBOARD_SHORTCUTS = listOf(
    ShortcutItem("一切返回操作", listOf("ESC")),
    ShortcutItem("主页面切换页面", listOf("Ctrl", "P")),
    ShortcutItem("主页面打开设置", listOf("Ctrl", "S")),
    ShortcutItem("返回页面", listOf("Ctrl", "B")),
    ShortcutItem("图片预览页缩小/图片排序页缩小", listOf("Ctrl", "[")),
    ShortcutItem("图片预览页放大/图片排序页放大", listOf("Ctrl", "]")),
    ShortcutItem("图片预览页图片左移/切换上一张图", listOf("←")),
    ShortcutItem("图片预览页图片右移/切换下一张图", listOf("→")),
    ShortcutItem("图片预览页图片上移/PDF预览页页面上移", listOf("↑")),
    ShortcutItem("图片预览页图片下移/PDF预览页页面下移", listOf("↓")),
    ShortcutItem("图片预览页切换底色/PDF预览页切换底色", listOf("Ctrl", "Alt", "G")),
    ShortcutItem("PDF预览页大幅移动", listOf("Space")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val debugState by viewModel.debuggable
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var clearEnabled by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    event.matches(key = Key.B, ctrl = true)
                ) {
                    activity?.finish()
                    true
                } else {
                    false
                }
            },
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.onSizeChanged {
                    HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                },
                title = { Text(stringResource(R.string.settings)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below
                        ),
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.back)) }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                onBack()
                            }) {
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
            val isKeyboardShortcutsDialogShow by viewModel.isKeyboardShortcutsDialogShow.collectAsState()
            SettingsGroupTitle("存储")
            ListItem(
                modifier = Modifier.clickable(
                    enabled = clearEnabled
                ) {
                    HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                    clearEnabled = false
                    viewModel.clearFileCache(context)
                },
                trailingContent = {
                    Button(
                        onClick = {
                            HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
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
                supportingContent = { Text("清理一些无用的缓存文件") },
                colors = ListItemDefaults.colors(),
                content = { Text("清理缓存") },
            )
            HorizontalDivider()
            SettingsGroupTitle("开发者选项")
            SettingsSwitchItem(
                title = "Show Debug Text",
                checked = debugState,
                onCheckedChange = {
                    HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                    viewModel.toggleDebug()
                }
            )
            HorizontalDivider()
            SettingsGroupTitle("关于")
            ListItem(
                modifier = Modifier.combinedClickable(
                    onClick = {
                        HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                        viewModel.setKeyboardShortcutsDialogShow()
                    },
                ),
                supportingContent = { Text("当然是键盘上的快捷键") },
                colors = ListItemDefaults.colors(),
                content = { Text("快捷键说明") },
            )
            ListItem(
                modifier = Modifier.combinedClickable(
                    onClick = {
                        HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                        val intent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                        intent.launchUrl(
                            context,
                            "https://github.com/LQSYMichaelLuo/PicturesAndPDF".toUri()
                        )
                    },
                    onLongClick = {
                        HapticManager.vibrate(context, HapticManager.EFFECT_HEAVY_CLICK)
                        isLicenseShow = true
                    }
                ),
                supportingContent = { Text("Apache License 2.0") },
                colors = ListItemDefaults.colors(),
                content = { Text("在 Github 上查看源码") },
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
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
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
            if (isKeyboardShortcutsDialogShow) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissKeyboardShortcutsDialog,
                    title = {
                        Text("快捷键说明")
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            KEYBOARD_SHORTCUTS.forEach { ShortcutRow(it) }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                HapticManager.vibrate(context, HapticManager.EFFECT_CLICK)
                                viewModel.dismissKeyboardShortcutsDialog()
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

private data class ShortcutItem(
    val desc: String,
    val keys: List<String>
)

@Composable
private fun ShortcutRow(item: ShortcutItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.desc,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            item.keys.forEach { KeyChip(it) }
        }
    }
}

@Composable
private fun KeyChip(key: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(key, style = MaterialTheme.typography.labelMedium) },
        contentPadding = PaddingValues(horizontal = 10.dp)
    )
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