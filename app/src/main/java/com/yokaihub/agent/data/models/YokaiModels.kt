package com.yokaihub.agent.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ProtocolAdapter(
    val title: String,
    val badgeHint: String,
    val endpointSuffix: String,
    val defaultBaseUrlHint: String
) {
    ANTHROPIC_NATIVE(
        title = "Anthropic Native (claude_native / Claude Code Protocol)",
        badgeHint = "claude_native • x-api-key + anthropic-version: 2023-06-01",
        endpointSuffix = "/v1/messages",
        defaultBaseUrlHint = "https://co.agentrouter.org"
    ),
    OPENAI_COMPATIBLE(
        title = "OpenAI Compatible (chat / responses Protocol)",
        badgeHint = "chat / responses • Authorization: Bearer <KEY>",
        endpointSuffix = "/chat/completions",
        defaultBaseUrlHint = "https://co.agentrouter.org/v1"
    )
}

@Serializable
data class ApiProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val protocol: ProtocolAdapter,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val verifiedAtIp: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ConnectionVerificationState(
    val isTesting: Boolean = false,
    val isVerified: Boolean = false,
    val statusCode: Int? = null,
    val latencyMs: Long? = null,
    val statusMessage: String = "Verification required before saving",
    val responsePreview: String = ""
)

@Serializable
data class GitHubAccountState(
    val isConnected: Boolean = false,
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val accessToken: String = "",
    val scopes: String = "repo, workflow",
    val activeRepoFullName: String = "",
    val activeBranch: String = "main"
)

@Serializable
data class GitHubOAuthAppCredentials(
    val clientId: String = "",
    val clientSecret: String = ""
)

data class GitHubDeviceCodeSession(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

@Serializable
data class GitHubRepoItem(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String = "",
    val defaultBranch: String = "main",
    val isPrivate: Boolean = false,
    val htmlUrl: String = "",
    val updatedAt: String = ""
)

@Serializable
data class GitHubFileNode(
    val path: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Long = 0
)

@Serializable
data class GitHubWorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String,
    val branch: String,
    val commitSha: String,
    val htmlUrl: String,
    val createdAt: String
)

@Serializable
data class GitHubArtifactItem(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val archiveDownloadUrl: String,
    val createdAt: String
)

@Serializable
data class GitHubFileCommitProposal(
    val id: String = UUID.randomUUID().toString(),
    val repoFullName: String,
    val branch: String,
    val filePath: String,
    val commitMessage: String,
    val newContent: String,
    val isCommitted: Boolean = false,
    val commitSha: String? = null,
    val commitHtmlUrl: String? = null
)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val providerLabel: String = "",
    val modelId: String = "",
    val protocolBadge: String = "",
    val attachedRepo: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val commitProposal: GitHubFileCommitProposal? = null
)
