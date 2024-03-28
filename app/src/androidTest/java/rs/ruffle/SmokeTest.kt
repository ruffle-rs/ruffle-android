package rs.ruffle

import android.R
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val BASIC_SAMPLE_PACKAGE = "rs.ruffle"
private const val LAUNCH_TIMEOUT = 5000L

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    private lateinit var device: UiDevice
    private lateinit var traceOutput: File
    private lateinit var swfFile: File

    @Before
    fun startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        assertThat(launcherPackage, notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT
        )

        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        traceOutput = File.createTempFile("trace", ".txt", context.cacheDir)
        swfFile = File.createTempFile("movie", ".swf", context.cacheDir)
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        val inStream = resources.openRawResource(
            rs.ruffle.test.R.raw.helloflash
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
    fun emulatorRunsASwf() {
        device.waitForWindowUpdate(null, 1000)
        assertThat(device, notNullValue())

        val trace = traceOutput.readLines()
        assertThat(trace, equalTo(listOf("Hello from Ruffle!")))
    }
}
