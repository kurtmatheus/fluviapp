package br.com.gruponaveg.services.printerservice.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class ThermalPrinterConnection(
    context: Context,
    deviceAdress: String
) : Printer {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val device: BluetoothDevice? = bluetoothManager.adapter?.getRemoteDevice(deviceAdress)
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    var isOpen = false

    @SuppressLint("MissingPermission")
    override fun open() {
        try {
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()
            isOpen = bluetoothSocket?.isConnected ?: false
            outputStream = bluetoothSocket?.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun write(command: ByteArray?) {
        try {
            outputStream?.write(command)
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun close() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
