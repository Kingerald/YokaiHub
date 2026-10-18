@file:OptIn(ExperimentalMaterial3Api::class)

package com.yokaihub.agent.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokaihub.agent.data.models.ChatMessage
import com.yokaihub.agent.data.models.GitHubFileCommitProposal
import com.yokaihub.agent.domain.YokaiViewModel
import com.yokaihub.agent.ui.screens.settings.yokaiTextFieldColors
import com.yokaihub.agent.ui.theme.ElevatedShrine
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
fun YokaiChatScreen(
    viewModel: YokaiViewModel,
    onNavigateToProviders: () -> Unit,
    onNavigateToPlugins: () -> Unit
) {
    val providers by viewModel.providers.collectAsState()
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    val githubAccount by viewModel.githubAccount.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isSending by viewModel.isSendingChat.collectAsState()

    val activeProvider = providers.firstOrNull { it.id == activeProviderId } ?: providers.firstOrNull()

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var attachRepoContext by rememberSaveable { mutableStateOf(true) }
    var promptText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShrineObsidian)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 1-Tap Provider & Model Switcher + Repo Context Header Bar
        Surface(
            color = ShadowSlate,
            border = BorderStroke(1.dp, ShrineBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = ElevatedShrine,
                            border = BorderStroke(
                                1.dp,
                                if (activeProvider != null) KitsuneCyan else ToriiCrimson
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (providers.isEmpty()) {
                                        onNavigateToProviders()
                                    } else {
                                        providerMenuExpanded = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeProvider?.label ?: "No API Provider Connected (Tap to Add)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (activeProvider != null) GhostWhite else ToriiCrimson
                                    )
                                    Text(
                                        text = if (activeProvider != null) {
                                            "${activeProvider.modelId} •${activeProvider.protocol.name}"
                                        } else {
                                            "Strict BYOK Mode • Connect your key in Settings"
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = KitsuneCyan
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Provider",
                                    tint = KitsuneCyan
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = providerMenuExpanded,
                            onDismissRequest = { providerMenuExpanded = false },
                            modifier = Modifier.background(ElevatedShrine)
                        ) {
                            providers.forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = provider.label,
                                                fontWeight = FontWeight.Bold,
                                                color = GhostWhite
                                            )
                                            Text(
                                                text = "${provider.modelId} (${provider.protocol.name})",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = KitsuneCyan
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setActiveProvider(provider.id)
                                        providerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (chatMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChatHistory() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = MutedSpirit
                            )
                        }
                    }
                }

                // Repository Context Attachment Chip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (githubAccount.isConnected && githubAccount.activeRepoFullName.isNotBlank()) {
                        FilterChip(
                            selected = attachRepoContext,
                            onClick = { attachRepoContext = !attachRepoContext },
                            label = {
                                Text(
                                    text = if (attachRepoContext) {
                                        "Repo Context: ${githubAccount.activeRepoFullName} (${githubAccount.activeBranch})"
                                    } else {
                                        "Attach Repo: ${githubAccount.activeRepoFullName}"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YokaiViolet.copy(alpha = 0.25f),
                                selectedLabelColor = GhostWhite,
                                selectedLeadingIconColor = KitsuneCyan
                            )
                        )
                    } else {
                        Text(
                            text = "GitHub Repo Context: Not attached (Connect in Plugins tab)",
                            fontSize = 11.sp,
                            color = MutedSpirit,
                            modifier = Modifier.clickable { onNavigateToPlugins() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Stream or Empty Zero-Preset Prompt State
        Box(modifier = Modifier.weight(1f)) {
            if (providers.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                    border = BorderStroke(1.dp, KitsuneCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = KitsuneCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "AWAITING YOUR OWN KEY (BYOK)",
                            style = MaterialTheme.typography.titleMedium,
                            color = GhostWhite
                        )
                        Text(
                            text = "Connect your AgentRouter Claude or OpenAI-Compatible API key in Settings → API Providers to summon the Yokai coding & research spirit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSpirit
                        )
                        Button(
                            onClick = onNavigateToProviders,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KitsuneCyan,
                                contentColor = ShrineObsidian
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Configure API Provider", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (chatMessages.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                    border = BorderStroke(1.dp, ShrineBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = KitsuneCyan,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "YOKAI SPIRIT READY",
                            style = MaterialTheme.typography.titleMedium,
                            color = KitsuneCyan
                        )
                        Text(
                            text = "Active Key: ${activeProvider?.label} (${activeProvider?.modelId})\n" +
                                "Try prompting: \"Read MainActivity.kt in ${githubAccount.activeRepoFullName.ifBlank { "Kingerald/Duplexa" }}, update the UI theme, and commit the changes.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSpirit
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onCommitProposal = { proposal ->
                                viewModel.executeCommitProposal(msg.id, proposal)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prompt Input Composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        text = if (activeProvider != null) {
                            "Summon ${activeProvider.modelId}..."
                        } else {
                            "Connect an API provider first..."
                        },
                        fontSize = 13.sp
                    )
                },
                enabled = activeProvider != null && !isSending,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                colors = yokaiTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val toSend = promptText
                    promptText = ""
                    viewModel.sendChatMessage(
                        userPrompt = toSend,
                        includeRepoContext = attachRepoContext
                    )
                },
                enabled = activeProvider != null && promptText.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KitsuneCyan,
                    contentColor = ShrineObsidian,
                    disabledContainerColor = ElevatedShrine,
                    disabledContentColor = MutedSpirit
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(54.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = KitsuneCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Prompt"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onCommitProposal: (GitHubFileCommitProposal) -> Unit
) {
    val isUser = message.role == "user"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) ElevatedShrine else ShadowSlate
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isUser) YokaiViolet.copy(alpha = 0.6f) else KitsuneCyan.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "YOU" else "YOKAI SPIRIT • ${message.providerLabel}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isUser) YokaiViolet else KitsuneCyan
                )
                if (message.modelId.isNotBlank()) {
                    Text(
                        text = message.modelId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MutedSpirit
                    )
                }
            }

            if (message.attachedRepo.isNotBlank()) {
                Text(
                    text = "Repo Context: ${message.attachedRepo}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpiritJade
                )
            }

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = GhostWhite
            )

            // Interactive "Review & Commit to GitHub" Card when AI proposes repository edits
            message.commitProposal?.let { proposal ->
                HorizontalDivider(color = ShrineBorder)
                Surface(
                    color = ShrineObsidian,
                    border = BorderStroke(
                        1.5.dp,
                        if (proposal.isCommitted) SpiritJade else KitsuneCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (proposal.isCommitted) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Terminal
                                },
                                contentDescription = null,
                                tint = if (proposal.isCommitted) SpiritJade else KitsuneCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (proposal.isCommitted) {
                                    "COMMITTED TO GITHUB (${proposal.commitSha ?: "HEAD"})"
                                } else {
                                    "REVIEW & COMMIT TO GITHUB"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (proposal.isCommitted) SpiritJade else KitsuneCyan
                            )
                        }

                        Text(
                            text = "Target: ${proposal.repoFullName} (${proposal.branch}) →${proposal.filePath}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = GhostWhite
                        )
                        Text(
                            text = "Commit Message: \"${proposal.commitMessage}\"",
                            fontSize = 12.sp,
                            color = MutedSpirit
                        )

                        if (!proposal.isCommitted) {
                            Button(
                                onClick = { onCommitProposal(proposal) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpiritJade,
                                    contentColor = ShrineObsidian
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Approve & Commit Directly to ${proposal.branch}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
