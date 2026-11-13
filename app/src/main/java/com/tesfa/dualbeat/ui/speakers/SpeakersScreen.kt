package com.tesfa.dualbeat.ui.speakers

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        contentPadding = PaddingValues(16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Audio Route Card
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

                    // Multi-Output / Samsung Dual Audio Status Tag (Honest, System-Managed Status)
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

        // Contextual Bluetooth Permission Card (if not yet granted on Android 12+)
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

        // Hardware Investigation & Limitation Transparency Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Multi-Bluetooth Audio Architecture",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = report.hardwareLimitationNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Android Bluetooth Settings")
                    }
                }
            }
        }

        // Paired & Connected Bluetooth Devices Section
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
                TextButton(onClick = { viewModel.refreshAll() }) {
                    Text("Refresh")
                }
            }
        }

        if (state.bondedDevices.isEmpty()) {
            item {
                Text(
                    text = "No paired Bluetooth audio devices found. Tap above to pair speakers in Android Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.bondedDevices) { device ->
                BluetoothDeviceCard(device = device)
            }
        }

        // Wi-Fi Multi-Speaker (Cast) Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wi-Fi Multi-Speaker (DualBeat Cast)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stream audio across phones or receiver devices over local LAN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Receiver Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Listen as Audio Receiver",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Turn this phone into a remote wireless speaker for other devices",
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
                        Text(
                            text = "Receiver listening on TCP port 4242. Stream audio to this device from another DualBeat transmitter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Transmitter Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Broadcast System Audio",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stream audio to discovered Wi-Fi receivers over local network",
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "DualBeat Cast Server is running on port 4242. Discovered receivers will appear below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Discovered Wi-Fi Receivers List
        if (state.isTransmitterRunning) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Wi-Fi Receivers (${state.discoveredReceivers.size})",
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
                        text = "Searching for DualBeat Cast receivers on Wi-Fi subnet…",
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
