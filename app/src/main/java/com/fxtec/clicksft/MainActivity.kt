package com.fxtec.clicksft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var textViewDeviceInfo: TextView
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var usbDeviceHandler: UsbDeviceHandler
    private lateinit var usbManager: UsbManager

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                usbDeviceHandler.disconnect()
                textViewDeviceInfo.text = "Device disconnected"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize USB related components
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        usbDeviceHandler = UsbDeviceHandler(this)

        // Initialize UI components
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        textViewDeviceInfo = findViewById(R.id.textViewDeviceInfo)
        textViewDeviceInfo.text = "Device disconnected"

        // Setup UI and USB handling
        setupViewPager()
        registerUsbReceiver()
        checkConnectedDevice()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Command"
                1 -> "Firmware"
                else -> ""
            }
        }.attach()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)
    }

    private fun checkConnectedDevice() {
        usbManager.deviceList.values.firstOrNull {
            it.vendorId == UsbDeviceHandler.VENDOR_ID &&
                    it.productId == UsbDeviceHandler.PRODUCT_ID
        }?.let { device ->
            connectToDevice(device)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle USB device attachment
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: UsbDevice?) {
        if (usbDeviceHandler.connectDevice(device)) {
            usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
                textViewDeviceInfo.text = "Serial No: $serialNumber\nDevice Version: $deviceVersion"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbDeviceHandler.disconnect()
    }
}


/*
class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var textViewDeviceInfo: TextView
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var usbDeviceHandler: UsbDeviceHandler
    private lateinit var usbManager: UsbManager

    companion object {
        private const val ACTION_USB_PERMISSION = "com.fxtec.clicksft.USB_PERMISSION"
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            connectToDevice(it)
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    usbDeviceHandler.disconnect()
                    textViewDeviceInfo.text = "Device disconnected"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbDeviceHandler = UsbDeviceHandler(this)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        textViewDeviceInfo = findViewById(R.id.textViewDeviceInfo)

        setupViewPager()
        registerUsbReceiver()
        handleIntent(intent)

        checkForConnectedDevice()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private fun checkForConnectedDevice() {
        usbManager.deviceList.values.firstOrNull {
            it.vendorId == UsbDeviceHandler.VENDOR_ID &&
                    it.productId == UsbDeviceHandler.PRODUCT_ID
        }?.let { device ->
            requestUsbPermission(device)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            device?.let { requestUsbPermission(it) }
        }
    }

    private fun connectToDevice(device: UsbDevice?) {
        try {
            if (usbDeviceHandler.connectDevice(device)) {
                updateDeviceInfo()
                usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
                    textViewDeviceInfo.text = "Serial No: $serialNumber\n" + "Device Version: $deviceVersion"
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error connecting to device", e)
            textViewDeviceInfo.text = "Connection error: ${e.message}"
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun updateDeviceInfo() {
        usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
            val info = "Serial No: $serialNumber\nDevice Version: $deviceVersion"
            textViewDeviceInfo.text = info
        } ?: run {
            textViewDeviceInfo.text = "Device info unavailable"
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPagerAdapter.setCurrentPosition(position)
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Command"
                1 -> "Firmware"
                else -> ""
            }
        }.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbDeviceHandler.disconnect()
    }
}
 */