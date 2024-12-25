package com.fxtec.clicksft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.util.Log

class FirmwareFragment : Fragment() {
    private var _textViewDebugInfo: TextView? = null
    private val textViewDebugInfo get() = _textViewDebugInfo!!
    private var usbDeviceHandler: UsbDeviceHandler? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _textViewDebugInfo = view.findViewById(R.id.textViewDebugInfo)
        textViewDebugInfo.text = "Waiting for USB device..."

        // Update interface info if device is already connected
        updateInterfaceInfo()
    }

    fun onDeviceConnected(handler: UsbDeviceHandler) {
        usbDeviceHandler = handler
        updateInterfaceInfo()
    }

    private fun updateInterfaceInfo() {
        activity?.runOnUiThread {
            _textViewDebugInfo?.let { textView ->
                val info = usbDeviceHandler?.getInterfaceInfo() ?: "No device connected"
                textView.text = info
                Log.d("FirmwareFragment", "Updated interface info")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _textViewDebugInfo = null
    }
}