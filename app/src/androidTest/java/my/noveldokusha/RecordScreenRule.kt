package my.noveldokusha

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.concurrent.thread

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class RecordScreenRule : TestWatcher() {

    private val folder = "/sdcard/test_recordings/"

    override fun starting(description: Description?) {
        val file = "$folder${description!!.displayName}.mp4"
        print(">>> video record test to file: $file")
        thread {
            runCatching { Utils.runCommand("mkdir $folder") }.onFailure { println(it) }
            runCatching { Utils.runCommand("rm $file") }.onFailure { println(it) }
            runCatching { Utils.runCommand("screenrecord --bit-rate 400000 $file") }.onFailure {
                println(
                    it
                )
            }
        }
        super.starting(description)
    }

    override fun finished(description: Description?) {
        runCatching { Utils.runCommand("pkill -2 screenrecord") }.onFailure { println(it) }
        super.finished(description)
    }
}