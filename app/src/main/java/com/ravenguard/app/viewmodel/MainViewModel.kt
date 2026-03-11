package com.ravenguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ravenguard.app.ble.AlertType
import com.ravenguard.app.ble.BleCommand
import com.ravenguard.app.ble.BleConstants
import com.ravenguard.app.ble.BleEvent
import com.ravenguard.app.ble.BleManager
import com.ravenguard.app.ble.ConnectionState
import com.ravenguard.app.data.database.ContactEntity
import com.ravenguard.app.data.repository.ContactRepository
import com.ravenguard.app.data.repository.LocationRepository
import com.ravenguard.app.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow

private data class DashboardMutableState(
    val batteryPercentage: Int = 0,
    val rssi: Int = -65,
    val testBuzzerOn: Boolean = false,
    val errorMessage: String? = null,
    val lastAlert: AlertType? = null
)

data class DashboardUiState(
    val batteryPercentage: Int,
    val connectionState: ConnectionState,
    val estimatedDistanceMeters: Double,
    val contacts: List<ContactEntity>,
    val testBuzzerOn: Boolean,
    val errorMessage: String?,
    val lastAlert: AlertType?
)

class MainViewModel(
    private val bleManager: BleManager,
    private val contactRepository: ContactRepository,
    private val locationRepository: LocationRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(DashboardMutableState())

    val uiState: StateFlow<DashboardUiState> = combine(
        internalState,
        bleManager.connectionState,
        contactRepository.observeContacts()
    ) { internal, connection, contacts ->
        DashboardUiState(
            batteryPercentage = internal.batteryPercentage,
            connectionState = connection,
            estimatedDistanceMeters = estimateDistance(internal.rssi),
            contacts = contacts,
            testBuzzerOn = internal.testBuzzerOn,
            errorMessage = internal.errorMessage,
            lastAlert = internal.lastAlert
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(
            batteryPercentage = 0,
            connectionState = ConnectionState.Disconnected,
            estimatedDistanceMeters = estimateDistance(-65),
            contacts = emptyList(),
            testBuzzerOn = false,
            errorMessage = null,
            lastAlert = null
        )
    )

    init {
        bleManager.startScan()
        observeBleEvents()
    }

    private fun observeBleEvents() {
        viewModelScope.launch {
            bleManager.events.collect { event ->
                when (event) {
                    is BleEvent.DeviceStatus -> {
                        internalState.value = internalState.value.copy(
                            batteryPercentage = event.battery ?: internalState.value.batteryPercentage,
                            rssi = event.rssi ?: internalState.value.rssi
                        )
                    }

                    is BleEvent.Alert -> {
                        internalState.value = internalState.value.copy(lastAlert = event.type)
                        handleEmergencyAlert(event.type)
                    }

                    is BleEvent.Error -> {
                        internalState.value = internalState.value.copy(errorMessage = event.message)
                    }

                    is BleEvent.Ack -> Unit
                }
            }
        }
    }

    private fun handleEmergencyAlert(alertType: AlertType) {
        viewModelScope.launch {
            val locationUrl = locationRepository.getCurrentLocationUrl()
            val baseMessage = "Emergency! I need help.\nTrigger: ${alertType.name}\nLocation:\n$locationUrl"
            val contacts = uiState.value.contacts
            val sendResult = smsRepository.sendEmergencySms(contacts, baseMessage)
            if (sendResult.isSuccess) {
                bleManager.sendCommand(BleCommand.AlertSentAck)
            } else {
                internalState.value = internalState.value.copy(
                    errorMessage = sendResult.exceptionOrNull()?.message ?: "SMS dispatch failed"
                )
            }
        }
    }

    fun toggleTestWearable() {
        val nextState = !internalState.value.testBuzzerOn
        internalState.value = internalState.value.copy(testBuzzerOn = nextState)
        bleManager.sendCommand(if (nextState) BleCommand.StartBuzzer else BleCommand.StopBuzzer)
    }

    fun stopPanic() {
        bleManager.sendCommand(BleCommand.StopAlert)
    }

    fun addContact(name: String, phone: String, message: String) {
        viewModelScope.launch {
            val result = contactRepository.addContact(
                ContactEntity(name = name, phoneNumber = phone, message = message)
            )
            if (result.isFailure) {
                internalState.value = internalState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun removeContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.removeContact(contact)
        }
    }

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }

    private fun estimateDistance(rssi: Int): Double {
        return 10.0.pow(
            (BleConstants.DEFAULT_TX_POWER - rssi) / (10.0 * BleConstants.ENV_FACTOR)
        )
    }

    class Factory(
        private val bleManager: BleManager,
        private val contactRepository: ContactRepository,
        private val locationRepository: LocationRepository,
        private val smsRepository: SmsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                bleManager,
                contactRepository,
                locationRepository,
                smsRepository
            ) as T
        }
    }
}
