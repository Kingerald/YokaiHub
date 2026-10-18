package com.yokaihub.agent.domain

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yokaihub.agent.data.local.SecureVault
import com.yokaihub.agent.data.models.ApiProviderConfig
import com.yokaihub.agent.data.models.ChatMessage
import com.yokaihub.agent.data.models.ConnectionVerificationState
import com.yokaihub.agent.data.models.GitHubAccountState
import com.yokaihub.agent.data.models.GitHubArtifactItem
import com.yokaihub.agent.data.models.GitHubDeviceCodeSession
import com.yokaihub.agent.data.models.GitHubFileCommitProposal
import com.yokaihub.agent.data.models.GitHubFileNode
import com.yokaihub.agent.data.models.GitHubOAuthAppCredentials
import com.yokaihub.agent.data.models.GitHubRepoItem
import com.yokaihub.agent.data.models.GitHubWorkflowRun
import com.yokaihub.agent.data.models.ProtocolAdapter
import com.yokaihub.agent.data.network.ByokAiClient
import com.yokaihub.agent.data.network.GitHubPluginClient
import com.yokaihub.agent.data.network.IpDetectorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class YokaiViewModel(application: Application) : AndroidViewModel(application) {

    private val vault = SecureVault(application)
    private val ipClient = IpDetectorClient()
    private val aiClient = ByokAiClient()
    private val githubClient = GitHubPluginClient()

    // Live Outbound IPv4 State (For AgentRouter IP Whitelist)
    private val _outboundIp = MutableStateFlow("Detecting IP...")
    val outboundIp: StateFlow<String> = _outboundIp.asStateFlow()

    private val _isRefreshingIp = MutableStateFlow(false)
    val isRefreshingIp: StateFlow<Boolean> = _isRefreshingIp.asStateFlow()

    // Strict Zero-Preset BYOK Providers State
    private val _providers = MutableStateFlow<List<ApiProviderConfig>>(emptyList())
    val providers: StateFlow<List<ApiProviderConfig>> = _providers.asStateFlow()

    private val _activeProviderId = MutableStateFlow<String?>(null)
    val activeProviderId: StateFlow<String?> = _activeProviderId.asStateFlow()

    // Verify-Before-Save Handshake State
    private val _verificationState = MutableStateFlow(ConnectionVerificationState())
    val verificationState: StateFlow<ConnectionVerificationState> = _verificationState.asStateFlow()

    // Official GitHub OAuth 2.0 & Autonomous Repo State
    private val _githubAccount = MutableStateFlow(GitHubAccountState())
    val githubAccount: StateFlow<GitHubAccountState> = _githubAccount.asStateFlow()

    private val _githubOAuthCreds = MutableStateFlow(GitHubOAuthAppCredentials())
    val githubOAuthCreds: StateFlow<GitHubOAuthAppCredentials> = _githubOAuthCreds.asStateFlow()

    private val _deviceCodeSession = MutableStateFlow<GitHubDeviceCodeSession?>(null)
    val deviceCodeSession: StateFlow<GitHubDeviceCodeSession?> = _deviceCodeSession.asStateFlow()

    private val _githubStatusBanner = MutableStateFlow("")
    val githubStatusBanner: StateFlow<String> = _githubStatusBanner.asStateFlow()

    private val _isGitHubLoading = MutableStateFlow(false)
    val isGitHubLoading: StateFlow<Boolean> = _isGitHubLoading.asStateFlow()

    private val _repositories = MutableStateFlow<List<GitHubRepoItem>>(emptyList())
    val repositories: StateFlow<List<GitHubRepoItem>> = _repositories.asStateFlow()

    private val _repoFiles = MutableStateFlow<List<GitHubFileNode>>(emptyList())
    val repoFiles: StateFlow<List<GitHubFileNode>> = _repoFiles.asStateFlow()

    private val _workflowRuns = MutableStateFlow<List<GitHubWorkflowRun>>(emptyList())
    val workflowRuns: StateFlow<List<GitHubWorkflowRun>> = _workflowRuns.asStateFlow()

    private val _artifacts = MutableStateFlow<List<GitHubArtifactItem>>(emptyList())
    val artifacts: StateFlow<List<GitHubArtifactItem>> = _artifacts.asStateFlow()

    private val _webReaderEnabled = MutableStateFlow(true)
    val webReaderEnabled: StateFlow<Boolean> = _webReaderEnabled.asStateFlow()

    // Multi-Key Chat & Autonomous Workspace State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    init {
        _providers.value = vault.getConnectedProviders()
        _activeProviderId.value = vault.getActiveProviderId() ?: _providers.value.firstOrNull()?.id
        _githubAccount.value = vault.getGitHubAccount()
        _githubOAuthCreds.value = vault.getGitHubOAuthCredentials()
        _chatMessages.value = vault.getChatMessages()
        _webReaderEnabled.value = vault.isWebReaderPluginEnabled()

        refreshOutboundIp()

        if (_githubAccount.value.isConnected && _githubAccount.value.accessToken.isNotBlank()) {
            refreshGitHubWorkspace()
        }
    }

    fun refreshOutboundIp() {
        viewModelScope.launch {
            _isRefreshingIp.value = true
            ipClient.fetchOutboundIpv4()
                .onSuccess { ip -> _outboundIp.value = ip }
                .onFailure { _outboundIp.value = "IP Unavailable" }
            _isRefreshingIp.value = false
        }
    }

    // --- BYOK Provider Setup & Verify-Before-Save Handshake ---

    fun resetVerificationState() {
        _verificationState.value = ConnectionVerificationState(
            isTesting = false,
            isVerified = false,
            statusMessage = "Tap 'Test Connection' to unlock Save Provider"
        )
    }

    fun testProviderConnection(
        protocol: ProtocolAdapter,
        baseUrl: String,
        apiKey: String,
        modelId: String
    ) {
        if (baseUrl.isBlank() || apiKey.isBlank() || modelId.isBlank()) {
            _verificationState.value = ConnectionVerificationState(
                isTesting = false,
                isVerified = false,
                statusMessage = "Base URL, API Key, and Model ID are all required."
            )
            return
        }

        viewModelScope.launch {
            _verificationState.value = ConnectionVerificationState(
                isTesting = true,
                isVerified = false,
                statusMessage = "Dispatching 1-token handshake via ${protocol.name}..."
            )
            val result = aiClient.testConnection(
                protocol = protocol,
                baseUrl = baseUrl,
                apiKey = apiKey,
                modelId = modelId
            )
            _verificationState.value = result
        }
    }

    fun saveVerifiedProvider(
        label: String,
        protocol: ProtocolAdapter,
        baseUrl: String,
        apiKey: String,
        modelId: String,
        onSaved: () -> Unit
    ) {
        if (!_verificationState.value.isVerified) return

        val cleanLabel = label.trim().ifBlank { "${protocol.name} • ${modelId.trim()}" }
        val newProvider = ApiProviderConfig(
            label = cleanLabel,
            protocol = protocol,
            baseUrl = baseUrl.trim().trimEnd('/'),
            apiKey = apiKey.trim(),
            modelId = modelId.trim(),
            verifiedAtIp = _outboundIp.value
        )

        val updated = _providers.value + newProvider
        _providers.value = updated
        vault.saveConnectedProviders(updated)

        if (_activeProviderId.value == null) {
            setActiveProvider(newProvider.id)
        }

        resetVerificationState()
        onSaved()
    }

    fun deleteProvider(providerId: String) {
        val updated = _providers.value.filterNot { it.id == providerId }
        _providers.value = updated
        vault.saveConnectedProviders(updated)

        if (_activeProviderId.value == providerId) {
            val nextId = updated.firstOrNull()?.id
            setActiveProvider(nextId)
        }
    }

    fun setActiveProvider(providerId: String?) {
        _activeProviderId.value = providerId
        vault.setActiveProviderId(providerId)
    }

    fun getActiveProvider(): ApiProviderConfig? {
        val id = _activeProviderId.value
        return _providers.value.firstOrNull { it.id == id } ?: _providers.value.firstOrNull()
    }

    // --- Official GitHub OAuth 2.0 Flow & Plugin Operations ---

    fun saveGitHubOAuthCreds(clientId: String, clientSecret: String) {
        val creds = GitHubOAuthAppCredentials(clientId.trim(), clientSecret.trim())
        _githubOAuthCreds.value = creds
        vault.saveGitHubOAuthCredentials(creds)
    }

    /**
     * Launches GitHub's Official Web OAuth 2.0 Authorize screen (`https://github.com/login/oauth/authorize`)
     * and starts a local callback listener on `http://localhost:8888/oauth/callback` to intercept the code.
     */
    fun launchGitHubWebOAuthFlow(
        clientId: String,
        clientSecret: String,
        openUrlCallback: (String) -> Unit
    ) {
        if (clientId.isBlank()) {
            _githubStatusBanner.value = "Enter your GitHub OAuth Client ID to launch the Authorize screen."
            return
        }
        saveGitHubOAuthCreds(clientId, clientSecret)
        val stateNonce = UUID.randomUUID().toString().take(8)
        val authorizeUrl = githubClient.buildWebAuthorizeUrl(clientId, stateNonce)

        viewModelScope.launch {
            _isGitHubLoading.value = true
            _githubStatusBanner.value = "Waiting for GitHub Authorize callback on localhost:8888..."
            openUrlCallback(authorizeUrl)

            githubClient.awaitLocalhostOAuthCode()
                .onSuccess { code ->
                    completeWebOAuthCodeExchange(code)
                }
                .onFailure {
                    _isGitHubLoading.value = false
                }
        }
    }

    fun handleIncomingOAuthCallbackUri(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            githubClient.stopLocalhostCallbackServer()
            completeWebOAuthCodeExchange(code)
        }
    }

    private fun completeWebOAuthCodeExchange(code: String) {
        val creds = _githubOAuthCreds.value
        if (creds.clientId.isBlank() || creds.clientSecret.isBlank()) {
            _githubStatusBanner.value = "Authorization code received ($code). Add Client Secret for Web Exchange or use Device Flow."
            _isGitHubLoading.value = false
            return
        }

        viewModelScope.launch {
            _isGitHubLoading.value = true
            _githubStatusBanner.value = "Exchanging authorization code for GitHub OAuth access token..."
            githubClient.exchangeAuthCodeForToken(creds.clientId, creds.clientSecret, code)
                .onSuccess { token ->
                    connectWithOAuthAccessToken(token)
                }
                .onFailure { err ->
                    _isGitHubLoading.value = false
                    _githubStatusBanner.value = "OAuth exchange failed: ${err.localizedMessage}"
                }
        }
    }

    /**
     * Launches Official GitHub OAuth 2.0 Device Flow (`https://github.com/login/device`)
     * which only requires a Client ID and lets the user tap the green Authorize button on GitHub.
     */
    fun startGitHubDeviceFlow(clientId: String, openBrowserCallback: (String) -> Unit) {
        if (clientId.isBlank()) {
            _githubStatusBanner.value = "Enter your GitHub OAuth App Client ID first."
            return
        }
        saveGitHubOAuthCreds(clientId, _githubOAuthCreds.value.clientSecret)

        viewModelScope.launch {
            _isGitHubLoading.value = true
            _githubStatusBanner.value = "Requesting official GitHub Device Authorization code..."

            githubClient.startDeviceFlow(clientId)
                .onSuccess { session ->
                    _deviceCodeSession.value = session
                    _githubStatusBanner.value = "Enter code ${session.userCode} on GitHub and tap Authorize."
                    openBrowserCallback(session.verificationUri)

                    githubClient.pollDeviceFlowToken(clientId, session)
                        .onSuccess { token ->
                            _deviceCodeSession.value = null
                            connectWithOAuthAccessToken(token)
                        }
                        .onFailure { err ->
                            _deviceCodeSession.value = null
                            _isGitHubLoading.value = false
                            _githubStatusBanner.value = err.localizedMessage ?: "Device authorization failed."
                        }
                }
                .onFailure { err ->
                    _isGitHubLoading.value = false
                    _githubStatusBanner.value = "Device Flow error: ${err.localizedMessage}"
                }
        }
    }

    fun connectWithOAuthAccessToken(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            _isGitHubLoading.value = true
            _githubStatusBanner.value = "Verifying GitHub identity & scopes..."
            githubClient.fetchAuthenticatedUser(token)
                .onSuccess { account ->
                    _githubAccount.value = account
                    vault.saveGitHubAccount(account)
                    _githubStatusBanner.value = "Connected (@${account.username})"
                    refreshGitHubWorkspace()
                }
                .onFailure { err ->
                    _isGitHubLoading.value = false
                    _githubStatusBanner.value = "GitHub Auth Failed: ${err.localizedMessage}"
                }
        }
    }

    fun disconnectGitHub() {
        githubClient.stopLocalhostCallbackServer()
        val cleared = GitHubAccountState()
        _githubAccount.value = cleared
        vault.saveGitHubAccount(cleared)
        _repositories.value = emptyList()
        _repoFiles.value = emptyList()
        _workflowRuns.value = emptyList()
        _artifacts.value = emptyList()
        _deviceCodeSession.value = null
        _githubStatusBanner.value = "GitHub plugin disconnected."
    }

    fun refreshGitHubWorkspace() {
        val account = _githubAccount.value
        if (!account.isConnected || account.accessToken.isBlank()) return

        viewModelScope.launch {
            _isGitHubLoading.value = true
            githubClient.fetchUserRepositories(account.accessToken)
                .onSuccess { repos ->
                    _repositories.value = repos
                    val currentActive = account.activeRepoFullName.ifBlank {
                        repos.firstOrNull()?.fullName.orEmpty()
                    }
                    val activeBranch = repos.firstOrNull { it.fullName == currentActive }?.defaultBranch ?: "main"
                    if (currentActive.isNotBlank()) {
                        selectActiveRepository(currentActive, activeBranch)
                    } else {
                        _isGitHubLoading.value = false
                    }
                }
                .onFailure { err ->
                    _isGitHubLoading.value = false
                    _githubStatusBanner.value = err.localizedMessage ?: "Failed to load repositories"
                }
        }
    }

    fun selectActiveRepository(repoFullName: String, defaultBranch: String = "main") {
        val account = _githubAccount.value
        if (!account.isConnected) return

        val updatedAccount = account.copy(
            activeRepoFullName = repoFullName,
            activeBranch = defaultBranch
        )
        _githubAccount.value = updatedAccount
        vault.saveGitHubAccount(updatedAccount)

        viewModelScope.launch {
            _isGitHubLoading.value = true
            githubClient.fetchRepositoryTree(account.accessToken, repoFullName, defaultBranch)
                .onSuccess { files -> _repoFiles.value = files }

            githubClient.fetchWorkflowRuns(account.accessToken, repoFullName)
                .onSuccess { runs -> _workflowRuns.value = runs }

            githubClient.fetchRepoArtifacts(account.accessToken, repoFullName)
                .onSuccess { arts -> _artifacts.value = arts }

            _isGitHubLoading.value = false
        }
    }

    fun toggleWebReaderPlugin(enabled: Boolean) {
        _webReaderEnabled.value = enabled
        vault.setWebReaderPluginEnabled(enabled)
    }

    // --- Autonomous Yokai Chat & Direct GitHub Commit Execution ---

    fun sendChatMessage(userPrompt: String, includeRepoContext: Boolean) {
        val provider = getActiveProvider() ?: return
        if (userPrompt.isBlank() || _isSendingChat.value) return

        val account = _githubAccount.value
        val attachedRepo = if (includeRepoContext && account.isConnected) account.activeRepoFullName else ""

        val userMsg = ChatMessage(
            role = "user",
            content = userPrompt.trim(),
            providerLabel = provider.label,
            modelId = provider.modelId,
            protocolBadge = provider.protocol.name,
            attachedRepo = attachedRepo
        )

        val updatedHistory = _chatMessages.value + userMsg
        _chatMessages.value = updatedHistory
        vault.saveChatMessages(updatedHistory)

        viewModelScope.launch {
            _isSendingChat.value = true

            // Build dynamic system prompt with optional live GitHub repository context
            val systemBuilder = StringBuilder()
            systemBuilder.appendLine(
                "You are YOKAI HUB, an elite autonomous AI coding and deep-research spirit operating inside the user's Android studio."
            )
            systemBuilder.appendLine(
                "Active BYOK Provider: ${provider.label} (${provider.modelId} via ${provider.protocol.name})."
            )

            if (attachedRepo.isNotBlank() && account.accessToken.isNotBlank()) {
                systemBuilder.appendLine("\n=== CONNECTED GITHUB REPOSITORY CONTEXT ===")
                systemBuilder.appendLine("Authenticated GitHub User: @${account.username}")
                systemBuilder.appendLine("Active Repository: $attachedRepo (Branch: ${account.activeBranch})")

                val blobPaths = _repoFiles.value.filter { it.type == "blob" }.map { it.path }
                if (blobPaths.isNotEmpty()) {
                    systemBuilder.appendLine("Repository File Tree (${blobPaths.size} files):")
                    systemBuilder.appendLine(blobPaths.take(120).joinToString("\n") { "- $it" })
                }

                // Automatically inspect up to 2 files mentioned by name in the user's prompt
                val matchedFiles = blobPaths.filter { path ->
                    val fileName = path.substringAfterLast('/')
                    fileName.length >= 4 && userPrompt.contains(fileName, ignoreCase = true)
                }.take(2)

                for (filePath in matchedFiles) {
                    githubClient.fetchFileContent(
                        accessToken = account.accessToken,
                        repoFullName = attachedRepo,
                        filePath = filePath,
                        branch = account.activeBranch
                    ).onSuccess { (fileContent, _) ->
                        systemBuilder.appendLine("\n--- LIVE CONTENT OF `$filePath` IN `$attachedRepo` ---")
                        systemBuilder.appendLine(fileContent.take(12000))
                        systemBuilder.appendLine("--- END OF `$filePath` ---")
                    }
                }

                systemBuilder.appendLine(
                    """
                    
                    IMPORTANT AUTONOMOUS COMMIT PROTOCOL:
                    Whenever the user asks you to create, modify, fix, update, or commit a file in `$attachedRepo`, include a fenced `yokai-commit` block at the end of your response using this exact format so YOKAI HUB renders an interactive "Review & Commit to GitHub" card:
                    ```yokai-commit
                    REPO: $attachedRepo
                    BRANCH: ${account.activeBranch}
                    PATH: path/to/file.ext
                    MESSAGE: Concise git commit message
                    ---CONTENT---
                    <complete updated file content with zero placeholders>
                    ```
                    """.trimIndent()
                )
            }

            aiClient.sendChatCompletion(
                provider = provider,
                history = updatedHistory,
                systemPrompt = systemBuilder.toString()
            ).onSuccess { (replyText, commitProposal) ->
                val assistantMsg = ChatMessage(
                    role = "assistant",
                    content = replyText,
                    providerLabel = provider.label,
                    modelId = provider.modelId,
                    protocolBadge = provider.protocol.name,
                    attachedRepo = attachedRepo,
                    commitProposal = commitProposal
                )
                val finalHistory = _chatMessages.value + assistantMsg
                _chatMessages.value = finalHistory
                vault.saveChatMessages(finalHistory)
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    role = "assistant",
                    content = "⚠️ **Spirit Channel Interrupted:** ${err.localizedMessage ?: "Request failed"}\n\n" +
                        "Verify that your outbound IP (`${_outboundIp.value}`) is whitelisted on your gateway and that the selected Protocol Adapter matches your model badge.",
                    providerLabel = provider.label,
                    modelId = provider.modelId
                )
                val finalHistory = _chatMessages.value + errorMsg
                _chatMessages.value = finalHistory
                vault.saveChatMessages(finalHistory)
            }

            _isSendingChat.value = false
        }
    }

    fun executeCommitProposal(messageId: String, proposal: GitHubFileCommitProposal) {
        val account = _githubAccount.value
        if (!account.isConnected || account.accessToken.isBlank()) return

        viewModelScope.launch {
            _isSendingChat.value = true
            githubClient.commitFileProposal(account.accessToken, proposal)
                .onSuccess { committedProposal ->
                    val updatedList = _chatMessages.value.map { msg ->
                        if (msg.id == messageId) msg.copy(commitProposal = committedProposal) else msg
                    }
                    _chatMessages.value = updatedList
                    vault.saveChatMessages(updatedList)
                    refreshGitHubWorkspace()
                }
                .onFailure { err ->
                    val errNotice = ChatMessage(
                        role = "assistant",
                        content = "⚠️ **GitHub Commit Failed:** ${err.localizedMessage}",
                        providerLabel = "GitHub Agent",
                        modelId = proposal.repoFullName
                    )
                    _chatMessages.value = _chatMessages.value + errNotice
                }
            _isSendingChat.value = false
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        vault.saveChatMessages(emptyList())
    }
}
