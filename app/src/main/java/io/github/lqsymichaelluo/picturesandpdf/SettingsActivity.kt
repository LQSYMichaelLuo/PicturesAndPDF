package io.github.lqsymichaelluo.picturesandpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.lqsymichaelluo.picturesandpdf.ui.theme.PicturesPDFTheme


class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PicturesPDFTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        this.finish()
                    }
                )
            }
        }
    }
}
