package com.ravenguard.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.ravenguard.app.ble.BleManager
import com.ravenguard.app.data.repository.LocationRepository
import com.ravenguard.app.data.repository.SmsRepository
import com.ravenguard.app.ui.screens.MainDashboardScreen
import com.ravenguard.app.ui.theme.RavenGuardTheme
import com.ravenguard.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val viewModel: MainViewModel by viewModels {
        val app = application as RavenGuardApp
        MainViewModel.Factory(
            bleManager = BleManager(this),
            contactRepository = app.contactRepository,
            locationRepository = LocationRepository(this),
            smsRepository = SmsRepository()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()

        setContent {
            RavenGuardTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainDashboardScreen(
                    uiState = uiState,
                    onSaveToWearable = { viewModel.toggleTestWearable() },
                    onToggleTest = viewModel::toggleTestWearable,
                    onStopPanic = viewModel::stopPanic,
                    onAddContact = viewModel::addContact,
                    onDeleteContact = viewModel::removeContact
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
