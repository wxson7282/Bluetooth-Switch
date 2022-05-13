package com.wxson.bluetooth_switch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothBean {
    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) : Boolean {
        var returnValue = false
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream
            returnValue = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return returnValue
    }

    fun disconnect() {
        try {
            outputStream?.close()
            outputStream = null
            inputStream?.close()
            inputStream = null
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}