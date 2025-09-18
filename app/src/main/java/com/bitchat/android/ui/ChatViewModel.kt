package com.bitchat.android.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.bitchat.android.mesh.BluetoothMeshDelegate
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryAck
import com.bitchat.android.model.PeerConnectionState
import com.bitchat.android.model.PeerDisplayData
import com.bitchat.android.model.ReadReceipt
import com.bitchat.android.mesh.PeerFingerprintManager
import com.bitchat.android.identity.NostrIdentity
import com.bitchat.android.identity.NostrIdentityProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

/**
 * Refactored ChatViewModel - Main coordinator for bitchat functionality
 * Delegates specific responsibilities to specialized managers while maintaining 100% iOS compatibility
 */
class ChatViewModel(
    application: Application,
    val meshService: BluetoothMeshService
) : AndroidViewModel(application), BluetoothMeshDelegate {

    companion object {
        private const val TAG = "ChatViewModel"
        private val BECH32_CHARSET: Set<Char> = "qpzry9x8gf2tvdw0s3jn54khce6mua7l0123456789".toSet()
    }

    // State management
    private val state = ChatState()
    
    // Specialized managers
    private val dataManager = DataManager(application.applicationContext)
    private val messageManager = MessageManager(state)
    private val channelManager = ChannelManager(state, messageManager, dataManager, viewModelScope)
    
    // Create Noise session delegate for clean dependency injection
    private val noiseSessionDelegate = object : NoiseSessionDelegate {
        override fun hasEstablishedSession(peerID: String): Boolean = meshService.hasEstablishedSession(peerID)
        override fun initiateHandshake(peerID: String) = meshService.initiateNoiseHandshake(peerID) 
        override fun broadcastNoiseIdentityAnnouncement() = meshService.broadcastNoiseIdentityAnnouncement()
        override fun sendHandshakeRequest(targetPeerID: String, pendingCount: UByte) = meshService.sendHandshakeRequest(targetPeerID, pendingCount)
        override fun getMyPeerID(): String = meshService.myPeerID
    }
    
    val privateChatManager = PrivateChatManager(state, messageManager, dataManager, noiseSessionDelegate)
    private val commandProcessor = CommandProcessor(state, messageManager, channelManager, privateChatManager)
    private val notificationManager = NotificationManager(application.applicationContext)
    private val fingerprintManager = PeerFingerprintManager.getInstance()
    private val nostrIdentityProvider = NostrIdentityProvider(application.applicationContext)

    private val _detectedNpub = MutableLiveData<String?>(null)
    val detectedNpub: LiveData<String?> = _detectedNpub

    private val _filteredPeers = MutableLiveData<List<PeerDisplayData>>(emptyList())
    val filteredPeers: LiveData<List<PeerDisplayData>> = _filteredPeers

    private val connectedPeersObserver = Observer<List<String>> { refreshAllPeers() }
    private val peerNicknamesObserver = Observer<Map<String, String>> { refreshAllPeers() }
    private val peerRssiObserver = Observer<Map<String, Int>> { refreshAllPeers() }
    private val favoriteObserver = Observer<Set<String>> { refreshAllPeers() }
    private val unreadObserver = Observer<Set<String>> { refreshAllPeers() }
    private val privateChatsObserver = Observer<Map<String, List<BitchatMessage>>> { refreshAllPeers() }
    
    // Delegate handler for mesh callbacks
    private val meshDelegateHandler = MeshDelegateHandler(
        state = state,
        messageManager = messageManager,
        channelManager = channelManager,
        privateChatManager = privateChatManager,
        notificationManager = notificationManager,
        coroutineScope = viewModelScope,
        onHapticFeedback = { ChatViewModelUtils.triggerHapticFeedback(application.applicationContext) },
        getMyPeerID = { meshService.myPeerID },
        getMeshService = { meshService },
        onPeerListChanged = { refreshAllPeers() }
    )
    
    // Expose state through LiveData (maintaining the same interface)
    val messages: LiveData<List<BitchatMessage>> = state.messages
    val connectedPeers: LiveData<List<String>> = state.connectedPeers
    val nickname: LiveData<String> = state.nickname
    val isConnected: LiveData<Boolean> = state.isConnected
    val privateChats: LiveData<Map<String, List<BitchatMessage>>> = state.privateChats
    val selectedPrivateChatPeer: LiveData<String?> = state.selectedPrivateChatPeer
    val unreadPrivateMessages: LiveData<Set<String>> = state.unreadPrivateMessages
    val privateChatStates: LiveData<Map<String, com.bitchat.android.model.PrivateChatState>> = state.privateChatStates
    val pendingPrivateChatRequestFrom: LiveData<String?> = state.pendingPrivateChatRequestFrom
    val joinedChannels: LiveData<Set<String>> = state.joinedChannels
    val currentChannel: LiveData<String?> = state.currentChannel
    val channelMessages: LiveData<Map<String, List<BitchatMessage>>> = state.channelMessages
    val unreadChannelMessages: LiveData<Map<String, Int>> = state.unreadChannelMessages
    val passwordProtectedChannels: LiveData<Set<String>> = state.passwordProtectedChannels
    val showPasswordPrompt: LiveData<Boolean> = state.showPasswordPrompt
    val passwordPromptChannel: LiveData<String?> = state.passwordPromptChannel
    val showSidebar: LiveData<Boolean> = state.showSidebar
    val hasUnreadChannels = state.hasUnreadChannels
    val hasUnreadPrivateMessages = state.hasUnreadPrivateMessages
    val showCommandSuggestions: LiveData<Boolean> = state.showCommandSuggestions
    val commandSuggestions: LiveData<List<CommandSuggestion>> = state.commandSuggestions
    val showMentionSuggestions: LiveData<Boolean> = state.showMentionSuggestions
    val mentionSuggestions: LiveData<List<String>> = state.mentionSuggestions
    val favoritePeers: LiveData<Set<String>> = state.favoritePeers
    val peerSessionStates: LiveData<Map<String, String>> = state.peerSessionStates
    val peerFingerprints: LiveData<Map<String, String>> = state.peerFingerprints
    val peerNicknames: LiveData<Map<String, String>> = state.peerNicknames
    val peerRSSI: LiveData<Map<String, Int>> = state.peerRSSI
    val allPeers: LiveData<List<PeerDisplayData>> = state.allPeers
    val peopleSearchQuery: LiveData<String> = state.peopleSearchQuery
    val showAppInfo: LiveData<Boolean> = state.showAppInfo
    val isStaff: LiveData<Boolean> = state.isStaff
    
    init {
        // Note: Mesh service delegate is now set by MainActivity
        loadAndInitialize()
        state.connectedPeers.observeForever(connectedPeersObserver)
        state.peerNicknames.observeForever(peerNicknamesObserver)
        state.peerRSSI.observeForever(peerRssiObserver)
        state.favoritePeers.observeForever(favoriteObserver)
        state.unreadPrivateMessages.observeForever(unreadObserver)
        state.privateChats.observeForever(privateChatsObserver)
        refreshAllPeers()
    }
    
    private fun loadAndInitialize() {
        // Load nickname
        val nickname = dataManager.loadNickname()
        state.setNickname(nickname)

        // Load staff flag
        state.setIsStaff(dataManager.isStaff())
        
        // Load data
        val (joinedChannels, protectedChannels) = channelManager.loadChannelData()
        state.setJoinedChannels(joinedChannels)
        state.setPasswordProtectedChannels(protectedChannels)
        
        // Initialize channel messages
        joinedChannels.forEach { channel ->
            if (!state.getChannelMessagesValue().containsKey(channel)) {
                val updatedChannelMessages = state.getChannelMessagesValue().toMutableMap()
                updatedChannelMessages[channel] = emptyList()
                state.setChannelMessages(updatedChannelMessages)
            }
        }
        
        // Load other data
        dataManager.loadFavorites()
        state.setFavoritePeers(dataManager.favoritePeers.toSet())
        dataManager.loadBlockedUsers()
        
        // Log all favorites at startup
        dataManager.logAllFavorites()
        logCurrentFavoriteState()
        
        // Initialize session state monitoring
        initializeSessionStateMonitoring()
        
        // Note: Mesh service is now started by MainActivity
        
        // Show welcome message if no peers after delay
        viewModelScope.launch {
            delay(10000)
            if (state.getConnectedPeersValue().isEmpty() && state.getMessagesValue().isEmpty()) {
                val welcomeMessage = BitchatMessage(
                    sender = "system",
                    content = "get people around you to download bitchat and chat with them here!",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(welcomeMessage)
            }
        }

        refreshAllPeers()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Note: Mesh service lifecycle is now managed by MainActivity
        state.connectedPeers.removeObserver(connectedPeersObserver)
        state.peerNicknames.removeObserver(peerNicknamesObserver)
        state.peerRSSI.removeObserver(peerRssiObserver)
        state.favoritePeers.removeObserver(favoriteObserver)
        state.unreadPrivateMessages.removeObserver(unreadObserver)
        state.privateChats.removeObserver(privateChatsObserver)
    }
    
    // MARK: - Nickname Management
    
    fun setNickname(newNickname: String) {
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)
        meshService.sendBroadcastAnnounce()
    }
    
    // MARK: - Channel Management (delegated)
    
    fun joinChannel(channel: String, password: String? = null): Boolean {
        return channelManager.joinChannel(channel, password, meshService.myPeerID)
    }

    fun dismissPasswordPrompt() {
        channelManager.hidePasswordPrompt()
    }

    fun switchToChannel(channel: String?) {
        channelManager.switchToChannel(channel)
    }
    
    fun leaveChannel(channel: String) {
        channelManager.leaveChannel(channel)
        meshService.sendMessage("left $channel")
    }

    // MARK: - Staff access management

    fun activateStaff(code: String): Boolean {
        val activated = dataManager.activateStaff(code)
        if (activated) {
            state.setIsStaff(true)
        }
        return activated
    }

    fun deactivateStaff() {
        dataManager.deactivateStaff()
        state.setIsStaff(false)
    }
    
    // MARK: - Sidebar & Peer Directory

    fun setPeopleSearchQuery(query: String) {
        val sanitized = query.replace("\n", " ")
        state.setPeopleSearchQuery(sanitized)
        detectNpubFromQuery(sanitized)
        applyPeerFilter(query = sanitized)
    }

    fun getCurrentNostrIdentity(): NostrIdentity = nostrIdentityProvider.getOrCreateIdentity()

    fun getCurrentNostrNpub(): String = getCurrentNostrIdentity().npub

    fun trySetNostrNpub(candidate: String): Boolean {
        val normalized = extractNpub(candidate.trim()) ?: return false
        nostrIdentityProvider.setNpub(normalized)
        return true
    }

    fun clearNostrNpub() {
        nostrIdentityProvider.clearNpub()
    }

    private fun detectNpubFromQuery(rawQuery: String) {
        val candidate = rawQuery.trim()
        _detectedNpub.value = extractNpub(candidate)
    }

    private fun applyPeerFilter(
        peers: List<PeerDisplayData> = state.getAllPeersValue(),
        query: String = state.getPeopleSearchQueryValue()
    ) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            peers
        } else {
            peers.filter { it.displayName.contains(trimmed, ignoreCase = true) }
        }
        _filteredPeers.value = filtered
    }

    private fun refreshAllPeers() {
        val connected = state.getConnectedPeersValue()
        val nicknames = state.getPeerNicknamesValue()
        val rssiValues = state.getPeerRSSIValue()
        val unreadPrivate = state.getUnreadPrivateMessagesValue()
        val privateChatsSnapshots = state.getPrivateChatsValue()
        val favoriteFingerprints = state.getFavoritePeersValue()
        val myPeerID = meshService.myPeerID
        val myNickname = state.getNicknameValue() ?: myPeerID
        val fingerprintMappings = fingerprintManager.getAllPeerFingerprints()

        val peers = mutableListOf<PeerDisplayData>()
        val seenKeys = mutableSetOf<String>()

        connected.forEach { peerID ->
            val key = peerID.ifEmpty { return@forEach }
            if (!seenKeys.add(key)) return@forEach

            val fingerprint = fingerprintMappings[peerID]
            val isMe = peerID == myPeerID
            val displayName = when {
                isMe -> myNickname
                else -> nicknames[peerID] ?: peerID
            }
            val lastMessageTime = privateChatsSnapshots[peerID]?.maxOfOrNull { it.timestamp.time }
            val isFavorite = fingerprint?.let { favoriteFingerprints.contains(it) } ?: false
            val connectionState = when {
                isMe -> PeerConnectionState.MESH_CONNECTED
                isFavorite && !connected.contains(peerID) -> PeerConnectionState.NOSTR_AVAILABLE
                else -> PeerConnectionState.MESH_CONNECTED
            }

            peers += PeerDisplayData(
                peerId = peerID,
                fingerprint = fingerprint,
                displayName = displayName,
                isMe = isMe,
                isFavorite = isFavorite,
                isMutualFavorite = isFavorite,
                hasUnreadMessages = unreadPrivate.contains(peerID),
                connectionState = connectionState,
                signalStrength = convertRssiToSignalStrength(rssiValues[peerID]),
                lastSeenEpochMillis = lastMessageTime
            )
        }

        if (seenKeys.add(myPeerID)) {
            peers += PeerDisplayData(
                peerId = myPeerID,
                fingerprint = fingerprintMappings[myPeerID],
                displayName = myNickname,
                isMe = true,
                isFavorite = false,
                isMutualFavorite = false,
                hasUnreadMessages = false,
                connectionState = PeerConnectionState.MESH_CONNECTED,
                signalStrength = convertRssiToSignalStrength(rssiValues[myPeerID]),
                lastSeenEpochMillis = null
            )
        }

        favoriteFingerprints.forEach { fingerprint ->
            if (fingerprint.isBlank()) return@forEach
            val alreadyRepresented = peers.any { it.fingerprint == fingerprint }
            if (alreadyRepresented) return@forEach
            val key = "fav_$fingerprint"
            if (!seenKeys.add(key)) return@forEach

            val mappedPeerID = fingerprintManager.getPeerIDForFingerprint(fingerprint)
            val isConnected = mappedPeerID != null && connected.contains(mappedPeerID)
            val displayName = when {
                mappedPeerID != null && mappedPeerID == myPeerID -> myNickname
                mappedPeerID != null -> nicknames[mappedPeerID] ?: mappedPeerID
                else -> fingerprint.take(12)
            }
            val lastMessageTime = mappedPeerID?.let { privateChatsSnapshots[it]?.maxOfOrNull { msg -> msg.timestamp.time } }
            val connectionState = when {
                isConnected -> PeerConnectionState.MESH_CONNECTED
                else -> PeerConnectionState.NOSTR_AVAILABLE
            }

            peers += PeerDisplayData(
                peerId = mappedPeerID,
                fingerprint = fingerprint,
                displayName = displayName,
                isMe = mappedPeerID == myPeerID,
                isFavorite = true,
                isMutualFavorite = true,
                hasUnreadMessages = mappedPeerID?.let { unreadPrivate.contains(it) } ?: false,
                connectionState = connectionState,
                signalStrength = convertRssiToSignalStrength(mappedPeerID?.let { rssiValues[it] }),
                lastSeenEpochMillis = lastMessageTime
            )
        }

        val distinctPeers = peers.distinctBy { it.peerId ?: it.fingerprint }
        val sorted = distinctPeers.sortedWith(
            compareByDescending<PeerDisplayData> { it.hasUnreadMessages }
                .thenByDescending { it.lastSeenEpochMillis ?: 0L }
                .thenByDescending { it.isFavorite }
                .thenBy { it.displayName.lowercase() }
        )

        state.setAllPeers(sorted)
        applyPeerFilter(sorted, state.getPeopleSearchQueryValue())
    }

    private fun convertRssiToSignalStrength(rssi: Int?): Int {
        val value = rssi ?: return 0
        return when {
            value >= -60 -> 3
            value >= -75 -> 2
            value >= -90 -> 1
            else -> 0
        }
    }

    private fun extractNpub(input: String): String? {
        if (input.isEmpty()) return null
        var candidate = input
        if (candidate.startsWith("nostr:", ignoreCase = true)) {
            candidate = candidate.substring(6)
        }
        val lower = candidate.lowercase()
        if (!lower.startsWith("npub")) return null
        if (candidate.length !in 20..120) return null
        return if (candidate.all { BECH32_CHARSET.contains(it.lowercaseChar()) }) candidate else null
    }

    // MARK: - Private Chat Management (delegated)
    
    fun startPrivateChat(peerID: String) {
        val currentState = state.getPrivateChatStatesValue()[peerID] ?: com.bitchat.android.model.PrivateChatState.NONE
        if (currentState != com.bitchat.android.model.PrivateChatState.ACTIVE) {
            requestPrivateChat(peerID)
            return
        }
        val success = privateChatManager.startPrivateChat(peerID, meshService)
        if (success) {
            setCurrentPrivateChatPeer(peerID)
            clearNotificationsForSender(peerID)
        }
    }

    fun requestPrivateChat(peerID: String) {
        val current = state.getPrivateChatStatesValue()[peerID]
        if (current == com.bitchat.android.model.PrivateChatState.ACTIVE || current == com.bitchat.android.model.PrivateChatState.REQUEST_SENT) return
        if (peerID == meshService.myPeerID) return

        // Ensure Noise session is initiated before sending PM control message
        if (!meshService.hasEstablishedSession(peerID)) {
            val myPeerID = meshService.myPeerID
            if (myPeerID < peerID) {
                // We initiate the handshake
                meshService.initiateNoiseHandshake(peerID)
            } else {
                // Prompt the other side to initiate
                meshService.broadcastNoiseIdentityAnnouncement()
                meshService.sendHandshakeRequest(peerID, 1u)
            }
        }

        state.setPrivateChatState(peerID, com.bitchat.android.model.PrivateChatState.REQUEST_SENT)
        val peerName = meshService.getPeerNicknames()[peerID] ?: peerID
        val sysMsg = BitchatMessage(
            sender = "system",
            content = "sent private chat request to $peerName",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(sysMsg)
        meshService.sendPrivateChatRequest(peerID)
    }

    fun acceptPrivateChatRequest(fromPeerID: String) {
        val current = state.getPrivateChatStatesValue()[fromPeerID]
        if (current != com.bitchat.android.model.PrivateChatState.REQUEST_RECEIVED) return

        // Ensure Noise session is initiated so the accept control message can be delivered
        if (!meshService.hasEstablishedSession(fromPeerID)) {
            val myPeerID = meshService.myPeerID
            if (myPeerID < fromPeerID) {
                meshService.initiateNoiseHandshake(fromPeerID)
            } else {
                meshService.broadcastNoiseIdentityAnnouncement()
                meshService.sendHandshakeRequest(fromPeerID, 1u)
            }
        }

        state.setPrivateChatState(fromPeerID, com.bitchat.android.model.PrivateChatState.ACTIVE)
        state.setPendingPrivateChatRequestFrom(null)
        meshService.sendPrivateChatResponse(fromPeerID, true)
        privateChatManager.startPrivateChat(fromPeerID, meshService)
    }

    fun declinePrivateChatRequest(fromPeerID: String) {
        val current = state.getPrivateChatStatesValue()[fromPeerID]
        if (current != com.bitchat.android.model.PrivateChatState.REQUEST_RECEIVED) return

        // Ensure Noise session is initiated so the decline control message can be delivered
        if (!meshService.hasEstablishedSession(fromPeerID)) {
            val myPeerID = meshService.myPeerID
            if (myPeerID < fromPeerID) {
                meshService.initiateNoiseHandshake(fromPeerID)
            } else {
                meshService.broadcastNoiseIdentityAnnouncement()
                meshService.sendHandshakeRequest(fromPeerID, 1u)
            }
        }

        state.setPrivateChatState(fromPeerID, com.bitchat.android.model.PrivateChatState.REJECTED)
        state.setPendingPrivateChatRequestFrom(null)
        meshService.sendPrivateChatResponse(fromPeerID, false)

        val peerName = meshService.getPeerNicknames()[fromPeerID] ?: fromPeerID
        val sysMsg = BitchatMessage(
            sender = "system",
            content = "declined private chat request from $peerName",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(sysMsg)
    }

    fun handleIncomingPrivateChatRequest(fromPeerID: String) {
        val current = state.getPrivateChatStatesValue()[fromPeerID]
        if (current == com.bitchat.android.model.PrivateChatState.ACTIVE) return
        state.setPrivateChatState(fromPeerID, com.bitchat.android.model.PrivateChatState.REQUEST_RECEIVED)
        state.setPendingPrivateChatRequestFrom(fromPeerID)
    }

    fun clearPendingPrivateChatRequest() {
        state.setPendingPrivateChatRequestFrom(null)
    }
    
    fun endPrivateChat() {
        privateChatManager.endPrivateChat()
        // Notify notification manager that no private chat is active
        setCurrentPrivateChatPeer(null)
    }
    
    // MARK: - Message Sending
    
    fun sendMessage(content: String) {
        if (content.isEmpty()) return
        
        // Check for commands
        if (content.startsWith("/")) {
            commandProcessor.processCommand(content, meshService, meshService.myPeerID) { messageContent, mentions, channel ->
                meshService.sendMessage(messageContent, mentions, channel)
            }
            return
        }
        
        val mentions = messageManager.parseMentions(content, meshService.getPeerNicknames().values.toSet(), state.getNicknameValue())
        val channels = messageManager.parseChannels(content)
        
        // Auto-join mentioned channels
        channels.forEach { channel ->
            if (!state.getJoinedChannelsValue().contains(channel)) {
                joinChannel(channel)
            }
        }
        
        val selectedPeer = state.getSelectedPrivateChatPeerValue()
        val currentChannelValue = state.getCurrentChannelValue()

        if (!state.getIsStaffValue() && selectedPeer == null && currentChannelValue == null) {
            Log.d(TAG, "sendMessage blocked: staff access required for main timeline")
            return
        }
        
        if (selectedPeer != null) {
            val pmState = state.getPrivateChatStatesValue()[selectedPeer]
            if (pmState != com.bitchat.android.model.PrivateChatState.ACTIVE) {
                return
            }
            // Send private message
            val recipientNickname = meshService.getPeerNicknames()[selectedPeer]
            privateChatManager.sendPrivateMessage(
                content, 
                selectedPeer, 
                recipientNickname,
                state.getNicknameValue(),
                meshService.myPeerID
            ) { messageContent, peerID, recipientNicknameParam, messageId ->
                meshService.sendPrivateMessage(messageContent, peerID, recipientNicknameParam, messageId)
            }
        } else {
            // Send public/channel message
            val message = BitchatMessage(
                sender = state.getNicknameValue() ?: meshService.myPeerID,
                content = content,
                timestamp = Date(),
                isRelay = false,
                senderPeerID = meshService.myPeerID,
                mentions = if (mentions.isNotEmpty()) mentions else null,
                channel = currentChannelValue
            )
            
            if (currentChannelValue != null) {
                channelManager.addChannelMessage(currentChannelValue, message, meshService.myPeerID)
                
                // Check if encrypted channel
                if (channelManager.hasChannelKey(currentChannelValue)) {
                    channelManager.sendEncryptedChannelMessage(
                        content, 
                        mentions, 
                        currentChannelValue, 
                        state.getNicknameValue(),
                        meshService.myPeerID,
                        onEncryptedPayload = { encryptedData ->
                            // This would need proper mesh service integration
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        },
                        onFallback = {
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        }
                    )
                } else {
                    meshService.sendMessage(content, mentions, currentChannelValue)
                }
            } else {
                messageManager.addMessage(message)
                meshService.sendMessage(content, mentions, null)
            }
        }
    }
    
    // MARK: - Utility Functions
    
    fun getPeerIDForNickname(nickname: String): String? {
        return meshService.getPeerNicknames().entries.find { it.value == nickname }?.key
    }
    
    fun toggleFavorite(peerID: String) {
        Log.d("ChatViewModel", "toggleFavorite called for peerID: $peerID")
        privateChatManager.toggleFavorite(peerID)
        
        // Log current state after toggle
        logCurrentFavoriteState()
        refreshAllPeers()
    }
    
    private fun logCurrentFavoriteState() {
        Log.i("ChatViewModel", "=== CURRENT FAVORITE STATE ===")
        Log.i("ChatViewModel", "LiveData favorite peers: ${favoritePeers.value}")
        Log.i("ChatViewModel", "DataManager favorite peers: ${dataManager.favoritePeers}")
        Log.i("ChatViewModel", "Peer fingerprints: ${privateChatManager.getAllPeerFingerprints()}")
        Log.i("ChatViewModel", "==============================")
    }
    
    /**
     * Initialize session state monitoring for reactive UI updates
     */
    private fun initializeSessionStateMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Check session states every second
                updateReactiveStates()
            }
        }
    }
    
    /**
     * Update reactive states for all connected peers (session states, fingerprints, nicknames, RSSI)
     */
    private fun updateReactiveStates() {
        val currentPeers = state.getConnectedPeersValue()
        
        // Update session states
        val sessionStates = currentPeers.associateWith { peerID ->
            meshService.getSessionState(peerID).toString()
        }
        state.setPeerSessionStates(sessionStates)
        // Update fingerprint mappings from centralized manager
        val fingerprints = privateChatManager.getAllPeerFingerprints()
        state.setPeerFingerprints(fingerprints)

        val nicknames = meshService.getPeerNicknames()
        state.setPeerNicknames(nicknames)

        val rssiValues = meshService.getPeerRSSI()
        state.setPeerRSSI(rssiValues)
    }
    
    // MARK: - Debug and Troubleshooting
    
    fun getDebugStatus(): String {
        return meshService.getDebugStatus()
    }
    
    // Note: Mesh service restart is now handled by MainActivity
    // This function is no longer needed
    
    fun setAppBackgroundState(inBackground: Boolean) {
        // Forward to notification manager for notification logic
        notificationManager.setAppBackgroundState(inBackground)
    }
    
    fun setCurrentPrivateChatPeer(peerID: String?) {
        // Update notification manager with current private chat peer
        notificationManager.setCurrentPrivateChatPeer(peerID)
    }
    
    fun clearNotificationsForSender(peerID: String) {
        // Clear notifications when user opens a chat
        notificationManager.clearNotificationsForSender(peerID)
    }
    
    // MARK: - Command Autocomplete (delegated)
    
    fun updateCommandSuggestions(input: String) {
        commandProcessor.updateCommandSuggestions(input)
    }
    
    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        return commandProcessor.selectCommandSuggestion(suggestion)
    }
    
    // MARK: - Mention Autocomplete
    
    fun updateMentionSuggestions(input: String) {
        commandProcessor.updateMentionSuggestions(input, meshService)
    }
    
    fun selectMentionSuggestion(nickname: String, currentText: String): String {
        return commandProcessor.selectMentionSuggestion(nickname, currentText)
    }
    
    // MARK: - BluetoothMeshDelegate Implementation (delegated)
    
    override fun didReceiveMessage(message: BitchatMessage) {
        meshDelegateHandler.didReceiveMessage(message)
    }
    
    override fun didUpdatePeerList(peers: List<String>) {
        meshDelegateHandler.didUpdatePeerList(peers)
    }
    
    override fun didReceiveChannelLeave(channel: String, fromPeer: String) {
        meshDelegateHandler.didReceiveChannelLeave(channel, fromPeer)
    }
    
    override fun didReceiveDeliveryAck(ack: DeliveryAck) {
        meshDelegateHandler.didReceiveDeliveryAck(ack)
    }
    
    override fun didReceiveReadReceipt(receipt: ReadReceipt) {
        meshDelegateHandler.didReceiveReadReceipt(receipt)
    }

    override fun didReceivePrivateChatRequest(fromPeerID: String) {
        meshDelegateHandler.didReceivePrivateChatRequest(fromPeerID)
    }

    override fun didReceivePrivateChatResponse(fromPeerID: String, accepted: Boolean) {
        meshDelegateHandler.didReceivePrivateChatResponse(fromPeerID, accepted)
    }
    
    override fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String? {
        return meshDelegateHandler.decryptChannelMessage(encryptedContent, channel)
    }
    
    override fun getNickname(): String? {
        return meshDelegateHandler.getNickname()
    }
    
    override fun isFavorite(peerID: String): Boolean {
        return meshDelegateHandler.isFavorite(peerID)
    }
    
    // registerPeerPublicKey REMOVED - fingerprints now handled centrally in PeerManager
    
    // MARK: - Emergency Clear
    
    fun panicClearAllData() {
        Log.w(TAG, "🚨 PANIC MODE ACTIVATED - Clearing all sensitive data")
        
        // Clear all UI managers
        messageManager.clearAllMessages()
        channelManager.clearAllChannels()
        privateChatManager.clearAllPrivateChats()
        dataManager.clearAllData()
        
        // Clear all mesh service data
        clearAllMeshServiceData()
        
        // Clear all cryptographic data
        clearAllCryptographicData()
        
        // Clear all notifications
        notificationManager.clearAllNotifications()
        
        // Reset nickname
        val newNickname = "anon${Random.nextInt(1000, 9999)}"
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)
        
        Log.w(TAG, "🚨 PANIC MODE COMPLETED - All sensitive data cleared")
        
        // Note: Mesh service restart is now handled by MainActivity
        // This method now only clears data, not mesh service lifecycle
    }
    
    /**
     * Clear all mesh service related data
     */
    private fun clearAllMeshServiceData() {
        try {
            // Request mesh service to clear all its internal data
            meshService.clearAllInternalData()
            
            Log.d(TAG, "✅ Cleared all mesh service data")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing mesh service data: ${e.message}")
        }
    }
    
    /**
     * Clear all cryptographic data including persistent identity
     */
    private fun clearAllCryptographicData() {
        try {
            // Clear encryption service persistent identity (Ed25519 signing keys)
            meshService.clearAllEncryptionData()
            
            // Clear secure identity state (if used)
            try {
                val identityManager = com.bitchat.android.identity.SecureIdentityStateManager(getApplication())
                identityManager.clearIdentityData()
                Log.d(TAG, "✅ Cleared secure identity state")
            } catch (e: Exception) {
                Log.d(TAG, "SecureIdentityStateManager not available or already cleared: ${e.message}")
            }
            
            Log.d(TAG, "✅ Cleared all cryptographic data")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing cryptographic data: ${e.message}")
        }
    }
    
    // MARK: - Navigation Management
    
    fun showAppInfo() {
        state.setShowAppInfo(true)
    }
    
    fun hideAppInfo() {
        state.setShowAppInfo(false)
    }
    
    fun showSidebar() {
        state.setShowSidebar(true)
    }
    
    fun hideSidebar() {
        state.setShowSidebar(false)
    }
    
    /**
     * Handle Android back navigation
     * Returns true if the back press was handled, false if it should be passed to the system
     */
    fun handleBackPressed(): Boolean {
        return when {
            // Close app info dialog
            state.getShowAppInfoValue() -> {
                hideAppInfo()
                true
            }
            // Close sidebar
            state.getShowSidebarValue() -> {
                hideSidebar()
                true
            }
            // Close password dialog
            state.getShowPasswordPromptValue() -> {
                state.setShowPasswordPrompt(false)
                state.setPasswordPromptChannel(null)
                true
            }
            // Exit private chat
            state.getSelectedPrivateChatPeerValue() != null -> {
                endPrivateChat()
                true
            }
            // Exit channel view
            state.getCurrentChannelValue() != null -> {
                switchToChannel(null)
                true
            }
            // No special navigation state - let system handle (usually exits app)
            else -> false
        }
    }
}
