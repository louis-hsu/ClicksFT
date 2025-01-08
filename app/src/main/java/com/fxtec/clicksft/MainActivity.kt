package com.fxtec.clicksft

//import android.app.PendingIntent
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
import android.hardware.usb.*
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var usbDeviceHandler: UsbDeviceHandler
    private lateinit var textViewDeviceInfo: TextView

    companion object {
        private const val TAG = "MainActivity"
    }

    // Receiver for USB device intents
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received action: ${intent?.action}")
            when (intent?.action) {
                UsbDeviceHandler.ACTION_USB_PERMISSION -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    usbDeviceHandler.handlePermission(device, granted)
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(TAG, "USB Device Attached")
                    usbDeviceHandler.findAndConnectDevice()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "USB Device Detached")
                    usbDeviceHandler.closeConnection()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UsbDeviceHandler
        usbDeviceHandler = UsbDeviceHandler(this)

        // Initialize UI components
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        textViewDeviceInfo = findViewById(R.id.textViewDeviceInfo)
        usbDeviceHandler.setDeviceInfo(findViewById(R.id.textViewDeviceInfo))
        //textViewDeviceInfo.text = "Device disconnected"

        // Register USB intents
        registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(UsbDeviceHandler.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            //addAction(Intent.ACTION_USER_PRESENT)
        }, Context.RECEIVER_NOT_EXPORTED)

        usbDeviceHandler.findAndConnectDevice()

        // Setup UI
        setupViewPager()
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
                        (viewPagerAdapter.getFragmentByPosition(1) as? FirmwareFragment)?.let { fragment ->
                            fragment.firmwareTabUpdate(usbDeviceHandler)
                        }
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbDeviceHandler.closeConnection()
    }
}