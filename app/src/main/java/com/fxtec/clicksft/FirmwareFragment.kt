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

    companion object {
        private const val TAG = "FirmwareFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_firmware, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _textViewDebugInfo = view.findViewById(R.id.textViewDebugInfo)
        _textViewDebugInfo?.text = "Waiting for USB device..."
        Log.d(TAG, "textViewDebugInfo init text set")
        //setInitialText()
    }

    private fun setInitialText() {
        _textViewDebugInfo?.let { textView ->
            // If we have a handler and device, show interface info, otherwise show waiting message
            if (usbDeviceHandler != null) {
                updateInterfaceInfo()
            } else {
                textView.text = "Waiting for USB device..."
            }
        }
    }

    fun onDeviceConnected(handler: UsbDeviceHandler?) {
        Log.d(TAG, "onDeviceConnected called")
        usbDeviceHandler = handler
        updateInterfaceInfo()
    }

    private fun updateInterfaceInfo() {
        Log.d(TAG, "updateInterfaceInfo called")
        activity?.runOnUiThread {
            _textViewDebugInfo?.let { textView ->
                if (usbDeviceHandler != null) {
                    val info = usbDeviceHandler?.getInterfaceInfo()
                    Log.d(TAG, "Interface info updated: $info")
                    textView.text = when (info) {
                        "No device connected" -> "Waiting for USB device..."
                        else -> info
                    }
                } else {
                    textView.text = "Waiting for USB device..."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _textViewDebugInfo = null
    }
}