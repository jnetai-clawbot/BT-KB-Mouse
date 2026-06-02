package com.jnetaol.btkbmouse.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.BuildConfig
import com.jnetaol.btkbmouse.logger.DebugLogger
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    val currentVersion = BuildConfig.VERSION_NAME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground, titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(64.dp)
                )
            }

            item {
                Text(
                    "BT KB & Mouse Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Text(
                    "Professional Bluetooth Keyboard & Mouse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            item {
                NeonCard {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Version", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                        Text(
                            currentVersion,
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                        if (updateMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                updateMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (updateMessage.contains("Update available")) WarningOrange else SuccessGreen
                            )
                        }
                    }
                }
            }

            item {
                Text("Made by", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                Text(
                    "jnetai.com",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                GlowButton(
                    text = if (isChecking) "Checking..." else "Check for Updates",
                    icon = Icons.Default.Update,
                    onClick = {
                        isChecking = true
                        updateMessage = ""
                        scope.launch {
                            try {
                                val url = URL("https://api.github.com/repos/jnetai-clawbot/BT-KB-Mouse/releases/latest")
                                val connection = url.openConnection() as HttpURLConnection
                                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                                connection.connectTimeout = 10000
                                connection.readTimeout = 10000

                                if (connection.responseCode == 200) {
                                    val response = connection.inputStream.bufferedReader().readText()
                                    val tagPattern = """\"tag_name\":\"v([^\"]+)\"""".toRegex()
                                    val match = tagPattern.find(response)
                                    val latestVersion = match?.groupValues?.get(1)
                                    if (latestVersion != null && latestVersion != currentVersion) {
                                        updateMessage = "Update available: v$latestVersion"
                                        DebugLogger.i("About", "BK-200 Update available: $latestVersion")
                                    } else {
                                        updateMessage = "You are up to date!"
                                        DebugLogger.i("About", "BK-201 Already latest")
                                    }
                                } else {
                                    updateMessage = "Could not check updates"
                                    DebugLogger.w("About", "BK-202 Update check failed: ${connection.responseCode}")
                                }
                                connection.disconnect()
                            } catch (e: Exception) {
                                updateMessage = "Could not check updates"
                                DebugLogger.e("About", "BK-014 Update check error", e)
                            } finally {
                                isChecking = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking
                )
            }

            item {
                GlowButton(
                    text = "Share App",
                    icon = Icons.Default.Share,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "BT KB & Mouse Pro")
                                putExtra(Intent.EXTRA_TEXT, "Control your devices wirelessly with BT KB & Mouse Pro!\n\nDownload: https://github.com/jnetai-clawbot/BT-KB-Mouse/releases\n\nMade by jnetai.com")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                            DebugLogger.i("About", "BK-203 Sharing app")
                        } catch (e: Exception) {
                            DebugLogger.e("About", "BK-015 Share error", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Control any device with Bluetooth connectivity — PC, laptop, Mac, Raspberry Pi, mobile, tablet and more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Supports keyboard, mouse, speaker, microphone, webcam and gamepad emulation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
