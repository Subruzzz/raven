package com.ravenguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ravenguard.app.ble.ConnectionState
import com.ravenguard.app.ui.components.ContactList
import com.ravenguard.app.ui.components.EmergencyButton
import com.ravenguard.app.ui.components.GlassCard
import com.ravenguard.app.ui.components.GlowButton
import com.ravenguard.app.ui.components.InfoCard
import com.ravenguard.app.ui.theme.NeonPurple
import com.ravenguard.app.viewmodel.DashboardUiState

@Composable
fun MainDashboardScreen(
    uiState: DashboardUiState,
    onSaveToWearable: () -> Unit,
    onToggleTest: () -> Unit,
    onStopPanic: () -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onDeleteContact: (com.ravenguard.app.data.database.ContactEntity) -> Unit
) {
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.Color(0xFF0A0A0A), androidx.compose.ui.graphics.Color(0xFF121225))
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "RavenGuard",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                GlowButton(
                    text = "SAVE TO WEARABLE",
                    onClick = onSaveToWearable,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard(
                        title = "Battery Percentage",
                        value = "Battery: ${uiState.batteryPercentage}%",
                        icon = Icons.Default.Battery4Bar,
                        highlighted = uiState.batteryPercentage <= 20
                    )
                    InfoCard(
                        title = "Connection Status",
                        value = if (uiState.connectionState == ConnectionState.Connected) "CONNECTED" else "DISCONNECTED",
                        icon = Icons.Default.Bluetooth,
                        highlighted = uiState.connectionState == ConnectionState.Connected
                    )
                    InfoCard(
                        title = "Distance",
                        value = "Distance: ${"%.0f".format(uiState.estimatedDistanceMeters)} meters",
                        icon = Icons.Default.LocationOn
                    )
                }

                GlassCard(modifier = Modifier.weight(1f).animateContentSize()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Emergency Contacts", fontWeight = FontWeight.SemiBold)
                        ContactList(contacts = uiState.contacts, onDelete = onDeleteContact)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            FloatingActionButton(
                                onClick = { showAddContactDialog = true },
                                shape = CircleShape,
                                containerColor = NeonPurple,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add contact")
                            }
                        }
                    }
                }
            }

            GlowButton(
                text = if (uiState.testBuzzerOn) "TEST WEARABLE (ON)" else "TEST WEARABLE",
                onClick = onToggleTest
            )

            EmergencyButton(onClick = onStopPanic)

            AnimatedVisibility(visible = !uiState.errorMessage.isNullOrBlank()) {
                GlassCard {
                    Text(text = uiState.errorMessage.orEmpty())
                }
            }
            Spacer(modifier = Modifier.size(20.dp))
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, phone, message ->
                onAddContact(name, phone, message)
                showAddContactDialog = false
            }
        )
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val padding by animateDpAsState(if (name.isBlank()) 0.dp else 4.dp, label = "contact-dialog")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, phone, message) }, enabled = name.isNotBlank() && phone.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
