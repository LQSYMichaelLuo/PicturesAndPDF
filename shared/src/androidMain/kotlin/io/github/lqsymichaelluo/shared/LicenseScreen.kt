package io.github.lqsymichaelluo.shared

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBack: () -> Unit,
    vibrate: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var licenseText by remember { mutableStateOf("License") }
    LaunchedEffect(Unit) {
        licenseText = context.assets.open("LICENSE").bufferedReader().use { it.readText() }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    event.matches(key = Key.B, ctrl = true)
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            },
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.onSizeChanged {
                    vibrate()
                },
                title = {
                    Text("License")
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(paddingValues)
        ) {
            Text(
                text = licenseText,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

fun KeyEvent.matches(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    meta: Boolean = false,
): Boolean {
    return type == KeyEventType.KeyDown &&
            this.key == key &&
            isCtrlPressed == ctrl &&
            isShiftPressed == shift &&
            isAltPressed == alt &&
            isMetaPressed == meta
}