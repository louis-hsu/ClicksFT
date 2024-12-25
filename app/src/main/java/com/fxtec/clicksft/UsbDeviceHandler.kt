package com.fxtec.clicksft

import android.content.Context
import android.hardware.usb.*
import android.util.Log

class UsbDeviceHandler(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    companion object {
        private const val TAG = "UsbDeviceHandler"
        /*
        Following info are defined by device vendor
        Hardcoded here as the fixed constants
         */
        const val VENDOR_ID = 13614
        const val PRODUCT_ID = 8966
        private const val INTERFACE_INDEX = 1
        private const val ENDPOINT_IN_INDEX = 0
        private const val ENDPOINT_OUT_INDEX = 1

        private const val TIMEOUT = 1000    // Milliconds
    }

    fun connectDevice(device: UsbDevice?): Boolean {
        try {
            if (device == null) return false

            if (device.vendorId != VENDOR_ID || device.productId != PRODUCT_ID) {
                Log.e(TAG, "Device ID mismatch")
                return false
            }
            disconnect()

            usbDevice = device
            usbConnection = usbManager.openDevice(device)
            return setupInterface()

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            disconnect()
            return false
        }
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

    fun getInterfaceInfo(): String {
        val device = usbDevice ?: return "No device connected"
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

        Log.d(TAG, "Interface info: $result")
        return result
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

    fun disconnect() {
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from device", e)
        } finally {
            usbConnection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
            usbDevice = null
        }
    }

    fun getDeviceInfo(): Pair<String, String>? {
        val connection = usbConnection ?: return null
        val device = usbDevice ?: return null

        try {
            val serialNumber = device.serialNumber ?: "Unknown"
            val deviceVersion = device.version ?: "Unknown"
            return Pair(serialNumber, deviceVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info", e)
            return null
        }
    }
}