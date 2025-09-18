package com.bitchat.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun PasswordPromptDialog(
    show: Boolean,
    channelName: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show && channelName != null) {
        val colorScheme = MaterialTheme.colorScheme
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Enter Channel Password",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Channel $channelName is password protected. Enter the password to join.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChange,
                        label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.SansSerif
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "Join",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                }
            },
            containerColor = colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun AppInfoDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        val colorScheme = MaterialTheme.colorScheme
        val accent = colorScheme.primary

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Echo",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "le chat bleu sans permission",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }

                    SectionHeader(title = "FONCTIONNALITÉS")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureRow(Icons.Outlined.Bluetooth, "communication hors-ligne", "fonctionne sans internet via Bluetooth Low Energy")
                        FeatureRow(Icons.Outlined.Lock, "chiffrement de bout en bout", "messages privés chiffrés avec le protocole Noise")
                        FeatureRow(Icons.Outlined.WifiTethering, "portée étendue", "les messages sont relayés par les pairs, plus loin")
                        FeatureRow(Icons.Outlined.People, "contacts", "ajoutez des contacts autour ou via Nostr")
                        FeatureRow(Icons.Outlined.Language, "contacts acceptés (fallback)", "discutez en privé via Nostr hors portée mesh")
                        FeatureRow(Icons.Outlined.AlternateEmail, "mentions", "utilisez @pseudo pour notifier quelqu’un")
                    }

                    SectionHeader(title = "CONFIDENTIALITÉ")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureRow(Icons.Outlined.VisibilityOff, "aucun pistage", "pas de serveurs, comptes ou collecte. hors mesh, Echo peut utiliser Nostr pour relayer des messages chiffrés de bout en bout.")
                        FeatureRow(Icons.Outlined.Timer, "identité éphémère", "un nouvel ID est généré régulièrement")
                    }

                    SectionHeader(title = "MODE D’EMPLOI")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InstructionLine("définissez votre pseudo en le touchant")
                        InstructionLine("balayez vers la gauche pour le panneau latéral")
                        InstructionLine("touchez un pair pour démarrer un chat privé")
                        InstructionLine("utilisez @pseudo pour mentionner")
                        InstructionLine("triple-tapez le chat pour nettoyer")
                    }

                    SectionHeader(title = "AVERTISSEMENT", color = colorScheme.error)
                    Surface(
                        color = colorScheme.error.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "la sécurité des messages privés n’a pas encore été totalement auditée. n’utilisez pas l’app dans des situations critiques tant que cet avertissement apparaît.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
                            Text("FERMER", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = color
    )
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colorScheme.primary)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstructionLine(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun PrivateChatRequestDialog(
    show: Boolean,
    requesterPeerID: String?,
    requesterNickname: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show && requesterPeerID != null) {
        val colorScheme = MaterialTheme.colorScheme
        val name = requesterNickname ?: requesterPeerID
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Demande de chat privé",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "$name souhaite démarrer un chat privé. Accepter ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = onAccept) {
                    Text(
                        text = "Accepter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDecline) {
                        Text(
                            text = "Refuser",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.error
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Annuler",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            },
            containerColor = colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}
