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

//    private lateinit var handler: Handler
//
//    fun setHandler(handler: Handler) {
//        this.handler = handler
//    }

//    fun startReadThread() {
//        thread {
//            val buffer = ByteArray(128)
//            while (true) {
//                val msg = Message.obtain()
//                inputStream?.let {
//                    val length: Int = it.read(buffer)
//                    if (length > 0) {
//                        val inputStr = toHexString(buffer)
//                        msg.obj = inputStr
//                        if (::handler.isInitialized){
//                            handler.sendMessage(msg)
//                        }
//                    }
//                }
//            }
//        }
//    }

//    private fun toHexString(bytes: ByteArray?): String {
//        var result = String()
//        var hexString: String
//        bytes?.let {
//            for (bt in it) {
//                hexString = Integer.toHexString((if(bt < 0) bt + 256 else bt).toInt())
//                result += if (hexString.length == 1)  "0$hexString" else "$hexString "
//            }
//        }
//        return result
//    }

}