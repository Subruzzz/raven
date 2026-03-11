package com.ravenguard.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var confirmationCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<BleEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<BleEvent> = _events.asSharedFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            stopScan()
            connect(device)
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = ConnectionState.Disconnected
            _events.tryEmit(BleEvent.Error("BLE scan failed: $errorCode"))
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                gatt.discoverServices()
            } else {
                _connectionState.value = ConnectionState.Disconnected
                clearGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(BleEvent.Error("Service discovery failed: $status"))
                return
            }
            bindCharacteristics(gatt.getService(BleConstants.SERVICE_UUID))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val packet = value.decodeToString()
            _events.tryEmit(BlePacketParser.parse(packet))
        }

        @Deprecated("Deprecated on older API")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val packet = characteristic.value?.decodeToString().orEmpty()
            _events.tryEmit(BlePacketParser.parse(packet))
        }
    }

    fun startScan() {
        if (scanner == null || bluetoothAdapter?.isEnabled != true) {
            _events.tryEmit(BleEvent.Error("Bluetooth unavailable"))
            return
        }

        _connectionState.value = ConnectionState.Scanning
        val filter = ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(BleConstants.SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        clearGatt()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendCommand(command: BleCommand) {
        val payload = BlePacketParser.toPayload(command).toByteArray()
        val characteristic = when (command) {
            BleCommand.AlertSentAck -> confirmationCharacteristic
            else -> commandCharacteristic
        }
        val gatt = bluetoothGatt
        if (characteristic == null || gatt == null) {
            _events.tryEmit(BleEvent.Error("Command channel unavailable"))
            return
        }
        characteristic.value = payload
        gatt.writeCharacteristic(characteristic)
    }

    private fun bindCharacteristics(service: BluetoothGattService?) {
        if (service == null) {
            _events.tryEmit(BleEvent.Error("Service not found"))
            return
        }

        val statusCharacteristic = service.getCharacteristic(BleConstants.DEVICE_STATUS_CHARACTERISTIC_UUID)
        val alertCharacteristic = service.getCharacteristic(BleConstants.ALERT_CHARACTERISTIC_UUID)
        commandCharacteristic = service.getCharacteristic(BleConstants.COMMAND_CHARACTERISTIC_UUID)
        confirmationCharacteristic = service.getCharacteristic(BleConstants.CONFIRMATION_CHARACTERISTIC_UUID)

        listOfNotNull(statusCharacteristic, alertCharacteristic).forEach { characteristic ->
            enableNotification(characteristic)
        }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val gatt = bluetoothGatt ?: return
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        descriptor?.let { gatt.writeDescriptor(it) }
    }

    private fun clearGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        commandCharacteristic = null
        confirmationCharacteristic = null
    }

    companion object {
        private val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
