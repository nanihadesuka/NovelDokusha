package my.noveldokusha

import androidx.annotation.WorkerThread
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

object Utils {

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @WorkerThread
    fun runCommand(text: String) {
        uiAutomation.executeShellCommand(text)
            .fileDescriptor
            .let { FileInputStream(it) }
            .use { it.bufferedReader().use { reader -> reader.readText() } }
            .also { result ->
                println(">>>> command: $text")
                println(">>>> result: $result")
            }

    }
}