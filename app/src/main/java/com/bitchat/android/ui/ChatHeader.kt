package com.bitchat.android.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.utils.singleOrTripleClickable
import com.bitchat.android.ui.theme.EchoMetrics
import com.bitchat.android.ui.theme.EchoPalette

/**
 * Header components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

/**
 * Reactive helper to compute favorite state from fingerprint mapping
 * This eliminates the need for static isFavorite parameters and makes
 * the UI reactive to fingerprint manager changes
 */
@Composable
fun isFavoriteReactive(
    peerID: String,
    peerFingerprints: Map<String, String>,
    favoritePeers: Set<String>
): Boolean {
    return remember(peerID, peerFingerprints, favoritePeers) {
        val fingerprint = peerFingerprints[peerID]
        fingerprint != null && favoritePeers.contains(fingerprint)
    }
}

@Composable
fun NoiseSessionIcon(
    sessionState: String?,
    modifier: Modifier = Modifier
) {
    val (icon, color, contentDescription) = when (sessionState) {
        "uninitialized" -> Triple(
            Icons.Outlined.NoEncryption,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Ready for handshake"
        )
        "handshaking" -> Triple(
            Icons.Outlined.Sync,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Handshake in progress"
        )
        "established" -> Triple(
            Icons.Filled.Lock,
            EchoPalette.AccentGreen,
            "End-to-end encrypted"
        )
        else -> { // "failed" or any other state
            Triple(
                Icons.Outlined.Warning,
                EchoPalette.AccentRed,
                "Handshake failed"
            )
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun NicknameEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Text(
                text = "@",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.primary
            )

            Spacer(modifier = Modifier.width(4.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = colorScheme.primary,
                    fontFamily = FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.widthIn(min = 48.dp, max = 80.dp)
            )
        }
    }
}

@Composable
fun PeerCounter(
    connectedPeers: List<String>,
    joinedChannels: Set<String>,
    hasUnreadChannels: Map<String, Int>,
    hasUnreadPrivateMessages: Set<String>,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasUnread = hasUnreadPrivateMessages.isNotEmpty()
    val channelUnread = hasUnreadChannels.values.any { it > 0 }
    val peerCount = connectedPeers.size
    val indicatorColor = when {
        peerCount == 0 -> colorScheme.error
        isConnected -> colorScheme.primary
        else -> colorScheme.secondary
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (hasUnread) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "Unread private messages",
                    tint = EchoPalette.AccentOrange,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (channelUnread) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.secondary
                )
            }

            Icon(
                imageVector = Icons.Filled.Group,
                contentDescription = "Connected peers",
                tint = indicatorColor,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = peerCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = indicatorColor,
                fontWeight = FontWeight.Medium
            )

            if (joinedChannels.isNotEmpty()) {
                Text(
                    text = "· ${joinedChannels.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatHeaderContent(
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onShowStaffSheet: () -> Unit,
    onPanicClear: () -> Unit,
    onShowAppInfo: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    when {
        selectedPrivatePeer != null -> {
            // Private chat header - Fully reactive state tracking
            val favoritePeers by viewModel.favoritePeers.observeAsState(emptySet())
            val peerFingerprints by viewModel.peerFingerprints.observeAsState(emptyMap())
            val peerSessionStates by viewModel.peerSessionStates.observeAsState(emptyMap())
            val peerNicknames by viewModel.peerNicknames.observeAsState(emptyMap())
            
            // Reactive favorite computation - no more static lookups!
            val isFavorite = isFavoriteReactive(
                peerID = selectedPrivatePeer,
                peerFingerprints = peerFingerprints,
                favoritePeers = favoritePeers
            )
            val sessionState = peerSessionStates[selectedPrivatePeer]
            
            Log.d("ChatHeader", "Header recomposing: peer=$selectedPrivatePeer, isFav=$isFavorite, sessionState=$sessionState")
            
            PrivateChatHeader(
                peerID = selectedPrivatePeer,
                peerNicknames = peerNicknames,
                isFavorite = isFavorite,
                sessionState = sessionState,
                onBackClick = onBackClick,
                onToggleFavorite = { viewModel.toggleFavorite(selectedPrivatePeer) }
            )
        }
        currentChannel != null -> {
            // Channel header
            ChannelHeader(
                channel = currentChannel,
                onBackClick = onBackClick,
                onLeaveChannel = { viewModel.leaveChannel(currentChannel) },
                onSidebarClick = onSidebarClick
            )
        }
        else -> {
            // Main header
            MainHeader(
                nickname = nickname,
                onNicknameChange = viewModel::setNickname,
                onTitleClick = onShowAppInfo,
                onShowStaffSheet = onShowStaffSheet,
                onPanicClear = onPanicClear,
                onSidebarClick = onSidebarClick,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun PrivateChatHeader(
    peerID: String,
    peerNicknames: Map<String, String>,
    isFavorite: Boolean,
    sessionState: String?,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val peerNickname = peerNicknames[peerID] ?: peerID

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EchoMetrics.HeaderHeight)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = peerNickname,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            NoiseSessionIcon(
                sessionState = sessionState,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                Log.d("ChatHeader", "Header toggle favorite: peerID=$peerID, currentFavorite=$isFavorite")
                onToggleFavorite()
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) EchoPalette.AccentOrange else colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChannelHeader(
    channel: String,
    onBackClick: () -> Unit,
    onLeaveChannel: () -> Unit,
    onSidebarClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EchoMetrics.HeaderHeight)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.primary
            )
        }

        Text(
            text = channel,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .clickable { onSidebarClick() }
        )

        TextButton(
            onClick = onLeaveChannel,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = "leave",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error
            )
        }
    }
}

@Composable
private fun MainHeader(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onTitleClick: () -> Unit,
    onShowStaffSheet: () -> Unit,
    onPanicClear: () -> Unit,
    onSidebarClick: () -> Unit,
    viewModel: ChatViewModel
) {
    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val hasUnreadChannels by viewModel.unreadChannelMessages.observeAsState(emptyMap())
    val hasUnreadPrivateMessages by viewModel.unreadPrivateMessages.observeAsState(emptySet())
    val isConnected by viewModel.isConnected.observeAsState(false)
    val isStaff by viewModel.isStaff.observeAsState(false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EchoMetrics.HeaderHeight)
            .padding(start = 0.dp, end = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        NicknameEditor(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp)
        )

        Text(
            text = "ECHO",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .singleOrTripleClickable(
                    onSingleClick = onTitleClick,
                    onTripleClick = onShowStaffSheet,
                    onLongClick = onPanicClear
                )
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StaffBadge(
                visible = isStaff,
                onDeactivate = { viewModel.deactivateStaff() }
            )

            PeerCounter(
                connectedPeers = connectedPeers.filter { it != viewModel.meshService.myPeerID },
                joinedChannels = joinedChannels,
                hasUnreadChannels = hasUnreadChannels,
                hasUnreadPrivateMessages = hasUnreadPrivateMessages,
                isConnected = isConnected,
                onClick = onSidebarClick
            )
        }
    }
}
