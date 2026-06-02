package com.jnetaol.btkbmouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BT KB & Mouse Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onNavigate("about") }) {
                        Icon(Icons.Default.Info, "About", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            ConnectionStatusBadge(
                isConnected = connectionState.isConnected,
                deviceName = connectionState.deviceName,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            ErrorDisplay(connectionState.error)
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (connectionState.isConnected) NeonBlue else TextTertiary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (connectionState.isConnected) "Connected to ${connectionState.deviceName}" else "Not Connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (connectionState.isConnected) SuccessGreen else TextTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (connectionState.isConnected) "Select a function below to control your device" else "Connect to a Bluetooth device to begin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FunctionCard(title = "Touchpad", icon = Icons.Default.Mouse, color = NeonGreen, modifier = Modifier.weight(1f), onClick = { onNavigate("touchpad") })
                FunctionCard(title = "Keyboard", icon = Icons.Default.Keyboard, color = NeonBlue, modifier = Modifier.weight(1f), onClick = { onNavigate("keyboard") })
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FunctionCard(title = "Connect", icon = Icons.Default.Bluetooth, color = NeonIndigo, modifier = Modifier.weight(1f), onClick = { onNavigate("connection") })
                FunctionCard(title = "Settings", icon = Icons.Default.Settings, color = ElectricViolet, modifier = Modifier.weight(1f), onClick = { onNavigate("settings") })
            }

            Spacer(Modifier.height(12.dp))

            NeonCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate("splitview") }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = "Split View", tint = CyanAccent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Split View", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("(KB + Mouse)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun FunctionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeonCard(modifier = modifier.aspectRatio(1f), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, textAlign = TextAlign.Center)
        }
    }
}
