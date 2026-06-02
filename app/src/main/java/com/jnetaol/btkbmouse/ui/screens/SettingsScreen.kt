package com.jnetaol.btkbmouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.data.model.Profile
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val emulatedDevices by viewModel.emulatedDevices.collectAsState()

    var showAddProfile by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var mouseSensitivity by remember { mutableFloatStateOf(1.0f) }
    var mouseScrollSpeed by remember { mutableFloatStateOf(1.0f) }
    var keyboardDelay by remember { mutableFloatStateOf(100f) }
    var repeatDelay by remember { mutableFloatStateOf(500f) }
    var speakerVolume by remember { mutableFloatStateOf(0.5f) }
    var micGain by remember { mutableFloatStateOf(0.5f) }
    var selectedProfileId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(profiles) {
        val active = profiles.find { it.isActive }
        if (active != null && selectedProfileId == 0L) {
            selectedProfileId = active.id
            mouseSensitivity = active.mouseSensitivity
            mouseScrollSpeed = active.mouseScrollSpeed
            keyboardDelay = active.keyboardDelay.toFloat()
            repeatDelay = active.keyboardRepeatDelay.toFloat()
            speakerVolume = active.speakerVolume
            micGain = active.micGain
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.revertSettings()
                        mouseSensitivity = 1.0f
                        mouseScrollSpeed = 1.0f
                        keyboardDelay = 100f
                        repeatDelay = 500f
                        speakerVolume = 0.5f
                        micGain = 0.5f
                    }) {
                        Icon(Icons.Default.Undo, null, tint = WarningOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Revert", color = WarningOrange)
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
            item { SectionHeader("Profiles") }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Profile", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            TextButton(onClick = { showAddProfile = !showAddProfile }) {
                                Icon(
                                    if (showAddProfile) Icons.Default.Close else Icons.Default.Add,
                                    null, tint = NeonBlue, modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showAddProfile) "Cancel" else "New Profile", color = NeonBlue)
                            }
                        }

                        if (showAddProfile) {
                            OutlinedTextField(
                                value = newProfileName,
                                onValueChange = { newProfileName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Profile name...", color = TextTertiary) },
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
                            GlowButton(
                                text = "Create Profile",
                                icon = Icons.Default.Save,
                                onClick = {
                                    if (newProfileName.isNotBlank()) {
                                        viewModel.createProfile(newProfileName.trim())
                                        newProfileName = ""
                                        showAddProfile = false
                                    }
                                },
                                enabled = newProfileName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        if (profiles.isEmpty()) {
                            Text("No profiles created", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }

                        profiles.forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProfileId = profile.id
                                        viewModel.setActiveProfile(profile)
                                        mouseSensitivity = profile.mouseSensitivity
                                        mouseScrollSpeed = profile.mouseScrollSpeed
                                        keyboardDelay = profile.keyboardDelay.toFloat()
                                        repeatDelay = profile.keyboardRepeatDelay.toFloat()
                                        speakerVolume = profile.speakerVolume
                                        micGain = profile.micGain
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = profile.id == selectedProfileId,
                                    onClick = {
                                        selectedProfileId = profile.id
                                        viewModel.setActiveProfile(profile)
                                        mouseSensitivity = profile.mouseSensitivity
                                        mouseScrollSpeed = profile.mouseScrollSpeed
                                        keyboardDelay = profile.keyboardDelay.toFloat()
                                        repeatDelay = profile.keyboardRepeatDelay.toFloat()
                                        speakerVolume = profile.speakerVolume
                                        micGain = profile.micGain
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonBlue)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    if (profile.deviceName.isNotBlank()) {
                                        Text(profile.deviceName, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                                    Icon(Icons.Default.Close, "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Emulated Devices") }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        emulatedDevices.forEach { device ->
                            SettingsToggle(
                                label = device.label,
                                isChecked = device.isEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleEmulatedDevice(device.deviceType, enabled)
                                },
                                icon = when (device.iconName) {
                                    "keyboard" -> Icons.Default.Keyboard
                                    "mouse" -> Icons.Default.Mouse
                                    "speaker" -> Icons.Default.Speaker
                                    "mic" -> Icons.Default.Mic
                                    "videocam" -> Icons.Default.Videocam
                                    "gamepad" -> Icons.Default.Gamepad
                                    "scanner" -> Icons.Default.Scanner
                                    else -> Icons.Default.DevicesOther
                                }
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Mouse Settings") }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        NeonSlider(
                            value = mouseSensitivity,
                            onValueChange = {
                                mouseSensitivity = it
                                viewModel.saveSetting("mouse_sensitivity", String.format("%.2f", it))
                            },
                            valueRange = 0.1f..3.0f,
                            label = "Mouse Sensitivity",
                            displayValue = String.format("%.1fx", mouseSensitivity)
                        )
                        Spacer(Modifier.height(8.dp))
                        NeonSlider(
                            value = mouseScrollSpeed,
                            onValueChange = {
                                mouseScrollSpeed = it
                                viewModel.saveSetting("scroll_speed", String.format("%.2f", it))
                            },
                            valueRange = 0.1f..5.0f,
                            label = "Scroll Speed",
                            displayValue = String.format("%.1fx", mouseScrollSpeed)
                        )
                    }
                }
            }

            item { SectionHeader("Keyboard Settings") }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        NeonSlider(
                            value = keyboardDelay,
                            onValueChange = {
                                keyboardDelay = it
                                viewModel.saveSetting("keyboard_delay", it.toInt().toString())
                            },
                            valueRange = 0f..500f,
                            label = "Key Send Delay",
                            displayValue = "${keyboardDelay.toInt()}ms"
                        )
                        Spacer(Modifier.height(8.dp))
                        NeonSlider(
                            value = repeatDelay,
                            onValueChange = {
                                repeatDelay = it
                                viewModel.saveSetting("keyboard_repeat_delay", it.toInt().toString())
                            },
                            valueRange = 100f..2000f,
                            label = "Repeat Delay",
                            displayValue = "${repeatDelay.toInt()}ms"
                        )
                    }
                }
            }

            item { SectionHeader("Audio Settings") }

            item {
                NeonCard {
                    Column(Modifier.padding(12.dp)) {
                        NeonSlider(
                            value = speakerVolume,
                            onValueChange = {
                                speakerVolume = it
                                viewModel.saveSetting("speaker_volume", String.format("%.2f", it))
                            },
                            valueRange = 0f..1f,
                            label = "Speaker Volume",
                            displayValue = "${(speakerVolume * 100).toInt()}%"
                        )
                        Spacer(Modifier.height(8.dp))
                        NeonSlider(
                            value = micGain,
                            onValueChange = {
                                micGain = it
                                viewModel.saveSetting("mic_gain", String.format("%.2f", it))
                            },
                            valueRange = 0f..1f,
                            label = "Microphone Gain",
                            displayValue = "${(micGain * 100).toInt()}%"
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
