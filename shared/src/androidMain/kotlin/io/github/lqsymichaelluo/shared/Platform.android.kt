package io.github.lqsymichaelluo.shared

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent

actual fun platform() = "Android"

@Composable
fun EasterEggScreen(
    onBack: () -> Unit
){
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Column(
        modifier = Modifier.fillMaxSize()
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
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            ":D",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}