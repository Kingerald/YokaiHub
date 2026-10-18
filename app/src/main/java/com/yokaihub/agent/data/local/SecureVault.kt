package com.yokaihub.agent.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yokaihub.agent.data.models.ApiProviderConfig
import com.yokaihub.agent.data.models.ChatMessage
import com.yokaihub.agent.data.models.GitHubAccountState
import com.yokaihub.agent.data.models.GitHubOAuthAppCredentials
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SecureVault(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "yokai_hub_encrypted_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("yokai_hub_fallback_vault", Context.MODE_PRIVATE)
        }
    }

    // Strict Zero-Preset Mandate: Returns emptyList() by default
    fun getConnectedProviders(): List<ApiProviderConfig> {
        val raw = prefs.getString(KEY_PROVIDERS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ApiProviderConfig>>(raw)
        }.getOrDefault(emptyList())
    }

    fun saveConnectedProviders(providers: List<ApiProviderConfig>) {
        prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(providers)).apply()
    }

    fun getActiveProviderId(): String? {
        return prefs.getString(KEY_ACTIVE_PROVIDER_ID, null)
    }

    fun setActiveProviderId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER_ID, id).apply()
    }

    fun getGitHubAccount(): GitHubAccountState {
        val raw = prefs.getString(KEY_GITHUB_ACCOUNT, null) ?: return GitHubAccountState()
        return runCatching {
            json.decodeFromString<GitHubAccountState>(raw)
        }.getOrDefault(GitHubAccountState())
    }

    fun saveGitHubAccount(state: GitHubAccountState) {
        prefs.edit().putString(KEY_GITHUB_ACCOUNT, json.encodeToString(state)).apply()
    }

    fun getGitHubOAuthCredentials(): GitHubOAuthAppCredentials {
        val raw = prefs.getString(KEY_GITHUB_OAUTH_CREDS, null) ?: return GitHubOAuthAppCredentials()
        return runCatching {
            json.decodeFromString<GitHubOAuthAppCredentials>(raw)
        }.getOrDefault(GitHubOAuthAppCredentials())
    }

    fun saveGitHubOAuthCredentials(creds: GitHubOAuthAppCredentials) {
        prefs.edit().putString(KEY_GITHUB_OAUTH_CREDS, json.encodeToString(creds)).apply()
    }

    fun getChatMessages(): List<ChatMessage> {
        val raw = prefs.getString(KEY_CHAT_HISTORY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ChatMessage>>(raw)
        }.getOrDefault(emptyList())
    }

    fun saveChatMessages(messages: List<ChatMessage>) {
        prefs.edit().putString(KEY_CHAT_HISTORY, json.encodeToString(messages.takeLast(100))).apply()
    }

    fun isWebReaderPluginEnabled(): Boolean {
        return prefs.getBoolean(KEY_WEB_READER_ENABLED, true)
    }

    fun setWebReaderPluginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEB_READER_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_PROVIDERS = "connected_byok_providers"
        private const val KEY_ACTIVE_PROVIDER_ID = "active_provider_id"
        private const val KEY_GITHUB_ACCOUNT = "github_oauth_account"
        private const val KEY_GITHUB_OAUTH_CREDS = "github_oauth_app_creds"
        private const val KEY_CHAT_HISTORY = "yokai_chat_history"
        private const val KEY_WEB_READER_ENABLED = "plugin_web_reader_enabled"
    }
}
