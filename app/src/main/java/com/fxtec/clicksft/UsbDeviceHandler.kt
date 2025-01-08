package com.fxtec.clicksft

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.*
import android.util.Log
import android.widget.TextView

class UsbDeviceHandler(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var deviceInfoTextView: TextView? = null

    companion object {
        private const val TAG = "UsbDeviceHandler"
        const val ACTION_USB_PERMISSION = "com.fxtec.clicksft.USB_PERMISSION"

        //Following info are defined by device vendor, hardcoded here as the fixed constants
        private const val VENDOR_ID = 13614
        private const val PRODUCT_ID = 8966
        private const val INTERFACE_INDEX = 1
        private const val ENDPOINT_IN_INDEX = 0
        private const val ENDPOINT_OUT_INDEX = 1

        private const val TIMEOUT = 1000    // Milliconds
    }

    // Initial textView assignment
    /*
    fun setDeviceInfo(textView: TextView) {
        Log.d(TAG, "deviceInfoTextView initialized")
        deviceInfoTextView = textView
        deviceInfoTextView?.let {
            it.text = "Device disconnected"
            Log.d(TAG, "TextView text set to: ${it.text}")
        } ?: Log.e(TAG, "deviceInfoTextView is null after assignment")
    }
     */
    fun setDeviceInfo(textView: TextView) {
        Log.d(TAG, "deviceInfoTextView initialized")
        deviceInfoTextView = textView
        deviceInfoTextView?.text = "Device disconnected"
    }

    fun findAndConnectDevice() {
        Log.d(TAG, "Searching for USB device...")
        usbManager.deviceList.values.find { device ->
            device.vendorId == VENDOR_ID && device.productId == PRODUCT_ID
        }?.let { device ->
            Log.d(TAG, "Target device found, requesting permission")
            requestPermission(device)
        }
    }

    fun handlePermission(device: UsbDevice?, granted: Boolean) {
        Log.d(TAG, "Handle permission - device: $device, granted: $granted")
        if (granted && device != null) {
            connectDevice(device)
        }
    }
    private fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "Already has permission")
            connectDevice(device)
            return
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        Log.d(TAG, "Requesting permission")
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun connectDevice(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            requestPermission(device)
            return
        }
        try {
            usbDevice = device
            usbConnection = usbManager.openDevice(device)
            usbConnection?.let { connection ->
                deviceInfoTextView?.text = getDeviceInfo()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Device disconnected. Exception: $e")
            deviceInfoTextView?.text = "Device disconnected"
        }
    }

    fun getDeviceInfo(): String? {
        //val connection = usbConnection ?: return null
        Log.d(TAG, "Getting device info")
        val device = usbDevice ?: return null
        val serialNumber = device.serialNumber ?: "Unknown"
        val deviceVersion = device.version ?: "Unknown"

        return "Serial No: $serialNumber\nDevice Version: $deviceVersion"
    }

    fun closeConnection() {
        Log.d(TAG, "Closing connection")
        usbConnection?.close()
        usbConnection = null
        usbDevice = null
        deviceInfoTextView?.text = "Device disconnected"
    }

    fun printInterfaceInfo(): String {
        val device = usbDevice ?: return "Device disconnected"
        Log.d(TAG, "Getting interface info for device: ${device.deviceName}")
        val sb = StringBuilder()

        for (i in 0 until device.interfaceCount) {
            val interface_ = device.getInterface(i)
            sb.append("Interface $i:\n")
            sb.append("  Interface ID: ${interface_.id}\n")
            sb.append("  Interface Class: ${interface_.interfaceClass}\n")
            sb.append("  Endpoint Count: ${interface_.endpointCount}\n")

            for (j in 0 until interface_.endpointCount) {
                val endpoint = interface_.getEndpoint(j)
                sb.append("    Endpoint $j:\n")
                sb.append("      Address: ${endpoint.address}\n")
                sb.append("      Direction: ${if (endpoint.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"}\n")
                sb.append("      Type: ${getEndpointType(endpoint.type)}\n")
                sb.append("      Max Packet Size: ${endpoint.maxPacketSize}\n")
            }
            sb.append("\n")
        }
        val result = sb.toString()

        //Log.d(TAG, "Interface info: $result")
        return result
    }

    private fun setupInterface(): Boolean {
        val device = usbDevice ?: return false

        // Get/claim interface with specific index
        usbInterface = device.getInterface(INTERFACE_INDEX)
        if (usbInterface == null) {
            Log.e(TAG, "Could not get interface")
            return false
        }

        if (!usbConnection?.claimInterface(usbInterface, true)!!) {
            Log.e(TAG, "Could not claim interface")
            return false
        }

        // Get endpoints with specific index
        endpointIn = usbInterface!!.getEndpoint(ENDPOINT_IN_INDEX)
        endpointOut = usbInterface!!.getEndpoint(ENDPOINT_OUT_INDEX)
        /*
        for (i in 0 until usbInterface!!.endpointCount) {
            val endpoint = usbInterface!!.getEndpoint(i)
            if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                endpointIn = endpoint
            } else if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                endpointOut = endpoint
            }
        }
         */

        if (endpointIn == null || endpointOut == null) {
            Log.e(TAG, "Could not get endpoints")
            return false
        }

        return true
    }



    private fun getEndpointType(type: Int): String = when (type) {
        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "Control"
        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "Isochronous"
        UsbConstants.USB_ENDPOINT_XFER_BULK -> "Bulk"
        UsbConstants.USB_ENDPOINT_XFER_INT -> "Interrupt"
        else -> "Unknown"
    }

    fun sendCommand(command: ByteArray): Boolean {
        if (!isDeviceReady()) return false

        val result = usbConnection?.bulkTransfer(
            endpointOut,
            command,
            command.size,
            TIMEOUT
        ) ?: -1

        return result >= 0
    }

    fun readCommand(bufferSize: Int = 64): ByteArray? {
        if (!isDeviceReady()) return null

        val buffer = ByteArray(bufferSize)
        val result = usbConnection?.bulkTransfer(
            endpointIn,
            buffer,
            buffer.size,
            TIMEOUT
        ) ?: -1

        return if (result > 0) buffer.copyOf(result) else null
    }

    private fun isDeviceReady(): Boolean {
        return usbConnection != null &&
                usbInterface != null &&
                endpointIn != null &&
                endpointOut != null
    }
}