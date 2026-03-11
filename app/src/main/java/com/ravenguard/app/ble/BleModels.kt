package com.ravenguard.app.ble

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val DEVICE_STATUS_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val ALERT_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e")
    val CONFIRMATION_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400005-b5a3-f393-e0a9-e50e24dcca9e")
    const val DEFAULT_TX_POWER = -59
    const val ENV_FACTOR = 2.0
}

enum class ConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected
}

enum class AlertType {
    TWIST,
    TAP,
    BUTTON,
    UNKNOWN
}

sealed interface BleEvent {
    data class DeviceStatus(val battery: Int? = null, val rssi: Int? = null) : BleEvent
    data class Alert(val type: AlertType) : BleEvent
    data class Ack(val value: String) : BleEvent
    data class Error(val message: String) : BleEvent
}

sealed interface BleCommand {
    data object StartBuzzer : BleCommand
    data object StopBuzzer : BleCommand
    data object StopAlert : BleCommand
    data object AlertSentAck : BleCommand
}
