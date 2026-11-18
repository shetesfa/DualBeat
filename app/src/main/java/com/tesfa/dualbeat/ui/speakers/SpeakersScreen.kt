package com.tesfa.dualbeat.ui.speakers

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesfa.dualbeat.core.util.PermissionManager
import com.tesfa.dualbeat.domain.model.DualAudioDetectionStatus
import com.tesfa.dualbeat.ui.components.BluetoothDeviceCard
import com.tesfa.dualbeat.ui.components.ContextualPermissionCard
import com.tesfa.dualbeat.ui.components.WiFilReceiverCard

@Composable
fun SpeakersScreen(
    viewModel: SpeakersViewModel
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val report = state.routeReport

    var hasBluetoothPermission by remember {
        mutableStateOf(PermissionManager.hasBluetoothPermissions(context))
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasBluetoothPermission = PermissionManager.hasBluetoothPermissions(context)
        if (hasBluetoothPermission) {
            viewModel.refreshAll()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Easy Step-by-Step Multi-Speaker Setup Guide Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SpeakerGroup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DualBeat 2-Speaker Dual Audio Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Connect Phone 1 (Galaxy A24) to Speaker A via Bluetooth.\n" +
                               "2. Connect Phone 2 to Speaker B via Bluetooth.\n" +
                               "3. Connect both phones via Wi-Fi or Phone 1's Mobile Hotspot.\n" +
                               "4. Turn ON 'Receiver Mode' on Phone 2.\n" +
                               "5. Turn ON 'Transmitter Mode' on Phone 1, tap Connect (or Direct IP), then play music!",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Connection Status Banner
        if (!state.connectionStatusMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.connectionStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // 2. Active Audio Output Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Audio Output",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = report.activeOutputName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (report.dualAudioStatus) {
                            DualAudioDetectionStatus.SYSTEM_MANAGED -> MaterialTheme.colorScheme.primaryContainer
                            DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceDim
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Routing: ${report.dualAudioStatus.displayText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Contextual Bluetooth Permission Card
        if (!hasBluetoothPermission) {
            item {
                ContextualPermissionCard(
                    icon = Icons.Rounded.Bluetooth,
                    title = "Bluetooth Permission Needed",
                    description = "DualBeat requires Bluetooth Nearby Devices permission to detect your connected wireless headphones and speakers.",
                    onRequestPermission = {
                        bluetoothPermissionLauncher.launch(PermissionManager.getRequiredBluetoothPermissions())
                    },
                    onOpenSettings = {
                        PermissionManager.openAppSettings(context)
                    }
                )
            }
        }

        // Paired Bluetooth Devices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paired Bluetooth Speakers",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(imageVector = Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BT Settings", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (state.bondedDevices.isEmpty()) {
            item {
                Text(
                    text = "No paired Bluetooth audio devices found. Connect your Bluetooth speaker in Android Bluetooth Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.bondedDevices) { device ->
                BluetoothDeviceCard(device = device)
            }
        }

        // 3. Wi-Fi Sound Mesh (DualBeat Cast) Hub
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Sound Mesh Network (DualBeat Cast)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sync audio between two phones so each phone plays through its own Bluetooth speaker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local IP & Network Info Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This Phone IP: ${state.localIpAddress}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refreshAll() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh IP", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // MODE A: Receiver Mode (Use THIS Phone as Speaker 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Receiver Mode (Phone 2)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Turn ON if this is the second phone playing Speaker 2.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isReceiverModeActive,
                            onCheckedChange = { viewModel.toggleReceiverMode(it) }
                        )
                    }

                    if (state.isReceiverModeActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "RECEIVER ACTIVE - LISTENING",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Broadcasting discovery on Wi-Fi/Hotspot port 8990 & 8991.\nOn Phone 1, look for this phone in the Discovered list or enter IP '${state.localIpAddress}'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // MODE B: Transmitter Mode (Use THIS Phone to Stream Audio - Galaxy A24)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transmitter Mode (Phone 1 - Music Source)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Turn ON on your main phone (Galaxy A24) to discover Phone 2 and stream your music to it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isTransmitterRunning,
                            onCheckedChange = { viewModel.toggleTransmitter() }
                        )
                    }

                    if (state.isTransmitterRunning) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct IP Connect Bar (Guaranteed Fallback)
                        Text(
                            text = "Direct Connect by IP (Guaranteed Link):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.manualIpInput,
                                onValueChange = { viewModel.setManualIpInput(it) },
                                placeholder = { Text("e.g. 192.168.43.155") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { viewModel.connectDirectByIp() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }

        // 4. Discovered Wi-Fi Receivers List
        if (state.isTransmitterRunning) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Receivers (${state.discoveredReceivers.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { viewModel.scanReceivers() }) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Scan")
                    }
                }
            }

            if (state.discoveredReceivers.isEmpty()) {
                item {
                    Text(
                        text = "Searching for Phone 2 over Wi-Fi / Hotspot… Make sure Receiver Mode is ON on Phone 2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.discoveredReceivers) { receiver ->
                    WiFilReceiverCard(
                        node = receiver,
                        onConnectToggle = { viewModel.toggleReceiverConnection(receiver) }
                    )
                }
            }
        }
    }
}
