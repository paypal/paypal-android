package com.paypal.android

import android.app.Instrumentation
import java.io.BufferedReader
import java.io.InputStreamReader

fun executeShellCommandWithOutput(
    instrumentation: Instrumentation,
    command: String
): String {
    val output = StringBuilder()
    try {
        val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        val inputStream = java.io.FileInputStream(parcelFileDescriptor.fileDescriptor)
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }
        parcelFileDescriptor.close()
    } catch (e: Exception) {
        output.append("ERROR: ${e.message}")
    }
    return output.toString().trim()
}
