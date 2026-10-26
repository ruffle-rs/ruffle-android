package rs.ruffle

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "rs.ruffle"
private const val TIMEOUT = 5000L

/**
 * Checks that the bar of buttons at the bottom left moves when one of its buttons is held
 * still and then dragged, and only then, that its buttons still work when tapped, and that
 * it stays where it was left when the player is opened again.
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
        val middle = Point(device.displayWidth / 2, device.displayHeight / 2)

        // A drag without holding first leaves the bar where it is. (UiObject2.drag would hold
        // first, for as long as a long press.) 20 steps of 5 ms take 0.1 s.
        device.swipe(before.centerX(), before.centerY(), middle.x, middle.y, 20)
        device.waitForIdle()
        val dragged = bounds("button_kb")
        assertTrue(
            "a drag without a hold shouldn't move the buttons: $before, $dragged",
            abs(dragged.left - before.left) <= 2 && abs(dragged.top - before.top) <= 2
        )

        // Holding still for 1 s (200 steps of 5 ms), then dragging over 1 s, moves it.
        val unmoved = screenshot()
        val start = Point(before.centerX(), before.centerY())
        device.swipe(arrayOf(start, start, middle), 200)
        device.waitForIdle()
        val after = bounds("button_kb")
        // The button is drawn where it was moved to, over the game, which a window drawn only
        // within where the buttons were at first would hide.
        val moved = screenshot()
        val x = after.left + after.width() / 4
        val y = after.centerY()
        if (moved.getPixel(x, y) == unmoved.getPixel(x, y)) {
            fail(
                "the moved button, at $after, should be drawn over the game at ($x, $y), " +
                    "which is %08x as before\n".format(moved.getPixel(x, y)) +
                    whyNotDrawn(before, after, unmoved, moved)
            )
        }
        // On narrow screens, the buttons have little room to move sideways.
        assertTrue(
            "the buttons should have moved up: $before, $after",
            after.top < before.top - 100
        )
        assertFalse(
            "moving the bar by its keyboard button shouldn't show the keyboard",
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

    /** What the screen and the window hold, to tell why the moved bar isn't drawn. */
    private fun whyNotDrawn(before: Rect, after: Rect, unmoved: Bitmap, moved: Bitmap): String {
        val report = StringBuilder()
        fun pixels(name: String, at: Rect) {
            val x = at.left + at.width() / 4
            report.append(
                "$name ($x, ${at.centerY()}): %08x before the move, %08x after\n".format(
                    unmoved.getPixel(x, at.centerY()),
                    moved.getPixel(x, at.centerY())
                )
            )
        }
        pixels("where the bar was", before)
        pixels("where the bar is", after)
        report.append("screenshots ${moved.width}x${moved.height}\n")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).firstOrNull() ?: return@runOnMainSync
            val toolbar = activity.findViewById<View>(R.id.toolbar)
            val location = IntArray(2).also { toolbar.getLocationInWindow(it) }
            report.append(
                "toolbar laid out at (${toolbar.left}, ${toolbar.top}), moved by " +
                    "(${toolbar.translationX}, ${toolbar.translationY}), in the window at " +
                    "${location.toList()}, shown ${toolbar.isShown}\n"
            )
            val root = activity.window.decorView.parent
            for (field in listOf("mTransparentRegion", "mPreviousTransparentRegion")) {
                val value = try {
                    root.javaClass.getDeclaredField(field).apply { isAccessible = true }.get(root)
                } catch (e: ReflectiveOperationException) {
                    "unreadable ($e)"
                }
                report.append("window's $field: $value\n")
            }
            val layout = toolbar.parent as ViewGroup
            for (index in 0 until layout.childCount) {
                val child = layout.getChildAt(index)
                report.append(
                    "  child $index: ${child.javaClass.simpleName} " +
                        "${child.left},${child.top}-${child.right},${child.bottom} " +
                        "visibility ${child.visibility} willNotDraw ${child.willNotDraw()}\n"
                )
            }
        }
        // The layers of the app, with what SurfaceFlinger keeps of their transparent regions
        val layers = device.executeShellCommand("dumpsys SurfaceFlinger").lines()
        var left = 0
        var shown = 0
        for (line in layers) {
            if (line.contains(PACKAGE) && (line.contains("Layer") || line.contains("+ "))) left = 14
            if (left > 0 && shown < 120) {
                report.append(line.take(160)).append("\n")
                left--
                shown++
            }
        }
        // Whether the bar is drawn there when it's laid out there, instead of moved there
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).firstOrNull() ?: return@runOnMainSync
            val toolbar = activity.findViewById<View>(R.id.toolbar)
            val layout = toolbar.parent as ViewGroup
            val params = toolbar.layoutParams as ViewGroup.MarginLayoutParams
            val left = (toolbar.left + toolbar.translationX).toInt()
            val top = (toolbar.top + toolbar.translationY).toInt()
            params.marginStart = left
            params.leftMargin = left
            params.bottomMargin = layout.height - top - toolbar.height
            toolbar.translationX = 0f
            toolbar.translationY = 0f
            toolbar.layoutParams = params
        }
        device.waitForIdle()
        val laidOut = screenshot()
        val x = after.left + after.width() / 4
        report.append(
            "laid out there instead: %08x, bar at %s\n".format(
                laidOut.getPixel(x, after.centerY()),
                bounds("button_kb")
            )
        )
        return report.toString()
    }

    private fun bounds(id: String): Rect = device.findObject(By.res(PACKAGE, id)).visibleBounds

    private fun screenshot(): Bitmap {
        val file = File.createTempFile("screen", ".png", context.cacheDir)
        assertTrue("a screenshot should be taken", device.takeScreenshot(file))
        return BitmapFactory.decodeFile(file.path)
    }
}
