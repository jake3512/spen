package com.jake.spenhotkey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jake.spenhotkey.data.AppListProvider
import com.jake.spenhotkey.data.MappingRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { MappingRepository(context) }
    val apps = remember { AppListProvider.loadLaunchableApps(context) }
    val overrides by repo.overrideAppsFlow.collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("앱별 동작 설정") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(apps, key = { it.packageName }) { app ->
                val customized = app.packageName in overrides
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(if (customized) "사용자 지정 동작 사용 중" else "기본 동작 사용") },
                    modifier = Modifier.clickable {
                        navController.navigate("app_mapping/${app.packageName}")
                    }
                )
                Divider()
            }
        }
    }
}
