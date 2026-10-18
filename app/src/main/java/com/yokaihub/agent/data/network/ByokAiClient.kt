package com.yokaihub.agent.data.network

import com.yokaihub.agent.data.models.ApiProviderConfig
import com.yokaihub.agent.data.models.ChatMessage
import com.yokaihub.agent.data.models.ConnectionVerificationState
import com.yokaihub.agent.data.models.GitHubFileCommitProposal
import com.yokaihub.agent.data.models.ProtocolAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ByokAiClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the exact endpoint URL based on the selected ProtocolAdapter.
     * - ANTHROPIC_NATIVE -> <BaseURL>/v1/messages
     * - OPENAI_COMPATIBLE -> <BaseURL>/chat/completions (ensuring /v1 on AgentRouter if omitted)
     */
    fun resolveEndpointUrl(baseUrl: String, protocol: ProtocolAdapter): String {
        val cleaned = baseUrl.trim().trimEnd('/')
        return when (protocol) {
            ProtocolAdapter.ANTHROPIC_NATIVE -> {
                when {
                    cleaned.endsWith("/v1/messages") -> cleaned
                    cleaned.endsWith("/v1") -> "$cleaned/messages"
                    else -> "$cleaned/v1/messages"
                }
            }
            ProtocolAdapter.OPENAI_COMPATIBLE -> {
                when {
                    cleaned.endsWith("/chat/completions") -> cleaned
                    cleaned.endsWith("/v1") || cleaned.endsWith("/v2") || cleaned.endsWith("/v4") ->
                        "$cleaned/chat/completions"
                    else -> "$cleaned/v1/chat/completions"
                }
            }
        }
    }

    /**
     * Mandatory Verify-Before-Save Handshake:
     * Dispatches a minimal 1-token verification request through the chosen protocol adapter.
     */
    suspend fun testConnection(
        protocol: ProtocolAdapter,
        baseUrl: String,
        apiKey: String,
        modelId: String
    ): ConnectionVerificationState = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val endpoint = resolveEndpointUrl(baseUrl, protocol)
        val cleanKey = apiKey.trim()
        val cleanModel = modelId.trim()

        try {
            val payload = when (protocol) {
                ProtocolAdapter.ANTHROPIC_NATIVE -> buildJsonObject {
                    put("model", cleanModel)
                    put("max_tokens", 8)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", "ping")
                        })
                    })
                }.toString()

                ProtocolAdapter.OPENAI_COMPATIBLE -> buildJsonObject {
                    put("model", cleanModel)
                    put("max_tokens", 8)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", "ping")
                        })
                    })
                }.toString()
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .post(payload.toRequestBody(jsonMediaType))

            when (protocol) {
                ProtocolAdapter.ANTHROPIC_NATIVE -> {
                    requestBuilder.header("x-api-key", cleanKey)
                    requestBuilder.header("anthropic-version", "2023-06-01")
                }
                ProtocolAdapter.OPENAI_COMPATIBLE -> {
                    requestBuilder.header("Authorization", "Bearer $cleanKey")
                }
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startMs
                val rawBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val preview = extractAssistantText(protocol, rawBody).take(60).ifBlank { "HTTP 200 OK" }
                    ConnectionVerificationState(
                        isTesting = false,
                        isVerified = true,
                        statusCode = response.code,
                        latencyMs = elapsed,
                        statusMessage = "Verified (HTTP ${response.code} OK • ${elapsed}ms)",
                        responsePreview = preview
                    )
                } else {
                    val errorDetail = extractErrorMessage(rawBody).ifBlank { response.message }
                    ConnectionVerificationState(
                        isTesting = false,
                        isVerified = false,
                        statusCode = response.code,
                        latencyMs = elapsed,
                        statusMessage = "HTTP ${response.code}:$errorDetail"
                    )
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            ConnectionVerificationState(
                isTesting = false,
                isVerified = false,
                statusCode = null,
                latencyMs = elapsed,
                statusMessage = "Connection error: ${e.localizedMessage ?: "Network unreachable"}"
            )
        }
    }

    /**
     * Sends a full multi-turn conversation + optional attached GitHub repository context
     * through the active BYOK provider and parses any autonomous GitHub commit proposals.
     */
    suspend fun sendChatCompletion(
        provider: ApiProviderConfig,
        history: List<ChatMessage>,
        systemPrompt: String
    ): Result<Pair<String, GitHubFileCommitProposal?>> = withContext(Dispatchers.IO) {
        val endpoint = resolveEndpointUrl(provider.baseUrl, provider.protocol)
        val cleanKey = provider.apiKey.trim()

        try {
            val payload = when (provider.protocol) {
                ProtocolAdapter.ANTHROPIC_NATIVE -> buildJsonObject {
                    put("model", provider.modelId.trim())
                    put("max_tokens", 4096)
                    if (systemPrompt.isNotBlank()) {
                        put("system", systemPrompt)
                    }
                    put("messages", buildJsonArray {
                        history.filter { it.role == "user" || it.role == "assistant" }.forEach { msg ->
                            add(buildJsonObject {
                                put("role", msg.role)
                                put("content", msg.content)
                            })
                        }
                    })
                }.toString()

                ProtocolAdapter.OPENAI_COMPATIBLE -> buildJsonObject {
                    put("model", provider.modelId.trim())
                    put("max_tokens", 4096)
                    put("messages", buildJsonArray {
                        if (systemPrompt.isNotBlank()) {
                            add(buildJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            })
                        }
                        history.filter { it.role == "user" || it.role == "assistant" }.forEach { msg ->
                            add(buildJsonObject {
                                put("role", msg.role)
                                put("content", msg.content)
                            })
                        }
                    })
                }.toString()
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .post(payload.toRequestBody(jsonMediaType))

            when (provider.protocol) {
                ProtocolAdapter.ANTHROPIC_NATIVE -> {
                    requestBuilder.header("x-api-key", cleanKey)
                    requestBuilder.header("anthropic-version", "2023-06-01")
                }
                ProtocolAdapter.OPENAI_COMPATIBLE -> {
                    requestBuilder.header("Authorization", "Bearer $cleanKey")
                }
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = extractErrorMessage(rawBody).ifBlank { "HTTP ${response.code}" }
                    return@withContext Result.failure(IllegalStateException("Provider Error (${response.code}):$err"))
                }

                val assistantReply = extractAssistantText(provider.protocol, rawBody)
                val commitProposal = parseCommitProposalBlock(assistantReply)
                Result.success(assistantReply to commitProposal)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractAssistantText(protocol: ProtocolAdapter, rawBody: String): String {
        return try {
            val root = json.parseToJsonElement(rawBody).jsonObject
            when (protocol) {
                ProtocolAdapter.ANTHROPIC_NATIVE -> {
                    val contentArr = root["content"]?.jsonArray ?: JsonArray(emptyList())
                    contentArr.mapNotNull { item ->
                        val obj = item.jsonObject
                        if (obj["type"]?.jsonPrimitive?.content == "text") {
                            obj["text"]?.jsonPrimitive?.content
                        } else null
                    }.joinToString("\n").ifBlank {
                        // Fallback in case gateway wraps OpenAI choices format
                        extractOpenAiChoices(root)
                    }
                }
                ProtocolAdapter.OPENAI_COMPATIBLE -> {
                    extractOpenAiChoices(root).ifBlank {
                        // Fallback if gateway returns Anthropic content array
                        val contentArr = root["content"]?.jsonArray
                        contentArr?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content.orEmpty()
                    }
                }
            }
        } catch (_: Exception) {
            rawBody.take(500)
        }
    }

    private fun extractOpenAiChoices(root: JsonObject): String {
        val choices = root["choices"]?.jsonArray ?: return ""
        val firstChoice = choices.firstOrNull()?.jsonObject ?: return ""
        return firstChoice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: firstChoice["text"]?.jsonPrimitive?.content.orEmpty()
    }

    private fun extractErrorMessage(rawBody: String): String {
        return try {
            val root = json.parseToJsonElement(rawBody).jsonObject
            val errElement = root["error"]
            when (errElement) {
                is JsonObject -> errElement["message"]?.jsonPrimitive?.content
                    ?: errElement["type"]?.jsonPrimitive?.content.orEmpty()
                is JsonPrimitive -> errElement.content
                else -> root["message"]?.jsonPrimitive?.content ?: rawBody.take(180)
            }
        } catch (_: Exception) {
            rawBody.take(180)
        }
    }

    /**
     * Parses structured autonomous commit proposals emitted by Yokai Chat:
     * ```yokai-commit
     * REPO: Kingerald/Duplexa
     * BRANCH: main
     * PATH: app/src/main/java/MainActivity.kt
     * MESSAGE: Update UI theme colors
     * ---CONTENT---
     * <file code>
     * ```
     */
    private fun parseCommitProposalBlock(reply: String): GitHubFileCommitProposal? {
        val regex = Regex("```yokai-commit\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = regex.find(reply) ?: return null
        val block = match.groupValues[1]

        val separatorIndex = block.indexOf("---CONTENT---")
        if (separatorIndex == -1) return null

        val headerPart = block.substring(0, separatorIndex)
        val contentPart = block.substring(separatorIndex + "---CONTENT---".length).trimStart('\r', '\n').trimEnd()

        var repo = ""
        var branch = "main"
        var path = ""
        var message = "Yokai Hub Autonomous Update"

        headerPart.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("REPO:", ignoreCase = true) ->
                    repo = trimmed.substringAfter(":").trim()
                trimmed.startsWith("BRANCH:", ignoreCase = true) ->
                    branch = trimmed.substringAfter(":").trim().ifBlank { "main" }
                trimmed.startsWith("PATH:", ignoreCase = true) ->
                    path = trimmed.substringAfter(":").trim()
                trimmed.startsWith("MESSAGE:", ignoreCase = true) ->
                    message = trimmed.substringAfter(":").trim()
            }
        }

        if (repo.isBlank() || path.isBlank() || contentPart.isBlank()) return null

        return GitHubFileCommitProposal(
            repoFullName = repo,
            branch = branch,
            filePath = path,
            commitMessage = message,
            newContent = contentPart
        )
    }
}
