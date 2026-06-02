package com.jnetaol.btkbmouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

enum class SplitInputMode { INSTANT, CLIPBOARD, PRESET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitViewScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val clipboardEntries by viewModel.clipboardEntries.collectAsState()

    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var inputMode by remember { mutableStateOf(SplitInputMode.INSTANT) }
    var textInput by remember { mutableStateOf("") }
    var presetLabel by remember { mutableStateOf("") }
    var presetText by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }
    var showKeyboardPanel by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split View", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showKeyboardPanel = !showKeyboardPanel }) {
                        Icon(
                            if (showKeyboardPanel) Icons.Default.Keyboard else Icons.Default.Mouse,
                            "Toggle keyboard", tint = NeonBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground, titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)
        ) {
            ConnectionStatusBadge(
                isConnected = connectionState.isConnected,
                deviceName = connectionState.deviceName,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            ErrorDisplay(connectionState.error)

            NeonSlider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.1f..3.0f,
                label = "Sensitivity",
                displayValue = String.format("%.1fx", sensitivity),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SplitInputMode.values().forEach { mode ->
                    FilterChip(
                        selected = inputMode == mode,
                        onClick = { inputMode = mode },
                        label = {
                            Text(
                                when (mode) {
                                    SplitInputMode.INSTANT -> "Instant"
                                    SplitInputMode.CLIPBOARD -> "Clipboard"
                                    SplitInputMode.PRESET -> "Presets"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue.copy(alpha = 0.2f),
                            selectedLabelColor = NeonBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (showKeyboardPanel) {
                when (inputMode) {
                    SplitInputMode.INSTANT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newText ->
                                if (newText.length > textInput.length) {
                                    val added = newText.removePrefix(textInput)
                                    viewModel.sendText(added)
                                }
                                textInput = newText
                                if (newText.isEmpty()) textInput = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type to send keys...", color = TextTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonBlue
                            )
                        )
                    }
                    SplitInputMode.CLIPBOARD -> {
                        if (clipboardEntries.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clipboardEntries.takeLast(5).reversed().forEach { entry ->
                                    NeonCard(
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.sendText(entry.text) }
                                    ) {
                                        Text(
                                            entry.text.take(20) + if (entry.text.length > 20) "..." else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonBlue,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "No history yet. Send text from Instant mode first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                    SplitInputMode.PRESET -> {
                        if (presets.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { preset ->
                                    NeonCard(
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.sendText(preset.text) }
                                    ) {
                                        Text(
                                            preset.label.take(15),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonBlue,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "No presets. Add in Keyboard screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

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
                            viewModel.sendMouseDelta(dragAmount.x * sensitivity * 0.5f, dragAmount.y * sensitivity * 0.5f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (connectionState.isConnected) "Touchpad" else "Not Connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (connectionState.isConnected) TextSecondary else WarningOrange
                    )
                    if (connectionState.isConnected) {
                        Text(
                            "Swipe: Move | Tap: Left Click | Long Press: Right Click",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.TouchApp, "Left", modifier = Modifier.size(16.dp))
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
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Right", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Right Click", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(onClick = { viewModel.sendMouseWheel(-1f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowUpward, "Scroll Up", tint = TextSecondary)
                }

                IconButton(onClick = { viewModel.sendMouseWheel(1f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowDownward, "Scroll Down", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
