package com.jnetaol.btkbmouse.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.data.model.PresetPhrase
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*

enum class InputMode { INSTANT, CLIPBOARD, PRESET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val clipboardEntries by viewModel.clipboardEntries.collectAsState()

    var inputMode by remember { mutableStateOf(InputMode.INSTANT) }
    var textInput by remember { mutableStateOf("") }
    var presetLabel by remember { mutableStateOf("") }
    var presetText by remember { mutableStateOf("") }
    var keyboardDelay by remember { mutableFloatStateOf(100f) }
    var repeatDelay by remember { mutableFloatStateOf(500f) }
    var showAddPreset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Input", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ConnectionStatusBadge(
                    isConnected = connectionState.isConnected,
                    deviceName = connectionState.deviceName,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { ErrorDisplay(connectionState.error) }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        Text("Input Mode", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InputMode.values().forEach { mode ->
                                FilterChip(
                                    selected = inputMode == mode,
                                    onClick = { inputMode = mode },
                                    label = {
                                        Text(
                                            when (mode) {
                                                InputMode.INSTANT -> "Instant"
                                                InputMode.CLIPBOARD -> "Clipboard"
                                                InputMode.PRESET -> "Presets"
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
                    }
                }
            }

            when (inputMode) {
                InputMode.INSTANT -> {
                    item {
                        NeonCard {
                            Column(Modifier.padding(12.dp)) {
                                Text("Type to send instantly", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { newText ->
                                        if (newText.length > textInput.length) {
                                            val added = newText.removePrefix(textInput)
                                            viewModel.sendText(added)
                                        }
                                        textInput = newText
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Type here...", color = TextTertiary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonBlue,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = NeonBlue
                                    ),
                                    singleLine = false,
                                    maxLines = 4
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlowButton(
                                        text = "Send",
                                        icon = Icons.Default.Send,
                                        onClick = {
                                            viewModel.sendText(textInput)
                                            viewModel.addClipboardEntry(textInput)
                                        },
                                        enabled = textInput.isNotBlank() && connectionState.isConnected,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { textInput = "" }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Clear, "Clear", tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        NeonSlider(
                            value = keyboardDelay,
                            onValueChange = {
                                keyboardDelay = it
                                viewModel.saveSetting("keyboard_delay", it.toInt().toString())
                            },
                            valueRange = 0f..500f,
                            label = "Key Send Delay",
                            displayValue = "${keyboardDelay.toInt()}ms",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        NeonSlider(
                            value = repeatDelay,
                            onValueChange = {
                                repeatDelay = it
                                viewModel.saveSetting("keyboard_repeat_delay", it.toInt().toString())
                            },
                            valueRange = 100f..2000f,
                            label = "Repeat Delay",
                            displayValue = "${repeatDelay.toInt()}ms",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                InputMode.CLIPBOARD -> {
                    item {
                        NeonCard {
                            Column(Modifier.padding(12.dp)) {
                                Text("Paste & Send as String", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Paste clipboard text...", color = TextTertiary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonBlue,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = NeonBlue
                                    ),
                                    singleLine = false,
                                    maxLines = 6
                                )
                                Spacer(Modifier.height(8.dp))
                                GlowButton(
                                    text = "Send as String",
                                    icon = Icons.Default.Send,
                                    onClick = {
                                        viewModel.sendText(textInput)
                                        viewModel.addClipboardEntry(textInput)
                                    },
                                    enabled = textInput.isNotBlank() && connectionState.isConnected,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item { SectionHeader("Sent History") }
                    if (clipboardEntries.isEmpty()) {
                        item {
                            Text("No history yet", style = MaterialTheme.typography.bodySmall, color = TextTertiary, modifier = Modifier.padding(16.dp))
                        }
                    }
                    items(clipboardEntries.take(20)) { entry ->
                        NeonCard(onClick = { textInput = entry.text }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    entry.text.take(100) + if (entry.text.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }

                InputMode.PRESET -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("Preset Phrases")
                            TextButton(onClick = { showAddPreset = !showAddPreset }) {
                                Icon(
                                    if (showAddPreset) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showAddPreset) "Cancel" else "Add", color = NeonBlue)
                            }
                        }
                    }

                    if (showAddPreset) {
                        item {
                            NeonCard {
                                Column(Modifier.padding(12.dp)) {
                                    OutlinedTextField(
                                        value = presetLabel,
                                        onValueChange = { presetLabel = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Label...", color = TextTertiary) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = NeonBlue
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = presetText,
                                        onValueChange = { presetText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Phrase text...", color = TextTertiary) },
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = NeonBlue
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    GlowButton(
                                        text = "Save Preset",
                                        icon = Icons.Default.Save,
                                        onClick = {
                                            if (presetLabel.isNotBlank() && presetText.isNotBlank()) {
                                                viewModel.addPresetPhrase(presetLabel.trim(), presetText.trim())
                                                presetLabel = ""
                                                presetText = ""
                                                showAddPreset = false
                                            }
                                        },
                                        enabled = presetLabel.isNotBlank() && presetText.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (presets.isEmpty() && !showAddPreset) {
                        item {
                            Text("No presets yet. Add one above.", style = MaterialTheme.typography.bodyMedium, color = TextTertiary, modifier = Modifier.padding(16.dp))
                        }
                    }

                    items(presets) { preset ->
                        NeonCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(preset.label, style = MaterialTheme.typography.bodyMedium, color = NeonBlue, fontWeight = FontWeight.Medium)
                                    Text(
                                        preset.text.take(80) + if (preset.text.length > 80) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.sendText(preset.text) },
                                    enabled = connectionState.isConnected
                                ) {
                                    Icon(
                                        Icons.Default.Send, "Send",
                                        tint = if (connectionState.isConnected) SuccessGreen else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.deletePresetPhrase(preset) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
