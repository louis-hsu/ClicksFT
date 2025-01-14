package com.fxtec.clicksft

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import android.view.View

class CommandTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface DocumentPickerHelper {
        fun launchDocumentPicker()
    }

    private lateinit var spinner: Spinner
    private lateinit var textViewCommand: TextView
    private lateinit var textViewResponse: TextView
    private lateinit var buttonLoadXml: Button
    private lateinit var buttonReset: Button
    private val commands = mutableListOf<UsbCommand>()
    private var xmlFileUri: Uri? = null
    private var documentPickerHelper: DocumentPickerHelper? = null
    private var usbDeviceHandler: UsbDeviceHandler? = null

    companion object {
        private const val DEFAULT_SELECTION = "Please select"
        private const val TAG = "CommandTabView"
    }

    init {
        inflate(context, R.layout.view_command_tab, this)
        initializeViews()
        setupButtons()
        loadCommands()
        setupSpinner()
    }

    fun setDocumentPickerHelper(handler: DocumentPickerHelper) {
        documentPickerHelper = handler
    }

    fun setUsbDeviceHandler(handler: UsbDeviceHandler) {
        usbDeviceHandler = handler
    }

    private fun initializeViews() {
        spinner = findViewById(R.id.spinner)
        textViewCommand = findViewById(R.id.textViewCommand)
        textViewResponse = findViewById(R.id.textViewResponse)
        buttonLoadXml = findViewById(R.id.buttonLoadXml)
        buttonReset = findViewById(R.id.buttonReset)
    }

    private fun setupButtons() {
        buttonLoadXml.setOnClickListener {
            documentPickerHelper?.launchDocumentPicker()
        }

        buttonReset.setOnClickListener {
            resetToDefault()
        }
    }

    fun handleXmlUri(uri: Uri) {
        xmlFileUri = uri
        loadCommands()
        setupSpinner()
    }

    private fun resetToDefault() {
        xmlFileUri = null
        loadCommands()
        setupSpinner()
    }

    private fun loadCommands() {
        commands.clear()

        // Try to load from custom XML file if available
        xmlFileUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    parseXmlFile(stream)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load default commands if no custom XML or if custom XML failed
        val defaultXmlStream = context.resources.openRawResource(R.raw.default_commands)
        parseXmlFile(defaultXmlStream)
    }

    private fun parseXmlFile(inputStream: InputStream) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentCommand: UsbCommand? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "command" -> currentCommand = UsbCommand()
                            "desc" -> currentCommand?.description = parser.nextText()
                            "cmd" -> currentCommand?.command = parser.nextText()
                            "resp" -> {
                                currentCommand?.response = parser.nextText()
                                currentCommand?.let { commands.add(it) }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeCommand(command: UsbCommand) {
        // Convert hex string to bytes (handles both with and without 0x prefix)
        val commandBytes = try {
            command.command.split(" ")
                .filter { it.isNotEmpty() }
                .map { hex ->
                    val cleanHex = hex.removePrefix("0x")
                    cleanHex.toInt(16).toByte()
                }
                .toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse command: ${command.command}", e)
            textViewResponse.text = "Invalid command format. Expected space-separated hex values (with optional 0x prefix)"
            return
        }

        // Ensure interface is setup before sending
        if (usbDeviceHandler?.setupInterface() != true) {
            textViewResponse.text = "Failed to setup USB interface"
            return
        }

        // Send command
        val sendSuccess = usbDeviceHandler?.sendCommand(commandBytes) ?: false
        if (!sendSuccess) {
            textViewResponse.text = "Failed to send command"
            return
        }

        // Read response and convert to hex string with proper byte handling
        val response = usbDeviceHandler?.readCommand()
        val responseText = when {
            response == null -> "Failed to read response"
            response.isEmpty() -> "No response data (0 bytes)"
            else -> response.joinToString(" ") { byte ->
                "0x%02X".format(byte.toInt() and 0xFF)
            }
        }

        Log.d(TAG, "Raw response bytes: ${response?.joinToString(", ") { it.toInt().toString() }}")

        /* Read response
        val response = usbDeviceHandler?.readCommand()
        val responseText = response?.let { String(it) } ?: "No response"
        */

        val resultText = StringBuilder().apply {
            //append("Command: ${command.command}\n")
            append("Response Comparison:\n")
            append("  Actual:    ${responseText}\n")
            append("  Expected:  ${command.response}")
        }.toString()

        textViewResponse.text = resultText
    }

    private fun setupSpinner() {
        val spinnerItems = mutableListOf(DEFAULT_SELECTION).apply {
            addAll(commands.map { it.description })
        }
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            spinnerItems
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    textViewCommand.text = ""
                    textViewResponse.text = ""
                } else {
                    val command = commands[position - 1]
                    textViewCommand.text = "Selected Command: ${command.command}"
                    //textViewResponse.text = "Expected Response: ${command.response}"
                    executeCommand(command)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                textViewCommand.text = ""
                textViewResponse.text = ""
            }
        }
    }
}