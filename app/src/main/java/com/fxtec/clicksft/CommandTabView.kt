package com.fxtec.clicksft

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
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
    private var testPlan: UsbTestPlan? = null
    private var xmlFormat = XmlFormat.COMMAND

    private var xmlFileUri: Uri? = null
    private var documentPickerHelper: DocumentPickerHelper? = null
    private var usbDeviceHandler: UsbDeviceHandler? = null

    companion object {
        private const val DEFAULT_SELECTION = "Please select"
        private const val RUN_TEST_PLAN = "Run the test plan!!"
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

    fun updateCommandTab(handler: UsbDeviceHandler) {
        usbDeviceHandler = handler

        // Reset tab to default status
        xmlFileUri = null
        xmlFormat = XmlFormat.COMMAND
        loadCommands()
        setupSpinner()
        spinner.setSelection(0)

        textViewCommand.text = ""
        textViewResponse.text = ""
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
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "testplan") {
                    xmlFormat = XmlFormat.TEST_PLAN
                    parseTestPlanFormat(parser)
                    return
                } else if (eventType == XmlPullParser.START_TAG && parser.name == "command") {
                    xmlFormat = XmlFormat.COMMAND
                    parseCommandFormat(parser)
                    return
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseCommandFormat(parser: XmlPullParser) {
       var currentCommand: UsbCommand? = null
       var eventType = parser.eventType

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
    }

    private fun parseTestPlanFormat(parser: XmlPullParser) {
        val testItems = mutableListOf<UsbTestItem>()
        var currentDesc = ""
        var currentCmd = ""
        var currentResp = ""
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "desc" -> currentDesc = parser.nextText()
                        "cmd" -> currentCmd = parser.nextText()
                        "resp" -> {
                            currentResp = parser.nextText()
                            testItems.add(UsbTestItem(currentDesc, currentCmd, currentResp))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        testPlan = UsbTestPlan(testItems)
    }

    private fun executeCommand(command: String): String? {
        // Convert hex string to bytes (handles both with and without 0x prefix)
        val commandBytes = try {
            command.split(" ")
                .filter { it.isNotEmpty() }
                .map { hex ->
                    val cleanHex = hex.removePrefix("0x")
                    cleanHex.toInt(16).toByte()
                }
                .toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse command: ${command}", e)
            textViewResponse.text = "Invalid command format which should be as '0x01 0x02'"
            return null
        }

        // Ensure interface is setup before sending
        if (usbDeviceHandler?.setupInterface() != true) {
            textViewResponse.text = "Failed to setup USB interface"
            return null
        }

        // Send command
        if (usbDeviceHandler?.sendCommand(commandBytes) != true) {
            textViewResponse.text = "Failed to send command"
            return null
        }

        Thread.sleep(500)

        // Read response and convert to hex string with proper byte handling
        val response = usbDeviceHandler?.readCommand()
        Log.d(TAG, "Raw response bytes: ${response?.joinToString(", ") { it.toInt().toString() }}")
        return when {
            response == null -> "Failed to read response"
            response.isEmpty() -> "No response data (0 byte)"
            else -> response.joinToString(" ") { byte ->
                "0x%02X".format(byte.toInt() and 0xFF)
            }
        }
    }

    private fun runTestPlan() {
        val results = SpannableStringBuilder()
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            testPlan?.items?.forEach { testItem ->
                val actualResponse = executeCommand(testItem.command) ?: "Failed to execute command"
                val isPassed = actualResponse == testItem.expectedResponse

                val result = TestResult(
                    description = testItem.description,
                    command = testItem.command,
                    actualResponse = actualResponse,
                    expectedResponse = testItem.expectedResponse,
                    isPassed = isPassed
                )

                results.append(result.toFormattedString())
                results.append("\n\n")

                mainHandler.post {
                    textViewResponse.text = results
                }
            }

            mainHandler.post {
                textViewCommand.text = "Test plan completed!"
            }
        }.start()
    }

    private fun setupSpinner() {
        val spinnerItems = when (xmlFormat) {
            XmlFormat.COMMAND -> mutableListOf(DEFAULT_SELECTION).apply {
                addAll(commands.map { it.description })
            }
            XmlFormat.TEST_PLAN -> listOf(DEFAULT_SELECTION, RUN_TEST_PLAN)
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
                when {
                    position == 0 -> {
                        textViewCommand.text = ""
                        textViewResponse.text = ""
                    }
                    xmlFormat == XmlFormat.TEST_PLAN && position == 1 -> {
                        textViewCommand.text = "Running test plan..."
                        runTestPlan()
                    }
                    xmlFormat == XmlFormat.COMMAND -> {
                        val command = commands[position - 1]
                        textViewCommand.text = "Selected Command: ${command.command}"
                        val actualResponse = executeCommand(command.command)
                        val responseText = buildString {
                            append("Response Comparison:\n")
                            append("  Actual:   ${actualResponse ?: "Failed to execute command"}\n")
                            append("  Expected: ${command.response}")
                        }
                        textViewResponse.text = responseText
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                textViewCommand.text = ""
                textViewResponse.text = ""
            }
        }
    }
}