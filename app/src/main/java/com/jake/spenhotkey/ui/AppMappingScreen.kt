package com.jake.spenhotkey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jake.spenhotkey.data.Mapping
import com.jake.spenhotkey.data.MappingRepository
import com.jake.spenhotkey.ui.components.ActionPicker
import kotlinx.coroutines.launch

@Composable
fun AppMappingScreen(navController: NavController, packageName: String) {
    val context = LocalContext.current
    val repo = remember { MappingRepository(context) }
    val scope = rememberCoroutineScope()

    fun resolveLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    val appLabel = remember { resolveLabel(packageName) }
    var mapping by remember { mutableStateOf(Mapping()) }
    var loaded by remember { mutableStateOf(false) }
    val overrides by repo.overrideAppsFlow.collectAsState(initial = emptySet())
    val isCustomized = packageName in overrides

    LaunchedEffect(packageName) {
        mapping = repo.effectiveMappingForPackage(packageName, ::resolveLabel)
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appLabel) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (isCustomized) {
                    "이 앱은 사용자 지정 동작을 사용합니다."
                } else {
                    "이 앱은 기본 동작을 상속합니다. 아래에서 변경하면 이 앱에서만 다르게 동작합니다."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            ActionPicker(
                label = "한 번 클릭",
                selected = mapping.singleClick,
                onSelected = {
                    val updated = mapping.copy(singleClick = it)
                    mapping = updated
                    scope.launch { repo.saveAppMapping(packageName, updated) }
                }
            )
            ActionPicker(
                label = "두 번 클릭",
                selected = mapping.doubleClick,
                onSelected = {
                    val updated = mapping.copy(doubleClick = it)
                    mapping = updated
                    scope.launch { repo.saveAppMapping(packageName, updated) }
                }
            )
            ActionPicker(
                label = "길게 누르기",
                selected = mapping.longPress,
                onSelected = {
                    val updated = mapping.copy(longPress = it)
                    mapping = updated
                    scope.launch { repo.saveAppMapping(packageName, updated) }
                }
            )

            if (isCustomized) {
                OutlinedButton(onClick = {
                    scope.launch {
                        repo.clearAppOverride(packageName)
                        mapping = repo.effectiveMappingForPackage(packageName, ::resolveLabel)
                    }
                }) {
                    Text("기본 동작으로 되돌리기")
                }
            }
        }
    }
}
