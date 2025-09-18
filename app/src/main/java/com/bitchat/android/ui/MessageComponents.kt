package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.ui.theme.EchoBrushes
import com.bitchat.android.ui.theme.EchoMetrics
import com.bitchat.android.ui.theme.EchoPalette

/**
 * Message display components for ChatScreen.
 * Mirrors the bubble based UI from the iOS implementation while preserving mesh optimisations.
 */
@Composable
fun MessagesList(
    messages: List<BitchatMessage>,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val darkTheme = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .background(EchoBrushes.chatBackground(darkTheme))
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                val previous = messages.getOrNull(index - 1)
                val topPadding = if (previous == null) 0.dp else if (previous.sender == message.sender) 4.dp else 14.dp
                Spacer(modifier = Modifier.height(topPadding))

                when {
                    message.sender.equals("system", ignoreCase = true) -> {
                        SystemMessageRow(message)
                    }
                    else -> {
                        ChatMessageRow(
                            message = message,
                            currentUserNickname = currentUserNickname,
                            meshService = meshService,
                            previousMessage = previous
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: BitchatMessage,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    previousMessage: BitchatMessage?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * EchoMetrics.BubbleWidthRatio
        val isMine = message.senderPeerID == meshService.myPeerID ||
            message.sender.equals(currentUserNickname, ignoreCase = true)
        val showAvatar = !isMine && (previousMessage == null || previousMessage.sender != message.sender)
        val showSenderName = showAvatar
        val displayName = if (isMine) currentUserNickname else message.sender
        val bubbleBrush: Brush = if (isMine) EchoBrushes.outgoingBubble() else EchoBrushes.incomingBubble()
        val textColor = Color.White

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine) {
                if (showAvatar) {
                    PeerAvatar(name = displayName)
                } else {
                    Spacer(modifier = Modifier.width(EchoMetrics.AvatarSize))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                if (showSenderName) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Surface(
                    shape = RoundedCornerShape(EchoMetrics.BubbleCorner),
                    shadowElevation = if (isMine) 2.dp else 3.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(bubbleBrush)
                            .padding(
                                horizontal = EchoMetrics.BubblePaddingHorizontal,
                                vertical = EchoMetrics.BubblePaddingVertical
                            )
                    ) {
                        SelectionContainer {
                            Text(
                                text = remember(message.id, message.content, message.mentions) {
                                    formatMessageContent(message, currentUserNickname)
                                },
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                                softWrap = true
                            )
                        }
                    }
                }

                if (message.isPrivate && isMine && message.deliveryStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DeliveryStatusLabel(status = message.deliveryStatus!!)
                }
            }

            if (isMine) {
                Spacer(modifier = Modifier.width(8.dp))
                Spacer(modifier = Modifier.width(EchoMetrics.AvatarSize))
            }
        }
    }
}

@Composable
private fun DeliveryStatusLabel(status: DeliveryStatus) {
    val (label, color) = when (status) {
        is DeliveryStatus.Sending -> "Sending…" to MaterialTheme.colorScheme.onSurfaceVariant
        is DeliveryStatus.Sent -> "Sent" to MaterialTheme.colorScheme.onSurfaceVariant
        is DeliveryStatus.Delivered -> "Delivered" to MaterialTheme.colorScheme.secondary
        is DeliveryStatus.Read -> "Read" to EchoPalette.BrandBlue
        is DeliveryStatus.Failed -> status.getDisplayText() to EchoPalette.AccentRed
        is DeliveryStatus.PartiallyDelivered -> status.getDisplayText() to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun SystemMessageRow(message: BitchatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = remember(message.id, message.content) {
                    formatSystemMessage(message.content)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PeerAvatar(name: String) {
    val initials = remember(name) {
        name.trim().takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
    }
    Box(
        modifier = Modifier
            .size(EchoMetrics.AvatarSize)
            .clip(CircleShape)
            .background(EchoPalette.BrandBlueDeep.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
