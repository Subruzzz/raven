package com.ravenguard.app.ble

object BlePacketParser {

    fun parse(rawPacket: String): BleEvent {
        val packet = rawPacket.trim()
        val chunks = packet.split("|")
        if (chunks.size < 2) return BleEvent.Error("Malformed packet: $packet")

        return when (chunks[0]) {
            "STAT" -> parseStatus(chunks[1])
            "ALERT" -> parseAlert(chunks[1])
            "ACK" -> BleEvent.Ack(chunks[1])
            else -> BleEvent.Error("Unknown header: ${chunks[0]}")
        }
    }

    fun toPayload(command: BleCommand): String {
        return when (command) {
            BleCommand.StartBuzzer -> "CMD|START_BUZZER"
            BleCommand.StopBuzzer -> "CMD|STOP_BUZZER"
            BleCommand.StopAlert -> "CMD|STOP_ALERT"
            BleCommand.AlertSentAck -> "ACK|ALERT_SENT"
        }
    }

    private fun parseStatus(statusData: String): BleEvent {
        return when {
            statusData.startsWith("BAT:") -> {
                val battery = statusData.removePrefix("BAT:").toIntOrNull()
                BleEvent.DeviceStatus(battery = battery)
            }

            statusData.startsWith("RSSI:") -> {
                val rssi = statusData.removePrefix("RSSI:").toIntOrNull()
                BleEvent.DeviceStatus(rssi = rssi)
            }

            else -> BleEvent.Error("Unknown status payload: $statusData")
        }
    }

    private fun parseAlert(alertData: String): BleEvent {
        val type = when (alertData) {
            "TWIST" -> AlertType.TWIST
            "TAP" -> AlertType.TAP
            "BUTTON" -> AlertType.BUTTON
            else -> AlertType.UNKNOWN
        }
        return BleEvent.Alert(type)
    }
}
