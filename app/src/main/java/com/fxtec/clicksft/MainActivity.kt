package com.fxtec.clicksft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import android.hardware.usb.*
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

class MainActivity : AppCompatActivity(), CommandTabView.DocumentPickerHelper {
    private lateinit var tabLayout: TabLayout
    private lateinit var commandTabView: CommandTabView
    private lateinit var firmwareTabView: FirmwareTabView
    private lateinit var textViewDeviceInfo: TextView
    private lateinit var usbDeviceHandler: UsbDeviceHandler

    companion object {
        private const val TAG = "MainActivity"
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            commandTabView.handleXmlUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupTabs()
        setupUsbReceiver()

        usbDeviceHandler.findAndConnectDevice()
    }

    // Implementation of interface in CommandTabView
    override fun launchDocumentPicker() {
        openDocument.launch(arrayOf("text/xml"))
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        commandTabView = findViewById(R.id.commandTabView)
        firmwareTabView = findViewById(R.id.firmwareTabView)
        textViewDeviceInfo = findViewById(R.id.textViewDeviceInfo)

        commandTabView.setDocumentPickerHelper(this)

        usbDeviceHandler = UsbDeviceHandler(this)
        usbDeviceHandler.setDeviceInfo(textViewDeviceInfo)

        commandTabView.updateCommandTab(usbDeviceHandler)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Command"))
        tabLayout.addTab(tabLayout.newTab().setText("Firmware"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        commandTabView.visibility = View.VISIBLE
                        firmwareTabView.visibility = View.GONE
                    }
                    1 -> {
                        commandTabView.visibility = View.GONE
                        firmwareTabView.visibility = View.VISIBLE
                        firmwareTabView.updateFirmwareTab(usbDeviceHandler)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbDeviceHandler.ACTION_USB_PERMISSION -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    usbDeviceHandler.handlePermission(device, granted)
                    updateTabsIfNeeded()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    usbDeviceHandler.findAndConnectDevice()
                    updateTabsIfNeeded()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    usbDeviceHandler.closeConnection()
                    updateTabsIfNeeded()
                }
            }
        }
    }

    private fun updateTabsIfNeeded() {
        when (tabLayout.selectedTabPosition) {
            0 -> commandTabView.updateCommandTab(usbDeviceHandler)
            1 -> firmwareTabView.updateFirmwareTab(usbDeviceHandler)
        }
        //if (tabLayout.selectedTabPosition == 1) {
        //    firmwareTabView.updateFirmwareInfo(usbDeviceHandler)
        //}
    }

    private fun setupUsbReceiver() {
        registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(UsbDeviceHandler.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbDeviceHandler.closeConnection()
    }
}