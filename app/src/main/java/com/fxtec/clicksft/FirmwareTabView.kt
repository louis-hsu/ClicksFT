package com.fxtec.clicksft

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView

class FirmwareTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var textViewDebugInfo: TextView
    private var usbDeviceHandler: UsbDeviceHandler? = null

    companion object {
        private const val TAG = "FirmwareTabView"
    }

    init {
        inflate(context, R.layout.view_firmware_tab, this)
        initializeViews()
    }

    private fun initializeViews() {
        textViewDebugInfo = findViewById(R.id.textViewDebugInfo)
        textViewDebugInfo.text = "Waiting for USB device..."
    }

    fun updateFirmwareTab(handler: UsbDeviceHandler?) {
        Log.d(TAG, "Updating firmware info")
        usbDeviceHandler = handler
        if (usbDeviceHandler?.getDeviceInfo() == null) {
            textViewDebugInfo.text = "Waiting for USB device..."
        } else {
            val info = usbDeviceHandler?.printInterfaceInfo()
            textViewDebugInfo.text = when (info) {
                "Device disconnected" -> "Waiting for USB device..."
                else -> info
            }
        }
    }
}