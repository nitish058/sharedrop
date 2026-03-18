package org.nitish.project.sharedrop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

@Composable
fun App() {
    MaterialTheme {
        HomeScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val nearbyDevices = remember { mutableStateListOf<DiscoveredDevice>() }
    val lastSeenMap = remember { mutableMapOf<String, Long>() }

    val discovery = remember { DeviceDiscovery() }
    val advertiser = remember { DeviceAdvertiser() }
    val receiver = remember { FileReceiver() }
    val scope = rememberCoroutineScope()
    val receivedFiles = remember { mutableStateListOf<String>() }
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var transferProgress by remember { mutableStateOf(0f) }
    
    val settings = remember { Settings(dataStore = provideDataStore()) }
    val appSettings by settings.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    val localDeviceName = appSettings.localName

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(localDeviceName, lifecycleState) {
        if (localDeviceName == null) return@LaunchedEffect

        if (lifecycleState == Lifecycle.State.RESUMED) {
            advertiser.startAdvertising(localDeviceName, 8080)

            discovery.startDiscovery { device ->
                // Listener: Updates lastSeen timestamp and adds new devices
                scope.launch(Dispatchers.Main) {
                    val currentTime = Clock.System.now().toEpochMilliseconds()
                    lastSeenMap[device.host] = currentTime

                    val existingIndex = nearbyDevices.indexOfFirst { it.host == device.host }
                    if (existingIndex == -1) {
                        if (device.name != localDeviceName) {
                            nearbyDevices.add(device)
                        }
                    } else if (nearbyDevices[existingIndex].name != device.name) {
                        nearbyDevices[existingIndex] = device
                    }
                }
            }
        }

        // Pruning Loop: Removes devices that haven't responded for >10s
        launch {
            while (isActive) {
                delay(2000)
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val timeout = 10000

                val expiredHosts = lastSeenMap.filter { (_, lastSeen) ->
                    currentTime - lastSeen > timeout
                }.keys

                if (expiredHosts.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        expiredHosts.forEach { host ->
                            nearbyDevices.removeAll { it.host == host }
                            lastSeenMap.remove(host)
                        }
                    }
                }
            }
        }

        receiver.startReceiving(8080) { fileName, bytes ->
            FileSaver().saveFile(fileName, bytes) { success, path ->
                receivedFiles.add(fileName)
                statusMessage = if (success) {
                    "Saved: $fileName to $path"
                } else {
                    "Received but failed to save: $fileName"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            advertiser.stopAdvertising()
            discovery.stopDiscovery()
            receiver.stopReceiving()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShareDrop") }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (statusMessage.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )

                        if (transferProgress > 0f && transferProgress < 1f) {
                            Text(
                                text = "${(transferProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (transferProgress > 0f && transferProgress < 1f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { transferProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val filePicker = remember { FilePicker() }

                Button(
                    onClick = {
                        val device = selectedDevice
                        if (device == null) {
                            statusMessage = "Please select a device first!"
                        } else {
                            filePicker.pickFile { fileName, bytes ->
                                statusMessage = "Sending $fileName..."
                                transferProgress = 0.01f

                                FileSender().sendFile(
                                    host = device.host,
                                    port = device.port,
                                    fileName = fileName,
                                    bytes = bytes,
                                    onProgress = { progress ->
                                        scope.launch(Dispatchers.Main) {
                                            transferProgress = progress
                                        }
                                    },
                                    onResult = { success ->
                                        scope.launch(Dispatchers.Main) {
                                            statusMessage = if (success) "Sent $fileName!" else "Failed!"
                                            transferProgress = 0f
                                        }
                                    }
                                )
                            }
                        }
                    }
                ){
                    Text(if (selectedDevice == null) "Select a device to send" else "Send File to ${selectedDevice!!.name}")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Your Device: ")
                    withStyle(
                        MaterialTheme.typography.bodyMedium.toSpanStyle()
                            .copy(fontWeight = FontWeight.SemiBold)
                    ) {
                        append(localDeviceName ?: "Loading...")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nearby Devices",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (nearbyDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Searching for devices...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(nearbyDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedDevice = if (selectedDevice == device) null else device
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDevice == device)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${device.host}:${device.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (receivedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Received Files",
                        style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(receivedFiles) { fileName ->
                        Text(
                            text = "📄 $fileName",
                                modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
