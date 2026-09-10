package com.jake.spenhotkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jake.spenhotkey.ui.AppListScreen
import com.jake.spenhotkey.ui.AppMappingScreen
import com.jake.spenhotkey.ui.DefaultMappingScreen
import com.jake.spenhotkey.ui.HomeScreen
import com.jake.spenhotkey.ui.theme.SPenHotkeyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SPenHotkeyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("default_mapping") { DefaultMappingScreen(navController) }
                        composable("app_list") { AppListScreen(navController) }
                        composable("app_mapping/{packageName}") { backStackEntry ->
                            val pkg = backStackEntry.arguments?.getString("packageName")
                            if (pkg != null) {
                                AppMappingScreen(navController, pkg)
                            }
                        }
                    }
                }
            }
        }
    }
}
