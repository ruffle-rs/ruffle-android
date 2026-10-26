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
 * The trace output of `scenes.swf` after jumping to frame 6, the start of its Scene 2.
 * Each frame script of that movie traces where it is, then goes to the next scene.
 */
private val TRACE_FROM_SCENE_2 = listOf(
    "//Scene 2, Frame 1",
    "//gotoAndStop(\"frame3\");",
    "//Scene 2, Frame 3",
    "//gotoAndStop(\"frame2\", \"Scene 3\");",
    "//Scene 3, Frame 2",
    "//gotoAndStop(2.1, \"Scene 4\");",
    "//Scene 4, Frame 3",
    "//gotoAndStop(\"2\", \"Scene 5\");",
    "//Scene 5, Frame 2"
)

@RunWith(AndroidJUnit4::class)
class SceneMenu {
    private lateinit var device: UiDevice
    private lateinit var traceOutput: File
    private var lastTraceSize: Long = 0
    private lateinit var swfFile: File

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

        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        traceOutput = File.createTempFile("trace", ".txt", context.cacheDir)
        swfFile = File.createTempFile("movie", ".swf", context.cacheDir)
        lastTraceSize = 0
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        val inStream = resources.openRawResource(
            rs.ruffle.test.R.raw.scenes
        )
        val bytes = inStream.readBytes()
        swfFile.writeBytes(bytes)
        val intent = context.packageManager.getLaunchIntentForPackage(
            BASIC_SAMPLE_PACKAGE
        )?.apply {
            component = ComponentName("rs.ruffle", "rs.ruffle.PlayerActivity")
            data = Uri.fromFile(swfFile)
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
    fun jumpToScene() {
        waitUntilNewLogAndIdle()
        val startupLines = traceOutput.readLines().size

        openMenu()
        device.wait(Until.findObject(By.text("Scenes")), LAUNCH_TIMEOUT).click()
        device.wait(Until.findObject(By.textStartsWith("Scene 2 ")), LAUNCH_TIMEOUT).click()
        waitUntilNewLogAndIdle()

        val trace = traceOutput.readLines().drop(startupLines)
        ViewMatchers.assertThat(trace, CoreMatchers.equalTo(TRACE_FROM_SCENE_2))
    }

    @Test
    fun jumpToFrameLabel() {
        waitUntilNewLogAndIdle()
        val startupLines = traceOutput.readLines().size

        openMenu()
        device.wait(Until.findObject(By.text("Frame labels")), LAUNCH_TIMEOUT).click()
        device.wait(Until.findObject(By.text("frame1 (6)")), LAUNCH_TIMEOUT).click()
        waitUntilNewLogAndIdle()

        val trace = traceOutput.readLines().drop(startupLines)
        ViewMatchers.assertThat(trace, CoreMatchers.equalTo(TRACE_FROM_SCENE_2))
    }

    @Test
    fun goToFrame() {
        waitUntilNewLogAndIdle()
        val startupLines = traceOutput.readLines().size

        openMenu()
        device.wait(Until.findObject(By.textStartsWith("Go to frame")), LAUNCH_TIMEOUT).click()
        val input = device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            LAUNCH_TIMEOUT
        )
        input.text = "6"
        device.findObject(By.res("android", "button1")).click()
        waitUntilNewLogAndIdle()

        val trace = traceOutput.readLines().drop(startupLines)
        ViewMatchers.assertThat(trace, CoreMatchers.equalTo(TRACE_FROM_SCENE_2))
    }

    private fun openMenu() {
        device.wait(Until.findObject(By.res(BASIC_SAMPLE_PACKAGE, "button_cm")), LAUNCH_TIMEOUT)
            .click()
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
