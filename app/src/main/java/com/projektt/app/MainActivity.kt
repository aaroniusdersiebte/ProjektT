package com.projektt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.projektt.app.ui.screens.HomeScreen
import com.projektt.app.ui.screens.SettingsScreen
import com.projektt.app.ui.theme.ProjektTTheme
import com.projektt.app.widget.ProjektTWidget
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openAddDialog = intent?.getBooleanExtra(ProjektTWidget.EXTRA_OPEN_ADD_DIALOG, false) ?: false

        setContent {
            ProjektTTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ProjektTTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                openAddDialogOnStart = openAddDialog,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
