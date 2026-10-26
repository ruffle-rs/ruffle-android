package rs.ruffle

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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "rs.ruffle"
private const val TIMEOUT = 5000L

/**
 * Checks that a game opened from the app's first screen keeps running when the user leaves
 * the app and comes back with its launcher icon, which used to bring back the first screen
 * and close the game. `game_scenes.swf` traces "showing intro" when it starts, and
 * "showing <scene>" whenever it shows another scene.
 */
@RunWith(AndroidJUnit4::class)
class LeaveAndReturn {
    private lateinit var device: UiDevice
    private lateinit var traceOutput: File
    private var lastTraceSize: Long = 0

    @Test
    fun keepsTheGameWhenComingBackFromTheLauncher() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = ApplicationProvider.getApplicationContext<Context>()
        traceOutput = File.createTempFile("trace", ".txt", context.cacheDir)
        val swfFile = File.createTempFile("movie", ".swf", context.cacheDir)
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        swfFile.writeBytes(resources.openRawResource(rs.ruffle.test.R.raw.game_scenes).readBytes())

        // Open the app as its launcher icon does, then a game from its first screen, as
        // picking one there does.
        goHome()
        context.startActivity(
            launcherIntent(context).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), TIMEOUT)
        context.startActivity(
            Intent(context, PlayerActivity::class.java).apply {
                data = Uri.fromFile(swfFile)
                putExtra("traceOutput", traceOutput.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        waitUntilNewLogAndIdle()

        // Leave the app, and come back with its launcher icon.
        goHome()
        context.startActivity(
            launcherIntent(context).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )

        // The game is still there, where it was, and goes on.
        assertTrue(
            "the game should be shown again",
            device.wait(Until.hasObject(By.res(PACKAGE, "button_scenes")), TIMEOUT)
        )
        device.findObject(By.res(PACKAGE, "button_scenes")).click()
        device.wait(Until.findObject(By.text("2. fight")), TIMEOUT).click()
        waitUntilNewLogAndIdle()
        ViewMatchers.assertThat(
            traceOutput.readLines(),
            CoreMatchers.equalTo(listOf("showing intro", "showing fight"))
        )
    }

    private fun launcherIntent(context: Context): Intent =
        context.packageManager.getLaunchIntentForPackage(PACKAGE)!!

    private fun goHome() {
        device.pressHome()
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), TIMEOUT)
    }

    /** Waits until the log file receives new trace output, and then stops receiving it. */
    private fun waitUntilNewLogAndIdle(idleWindowMillis: Long = 1000, timeoutMillis: Long = 10000) {
        val timeoutAt = SystemClock.uptimeMillis() + timeoutMillis
        while (traceOutput.length() <= lastTraceSize) {
            if (SystemClock.uptimeMillis() >= timeoutAt) {
                throw TimeoutException("No trace output was received within $timeoutMillis ms")
            }
            Thread.sleep(100)
        }
        do {
            lastTraceSize = traceOutput.length()
            Thread.sleep(idleWindowMillis)
            if (SystemClock.uptimeMillis() >= timeoutAt) {
                throw TimeoutException("The trace output didn't stop within $timeoutMillis ms")
            }
        } while (traceOutput.length() != lastTraceSize)
    }
}
