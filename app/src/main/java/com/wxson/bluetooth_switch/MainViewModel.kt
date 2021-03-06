package com.wxson.bluetooth_switch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.IOException
import kotlin.collections.ArrayList

val bluetoothAdapter: BluetoothAdapter = (MyApplication.context
    .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    .adapter

@SuppressLint("MissingPermission")
class MainViewModel : ViewModel() {
    //region variable
    private val tag = this.javaClass.simpleName
    val deviceList: MutableList<BluetoothDevice> = ArrayList()
    private var bluetoothBean: BluetoothBean

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            action?.let{
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        _isConnectedLiveData.value = true
//                        if (::bluetoothBean.isInitialized)
//                            bluetoothBean.startReadThread()
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        _isConnectedLiveData.value = false
                    }
                }
            }
        }
    }

    //endregion

    //region LiveData
    val isConnectedLiveData: LiveData<Boolean>
        get() = _isConnectedLiveData
    private val _isConnectedLiveData = MutableLiveData<Boolean>()

    val msgLiveData: LiveData<String>
        get() = _msgLiveData
    private val _msgLiveData = MutableLiveData<String>()

    //endregion

    init {
        Log.i(tag, "init")
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        MyApplication.context.registerReceiver(receiver, filter)
        getBondedDevices()
        bluetoothBean = BluetoothBean()
        _isConnectedLiveData.value = false
    }

    //region public method

    /**
     * ????????????????????????????????????????????????
     * @param switchId ???????????????
     * @param isOn ????????????
     * @return ????????????String
     */
    fun setSwitch(switchId: Int, isOn: Boolean) {
        val outputString = with(StringBuilder()) {
            append("A0 ")
            when (switchId) {
                1 -> {
                    append("01 ")
                    append(if (isOn) "01 A2" else "00 A1")
                }
                2 -> {
                    append("02 ")
                    append(if (isOn) "01 A3" else "00 A2")
                }
                3 -> {
                    append("03 ")
                    append(if (isOn) "01 A4" else "00 A3")
                }
                4 -> {
                    append("04 ")
                    append(if (isOn) "01 A5" else "00 A4")
                }
                else -> {}
            }
            toString()
        }
        val outputByteArray: ByteArray = toByteArray(outputString)
        try {
            bluetoothBean.outputStream?.write(outputByteArray)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    fun setHandler(handler: Handler) {
//        bluetoothBean.setHandler(handler)
//    }

    fun sendTestMsg() {
        try {
            bluetoothBean.outputStream?.write(toByteArray("FF"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //endregion

    //region private method
    private fun getBondedDevices() {
        deviceList.apply {
            if (isNotEmpty()) clear()
            for (device in bluetoothAdapter.bondedDevices) {
                add(device)
            }
        }
    }

    /**
     * ???String?????????byte[]??????
     * @param str ???????????????String??????
     * @return ????????????byte[]??????
     */
    private fun toByteArray(str: String): ByteArray {
        /* 1.?????????String??????' '????????????String?????????char?????? */
        val charArray = str.toCharArray().filterNot { it.isWhitespace()}.toCharArray()
        val length = charArray.size
        /* ???char??????????????????????????????????????????????????? */
        val evenLength = if (length % 2 == 0) length else length + 1 //??????????????????????????????
        if (evenLength != 0) {
            val data = IntArray(evenLength)
            data[evenLength - 1] = 0
            for (i in 0 until length) {
                when (val char: Char = charArray[i]) {
                    in '0'..'9' -> {
                        data[i] = char - '0'
                    }
                    in 'a'..'f' -> {
                        data[i] = char - 'a' + 10
                    }
                    in 'A'..'F' -> {
                        data[i] = char - 'A' + 10
                    }
                }
            }
            /* ?????????char??????(?????????)?????????????????????16???????????? */
            val byteArray = ByteArray(evenLength / 2)
            for (i in 0 until evenLength / 2) {
                byteArray[i] = (data[i * 2] * 16 + data[i * 2 + 1]).toByte()
            }
            return byteArray
        }
        return byteArrayOf()
    }

    fun connectAction(device: BluetoothDevice) {
        if (isConnectedLiveData.value!!) {
            bluetoothBean.disconnect()
        } else {
            if (!bluetoothBean.connect(device)) {
                _msgLiveData.value = "No response from Bluetooth Device"
            }
        }
    }

    //endregion
}