package com.fxtec.clicksft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import android.widget.TextView
import androidx.fragment.app.Fragment
import android.util.Log
import com.fxtec.clicksft.databinding.FragmentFirmwareBinding

class FirmwareFragment : Fragment() {
    private var _binding: FragmentFirmwareBinding? = null
    private val binding get() = _binding!!

    //private var _textViewDebugInfo: TextView? = null
    //private val textViewDebugInfo get() = _textViewDebugInfo!!
    private var usbDeviceHandler: UsbDeviceHandler? = null

    companion object {
        private const val TAG = "FirmwareFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirmwareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewDebugInfo.text = "Waiting for USB device..."
        //_textViewDebugInfo = view.findViewById(R.id.textViewDebugInfo)
        //_textViewDebugInfo?.text = "Waiting for USB device..."
        //setInitialText()
    }

    /*
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
    */

    fun firmwareTabUpdate(handler: UsbDeviceHandler?) {
        Log.d(TAG, "FirmwareTab: Updating fragment")
        usbDeviceHandler = handler

        // Print out interface info temporary as debug info
        if (usbDeviceHandler?.getDeviceInfo() == null) {
            binding.textViewDebugInfo.text = "Waiting for USB device..."
        } else {
            Log.d(TAG, "FirmwareTab: Updating interfaces info")
            binding.textViewDebugInfo.let { textView ->
                val info = usbDeviceHandler?.printInterfaceInfo()
                textView.text = when (info) {
                    "Device disconnected" -> "Waiting for USB device..."
                    else -> info
                }
            }
        }
    }
    /*
    private fun updateInterfaceInfo() {
        activity?.runOnUiThread {
            binding.textViewDebugInfo.let { textView ->
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
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}