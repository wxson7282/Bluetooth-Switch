package com.wxson.bluetooth_switch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.permissionx.guolindev.PermissionX
import com.wxson.bluetooth_switch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: MainViewModel
    private val resultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK) {  //if the user has rejected the request
            this.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestBluetoothPermission()
        if (!bluetoothAdapter.isEnabled) {    // 蓝牙未打开
            // 转移到系统蓝牙开启activity
            resultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // set adapter
        val deviceAdapter = DeviceAdapter(viewModel.deviceList, viewModel::connectAction)
        binding.recyclerView.adapter = deviceAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        // 设置item间隔，单位时dp
        binding.recyclerView.addItemDecoration(SpacesItemDecoration(10))
        // 与连接状态相关的设置
        viewModel.isConnectedLiveData.observe(this) { isConnected ->
            binding.ivConnectState.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    if (isConnected) R.drawable.ic_connected else R.drawable.ic_disconnected,
                    this.theme
                )
            )
            binding.apply {
                toggleBtn1.isEnabled = isConnected
                toggleBtn2.isEnabled = isConnected
                toggleBtn3.isEnabled = isConnected
                toggleBtn4.isEnabled = isConnected
            }
            // 设置deviceAdapter的item连接成功时有不同颜色
            deviceAdapter.currentPosition = if (isConnected) deviceAdapter.currentPosition else -1
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }

        viewModel.msgLiveData.observe(this) { msg -> showMsg(msg) }

        binding.apply {
            toggleBtn1.setOnCheckedChangeListener { _, isChecked -> viewModel.setSwitch(1, isChecked)}
            toggleBtn2.setOnCheckedChangeListener { _, isChecked -> viewModel.setSwitch(2, isChecked)}
            toggleBtn3.setOnCheckedChangeListener { _, isChecked -> viewModel.setSwitch(3, isChecked)}
            toggleBtn4.setOnCheckedChangeListener { _, isChecked -> viewModel.setSwitch(4, isChecked)}
        }
    }

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

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}