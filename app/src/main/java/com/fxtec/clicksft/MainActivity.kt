package com.fxtec.clicksft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
    private var xmlFileUri: Uri? = null
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var usbDeviceHandler: UsbDeviceHandler

    private val usbDetachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                usbDeviceHandler.disconnect()
                textViewDeviceInfo.text = "Device disconnected"
            }
        }
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            xmlFileUri = it
            viewPagerAdapter.getCurrentFragment()?.let { fragment ->
                if (fragment is CommandFragment) {
                    fragment.updateXmlFile(it)
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
        registerUsbDetachment()
        handleIntent(intent)
        checkPersistedXmlFile()
    }

    private fun registerUsbDetachment() {
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbDetachReceiver, filter)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: UsbDevice?) {
        if (usbDeviceHandler.connectDevice(device)) {
            updateDeviceInfo()
        }
    }

    private fun updateDeviceInfo() {
        usbDeviceHandler.getDeviceInfo()?.let { (serialNumber, deviceVersion) ->
            val info = "Serial No: $serialNumber\nDevice Version: $deviceVersion"
            textViewDeviceInfo.text = info
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
        unregisterReceiver(usbDetachReceiver)
        viewPager.unregisterOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
        usbDeviceHandler.disconnect()
    }

    private fun checkPersistedXmlFile() {
        contentResolver.persistedUriPermissions.firstOrNull()?.uri?.let {
            xmlFileUri = it
        }
    }

    fun openXmlFilePicker() {
        openDocument.launch(arrayOf("text/xml"))
    }

    fun getXmlFileUri(): Uri? = xmlFileUri
}