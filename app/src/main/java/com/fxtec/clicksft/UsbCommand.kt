package com.fxtec.clicksft

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Color

data class UsbCommand(
    var description: String = "",
    var command: String = "",
    var response: String = ""
)

// Test plan format
data class UsbTestPlan(
    val items: List<UsbTestItem>
)

data class UsbTestItem(
    val description: String,
    val command: String,
    val expectedResponse: String
)

data class TestResult(
    val description: String,
    val command: String,
    val actualResponse: String,
    val expectedResponse: String,
    val isPassed: Boolean
) {
    fun toFormattedString(): CharSequence {
        val builder = SpannableStringBuilder()
        builder.append("Description: $description\n")
        builder.append("Command: $command\n")
        builder.append("Response Comparison:\n")
        builder.append("  Actual:    $actualResponse\n")
        builder.append("  Expected:  $expectedResponse\n")
        builder.append("Result: ")

        val resultStart = builder.length
        val resultText = if (isPassed) "PASS" else "FAIL"
        builder.append(resultText)

        if (!isPassed) {
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                resultStart, builder.length,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(Color.RED),
                resultStart, builder.length,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }
}

// XML format type
enum class XmlFormat {
    COMMAND,
    TEST_PLAN
}