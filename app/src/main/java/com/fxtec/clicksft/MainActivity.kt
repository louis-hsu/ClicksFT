package com.fxtec.clicksft

import android.app.PendingIntent
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
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var textViewDeviceInfo: TextView
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var usbDeviceHandler: UsbDeviceHandler
    private lateinit var usbManager: UsbManager

    companion object {
        private const val TAG = "MainActivity"
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.d(TAG, "USB Device Detached")
                usbDeviceHandler.disconnect()
                textViewDeviceInfo.text = "Device disconnected"
                if (viewPager.currentItem == 1) {
                    updateFirmwareFragment()
                }
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

        // Handle initial USB connection if launched by USB attachment
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            connectToDevice(device)
        } else {
            checkConnectedDevice()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        // Force create both fragments immediately
        viewPager.offscreenPageLimit = 2

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPagerAdapter.setCurrentPosition(position)
                Log.d(TAG, "Tab changed to position: $position")

                // For tab 'Firmware'
                if (position == 1) {
                    viewPager.post {
                        Log.d(TAG, "updateFirmwareFragment called from setupViewPager")
                        updateFirmwareFragment()
                    }
                }
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

    private fun registerUsbReceiver() {
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private fun checkConnectedDevice() {
        usbManager.deviceList.values.firstOrNull {
            it.vendorId == UsbDeviceHandler.VENDOR_ID &&
                    it.productId == UsbDeviceHandler.PRODUCT_ID
        }?.let { device ->
            connectToDevice(device)
        }
    }

    private fun updateFirmwareFragment() {
        if (usbDeviceHandler.getDeviceInfo() != null) {
            (viewPagerAdapter.getFragmentByPosition(1) as? FirmwareFragment)?.let { fragment ->
                Log.d(TAG, "onDeviceConnected called from updateFirmwareFragment")
                fragment.onDeviceConnected(usbDeviceHandler)
            }
        } else {
            // Handle disconnected state
            (viewPagerAdapter.getFragmentByPosition(1) as? FirmwareFragment)?.let { fragment ->
                Log.d(TAG, "Setting firmware fragment to disconnected state")
                fragment.onDeviceConnected(null)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called with action: ${intent?.action}")
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: UsbDevice?) {
        if (usbDeviceHandler.connectDevice(device)) {
            Log.d(TAG, "Device connected successfully")
            usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
                textViewDeviceInfo.text = "Serial No: $serialNumber\nDevice Version: $deviceVersion"
            }

            // For tab 'Firmware'
            if (viewPager.currentItem == 1) {
                viewPager.post {
                    Log.d(TAG, "updateFirmwareFragment called from connectToDevice")
                    updateFirmwareFragment()
                }
            }
        } else {
            Log.d(TAG, "Device connection failed")
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
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.fxtec.clicksft.USB_PERMISSION"
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "BroadcastReceiver received action: ${intent?.action}")
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "USB Device Detached - via Broadcast")
                    usbDeviceHandler.disconnect()
                    textViewDeviceInfo.text = "Device disconnected"
                    if (viewPager.currentItem == 1) {
                        updateFirmwareFragment()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(TAG, "USB Device Attached - via Broadcast")
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    requestPermission(device)
                }
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "USB Permission granted")
                        device?.let { connectToDevice(it) }
                    }
                }
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

    private fun requestPermission(device: UsbDevice?) {
        if (device == null) return

        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        // Force create both fragments immediately
        viewPager.offscreenPageLimit = 2

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPagerAdapter.setCurrentPosition(position)

                Log.d(TAG, "Tab changed to position: $position")

                // For tab 'Firmware'
                if (position == 1) {
                    viewPager.post {
                        Log.d(TAG, "updateFirmwareFragment called from setupViewPager")
                        updateFirmwareFragment()
                    }
                }
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

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    private fun checkConnectedDevice() {
        usbManager.deviceList.values.firstOrNull {
            it.vendorId == UsbDeviceHandler.VENDOR_ID &&
                    it.productId == UsbDeviceHandler.PRODUCT_ID
        }?.let { device ->
            requestPermission(device)
        }
    }

    private fun updateFirmwareFragment() {
        if (usbDeviceHandler.getDeviceInfo() != null) {
            (viewPagerAdapter.getFragmentByPosition(1) as? FirmwareFragment)?.let { fragment ->
                Log.d(TAG, "onDeviceConnected called from updateFirmwareFragment")
                fragment.onDeviceConnected(usbDeviceHandler)
            }
        } else {
            // Handle disconnected state
            (viewPagerAdapter.getFragmentByPosition(1) as? FirmwareFragment)?.let { fragment ->
                Log.d(TAG, "Setting firmware fragment to disconnected state")
                fragment.onDeviceConnected(null)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called with action: ${intent?.action}")
        // Handle USB device attachment
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            Log.d(TAG, "USB Device Attached via Intent")
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: UsbDevice?) {
        Log.d(TAG, "connectToDevice called")
        if (usbDeviceHandler.connectDevice(device)) {
            Log.d(TAG, "Device connected")
            usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
                textViewDeviceInfo.text = "Serial No: $serialNumber\nDevice Version: $deviceVersion"

                // Update firmware fragment regardless of current tab
                viewPager.post {
                    Log.d(TAG, "updateFirmwareFragment called from connectToDevice")
                    updateFirmwareFragment()
                }
            }
        } else {
            Log.d(TAG, "Device connection failed")
        }
    }

    /*
    private fun connectToDevice(device: UsbDevice?) {
        if (usbDeviceHandler.connectDevice(device)) {
            Log.d(TAG, "Device connected")
            usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
                textViewDeviceInfo.text = "Serial No: $serialNumber\nDevice Version: $deviceVersion"
            }

            // For tab 'Firmware'
            if (viewPager.currentItem == 1) {
                Log.d(TAG, "updateFirmwareFragment called from connectToDevice")
                updateFirmwareFragment()
            }
        }
    }

     */

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbDeviceHandler.disconnect()
    }
}

 */