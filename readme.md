通过手机控制蓝牙设备的APP已经很多，不过自己动手开发一款蓝牙应用对我来说仍有很大吸引力。
首先需要一个蓝牙模块作为控制对象，网购了一个5V 4路蓝牙继电器模块，型号是LC-WM-Relay-5V-4，价格不贵。是这个样子。
![蓝牙模块](https://img-blog.csdnimg.cn/e7c9c523e0a642aba73179ab5bd7c113.jpeg#pic_center)
蓝牙模块上板载高性能 MCU 和 SPP-C 蓝牙 2.1 从机模块，以及四个继电器。细节请参考[说明书](http://www.lctech-inc.com/cpzx/4/510.html)
#### 为了使用蓝牙，需要设置以下权限。
```xml
    <uses-permission android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"
        android:maxSdkVersion="30"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```
请注意，Android11和Android12所需权限有所不同，请参考相应文档。
申请运行时权限采用了郭霖老师开源的PermissionX，具体内容请参照[PermissionX 1.6发布，支持Android 12，可能是今年最大的版本升级](https://guolin.blog.csdn.net/article/details/120685379?spm=1001.2014.3001.5502)。
方法如下：

```kotlin
    // 申请权限
    private fun requestBluetoothPermission() {
        val requestList = ArrayList<String>()
        requestList.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {     // version >= api31(android12)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        if (requestList.isNotEmpty()) {
            PermissionX.init(this)
                .permissions(requestList)
                .explainReasonBeforeRequest()
                .onExplainRequestReason {scope, deniedList ->
                    val message = "PermissionX需要您同意以下权限才能正常使用"
                    scope.showRequestReasonDialog(deniedList, message, "允许", "拒绝")
                }
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
                        Toast.makeText(this, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                        if (!this.isFinishing) {
                            this.finish()
                        }
                    }
                }
        }
    }

```
#### 未设蓝牙搜索和配对功能
安卓手机通常都有蓝牙搜索和配对功能，因此本APP只有蓝牙连接功能，在手机上进行蓝牙搜索和配对，在APP中连接蓝牙模块。本系统中手机作为蓝牙通信的主机，蓝牙模块作为蓝牙通信的从机。手机发出控制指令，蓝牙模块根据指令开关继电器。
#### 用户界面
![MainActivity](https://img-blog.csdnimg.cn/9cd27b6619b14eb888c3a1263902b083.jpeg#pic_center)
用户界面只有这一个activity。四个按钮开关控制蓝牙模块上继电器的开闭。下面有一个设备列表，点击对应的设备item，可以连接或断开蓝牙设备。我用的设备名称是JDY-31-SPP。
#### MyApplication
为了方便对context的访问，建立 MyApplication，作为该系统的application。

```kotlin
class MyApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}
```
#### 蓝牙适配器
为了操作蓝牙，需要一个蓝牙适配器bluetoothAdapter。

```kotlin
val bluetoothAdapter: BluetoothAdapter = (MyApplication.context
    .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    .adapter
```

#### 获取系统蓝牙设备清单
用手机本身的功能进行蓝牙配对后，android系统会保留一个已经配对（绑定）的设备清单，在设备清单中可以找到要连接的蓝牙模块。

```kotlin
	val deviceList: MutableList<BluetoothDevice> = ArrayList()
    private fun getBondedDevices() {
        deviceList.apply {
            if (isNotEmpty()) clear()
            for (device in bluetoothAdapter.bondedDevices) {
                add(device)
            }
        }
    }
```
从系统取回的设备清单存放在deviceList中。
#### 蓝牙连接和断开
蓝牙连接和断开放在BluetoothBean类里，outputStream和inputStream也在其中。
```kotlin
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
```
这里使用的是蓝牙spp（串口协议），UUID是固定的。
#### DeviceAdapter
设备清单的访问用RecyclerView实现，DeviceAdapter是相应的数据适配器。
```kotlin
class DeviceAdapter(private val deviceList: List<BluetoothDevice>, val actionFun : (BluetoothDevice) -> Unit) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var currentPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val deviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val deviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        //add item click listener
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            val device = deviceList[viewHolder.adapterPosition]
            actionFun(device)
            currentPosition = viewHolder.adapterPosition
        }
        return viewHolder
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.name
        holder.deviceAddress.text = device.address
        if (position == currentPosition) {
            holder.deviceName.setTextColor(Color.RED)
            holder.deviceAddress.setTextColor(Color.RED)
        } else {
            holder.deviceName.setTextColor(Color.BLACK)
            holder.deviceAddress.setTextColor(Color.BLACK)
        }

    }

    override fun getItemCount() = deviceList.size

}
```
当用户点击设备清单某一item时，就会触发itemView的OnClickListener。在该listener中，从设备清单中取得设备实例device，调用actionFun(device)。actionFun是一个函数类型的参数，DeviceAdapter实例化时从外部viewModel::connectAction传入。

```kotlin
        // set adapter
        val deviceAdapter = DeviceAdapter(viewModel.deviceList, viewModel::connectAction)
        binding.recyclerView.adapter = deviceAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        // 设置item间隔，单位时dp
        binding.recyclerView.addItemDecoration(SpacesItemDecoration(10))
```
传给DeviceAdapter的是这样的函数，在这个函数里调用了bluetoothBean.connect(device)或bluetoothBean.disconnect()。
```kotlin
    fun connectAction(device: BluetoothDevice) {
        if (isConnectedLiveData.value!!) {
            bluetoothBean.disconnect()
        } else {
            if (!bluetoothBean.connect(device)) {
                _msgLiveData.value = "No response from Bluetooth Device"
            }
        }
    }
```
#### 蓝牙事件广播接收器
需要注册一个蓝牙事件广播接收器，在对蓝牙操作后，用以接受手机反馈的各种蓝牙事件。

```kotlin
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
    
    val filter = IntentFilter()
    filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    MyApplication.context.registerReceiver(receiver, filter)

```
这里只用到两个事件，蓝牙连接成功和蓝牙连接断开，事件发生时用liveData通知MainActivity变更连接标志。
#### 编辑发送继电器控制指令
蓝牙连接成功后，根据用户界面上的四个按钮状态，可以对蓝牙模块发出继电器控制指令。
控制指令如下：

![继电器控制指令](https://img-blog.csdnimg.cn/3e5c0396bbc045259a731b81e9b45722.png#pic_center)
相应的实现方法：
```kotlin
    /**
     * 通过蓝牙输出数据控制继电器的开关
     * @param switchId 继电器序号
     * @param isOn 开关状态
     * @return 执行结果String
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
```
指令发出后，蓝牙模块上的继电器就会根据指令动作。
需要源码的可以下载。[下载地址](https://download.csdn.net/download/wxson/85388847)
如果有对内容的纠错、指摘、提问，也可以直接联系wxson@126.com。
