package com.fxtec.clicksft

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class CommandFragment : Fragment() {
    private lateinit var spinner: Spinner
    private lateinit var textViewCommand: TextView
    private lateinit var textViewResponse: TextView
    private lateinit var buttonLoadXml: Button
    private lateinit var buttonReset: Button
    private val commands = mutableListOf<UsbCommand>()
    private var xmlFileUri: Uri? = null

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            requireActivity().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            xmlFileUri = it
            loadCommands()
            setupSpinner()
        }
    }

    companion object {
        private const val DEFAULT_SELECTION = "Please select"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_command, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner = view.findViewById(R.id.spinner)
        textViewCommand = view.findViewById(R.id.textViewCommand)
        textViewResponse = view.findViewById(R.id.textViewResponse)
        buttonLoadXml = view.findViewById(R.id.buttonLoadXml)
        buttonReset = view.findViewById(R.id.buttonReset)

        buttonLoadXml.setOnClickListener {
            openXmlFilePicker()
        }

        buttonReset.setOnClickListener {
            resetToDefault()
        }

        //checkPersistedXmlFile()
        loadCommands()
        setupSpinner()
    }

    private fun resetToDefault() {
        // Clear persisted permissions if any
        xmlFileUri?.let { uri ->
            try {
                requireActivity().contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        xmlFileUri = null
        loadCommands()
        setupSpinner()
    }

    private fun checkPersistedXmlFile() {
        requireActivity().contentResolver.persistedUriPermissions.firstOrNull()?.uri?.let {
            xmlFileUri = it
        }
    }

    private fun openXmlFilePicker() {
        openDocument.launch(arrayOf("text/xml"))
    }

    private fun loadCommands() {
        commands.clear()

        // Try to load from custom XML file if available
        xmlFileUri?.let { uri ->
            try {
                requireActivity().contentResolver.openInputStream(uri)?.use { stream ->
                    parseXmlFile(stream)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If custom XML fails, fall back to default
            }
        }

        // Load default commands if no custom XML or if custom XML failed
        val defaultXmlStream = resources.openRawResource(R.raw.default_commands)
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

    private fun setupSpinner() {
        val spinnerItems = mutableListOf(DEFAULT_SELECTION).apply {
            addAll(commands.map { it.description })
        }
        val adapter = ArrayAdapter(
            requireContext(),
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
                    textViewCommand.text = "Command: ${command.command}"
                    textViewResponse.text = "Expected Response: ${command.response}"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                textViewCommand.text = ""
                textViewResponse.text = ""
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetToDefault()
    }
}