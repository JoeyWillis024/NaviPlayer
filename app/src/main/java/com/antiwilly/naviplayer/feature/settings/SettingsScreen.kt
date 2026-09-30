package com.antiwilly.naviplayer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.model.TranscodeFormat

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Server Profiles Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Navidrome Servers", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { showAddServerDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Server")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (allProfiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No servers added yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add your Navidrome server address and credentials to start streaming.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allProfiles) { profile ->
                val isSelected = profile.id == activeProfile?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.setActiveProfile(profile.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setActiveProfile(profile.id) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${profile.username} @ ${profile.baseUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Server",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Audio Quality & Transcoding Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Streaming Quality", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "WiFi Streaming", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Direct Stream / Original Lossless (FLAC, MP3, AAC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Cellular Streaming", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Adaptive Transcoding (Opus @ 192 kbps)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Server Dialog
    if (showAddServerDialog) {
        var serverName by remember { mutableStateOf("") }
        var serverUrl by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var customHeaderKey by remember { mutableStateOf("") }
        var customHeaderValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddServerDialog = false
                viewModel.clearConnectionTestResult()
            },
            title = { Text("Connect to Navidrome") },
            text = {
                Column {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Name (e.g. Home Server)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Base URL (e.g. https://music.example.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reverse Proxy Header (Optional)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customHeaderKey,
                            onValueChange = { customHeaderKey = it },
                            placeholder = { Text("Header Key") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = customHeaderValue,
                            onValueChange = { customHeaderValue = it },
                            placeholder = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (connectionTestResult != null) {
                        Text(
                            text = connectionTestResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (connectionTestResult!!.startsWith("Connection successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverName.isNotBlank() && serverUrl.isNotBlank() && username.isNotBlank()) {
                            val headers = if (customHeaderKey.isNotBlank() && customHeaderValue.isNotBlank()) {
                                mapOf(customHeaderKey.trim() to customHeaderValue.trim())
                            } else emptyMap()

                            viewModel.saveProfile(
                                name = serverName.trim(),
                                baseUrl = serverUrl.trim(),
                                username = username.trim(),
                                passwordOrToken = password.trim(),
                                customHeaders = headers
                            )
                            showAddServerDialog = false
                            viewModel.clearConnectionTestResult()
                        }
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val headers = if (customHeaderKey.isNotBlank() && customHeaderValue.isNotBlank()) {
                            mapOf(customHeaderKey.trim() to customHeaderValue.trim())
                        } else emptyMap()
                        viewModel.testConnection(serverUrl.trim(), username.trim(), password.trim(), headers)
                    }
                ) {
                    Text("Test Connection")
                }
            }
        )
    }
}
