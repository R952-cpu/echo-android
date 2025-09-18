package com.bitchat.android.ui

import com.bitchat.android.R
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.bitchat.android.model.PeerDisplayData
import com.bitchat.android.model.PeerConnectionState
import com.bitchat.android.identity.NostrIdentity


/**
 * Sidebar components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun SidebarOverlay(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onShowMyNostr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val panelInteractionSource = remember { MutableInteractionSource() }

    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val currentChannel by viewModel.currentChannel.observeAsState()
    val selectedPrivatePeer by viewModel.selectedPrivateChatPeer.observeAsState()
    val unreadChannelMessages by viewModel.unreadChannelMessages.observeAsState(emptyMap())
    val filteredPeers by viewModel.filteredPeers.observeAsState(emptyList())
    val allPeers by viewModel.allPeers.observeAsState(emptyList())
    val searchQuery by viewModel.peopleSearchQuery.observeAsState("")
    val detectedNpub by viewModel.detectedNpub.observeAsState(null)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(indication = null, interactionSource = interactionSource) { onDismiss() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 300.dp, max = 360.dp)
                .align(Alignment.CenterEnd)
                .clickable(indication = null, interactionSource = panelInteractionSource) { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(colorScheme.background.copy(alpha = 0.95f))
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                SidebarTopBar(
                    colorScheme = colorScheme,
                    onDismiss = onDismiss,
                    onShowMyNostr = onShowMyNostr
                )

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PeopleSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::setPeopleSearchQuery,
                            onClear = { viewModel.setPeopleSearchQuery("") }
                        )
                    }

                    detectedNpub?.let { npub ->
                        item {
                            NostrDetectedCard(
                                npub = npub,
                                colorScheme = colorScheme
                            )
                        }
                    }

                    if (joinedChannels.isNotEmpty()) {
                        item {
                            ChannelsSection(
                                channels = joinedChannels.toList(),
                                currentChannel = currentChannel,
                                colorScheme = colorScheme,
                                onChannelClick = { channel ->
                                    viewModel.switchToChannel(channel)
                                    onDismiss()
                                },
                                onLeaveChannel = { channel -> viewModel.leaveChannel(channel) },
                                unreadChannelMessages = unreadChannelMessages
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    item {
                        PeopleSectionHeader(colorScheme = colorScheme)
                    }

                    if (filteredPeers.isEmpty()) {
                        item {
                            PeopleEmptyState(
                                searchQuery = searchQuery,
                                colorScheme = colorScheme,
                                hasAnyPeers = allPeers.isNotEmpty()
                            )
                        }
                    } else {
                        items(filteredPeers, key = { peer -> peer.peerId ?: peer.fingerprint ?: peer.displayName }) { peer ->
                            PeerItem(
                                peer = peer,
                                isSelected = peer.peerId == selectedPrivatePeer,
                                colorScheme = colorScheme,
                                onItemClick = {
                                    peer.peerId?.let { target ->
                                        viewModel.startPrivateChat(target)
                                        onDismiss()
                                    }
                                },
                                onToggleFavorite = {
                                    peer.peerId?.let { viewModel.toggleFavorite(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelsSection(
    channels: List<String>,
    currentChannel: String?,
    colorScheme: ColorScheme,
    onChannelClick: (String) -> Unit,
    onLeaveChannel: (String) -> Unit,
    unreadChannelMessages: Map<String, Int> = emptyMap()
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person, // Using Person icon as placeholder
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.channels).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
        
        channels.forEach { channel ->
            val isSelected = channel == currentChannel
            val unreadCount = unreadChannelMessages[channel] ?: 0
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChannelClick(channel) }
                    .background(
                        if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unread badge for channels
                UnreadBadge(
                    count = unreadCount,
                    colorScheme = colorScheme,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Text(
                    text = channel, // Channel already contains the # prefix
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                
                // Leave channel button
                IconButton(
                    onClick = { onLeaveChannel(channel) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Leave channel",
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarTopBar(
    colorScheme: ColorScheme,
    onDismiss: () -> Unit,
    onShowMyNostr: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.sidebar_title_around_me).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onShowMyNostr) {
            Icon(
                imageVector = Icons.Outlined.QrCode,
                contentDescription = stringResource(id = R.string.sidebar_my_nostr)
            )
        }
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(id = R.string.sidebar_done))
        }
    }
}

@Composable
private fun PeopleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val clipboardText = clipboardManager.getText()?.text
    val clipboardEligible = clipboardText?.let { text ->
        text.contains("npub", ignoreCase = true) || text.startsWith("nostr:", ignoreCase = true)
    } ?: false

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(id = R.string.sidebar_search_hint)) },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (clipboardEligible) {
                    IconButton(onClick = {
                        clipboardText?.let { onQueryChange(it) }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(id = R.string.sidebar_paste_clipboard)
                        )
                    }
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(id = R.string.sidebar_clear_search))
                    }
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { /* handled via state */ })
    )
}

@Composable
private fun NostrDetectedCard(
    npub: String,
    colorScheme: ColorScheme
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(0.8.dp, Color(0xFF9C6BFF).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = Color(0xFF9C6BFF)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.sidebar_detected_npub_title),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface
                )
                Text(
                    text = npub,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(npub))
                Toast.makeText(context, R.string.sidebar_npub_copied, Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = stringResource(id = R.string.sidebar_copy_npub)
                )
            }
        }
    }
}

@Composable
private fun PeopleSectionHeader(colorScheme: ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.sidebar_people_section),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PeopleEmptyState(
    searchQuery: String,
    colorScheme: ColorScheme,
    hasAnyPeers: Boolean
) {
    val textRes = if (searchQuery.isBlank()) {
        if (hasAnyPeers) R.string.sidebar_no_results else R.string.sidebar_no_people
    } else {
        R.string.sidebar_no_results
    }
    Text(
        text = stringResource(id = textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun PeerItem(
    peer: PeerDisplayData,
    isSelected: Boolean,
    colorScheme: ColorScheme,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isClickable = peer.peerId != null && !peer.isMe
    val backgroundColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .let { base ->
                if (isClickable) {
                    base.clickable { onItemClick() }
                } else {
                    base
                }
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            peer.hasUnreadMessages -> {
                UnreadBadge(count = 1, colorScheme = colorScheme)
            }
            peer.connectionState == PeerConnectionState.MESH_CONNECTED -> {
                SignalStrengthIndicator(signalStrength = peer.signalStrength, colorScheme = colorScheme)
            }
            peer.connectionState == PeerConnectionState.RELAY_CONNECTED -> {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
            peer.connectionState == PeerConnectionState.NOSTR_AVAILABLE -> {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF9C6BFF)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val displayName = when {
            peer.isMe -> stringResource(id = R.string.sidebar_label_you)
            else -> peer.displayName
        }

        val statusLabel = when (peer.connectionState) {
            PeerConnectionState.MESH_CONNECTED -> stringResource(id = R.string.sidebar_status_mesh)
            PeerConnectionState.RELAY_CONNECTED -> stringResource(id = R.string.sidebar_status_relay)
            PeerConnectionState.NOSTR_AVAILABLE -> stringResource(id = R.string.sidebar_status_nostr)
            PeerConnectionState.OFFLINE -> stringResource(id = R.string.sidebar_status_offline)
        }

        val lastMessageLabel = peer.lastSeenEpochMillis?.takeIf { it > 0 }?.let { timestamp ->
            val relative = DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            stringResource(id = R.string.sidebar_last_message, relative)
        }

        val subtitle = listOfNotNull(statusLabel, lastMessageLabel).joinToString(" • ")

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        if (!peer.isMe) {
            IconButton(
                onClick = onToggleFavorite,
                enabled = peer.peerId != null
            ) {
                Icon(
                    imageVector = if (peer.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (peer.isFavorite) stringResource(id = R.string.sidebar_remove_favorite) else stringResource(id = R.string.sidebar_add_favorite),
                    tint = if (peer.isFavorite) Color(0xFFFFD700) else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MyNostrSheet(
    identity: NostrIdentity,
    onDismiss: () -> Unit,
    onSaveNpub: (String) -> Boolean,
    onClearNpub: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val qrBitmap = remember(identity.npub) { generateQrBitmap(identity.npub, 480) }
    var input by remember(identity.npub) { mutableStateOf(identity.npub) }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp, max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nostr_identity_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = stringResource(id = R.string.nostr_identity_input_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (showError) {
                    Text(
                        text = stringResource(id = R.string.nostr_identity_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = stringResource(id = R.string.nostr_identity_title),
                        modifier = Modifier.size(220.dp)
                    )
                    Text(
                        text = identity.npub,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(identity.npub))
                        Toast.makeText(context, R.string.sidebar_npub_copied, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(text = stringResource(id = R.string.nostr_identity_copy))
                    }
                    identity.encodedPrivate?.let { encodedNsec ->
                        Text(
                            text = encodedNsec,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(encodedNsec))
                        Toast.makeText(context, R.string.nostr_identity_secret_copied, Toast.LENGTH_SHORT).show()
                    }) {
                            Text(text = stringResource(id = R.string.nostr_identity_copy_secret))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.nostr_identity_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        val success = onSaveNpub(input)
                        if (success) {
                            Toast.makeText(context, R.string.nostr_identity_saved, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            showError = true
                        }
                    }) {
                        Text(text = stringResource(id = R.string.nostr_identity_save))
                    }

                    TextButton(onClick = {
                        onClearNpub()
                        onDismiss()
                    }) {
                        Text(text = stringResource(id = R.string.nostr_identity_clear))
                    }
                }

                Text(
                    text = stringResource(id = R.string.nostr_identity_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.close))
                }
            }
        }
    }
}

private fun generateQrBitmap(data: String, size: Int): ImageBitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dark = Color.Black.toArgb()
        val light = Color.White.toArgb()
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) dark else light)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun SignalStrengthIndicator(
    signalStrength: Int,
    colorScheme: ColorScheme
) {
    Row(modifier = Modifier.width(24.dp)) {
        repeat(3) { index ->
            val opacity = when {
                signalStrength >= (index + 1) * 33 -> 1f
                else -> 0.2f
            }
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = (4 + index * 2).dp)
                    .background(
                        colorScheme.onSurface.copy(alpha = opacity),
                        RoundedCornerShape(1.dp)
                    )
            )
            if (index < 2) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

/**
 * Reusable unread badge component for both channels and private messages
 */
@Composable
private fun UnreadBadge(
    count: Int,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .background(
                    color = Color(0xFFFFD700), // Yellow color
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 2.dp, vertical = 0.dp)
                .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black // Black text on yellow background
            )
        }
    }
}

/**
 * Convert RSSI value (dBm) to signal strength percentage (0-100)
 * RSSI typically ranges from -30 (excellent) to -100 (very poor)
 * Maps to 0-100 scale where:
 * - 0-32: No signal (0 bars)
 * - 33-65: Weak (1 bar) 
 * - 66-98: Good (2 bars)
 * - 99-100: Excellent (3 bars)
 */
private fun convertRSSIToSignalStrength(rssi: Int?): Int {
    if (rssi == null) return 0
    
    return when {
        rssi >= -40 -> 100  // Excellent signal
        rssi >= -55 -> 85   // Very good signal  
        rssi >= -70 -> 70   // Good signal
        rssi >= -85 -> 50   // Fair signal
        rssi >= -100 -> 25  // Poor signal
        else -> 0           // Very poor or no signal
    }
}
