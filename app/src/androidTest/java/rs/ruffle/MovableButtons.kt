package rs.ruffle

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "rs.ruffle"
private const val TIMEOUT = 5000L

/**
 * Checks that the buttons at the bottom left move when one of them is dragged, still work
 * when tapped, and stay where they were left when the player is opened again.
 */
@RunWith(AndroidJUnit4::class)
class MovableButtons {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun putTheButtonsBack() {
        context.getSharedPreferences(PlayerActivity.PREFERENCES, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun movesTheButtonsWhereTheyreDragged() {
        val swfFile = File.createTempFile("movie", ".swf", context.cacheDir)
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        swfFile.writeBytes(resources.openRawResource(rs.ruffle.test.R.raw.game_scenes).readBytes())
        openPlayer(swfFile)
        val before = bounds("button_kb")

        device.findObject(By.res(PACKAGE, "button_kb"))
            .drag(Point(device.displayWidth / 2, device.displayHeight / 2))
        device.waitForIdle()
        val after = bounds("button_kb")
        // On narrow screens, the buttons have little room to move sideways.
        assertTrue(
            "the buttons should have moved up: $before, $after",
            after.top < before.top - 100
        )
        assertFalse(
            "dragging the keyboard button shouldn't show the keyboard",
            device.hasObject(By.res(PACKAGE, "button_1"))
        )

        device.findObject(By.res(PACKAGE, "button_scenes")).click()
        assertTrue(
            "the scene button should still open its menu",
            device.wait(Until.hasObject(By.text("2. fight")), TIMEOUT)
        )
        device.pressBack()

        openPlayer(swfFile)
        val reopened = bounds("button_kb")
        assertTrue(
            "the buttons should be where they were left: $after, $reopened",
            abs(reopened.left - after.left) <= 2 && abs(reopened.top - after.top) <= 2
        )
    }

    private fun openPlayer(swfFile: File) {
        context.startActivity(
            Intent(context, PlayerActivity::class.java).apply {
                data = Uri.fromFile(swfFile)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        device.wait(Until.hasObject(By.res(PACKAGE, "button_kb")), TIMEOUT)
        device.waitForIdle()
    }

    private fun bounds(id: String): Rect = device.findObject(By.res(PACKAGE, id)).visibleBounds
}
