package com.shortsblockerkids.presentation.language

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role

@Composable
fun LanguagePicker(
    selectedTag: String,
    onSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val languages = listOf("en" to "English", "de" to "Deutsch", "uk" to "Українська")
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(languages.first { it.first == selectedTag }.second)
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("English / Deutsch / Українська") },
            text = {
                Column {
                    languages.forEach { (tag, label) ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().selectable(
                                    selected = tag == selectedTag,
                                    role = Role.RadioButton,
                                    onClick = {
                                        expanded = false
                                        onSelected(tag)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tag == selectedTag, onClick = null)
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
