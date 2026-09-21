package com.yogev.youtubeupdater.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    token: String,
    includePrereleases: Boolean,
    onToken: (String) -> Unit,
    onPrerelease: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var tokenField by remember { mutableStateOf(token) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הגדרות") },
        text = {
            Column {
                OutlinedTextField(
                    value = tokenField,
                    onValueChange = { tokenField = it },
                    label = { Text("GitHub Token (אופציונלי)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "מעלה את מגבלת הבקשות ל-GitHub. לא חובה.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("כלול גרסאות pre-release")
                    Switch(checked = includePrereleases, onCheckedChange = onPrerelease)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "בדיקת עדכונים אוטומטית: כל 6 שעות",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onToken(tokenField)
                onDismiss()
            }) { Text("שמור") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        },
    )
}
