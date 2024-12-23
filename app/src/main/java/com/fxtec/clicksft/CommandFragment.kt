package com.fxtec.clicksft

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
import androidx.fragment.app.Fragment
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class CommandFragment : Fragment() {
    private lateinit var spinner: Spinner
    private lateinit var textViewCommand: TextView
    private lateinit var textViewResponse: TextView
    private lateinit var buttonLoadXml: Button
    private val commands = mutableListOf<UsbCommand>()

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

        buttonLoadXml.setOnClickListener {
            (activity as? MainActivity)?.openXmlFilePicker()
        }

        loadCommands()
        setupSpinner()
    }

    private fun loadCommands() {
        commands.clear()

        (activity as? MainActivity)?.getXmlFileUri()?.let { uri ->
            activity?.contentResolver?.openInputStream(uri)?.use { stream ->
                parseXmlFile(stream)
                return
            }
        }

        val defaultXmlStream = resources.openRawResource(R.raw.default_commands)
        parseXmlFile(defaultXmlStream)
    }

    fun updateXmlFile(uri: Uri) {
        commands.clear()
        activity?.contentResolver?.openInputStream(uri)?.use { stream ->
            parseXmlFile(stream)
            setupSpinner()
        }
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
            if (inputStream != resources.openRawResource(R.raw.default_commands)) {
                val defaultXmlStream = resources.openRawResource(R.raw.default_commands)
                parseXmlFile(defaultXmlStream)
            }
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
}