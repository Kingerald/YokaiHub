package com.yokaihub.agent.data.network

import android.util.Base64
import com.yokaihub.agent.data.models.GitHubAccountState
import com.yokaihub.agent.data.models.GitHubArtifactItem
import com.yokaihub.agent.data.models.GitHubDeviceCodeSession
import com.yokaihub.agent.data.models.GitHubFileCommitProposal
import com.yokaihub.agent.data.models.GitHubFileNode
import com.yokaihub.agent.data.models.GitHubRepoItem
import com.yokaihub.agent.data.models.GitHubWorkflowRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class GitHubPluginClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var activeCallbackServer: ServerSocket? = null

    /**
     * Builds the Official GitHub OAuth 2.0 Web Authorization URL
     * (`https://github.com/login/oauth/authorize` with `repo` and `workflow` scopes).
     */
    fun buildWebAuthorizeUrl(clientId: String, stateNonce: String): String {
        val cleanClientId = clientId.trim()
        return "https://github.com/login/oauth/authorize" +
            "?client_id=$cleanClientId" +
            "&redirect_uri=http://localhost:8888/oauth/callback" +
            "&scope=repo%20workflow" +
            "&state=$stateNonce"
    }

    /**
     * Starts a lightweight local loopback listener on 127.0.0.1:8888 so that when GitHub
     * redirects to `http://localhost:8888/oauth/callback?code=...` after tapping Authorize,
     * the app captures the authorization code automatically and displays a Cyber-Shrine confirmation page.
     */
    suspend fun awaitLocalhostOAuthCode(timeoutSeconds: Int = 180): Result<String> = withContext(Dispatchers.IO) {
        try {
            stopLocalhostCallbackServer()
            val server = ServerSocket(8888)
            server.soTimeout = timeoutSeconds * 1000
            activeCallbackServer = server

            val socket = server.accept()
            socket.use { conn ->
                val reader = BufferedReader(InputStreamReader(conn.getInputStream()))
                val requestLine = reader.readLine().orEmpty()
                // Format: GET /oauth/callback?code=XXXX&state=YYYY HTTP/1.1
                val queryPart = requestLine.substringAfter("?", "").substringBefore(" ")
                val params = queryPart.split("&").associate { pair ->
                    val k = pair.substringBefore("=")
                    val v = URLDecoder.decode(pair.substringAfter("=", ""), "UTF-8")
                    k to v
                }

                val code = params["code"].orEmpty()
                val htmlResponse = """
                    HTTP/1.1 200 OK
                    Content-Type: text/html; charset=utf-8
                    Connection: close

                    <!DOCTYPE html>
                    <html>
                    <head><title>YOKAI HUB Authorized</title>
                    <style>
                      body { background:#090C12; color:#F0F4F8; font-family:monospace; text-align:center; padding-top:80px; }
                      .card { display:inline-block; background:#121722; border:2px solid #00E5FF; border-radius:14px; padding:32px; }
                      h2 { color:#00E5FF; }
                      a { display:inline-block; margin-top:16px; padding:12px 24px; background:#7C4DFF; color:#FFF; text-decoration:none; border-radius:8px; }
                    </style></head>
                    <body>
                      <div class="card">
                        <h2>⚡ YOKAI HUB AUTHORIZED</h2>
                        <p>GitHub OAuth 2.0 handshake completed. Finishing token exchange...</p>
                        <a href="yokaihub://oauth?code=$code">Return to YOKAI HUB</a>
                      </div>
                    </body>
                    </html>
                """.trimIndent()

                val writer = PrintWriter(conn.getOutputStream(), true)
                writer.print(htmlResponse)
                writer.flush()

                stopLocalhostCallbackServer()
                if (code.isNotBlank()) {
                    Result.success(code)
                } else {
                    Result.failure(IllegalStateException("No authorization code returned from GitHub."))
                }
            }
        } catch (e: Exception) {
            stopLocalhostCallbackServer()
            Result.failure(e)
        }
    }

    fun stopLocalhostCallbackServer() {
        try {
            activeCallbackServer?.close()
        } catch (_: Exception) {
        } finally {
            activeCallbackServer = null
        }
    }

    /**
     * Exchanges the GitHub OAuth 2.0 authorization `code` for an access token.
     */
    suspend fun exchangeAuthCodeForToken(
        clientId: String,
        clientSecret: String,
        code: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("client_secret", clientSecret.trim())
                .add("code", code.trim())
                .add("redirect_uri", "http://localhost:8888/oauth/callback")
                .build()

            val request = Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val obj = json.parseToJsonElement(raw).jsonObject
                val token = obj["access_token"]?.jsonPrimitive?.content.orEmpty()
                if (token.isNotBlank()) {
                    Result.success(token)
                } else {
                    val err = obj["error_description"]?.jsonPrimitive?.content
                        ?: obj["error"]?.jsonPrimitive?.content
                        ?: "Failed to exchange code"
                    Result.failure(IllegalStateException(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initiates Official GitHub OAuth Device Flow (`https://github.com/login/device/code`)
     * so the user can authorize via `https://github.com/login/device` with 0 client secret required.
     */
    suspend fun startDeviceFlow(clientId: String): Result<GitHubDeviceCodeSession> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("scope", "repo workflow")
                .build()

            val request = Request.Builder()
                .url("https://github.com/login/device/code")
                .header("Accept", "application/json")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val obj = json.parseToJsonElement(raw).jsonObject
                val deviceCode = obj["device_code"]?.jsonPrimitive?.content.orEmpty()
                val userCode = obj["user_code"]?.jsonPrimitive?.content.orEmpty()
                val verificationUri = obj["verification_uri"]?.jsonPrimitive?.content ?: "https://github.com/login/device"
                val expiresIn = obj["expires_in"]?.jsonPrimitive?.intOrNull ?: 900
                val interval = obj["interval"]?.jsonPrimitive?.intOrNull ?: 5

                if (deviceCode.isNotBlank() && userCode.isNotBlank()) {
                    Result.success(
                        GitHubDeviceCodeSession(
                            deviceCode = deviceCode,
                            userCode = userCode,
                            verificationUri = verificationUri,
                            expiresInSeconds = expiresIn,
                            intervalSeconds = interval
                        )
                    )
                } else {
                    val err = obj["error_description"]?.jsonPrimitive?.content ?: "Invalid OAuth Client ID"
                    Result.failure(IllegalStateException(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Polls GitHub until the user clicks the green Authorize button on GitHub.
     */
    suspend fun pollDeviceFlowToken(
        clientId: String,
        session: GitHubDeviceCodeSession
    ): Result<String> = withContext(Dispatchers.IO) {
        var waitSeconds = session.intervalSeconds.coerceAtLeast(5)
        val deadline = System.currentTimeMillis() + (session.expiresInSeconds * 1000L)

        while (System.currentTimeMillis() < deadline) {
            delay(waitSeconds * 1000L)
            try {
                val formBody = FormBody.Builder()
                    .add("client_id", clientId.trim())
                    .add("device_code", session.deviceCode)
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .build()

                val request = Request.Builder()
                    .url("https://github.com/login/oauth/access_token")
                    .header("Accept", "application/json")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val obj = json.parseToJsonElement(raw).jsonObject
                    val accessToken = obj["access_token"]?.jsonPrimitive?.content.orEmpty()
                    if (accessToken.isNotBlank()) {
                        return@withContext Result.success(accessToken)
                    }

                    when (obj["error"]?.jsonPrimitive?.content) {
                        "authorization_pending" -> Unit
                        "slow_down" -> waitSeconds += 5
                        "expired_token" -> return@withContext Result.failure(IllegalStateException("Device code expired."))
                        "access_denied" -> return@withContext Result.failure(IllegalStateException("Authorization cancelled by user."))
                        else -> Unit
                    }
                }
            } catch (_: Exception) {
            }
        }
        Result.failure(IllegalStateException("GitHub authorization timed out."))
    }

    /**
     * Verifies an OAuth token and fetches the authenticated GitHub user profile (e.g., @Kingerald).
     */
    suspend fun fetchAuthenticatedUser(accessToken: String): Result<GitHubAccountState> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("GitHub Auth Failed (HTTP ${response.code})"))
                }
                val obj = json.parseToJsonElement(raw).jsonObject
                val login = obj["login"]?.jsonPrimitive?.content.orEmpty()
                val name = obj["name"]?.jsonPrimitive?.content ?: login
                val avatar = obj["avatar_url"]?.jsonPrimitive?.content.orEmpty()
                val scopes = response.header("X-OAuth-Scopes") ?: "repo, workflow"

                Result.success(
                    GitHubAccountState(
                        isConnected = true,
                        username = login,
                        displayName = name,
                        avatarUrl = avatar,
                        accessToken = accessToken.trim(),
                        scopes = scopes
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists the authenticated user's repositories (e.g., Kingerald/Duplexa, Kingerald/Yokai-Hub).
     */
    suspend fun fetchUserRepositories(accessToken: String): Result<List<GitHubRepoItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user/repos?sort=updated&per_page=50&affiliation=owner,collaborator")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Unable to fetch repositories"))
                }
                val arr = json.parseToJsonElement(raw).jsonArray
                val repos = arr.map { elem ->
                    val o = elem.jsonObject
                    GitHubRepoItem(
                        id = o["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        name = o["name"]?.jsonPrimitive?.content.orEmpty(),
                        fullName = o["full_name"]?.jsonPrimitive?.content.orEmpty(),
                        description = o["description"]?.jsonPrimitive?.content.orEmpty(),
                        defaultBranch = o["default_branch"]?.jsonPrimitive?.content ?: "main",
                        isPrivate = o["private"]?.jsonPrimitive?.booleanOrNull ?: false,
                        htmlUrl = o["html_url"]?.jsonPrimitive?.content.orEmpty(),
                        updatedAt = o["updated_at"]?.jsonPrimitive?.content.orEmpty()
                    )
                }
                Result.success(repos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the recursive file tree of a repository so the AI Spirit can inspect paths.
     */
    suspend fun fetchRepositoryTree(
        accessToken: String,
        repoFullName: String,
        branch: String = "main"
    ): Result<List<GitHubFileNode>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repoFullName/git/trees/$branch?recursive=1")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Failed to load tree for$repoFullName"))
                }
                val root = json.parseToJsonElement(raw).jsonObject
                val treeArr = root["tree"]?.jsonArray ?: return@withContext Result.success(emptyList())
                val nodes = treeArr.mapNotNull { item ->
                    val o = item.jsonObject
                    val path = o["path"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val type = o["type"]?.jsonPrimitive?.content ?: "blob"
                    val sha = o["sha"]?.jsonPrimitive?.content.orEmpty()
                    val size = o["size"]?.jsonPrimitive?.longOrNull ?: 0L
                    GitHubFileNode(path = path, type = type, sha = sha, size = size)
                }
                Result.success(nodes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads a specific file's decoded text content and current SHA from a GitHub repository.
     */
    suspend fun fetchFileContent(
        accessToken: String,
        repoFullName: String,
        filePath: String,
        branch: String = "main"
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = filePath.trimStart('/')
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repoFullName/contents/$cleanPath?ref=$branch")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("File $cleanPath not found (HTTP${response.code})"))
                }
                val obj = json.parseToJsonElement(raw).jsonObject
                val sha = obj["sha"]?.jsonPrimitive?.content.orEmpty()
                val encoded = obj["content"]?.jsonPrimitive?.content.orEmpty().replace("\n", "").replace("\r", "")
                val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
                val content = String(decodedBytes, Charsets.UTF_8)
                Result.success(content to sha)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Commits a created or modified file directly to a GitHub branch via the REST API.
     */
    suspend fun commitFileProposal(
        accessToken: String,
        proposal: GitHubFileCommitProposal
    ): Result<GitHubFileCommitProposal> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = proposal.filePath.trimStart('/')
            // Check if file already exists to include its current blob SHA
            val existingSha = fetchFileContent(
                accessToken = accessToken,
                repoFullName = proposal.repoFullName,
                filePath = cleanPath,
                branch = proposal.branch
            ).getOrNull()?.second

            val base64Content = Base64.encodeToString(
                proposal.newContent.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val bodyJson = buildJsonObject {
                put("message", proposal.commitMessage)
                put("content", base64Content)
                put("branch", proposal.branch)
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }.toString()

            val request = Request.Builder()
                .url("https://api.github.com/repos/${proposal.repoFullName}/contents/$cleanPath")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .put(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Commit failed (HTTP ${response.code}):${raw.take(200)}")
                    )
                }
                val root = json.parseToJsonElement(raw).jsonObject
                val commitObj = root["commit"]?.jsonObject
                val commitSha = commitObj?.get("sha")?.jsonPrimitive?.content?.take(7)
                val htmlUrl = commitObj?.get("html_url")?.jsonPrimitive?.content

                Result.success(
                    proposal.copy(
                        isCommitted = true,
                        commitSha = commitSha,
                        commitHtmlUrl = htmlUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches recent GitHub Actions workflow runs for the active repository.
     */
    suspend fun fetchWorkflowRuns(
        accessToken: String,
        repoFullName: String
    ): Result<List<GitHubWorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repoFullName/actions/runs?per_page=10")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Unable to fetch Actions runs"))
                }
                val root = json.parseToJsonElement(raw).jsonObject
                val runsArr = root["workflow_runs"]?.jsonArray ?: return@withContext Result.success(emptyList())
                val runs = runsArr.map { elem ->
                    val o = elem.jsonObject
                    GitHubWorkflowRun(
                        id = o["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        name = o["name"]?.jsonPrimitive?.content ?: "Workflow",
                        status = o["status"]?.jsonPrimitive?.content.orEmpty(),
                        conclusion = o["conclusion"]?.jsonPrimitive?.content ?: "in_progress",
                        branch = o["head_branch"]?.jsonPrimitive?.content.orEmpty(),
                        commitSha = o["head_sha"]?.jsonPrimitive?.content?.take(7).orEmpty(),
                        htmlUrl = o["html_url"]?.jsonPrimitive?.content.orEmpty(),
                        createdAt = o["created_at"]?.jsonPrimitive?.content.orEmpty()
                    )
                }
                Result.success(runs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches compiled build artifacts (e.g., signed release APKs) from a repository's Actions.
     */
    suspend fun fetchRepoArtifacts(
        accessToken: String,
        repoFullName: String
    ): Result<List<GitHubArtifactItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repoFullName/actions/artifacts?per_page=10")
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YokaiHub-Agent/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Unable to fetch artifacts"))
                }
                val root = json.parseToJsonElement(raw).jsonObject
                val arr = root["artifacts"]?.jsonArray ?: return@withContext Result.success(emptyList())
                val items = arr.map { elem ->
                    val o = elem.jsonObject
                    GitHubArtifactItem(
                        id = o["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        name = o["name"]?.jsonPrimitive?.content ?: "apk-artifact",
                        sizeInBytes = o["size_in_bytes"]?.jsonPrimitive?.longOrNull ?: 0L,
                        archiveDownloadUrl = o["archive_download_url"]?.jsonPrimitive?.content.orEmpty(),
                        createdAt = o["created_at"]?.jsonPrimitive?.content.orEmpty()
                    )
                }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
