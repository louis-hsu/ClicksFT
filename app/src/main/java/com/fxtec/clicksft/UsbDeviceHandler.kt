package com.fxtec.clicksft

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log

class UsbDeviceHandler(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null

    companion object {
        private const val TAG = "UsbDeviceHandler"
        const val VENDOR_ID = 13614
        const val PRODUCT_ID = 8966
    }

    fun connectDevice(device: UsbDevice?): Boolean {
        if (device == null) return false

        if (device.vendorId != VENDOR_ID || device.productId != PRODUCT_ID) {
            Log.e(TAG, "Device ID mismatch")
            return false
        }

        usbDevice = device
        usbConnection = usbManager.openDevice(device)
        return usbConnection != null
    }

    fun getDeviceInfo(): Pair<String, String>? {
        val connection = usbConnection ?: return null
        val device = usbDevice ?: return null

        try {
            // Get serial number
            val serialNumber = device.serialNumber ?: "Unknown"
            val deviceVersion = device.version ?: "Unknown"

            return Pair(serialNumber, deviceVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info", e)
            return null
        }
    }

    fun disconnect() {
        usbConnection?.close()
        usbConnection = null
        usbDevice = null
    }
}