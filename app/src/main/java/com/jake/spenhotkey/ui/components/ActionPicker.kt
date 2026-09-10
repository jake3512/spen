package com.jake.spenhotkey.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jake.spenhotkey.data.ActionSpec
import com.jake.spenhotkey.data.AppListProvider

/** Dropdown for choosing the [ActionSpec] bound to one S Pen gesture, with an app picker for "launch app". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPicker(
    label: String,
    selected: ActionSpec,
    onSelected: (ActionSpec) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActionSpec.defaultChoices().forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        expanded = false
                        onSelected(choice)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("앱 실행 선택...") },
                onClick = {
                    expanded = false
                    showAppPicker = true
                }
            )
        }
    }

    if (showAppPicker) {
        val apps = remember { AppListProvider.loadLaunchableApps(context) }
        Dialog(onDismissRequest = { showAppPicker = false }) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("실행할 앱 선택", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(apps, key = { it.packageName }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = { Text(app.packageName) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(ActionSpec.LaunchApp(app.packageName, app.label))
                                        showAppPicker = false
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
