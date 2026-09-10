package com.jake.spenhotkey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.jake.spenhotkey.util.AccessibilityUtils

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled by remember { mutableStateOf(AccessibilityUtils.isServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = AccessibilityUtils.isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("S펜 사이드 버튼 핫키", style = MaterialTheme.typography.headlineSmall)
        Text(
            "S펜 사이드 버튼을 한 번 클릭 / 두 번 클릭 / 길게 누르기로 구분해서 원하는 동작을 실행합니다.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (serviceEnabled) "접근성 서비스: 켜짐" else "접근성 서비스: 꺼짐",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    "S펜 버튼을 인식하려면 설정 > 접근성에서 'S펜 핫키' 서비스를 켜야 합니다.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = { AccessibilityUtils.openAccessibilitySettings(context) }) {
                    Text(if (serviceEnabled) "접근성 설정 열기" else "접근성 서비스 켜기")
                }
            }
        }

        OutlinedButton(
            onClick = { navController.navigate("default_mapping") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("기본 동작 설정")
        }

        OutlinedButton(
            onClick = { navController.navigate("app_list") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Apps, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("앱별로 다르게 설정")
        }

        Spacer(Modifier.weight(1f))
        Text(
            "참고: 삼성 설정의 'S펜 > S펜 버튼' 기능과 충돌할 수 있습니다. " +
                "이 앱을 사용하려면 삼성 S펜 버튼 설정을 꺼두는 것을 권장합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
