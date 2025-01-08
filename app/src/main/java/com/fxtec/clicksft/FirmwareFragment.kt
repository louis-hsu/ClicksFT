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
    private var pendingHandler: UsbDeviceHandler? = null

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

        pendingHandler?.let{
            firmwareTabUpdate(it)
        }
    }

    fun firmwareTabUpdate(handler: UsbDeviceHandler?) {
        Log.d(TAG, "FirmwareTab: Updating fragment")

        // If view is not created yet, store the handler for later
        if (_binding == null) {
            pendingHandler = handler
            return
        }

        try {
            if (handler?.getDeviceInfo() == null) {
                binding.textViewDebugInfo.text = "Waiting for USB device..."
            } else {
                Log.d(TAG, "FirmwareTab: Updating interfaces info")
                val info = handler.printInterfaceInfo()
                binding.textViewDebugInfo.text = when (info) {
                    "Device disconnected" -> "Waiting for USB device..."
                    else -> info
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating firmware tab", e)
            _binding?.textViewDebugInfo?.text = "Waiting for USB device..."
        }


        /*
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
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}