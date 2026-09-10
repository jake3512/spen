package com.jake.spenhotkey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jake.spenhotkey.data.Mapping
import com.jake.spenhotkey.data.MappingRepository
import com.jake.spenhotkey.ui.components.ActionPicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultMappingScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { MappingRepository(context) }
    val scope = rememberCoroutineScope()

    fun resolveLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    val mapping by repo.defaultMappingFlow(::resolveLabel).collectAsState(initial = Mapping())
    var longPressMs by remember { mutableStateOf(MappingRepository.DEFAULT_LONG_PRESS_MS) }
    var doubleClickMs by remember { mutableStateOf(MappingRepository.DEFAULT_DOUBLE_CLICK_MS) }

    LaunchedEffect(Unit) {
        repo.longPressMsFlow.collect { longPressMs = it }
    }
    LaunchedEffect(Unit) {
        repo.doubleClickMsFlow.collect { doubleClickMs = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기본 동작 설정") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("모든 앱에서 기본으로 사용할 S펜 버튼 동작입니다.", style = MaterialTheme.typography.bodyMedium)

            ActionPicker(
                label = "한 번 클릭",
                selected = mapping.singleClick,
                onSelected = { scope.launch { repo.saveDefaultMapping(mapping.copy(singleClick = it)) } }
            )
            ActionPicker(
                label = "두 번 클릭",
                selected = mapping.doubleClick,
                onSelected = { scope.launch { repo.saveDefaultMapping(mapping.copy(doubleClick = it)) } }
            )
            ActionPicker(
                label = "길게 누르기",
                selected = mapping.longPress,
                onSelected = { scope.launch { repo.saveDefaultMapping(mapping.copy(longPress = it)) } }
            )

            Divider()

            Text("타이밍 조절", style = MaterialTheme.typography.titleMedium)
            Text("길게 누르기 인식 시간: ${longPressMs}ms")
            Slider(
                value = longPressMs.toFloat(),
                onValueChange = { longPressMs = it.toLong() },
                onValueChangeFinished = {
                    scope.launch { repo.setTimings(longPressMs, doubleClickMs) }
                },
                valueRange = 250f..1000f
            )
            Text("더블클릭 인식 시간: ${doubleClickMs}ms")
            Slider(
                value = doubleClickMs.toFloat(),
                onValueChange = { doubleClickMs = it.toLong() },
                onValueChangeFinished = {
                    scope.launch { repo.setTimings(longPressMs, doubleClickMs) }
                },
                valueRange = 150f..600f
            )
        }
    }
}
