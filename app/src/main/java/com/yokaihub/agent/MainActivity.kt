@file:OptIn(ExperimentalMaterial3Api::class)

package com.yokaihub.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokaihub.agent.domain.YokaiViewModel
import com.yokaihub.agent.ui.screens.chat.YokaiChatScreen
import com.yokaihub.agent.ui.screens.plugins.PluginsHubScreen
import com.yokaihub.agent.ui.screens.settings.ApiProvidersScreen
import com.yokaihub.agent.ui.theme.ElevatedShrine
import com.yokaihub.agent.ui.theme.GhostWhite
import com.yokaihub.agent.ui.theme.KitsuneCyan
import com.yokaihub.agent.ui.theme.MutedSpirit
import com.yokaihub.agent.ui.theme.ShadowSlate
import com.yokaihub.agent.ui.theme.ShrineBorder
import com.yokaihub.agent.ui.theme.ShrineObsidian
import com.yokaihub.agent.ui.theme.YokaiHubTheme

class MainActivity : ComponentActivity() {

    private val viewModel: YokaiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)

        setContent {
            YokaiHubTheme {
                YokaiHubRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "yokaihub" || (data.host == "localhost" && data.port == 8888)) {
            viewModel.handleIncomingOAuthCallbackUri(data)
        }
    }
}

@Composable
fun YokaiHubRootApp(viewModel: YokaiViewModel) {
    val outboundIp by viewModel.outboundIp.collectAsState()
    val githubAccount by viewModel.githubAccount.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ShrineObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShadowSlate,
                    titleContentColor = GhostWhite
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_yokai_logo),
                            contentDescription = "Yokai Hub Emblem",
                            modifier = Modifier.size(34.dp)
                        )
                        Column {
                            Text(
                                text = "YOKAI HUB",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = GhostWhite
                            )
                            Text(
                                text = if (githubAccount.isConnected) {
                                    "BYOK Spirit • @${githubAccount.username}"
                                } else {
                                    "BYOK AI + GitHub Autonomous Studio"
                                },
                                fontSize = 11.sp,
                                color = KitsuneCyan
                            )
                        }
                    }
                },
                actions = {
                    // Live Outbound IP Pill Indicator in App Header (1-Tap Copy + Refresh)
                    Surface(
                        color = ElevatedShrine,
                        border = BorderStroke(1.dp, KitsuneCyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(outboundIp))
                                viewModel.refreshOutboundIp()
                                Toast.makeText(
                                    context,
                                    "Live IP ($outboundIp) copied to clipboard!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Live Outbound IP",
                                tint = KitsuneCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = outboundIp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = KitsuneCyan
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy IP",
                                tint = MutedSpirit,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ShadowSlate,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                    label = { Text("Yokai Chat", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShrineObsidian,
                        selectedTextColor = KitsuneCyan,
                        indicatorColor = KitsuneCyan,
                        unselectedIconColor = MutedSpirit,
                        unselectedTextColor = MutedSpirit
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Extension, contentDescription = null) },
                    label = { Text("Plugins Hub", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShrineObsidian,
                        selectedTextColor = KitsuneCyan,
                        indicatorColor = KitsuneCyan,
                        unselectedIconColor = MutedSpirit,
                        unselectedTextColor = MutedSpirit
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("API Providers", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShrineObsidian,
                        selectedTextColor = KitsuneCyan,
                        indicatorColor = KitsuneCyan,
                        unselectedIconColor = MutedSpirit,
                        unselectedTextColor = MutedSpirit
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> YokaiChatScreen(
                    viewModel = viewModel,
                    onNavigateToProviders = { selectedTab = 2 },
                    onNavigateToPlugins = { selectedTab = 1 }
                )
                1 -> PluginsHubScreen(viewModel = viewModel)
                2 -> ApiProvidersScreen(viewModel = viewModel)
            }
        }
    }
}
