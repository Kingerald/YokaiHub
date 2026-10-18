@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.yokaihub.agent.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokaihub.agent.data.models.ApiProviderConfig
import com.yokaihub.agent.data.models.ProtocolAdapter
import com.yokaihub.agent.domain.YokaiViewModel
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
fun ApiProvidersScreen(viewModel: YokaiViewModel) {
    val providers by viewModel.providers.collectAsState()
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    val outboundIp by viewModel.outboundIp.collectAsState()
    val isRefreshingIp by viewModel.isRefreshingIp.collectAsState()
    val verificationState by viewModel.verificationState.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var providerLabel by rememberSaveable { mutableStateOf("") }
    var selectedProtocol by rememberSaveable { mutableStateOf(ProtocolAdapter.ANTHROPIC_NATIVE) }
    var baseUrl by rememberSaveable { mutableStateOf("https://co.agentrouter.org") }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var modelId by rememberSaveable { mutableStateOf("") }

    val modelSuggestions = remember(selectedProtocol) {
        when (selectedProtocol) {
            ProtocolAdapter.ANTHROPIC_NATIVE -> listOf(
                "claude-opus-5-5",
                "deepseek-v4-flash",
                "claude-sonnet-4-5"
            )
            ProtocolAdapter.OPENAI_COMPATIBLE -> listOf(
                "gpt-6-astra",
                "gpt-6.1-sol",
                "gpt-5-turbo"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShrineObsidian)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header + Prominent + Add Provider Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SETTINGS → API PROVIDERS",
                        style = MaterialTheme.typography.titleMedium,
                        color = KitsuneCyan
                    )
                    Text(
                        text = "Zero-Preset BYOK Engine • Encrypted AES-256 Vault",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        showAddForm = !showAddForm
                        viewModel.resetVerificationState()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddForm) ElevatedShrine else KitsuneCyan,
                        contentColor = if (showAddForm) GhostWhite else ShrineObsidian
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showAddForm) "Close" else "+ Add Provider",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Provider Setup Form Card (Revealed on clicking + Add Provider)
        item {
            AnimatedVisibility(visible = showAddForm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ShadowSlate),
                    border = BorderStroke(1.5.dp, KitsuneCyan.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = KitsuneCyan
                            )
                            Text(
                                text = "NEW BYOK PROVIDER SETUP",
                                style = MaterialTheme.typography.titleMedium,
                                color = GhostWhite
                            )
                        }

                        // Live Outbound IP Detector Card inside Setup Form
                        Surface(
                            color = ElevatedShrine,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, YokaiViolet.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = KitsuneCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Live Outbound IPv4 (IP Whitelist)",
                                                fontSize = 11.sp,
                                                color = MutedSpirit
                                            )
                                            Text(
                                                text = outboundIp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = KitsuneCyan
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { viewModel.refreshOutboundIp() },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            if (isRefreshingIp) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = KitsuneCyan
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Refresh IP",
                                                    tint = GhostWhite,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(outboundIp))
                                                Toast.makeText(
                                                    context,
                                                    "Outbound IP ($outboundIp) copied for AgentRouter IP Whitelist!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            border = BorderStroke(1.dp, KitsuneCyan),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = KitsuneCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy IP", color = KitsuneCyan, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tip: Copy this IP and paste it into your AgentRouter token's IP Whitelist before tapping Test Connection.",
                                    fontSize = 11.sp,
                                    color = MutedSpirit
                                )
                            }
                        }

                        // 1. Provider Label
                        OutlinedTextField(
                            value = providerLabel,
                            onValueChange = { providerLabel = it },
                            label = { Text("Provider Label") },
                            placeholder = { Text("e.g., AgentRouter - Claude Coding") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = yokaiTextFieldColors()
                        )

                        // 2. Protocol Adapter Selector (Critical for AgentRouter Compatibility)
                        Text(
                            text = "Protocol Adapter Selector",
                            style = MaterialTheme.typography.labelMedium,
                            color = KitsuneCyan
                        )

                        ProtocolAdapter.entries.forEach { adapter ->
                            val isSelected = selectedProtocol == adapter
                            Surface(
                                color = if (isSelected) ElevatedShrine else ShrineObsidian,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) KitsuneCyan else ShrineBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProtocol = adapter
                                        baseUrl = adapter.defaultBaseUrlHint
                                        viewModel.resetVerificationState()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedProtocol = adapter
                                            baseUrl = adapter.defaultBaseUrlHint
                                            viewModel.resetVerificationState()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = KitsuneCyan)
                                    )
                                    Column(modifier = Modifier.padding(start = 6.dp)) {
                                        Text(
                                            text = adapter.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = GhostWhite
                                        )
                                        Text(
                                            text = "${adapter.badgeHint} → ${adapter.endpointSuffix}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedSpirit
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Base URL Input
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = {
                                baseUrl = it
                                viewModel.resetVerificationState()
                            },
                            label = { Text("Base URL") },
                            placeholder = { Text(selectedProtocol.defaultBaseUrlHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = yokaiTextFieldColors()
                        )

                        // 4. Masked API Key Input
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                viewModel.resetVerificationState()
                            },
                            label = { Text("API Key (Stored in AES-256 EncryptedVault)") },
                            placeholder = { Text("sk-...") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Key Visibility",
                                        tint = MutedSpirit
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = yokaiTextFieldColors()
                        )

                        // 5. Model ID Input + Quick Fill Chips
                        OutlinedTextField(
                            value = modelId,
                            onValueChange = {
                                modelId = it
                                viewModel.resetVerificationState()
                            },
                            label = { Text("Model ID") },
                            placeholder = { Text("Type any model ID (e.g., claude-opus-5-5)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = yokaiTextFieldColors()
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            modelSuggestions.forEach { suggested ->
                                AssistChip(
                                    onClick = {
                                        modelId = suggested
                                        if (providerLabel.isBlank()) {
                                            providerLabel = "AgentRouter • $suggested"
                                        }
                                        viewModel.resetVerificationState()
                                    },
                                    label = {
                                        Text(
                                            text = suggested,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = ElevatedShrine,
                                        labelColor = KitsuneCyan
                                    ),
                                    border = BorderStroke(1.dp, ShrineBorder)
                                )
                            }
                        }

                        HorizontalDivider(color = ShrineBorder)

                        // Verification Status Badge Banner
                        val statusColor = when {
                            verificationState.isVerified -> SpiritJade
                            verificationState.isTesting -> KitsuneCyan
                            verificationState.statusCode != null -> ToriiCrimson
                            else -> FoxAmber
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (verificationState.isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = KitsuneCyan
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (verificationState.isVerified) {
                                            Icons.Default.Verified
                                        } else {
                                            Icons.Default.Shield
                                        },
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (verificationState.isVerified) "VERIFIED • HTTP 200 OK" else "HANDSHAKE STATUS",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = statusColor
                                    )
                                    Text(
                                        text = verificationState.statusMessage,
                                        fontSize = 12.sp,
                                        color = GhostWhite
                                    )
                                }
                            }
                        }

                        // Mandatory Verify-Before-Save Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testProviderConnection(
                                        protocol = selectedProtocol,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        modelId = modelId
                                    )
                                },
                                enabled = !verificationState.isTesting,
                                border = BorderStroke(1.5.dp, KitsuneCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (verificationState.isTesting) "Testing..." else "Test Connection",
                                    color = KitsuneCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.saveVerifiedProvider(
                                        label = providerLabel,
                                        protocol = selectedProtocol,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        modelId = modelId,
                                        onSaved = {
                                            providerLabel = ""
                                            apiKey = ""
                                            modelId = ""
                                            showAddForm = false
                                            Toast.makeText(
                                                context,
                                                "Provider verified and saved to Connected Providers!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                enabled = verificationState.isVerified && !verificationState.isTesting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpiritJade,
                                    contentColor = ShrineObsidian,
                                    disabledContainerColor = ElevatedShrine,
                                    disabledContentColor = MutedSpirit
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (verificationState.isVerified) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save Provider",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connected Providers Section Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connected Providers:",
                style = MaterialTheme.typography.titleMedium,
                color = GhostWhite
            )
        }

        // Empty Initial State ("No API providers connected yet")
        if (providers.isEmpty()) {
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
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MutedSpirit,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No API providers connected yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = GhostWhite
                        )
                        Text(
                            text = "YOKAI HUB ships with zero hardcoded keys or preset providers. Tap '+ Add Provider' above to connect your Anthropic Native or OpenAI Compatible key.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSpirit
                        )
                    }
                }
            }
        } else {
            items(providers, key = { it.id }) { provider ->
                ConnectedProviderCard(
                    provider = provider,
                    isActive = provider.id == activeProviderId,
                    onSelectActive = { viewModel.setActiveProvider(provider.id) },
                    onDelete = { viewModel.deleteProvider(provider.id) }
                )
            }
        }
    }
}

@Composable
private fun ConnectedProviderCard(
    provider: ApiProviderConfig,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectActive() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ElevatedShrine else ShadowSlate
        ),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) KitsuneCyan else ShrineBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = SpiritJade,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = provider.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GhostWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = SpiritJade.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, SpiritJade),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE • VERIFIED" else "VERIFIED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpiritJade,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Provider",
                            tint = ToriiCrimson
                        )
                    }
                }
            }

            Text(
                text = "Model: ${provider.modelId}  •  Protocol: ${provider.protocol.name}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = KitsuneCyan
            )
            Text(
                text = "Gateway: ${provider.baseUrl}${provider.protocol.endpointSuffix}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MutedSpirit
            )
        }
    }
}

@Composable
fun yokaiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KitsuneCyan,
    unfocusedBorderColor = ShrineBorder,
    focusedLabelColor = KitsuneCyan,
    unfocusedLabelColor = MutedSpirit,
    cursorColor = KitsuneCyan,
    focusedTextColor = GhostWhite,
    unfocusedTextColor = GhostWhite,
    focusedContainerColor = ShrineObsidian,
    unfocusedContainerColor = ShrineObsidian
)
