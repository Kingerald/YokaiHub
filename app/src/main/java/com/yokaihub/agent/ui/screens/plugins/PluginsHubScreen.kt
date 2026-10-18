@file:OptIn(ExperimentalMaterial3Api::class)

package com.yokaihub.agent.ui.screens.plugins

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokaihub.agent.domain.YokaiViewModel
import com.yokaihub.agent.ui.screens.settings.yokaiTextFieldColors
import com.yokaihub.agent.ui.theme.ElevatedShrine
import com.yokaihub.agent.ui.theme.FoxAmber
import com.yokaihub.agent.ui.theme.GhostWhite
import com.yokaihub.agent.ui.theme.KitsuneCyan
import com.yokaihub.agent.ui.theme.MutedSpirit
import com.yokaihub.agent.ui.theme.ShadowSlate
import com.yokaihub.agent.ui.theme.ShrineBorder
import com.yokaihub.agent.ui.theme.ShrineObsidian
import com.yokaihub.agent.ui.theme.SpiritJade
import com.yokaihub.agent.ui.theme.ToriiCrimson
import com.yokaihub.agent.ui.theme.YokaiViolet

@Composable
fun PluginsHubScreen(viewModel: YokaiViewModel) {
    val githubAccount by viewModel.githubAccount.collectAsState()
    val oauthCreds by viewModel.githubOAuthCreds.collectAsState()
    val deviceSession by viewModel.deviceCodeSession.collectAsState()
    val statusBanner by viewModel.githubStatusBanner.collectAsState()
    val isLoading by viewModel.isGitHubLoading.collectAsState()
    val repositories by viewModel.repositories.collectAsState()
    val repoFiles by viewModel.repoFiles.collectAsState()
    val workflowRuns by viewModel.workflowRuns.collectAsState()
    val artifacts by viewModel.artifacts.collectAsState()
    val webReaderEnabled by viewModel.webReaderEnabled.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showOAuthModal by rememberSaveable { mutableStateOf(false) }
    var clientId by rememberSaveable(oauthCreds.clientId) { mutableStateOf(oauthCreds.clientId) }
    var clientSecret by rememberSaveable(oauthCreds.clientSecret) { mutableStateOf(oauthCreds.clientSecret) }

    fun openExternalUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShrineObsidian)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PLUGINS HUB • NATIVE INTEGRATIONS",
                    style = MaterialTheme.typography.titleMedium,
                    color = YokaiViolet
                )
                Text(
                    text = "Official GitHub OAuth 2.0 Authorize Flow • Autonomous Repo & CI Agent",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Plugin 1: GitHub Repositories & Code Agent
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (githubAccount.isConnected) SpiritJade else YokaiViolet
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (githubAccount.isConnected) SpiritJade else YokaiViolet,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "GitHub Repositories & Code Agent",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GhostWhite
                                )
                                Text(
                                    text = if (githubAccount.isConnected) {
                                        "Connected (@${githubAccount.username}) • Scopes: ${githubAccount.scopes}"
                                    } else {
                                        "Read trees, edit code & commit directly via GitHub OAuth 2.0"
                                    },
                                    fontSize = 12.sp,
                                    color = if (githubAccount.isConnected) SpiritJade else MutedSpirit
                                )
                            }
                        }

                        if (githubAccount.isConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectGitHub() },
                                border = BorderStroke(1.dp, ToriiCrimson),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect", color = ToriiCrimson, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { showOAuthModal = !showOAuthModal },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = YokaiViolet,
                                    contentColor = GhostWhite
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Official GitHub Login -> Authorize Setup Drawer
                    AnimatedVisibility(visible = showOAuthModal && !githubAccount.isConnected) {
                        Surface(
                            color = ElevatedShrine,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, YokaiViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL GITHUB OAUTH 2.0 HANDSHAKE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = KitsuneCyan
                                )
                                Text(
                                    text = "Log into your GitHub account (@Kingerald) and click GitHub's green Authorize button. Supports both Web Redirect (http://localhost:8888/oauth/callback) and GitHub Device Flow.",
                                    fontSize = 12.sp,
                                    color = MutedSpirit
                                )

                                OutlinedTextField(
                                    value = clientId,
                                    onValueChange = { clientId = it },
                                    label = { Text("GitHub OAuth Client ID") },
                                    placeholder = { Text("Ov23li... or Iv1...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = yokaiTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = clientSecret,
                                    onValueChange = { clientSecret = it },
                                    label = { Text("GitHub OAuth Client Secret (For Web Redirect)") },
                                    placeholder = { Text("Optional if using GitHub Device Flow") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = yokaiTextFieldColors()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.launchGitHubWebOAuthFlow(
                                                clientId = clientId,
                                                clientSecret = clientSecret,
                                                openUrlCallback = { url -> openExternalUrl(url) }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SpiritJade,
                                            contentColor = ShrineObsidian
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Web Authorize",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.startGitHubDeviceFlow(
                                                clientId = clientId,
                                                openBrowserCallback = { url -> openExternalUrl(url) }
                                            )
                                        },
                                        border = BorderStroke(1.dp, KitsuneCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Device Authorize",
                                            color = KitsuneCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // If Device Flow session is active, show 1-tap copy code card
                                deviceSession?.let { session ->
                                    Surface(
                                        color = ShrineObsidian,
                                        border = BorderStroke(1.dp, KitsuneCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "GITHUB DEVICE USER CODE",
                                                    fontSize = 10.sp,
                                                    color = MutedSpirit
                                                )
                                                Text(
                                                    text = session.userCode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = KitsuneCyan
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(session.userCode))
                                                    Toast.makeText(context, "Device code copied!", Toast.LENGTH_SHORT).show()
                                                    openExternalUrl(session.verificationUri)
                                                },
                                                border = BorderStroke(1.dp, KitsuneCyan)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = KitsuneCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copy & Open GitHub", color = KitsuneCyan, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                if (statusBanner.isNotBlank()) {
                                    Text(
                                        text = statusBanner,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = KitsuneCyan
                                    )
                                }
                            }
                        }
                    }

                    // Connected Repository Browser
                    if (githubAccount.isConnected) {
                        HorizontalDivider(color = ShrineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Repository: ${githubAccount.activeRepoFullName.ifBlank { "Select below" }}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = KitsuneCyan
                            )
                            IconButton(
                                onClick = { viewModel.refreshGitHubWorkspace() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = KitsuneCyan
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Repos",
                                        tint = GhostWhite
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Loaded File Tree Nodes: ${repoFiles.size} files ready for Yokai Chat context",
                            fontSize = 11.sp,
                            color = MutedSpirit
                        )

                        repositories.take(8).forEach { repo ->
                            val isSelected = repo.fullName == githubAccount.activeRepoFullName
                            Surface(
                                color = if (isSelected) ElevatedShrine else ShrineObsidian,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) KitsuneCyan else ShrineBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectActiveRepository(repo.fullName, repo.defaultBranch)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) KitsuneCyan else MutedSpirit,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = repo.fullName,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = GhostWhite
                                            )
                                            Text(
                                                text = "Branch: ${repo.defaultBranch}",
                                                fontSize = 11.sp,
                                                color = MutedSpirit
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Attached",
                                            tint = SpiritJade,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Plugin 2: GitHub Actions CI & APK Downloader
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                border = BorderStroke(1.dp, ShrineBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = KitsuneCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "GitHub Actions CI & APK Downloader",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GhostWhite
                                )
                                Text(
                                    text = if (githubAccount.isConnected) {
                                        "Monitoring workflow runs for ${githubAccount.activeRepoFullName}"
                                    } else {
                                        "Connect GitHub OAuth above to monitor cloud APK builds"
                                    },
                                    fontSize = 12.sp,
                                    color = MutedSpirit
                                )
                            }
                        }

                        Surface(
                            color = if (githubAccount.isConnected) SpiritJade.copy(alpha = 0.15f) else ElevatedShrine,
                            border = BorderStroke(1.dp, if (githubAccount.isConnected) SpiritJade else ShrineBorder),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (githubAccount.isConnected) "ONLINE" else "STANDBY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (githubAccount.isConnected) SpiritJade else MutedSpirit,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (githubAccount.isConnected) {
                        if (workflowRuns.isEmpty()) {
                            Text(
                                text = "No workflow runs found for ${githubAccount.activeRepoFullName} yet.",
                                fontSize = 12.sp,
                                color = MutedSpirit
                            )
                        } else {
                            workflowRuns.take(4).forEach { run ->
                                val badgeColor = when (run.conclusion) {
                                    "success" -> SpiritJade
                                    "failure" -> ToriiCrimson
                                    else -> FoxAmber
                                }
                                Surface(
                                    color = ShrineObsidian,
                                    border = BorderStroke(1.dp, ShrineBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${run.name} (${run.commitSha})",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GhostWhite
                                            )
                                            Text(
                                                text = "Status: ${run.status} • Result: ${run.conclusion}",
                                                fontSize = 11.sp,
                                                color = badgeColor
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { openExternalUrl(run.htmlUrl) },
                                            border = BorderStroke(1.dp, KitsuneCyan)
                                        ) {
                                            Text("Logs", fontSize = 11.sp, color = KitsuneCyan)
                                        }
                                    }
                                }
                            }
                        }

                        if (artifacts.isNotEmpty()) {
                            HorizontalDivider(color = ShrineBorder)
                            Text(
                                text = "Compiled Release Artifacts (APKs):",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = KitsuneCyan
                            )
                            artifacts.take(4).forEach { artifact ->
                                val sizeMb = String.format("%.1f MB", artifact.sizeInBytes / (1024.0 * 1024.0))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = artifact.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = GhostWhite
                                        )
                                        Text(
                                            text = sizeMb,
                                            fontSize = 11.sp,
                                            color = MutedSpirit
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            openExternalUrl("https://github.com/${githubAccount.activeRepoFullName}/actions")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = KitsuneCyan,
                                            contentColor = ShrineObsidian
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Plugin 3: Web & Docs Live Reader
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                border = BorderStroke(1.dp, ShrineBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = KitsuneCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Web & Docs Live Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = GhostWhite
                            )
                            Text(
                                text = "Allows Yokai Spirit to fetch live documentation & API specs during research",
                                fontSize = 12.sp,
                                color = MutedSpirit
                            )
                        }
                    }

                    Switch(
                        checked = webReaderEnabled,
                        onCheckedChange = { viewModel.toggleWebReaderPlugin(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ShrineObsidian,
                            checkedTrackColor = KitsuneCyan
                        )
                    )
                }
            }
        }
    }
}
