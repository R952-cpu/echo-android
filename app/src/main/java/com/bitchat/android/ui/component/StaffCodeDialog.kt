

package com.bitchat.android.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.bitchat.android.util.StaffAuth

@Composable
fun StaffCodeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (StaffAuth.activate(context, codeInput)) {
                    Toast.makeText(context, "Accès staff activé", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    showError = true
                }
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        title = { Text("Code Staff") },
        text = {
            Column {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Entrez le code") }
                )
                if (showError) {
                    Text(
                        "Code incorrect",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}
