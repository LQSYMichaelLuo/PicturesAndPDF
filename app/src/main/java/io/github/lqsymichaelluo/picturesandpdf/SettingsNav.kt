package io.github.lqsymichaelluo.picturesandpdf

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.lqsymichaelluo.picturesandpdf.ui.theme.PicturesPDFTheme
import io.github.lqsymichaelluo.shared.EasterEggScreen
import io.github.lqsymichaelluo.shared.LicenseScreen

@Composable
fun SettingsNav(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {}
) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "settings") {
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = onBack,
                navController = nav
            )
        }
        composable("easter_egg") {
            PicturesPDFTheme{
                EasterEggScreen(
                    onBack = {
                        nav.safePopOnce()
                    }
                )
            }
        }
        composable("license"){
            val context = LocalContext.current
            LicenseScreen(
                onBack = {
                    nav.safePopOnce()
                },
                vibrate = {
                    HapticManager.vibrate(context, HapticManager.EFFECT_TICK)
                }
            )
        }
    }
}