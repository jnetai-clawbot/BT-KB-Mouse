package com.jnetaol.btkbmouse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()
    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var scrollSpeed by remember { mutableFloatStateOf(1.0f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touchpad", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionStatusBadge(
                isConnected = connectionState.isConnected,
                deviceName = connectionState.deviceName,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            ErrorDisplay(connectionState.error)
            Spacer(Modifier.height(8.dp))

            NeonSlider(
                value = sensitivity,
                onValueChange = {
                    sensitivity = it
                    viewModel.saveSetting("mouse_sensitivity", String.format("%.2f", it))
                },
                valueRange = 0.1f..3.0f,
                label = "Sensitivity",
                displayValue = String.format("%.1fx", sensitivity),
                modifier = Modifier.fillMaxWidth()
            )

            NeonSlider(
                value = scrollSpeed,
                onValueChange = {
                    scrollSpeed = it
                    viewModel.saveSetting("scroll_speed", String.format("%.2f", it))
                },
                valueRange = 0.1f..5.0f,
                label = "Scroll Speed",
                displayValue = String.format("%.1fx", scrollSpeed),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.sendMouseDelta(dragAmount.x * sensitivity, dragAmount.y * sensitivity)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.sendMouseLeftClick(true)
                                scope.launch { delay(60); viewModel.sendMouseLeftClick(false) }
                            },
                            onDoubleTap = {
                                viewModel.sendMouseLeftClick(true)
                                scope.launch { delay(60); viewModel.sendMouseLeftClick(false) }
                            },
                            onLongPress = {
                                viewModel.sendMouseRightClick(true)
                                scope.launch { delay(60); viewModel.sendMouseRightClick(false) }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (connectionState.isConnected) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Mouse, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Swipe: Move | Tap: Left Click | Long Press: Right Click", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                } else {
                    Text("Not connected", style = MaterialTheme.typography.bodyMedium, color = WarningOrange, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.sendMouseLeftClick(true)
                        scope.launch { delay(60); viewModel.sendMouseLeftClick(false) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.TouchApp, "Left Click", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Left Click", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        viewModel.sendMouseRightClick(true)
                        scope.launch { delay(60); viewModel.sendMouseRightClick(false) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.TouchApp, "Right Click", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Right Click", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        viewModel.sendMouseMiddleClick(true)
                        scope.launch { delay(60); viewModel.sendMouseMiddleClick(false) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Middle", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Middle", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.sendMouseWheel(-scrollSpeed) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ArrowUpward, "Scroll Up", tint = TextSecondary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("Scroll", style = MaterialTheme.typography.bodySmall, color = TextTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.sendMouseWheel(scrollSpeed) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ArrowDownward, "Scroll Down", tint = TextSecondary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
