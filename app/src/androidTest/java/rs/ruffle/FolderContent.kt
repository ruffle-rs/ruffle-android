package rs.ruffle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import java.util.concurrent.TimeoutException
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val BASIC_SAMPLE_PACKAGE = "rs.ruffle"
private const val LAUNCH_TIMEOUT = 5000L

/**
 * Opens a movie from a folder, as "Select a folder" does, and checks that it can load
 * files from a subfolder next to it. `folder_test.swf` loads `parts/hello.txt`, and then
 * `PARTS/HELLO.TXT`, which only exists when names are matched ignoring case, tracing both.
 */
@RunWith(AndroidJUnit4::class)
class FolderContent {
    private lateinit var device: UiDevice
    private lateinit var traceOutput: File
    private var lastTraceSize: Long = 0

    @Before
    fun startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        ViewMatchers.assertThat(launcherPackage, CoreMatchers.notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT
        )

        // Lay out a folder with the movie, and a subfolder with the file it loads
        val context = ApplicationProvider.getApplicationContext<Context>()
        traceOutput = File.createTempFile("trace", ".txt", context.cacheDir)
        lastTraceSize = 0
        val folder = File(context.cacheDir, "folder_test")
        folder.deleteRecursively()
        File(folder, "parts").mkdirs()
        File(folder, "parts/hello.txt").writeText("hello from parts")
        val swfFile = File(folder, "Folder Test.swf")
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        val inStream = resources.openRawResource(
            rs.ruffle.test.R.raw.folder_test
        )
        swfFile.writeBytes(inStream.readBytes())

        // Launch the app
        val intent = context.packageManager.getLaunchIntentForPackage(
            BASIC_SAMPLE_PACKAGE
        )?.apply {
            component = ComponentName("rs.ruffle", "rs.ruffle.PlayerActivity")
            data = Uri.fromFile(swfFile)
            putExtra(PlayerActivity.EXTRA_FOLDER, Uri.fromFile(folder).toString())
            putExtra(PlayerActivity.EXTRA_FOLDER_SWF_PATH, swfFile.name)
            putExtra("traceOutput", traceOutput.absolutePath)
            // Clear out any previous instances
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Wait for the app to appear
        device.wait(
            Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
            LAUNCH_TIMEOUT
        )
    }

    @Test
    fun loadsFilesNextToTheMovie() {
        waitUntilNewLogAndIdle()

        val trace = traceOutput.readLines()
        ViewMatchers.assertThat(
            trace,
            CoreMatchers.equalTo(listOf("hello from parts", "hello from parts"))
        )
    }

    /**
     * Waits until the log file receives new trace output, and then waits for it to become idle with
     * no more output for a period of time.
     *
     * @param idleWindowMillis How long the log file must stay the same size for, before it's considered idle
     * @param timeoutMillis How long to keep waiting for, before throwing an error
     */
    private fun waitUntilNewLogAndIdle(idleWindowMillis: Long = 1000, timeoutMillis: Long = 10000) {
        val startTimeMillis = SystemClock.uptimeMillis()
        val timeoutAt = startTimeMillis + timeoutMillis
        waitUntilNewLog(timeoutAt - SystemClock.uptimeMillis())
        waitUntilLogIdles(idleWindowMillis, timeoutAt - SystemClock.uptimeMillis())
    }

    /**
     * Waits until new trace output has been written to the log file.
     *
     * @param timeoutMillis How long to keep waiting for, before throwing an error
     */
    private fun waitUntilNewLog(timeoutMillis: Long = 10000) {
        val startTimeMillis = SystemClock.uptimeMillis()
        val timeoutAt = startTimeMillis + timeoutMillis
        while (true) {
            val size = traceOutput.length()
            if (size > lastTraceSize) {
                lastTraceSize = size
                return
            }

            if (SystemClock.uptimeMillis() >= timeoutAt) {
                throw TimeoutException("No trace output was received within $timeoutMillis ms")
            }
            Thread.sleep(100)
        }
    }

    /**
     * Waits until the log file stops receiving any trace output.
     *
     * @param idleWindowMillis How long the log file must stay the same size for, before it's considered idle
     * @param timeoutMillis How long to keep waiting for, before throwing an error
     */
    private fun waitUntilLogIdles(idleWindowMillis: Long = 1000, timeoutMillis: Long = 10000) {
        val startTimeMillis = SystemClock.uptimeMillis()
        val timeoutAt = startTimeMillis + timeoutMillis

        lastTraceSize = traceOutput.length()
        Thread.sleep(idleWindowMillis)

        while (true) {
            val size = traceOutput.length()
            if (size == lastTraceSize) {
                return
            }
            lastTraceSize = size

            if (SystemClock.uptimeMillis() >= timeoutAt) {
                throw TimeoutException("No trace output was received within $timeoutMillis ms")
            }
            Thread.sleep(idleWindowMillis)
        }
    }
}
