package com.bitchat.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.bitchat.android.model.PrivateChatState
import com.bitchat.android.ui.theme.EchoBrushes
import com.bitchat.android.ui.theme.EchoMetrics

/**
 * Main ChatScreen - REFACTORED to use component-based architecture
 * This is now a coordinator that orchestrates the following UI components:
 * - ChatHeader: App bar, navigation, peer counter
 * - MessageComponents: Message display and formatting
 * - InputComponents: Message input and command suggestions
 * - SidebarComponents: Navigation drawer with channels and people
 * - DialogComponents: Password prompts and modals
 * - ChatUIUtils: Utility functions for formatting and colors
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.observeAsState(emptyList())
    val nickname by viewModel.nickname.observeAsState("")
    val selectedPrivatePeer by viewModel.selectedPrivateChatPeer.observeAsState()
    val currentChannel by viewModel.currentChannel.observeAsState()
    val privateChats by viewModel.privateChats.observeAsState(emptyMap())
    val channelMessages by viewModel.channelMessages.observeAsState(emptyMap())
    val showSidebar by viewModel.showSidebar.observeAsState(false)
    val showCommandSuggestions by viewModel.showCommandSuggestions.observeAsState(false)
    val commandSuggestions by viewModel.commandSuggestions.observeAsState(emptyList())
    val showMentionSuggestions by viewModel.showMentionSuggestions.observeAsState(false)
    val mentionSuggestions by viewModel.mentionSuggestions.observeAsState(emptyList())
    val showAppInfo by viewModel.showAppInfo.observeAsState(false)
    val isStaff by viewModel.isStaff.observeAsState(false)
    val privateChatStates by viewModel.privateChatStates.observeAsState(emptyMap<String, PrivateChatState>())
    val pendingPMFrom by viewModel.pendingPrivateChatRequestFrom.observeAsState(null)
    val peerNicknames by viewModel.peerNicknames.observeAsState(emptyMap())
    val showPasswordPrompt by viewModel.showPasswordPrompt.observeAsState(false)

    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showStaffDialog by remember { mutableStateOf(false) }

    // Show password dialog when needed
    LaunchedEffect(showPasswordPrompt) {
        showPasswordDialog = showPasswordPrompt
    }

    val passwordPromptChannel by viewModel.passwordPromptChannel.observeAsState(null)

    // Determine what messages to show
    val displayMessages = when {
        selectedPrivatePeer != null -> privateChats[selectedPrivatePeer] ?: emptyList()
        currentChannel != null -> channelMessages[currentChannel] ?: emptyList()
        else -> messages
    }

    val darkTheme = isSystemInDarkTheme()

    // Use WindowInsets to handle keyboard properly
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EchoBrushes.appBackground(darkTheme))
    ) {
        val headerHeight = EchoMetrics.HeaderHeight

        // Main content area that responds to keyboard/window insets
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime) // This handles keyboard insets
        ) {
            // Header spacer - creates space for the floating header
            Spacer(modifier = Modifier.height(headerHeight))

            // Messages area - takes up available space, will compress when keyboard appears
            MessagesList(
                messages = displayMessages,
                currentUserNickname = nickname,
                meshService = viewModel.meshService,
                modifier = Modifier.weight(1f)
            )
            // Input area - stays at bottom
            val isMainTimeline = selectedPrivatePeer == null && currentChannel == null
            val timelineLocked = isMainTimeline && !isStaff
            val inputEnabled = when (val peer = selectedPrivatePeer) {
                null -> !timelineLocked
                else -> privateChatStates[peer] == PrivateChatState.ACTIVE
            }
            val readOnlyHint = when {
                timelineLocked -> "Lecture seule : triple-tapez sur \"ECHO\" et saisissez le code STAFF pour écrire."
                else -> null
            }
            ChatInputSection(
                messageText = messageText,
                onMessageTextChange = { newText: TextFieldValue ->
                    messageText = newText
                    viewModel.updateCommandSuggestions(newText.text)
                    viewModel.updateMentionSuggestions(newText.text)
                },
                onSend = {
                    if (messageText.text.trim().isNotEmpty()) {
                        viewModel.sendMessage(messageText.text.trim())
                        messageText = TextFieldValue("")
                    }
                },
                showCommandSuggestions = showCommandSuggestions,
                commandSuggestions = commandSuggestions,
                showMentionSuggestions = showMentionSuggestions,
                mentionSuggestions = mentionSuggestions,
                onCommandSuggestionClick = { suggestion: CommandSuggestion ->
                    val commandText = viewModel.selectCommandSuggestion(suggestion)
                    messageText = TextFieldValue(
                        text = commandText,
                        selection = TextRange(commandText.length)
                    )
                },
                onMentionSuggestionClick = { mention: String ->
                    val mentionText = viewModel.selectMentionSuggestion(mention, messageText.text)
                    messageText = TextFieldValue(
                        text = mentionText,
                        selection = TextRange(mentionText.length)
                    )
                },
                selectedPrivatePeer = selectedPrivatePeer,
                currentChannel = currentChannel,
                nickname = nickname,
                inputEnabled = inputEnabled,
                readOnlyHint = readOnlyHint
            )
        }

        // Floating header - positioned absolutely at top, ignores keyboard
        ChatFloatingHeader(
            headerHeight = headerHeight,
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            nickname = nickname,
            viewModel = viewModel,
            onSidebarToggle = { viewModel.showSidebar() },
            onShowAppInfo = { viewModel.showAppInfo() },
            onShowStaffSheet = { showStaffDialog = true },
            onPanicClear = { viewModel.panicClearAllData() }
        )

        val alpha by animateFloatAsState(
            targetValue = if (showSidebar) 0.5f else 0f,
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseOutCubic
            ), label = "overlayAlpha"
        )

        // Only render the background if it's visible
        if (alpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
                    .clickable { viewModel.hideSidebar() }
                    .zIndex(1f)
            )
        }

        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.zIndex(2f)
        ) {
            SidebarOverlay(
                viewModel = viewModel,
                onDismiss = { viewModel.hideSidebar() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Dialogs
    StaffCodeDialog(
        visible = showStaffDialog,
        onDismiss = { showStaffDialog = false },
        onActivate = { code -> viewModel.activateStaff(code) }
    )
    ChatDialogs(
        showPasswordDialog = showPasswordDialog,
        passwordPromptChannel = passwordPromptChannel,
        passwordInput = passwordInput,
        onPasswordChange = { passwordInput = it },
        onPasswordConfirm = {
            if (passwordInput.isNotEmpty()) {
                val success = viewModel.joinChannel(passwordPromptChannel!!, passwordInput)
                if (success) {
                    showPasswordDialog = false
                    passwordInput = ""
                    viewModel.dismissPasswordPrompt()
                }
            }
        },
        onPasswordDismiss = {
            showPasswordDialog = false
            passwordInput = ""
            viewModel.dismissPasswordPrompt()
        },
        showAppInfo = showAppInfo,
        onAppInfoDismiss = { viewModel.hideAppInfo() }
    )
    PrivateChatRequestDialog(
        show = pendingPMFrom != null,
        requesterPeerID = pendingPMFrom,
        requesterNickname = pendingPMFrom?.let { peerNicknames[it] },
        onAccept = { pendingPMFrom?.let { viewModel.acceptPrivateChatRequest(it) } },
        onDecline = { pendingPMFrom?.let { viewModel.declinePrivateChatRequest(it) } },
        onDismiss = { viewModel.clearPendingPrivateChatRequest() }
    )
}

@Composable
private fun ChatInputSection(
    messageText: TextFieldValue,
    onMessageTextChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    showCommandSuggestions: Boolean,
    commandSuggestions: List<CommandSuggestion>,
    showMentionSuggestions: Boolean,
    mentionSuggestions: List<String>,
    onCommandSuggestionClick: (CommandSuggestion) -> Unit,
    onMentionSuggestionClick: (String) -> Unit,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    inputEnabled: Boolean,
    readOnlyHint: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
            if (showMentionSuggestions && mentionSuggestions.isNotEmpty()) {
                MentionSuggestionPanel(
                    suggestions = mentionSuggestions,
                    onSuggestionClick = onMentionSuggestionClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showCommandSuggestions && commandSuggestions.isNotEmpty()) {
                CommandSuggestionPanel(
                    suggestions = commandSuggestions,
                    onSuggestionClick = onCommandSuggestionClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            MessageInput(
                value = messageText,
                onValueChange = onMessageTextChange,
                onSend = onSend,
                selectedPrivatePeer = selectedPrivatePeer,
                currentChannel = currentChannel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                inputEnabled = inputEnabled,
                readOnlyHint = readOnlyHint
            )
        }
    }
}

@Composable
private fun MentionSuggestionPanel(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column {
            suggestions.take(4).forEachIndexed { index, suggestion ->
                Text(
                    text = "@${suggestion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                if (index != minOf(3, suggestions.size - 1)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
private fun CommandSuggestionPanel(
    suggestions: List<CommandSuggestion>,
    onSuggestionClick: (CommandSuggestion) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = buildCommandLabel(suggestion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    suggestion.syntax?.let { syntax ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = syntax,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                if (index != suggestions.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }
}

private fun buildCommandLabel(suggestion: CommandSuggestion): String {
    return if (suggestion.aliases.isEmpty()) {
        suggestion.command
    } else {
        (listOf(suggestion.command) + suggestion.aliases).joinToString(separator = ", ")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatFloatingHeader(
    headerHeight: Dp,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    viewModel: ChatViewModel,
    onSidebarToggle: () -> Unit,
    onShowAppInfo: () -> Unit,
    onShowStaffSheet: () -> Unit,
    onPanicClear: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .zIndex(1f)
            .windowInsetsPadding(WindowInsets.statusBars), // Only respond to status bar
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EchoBrushes.headerBackground(darkTheme))
        ) {
            TopAppBar(
                title = {
                    ChatHeaderContent(
                        selectedPrivatePeer = selectedPrivatePeer,
                        currentChannel = currentChannel,
                        nickname = nickname,
                        viewModel = viewModel,
                        onBackClick = {
                            when {
                                selectedPrivatePeer != null -> viewModel.endPrivateChat()
                                currentChannel != null -> viewModel.switchToChannel(null)
                            }
                        },
                        onSidebarClick = onSidebarToggle,
                        onShowStaffSheet = onShowStaffSheet,
                        onPanicClear = onPanicClear,
                        onShowAppInfo = onShowAppInfo
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }

    // Divider under header
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = headerHeight)
            .zIndex(1f),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun ChatDialogs(
    showPasswordDialog: Boolean,
    passwordPromptChannel: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirm: () -> Unit,
    onPasswordDismiss: () -> Unit,
    showAppInfo: Boolean,
    onAppInfoDismiss: () -> Unit
) {
    // Password dialog
    PasswordPromptDialog(
        show = showPasswordDialog,
        channelName = passwordPromptChannel,
        passwordInput = passwordInput,
        onPasswordChange = onPasswordChange,
        onConfirm = onPasswordConfirm,
        onDismiss = onPasswordDismiss
    )

    // App info dialog
    AppInfoDialog(
        show = showAppInfo,
        onDismiss = onAppInfoDismiss
    )
}
