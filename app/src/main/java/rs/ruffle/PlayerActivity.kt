package rs.ruffle

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.androidgamesdk.GameActivity
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import kotlin.math.hypot

class PlayerActivity : GameActivity() {
    @Suppress("unused")
    // Used by Rust
    private val swfBytes: ByteArray?
        get() {
            val uri = intent.data
            if (uri?.scheme == "content") {
                try {
                    contentResolver.openInputStream(uri).use { inputStream ->
                        if (inputStream == null) {
                            return null
                        }
                        val bytes = ByteArray(inputStream.available())
                        val dataInputStream = DataInputStream(inputStream)
                        dataInputStream.readFully(bytes)
                        return bytes
                    }
                } catch (ignored: IOException) {
                }
            }
            return null
        }

    @Suppress("unused")
    // Used by Rust
    private val swfUri: String?
        get() {
            return intent.dataString
        }

    @Suppress("unused")
    // Used by Rust
    private val traceOutput: String?
        get() {
            return intent.getStringExtra("traceOutput")
        }

    /**
     * The movie as a file on disk, when Ruffle can read it directly. Then the movie
     * can load the files in its folder, as if opened with "Select a folder".
     */
    private val swfFile: File? by lazy {
        intent.data?.let { StorageAccess.fileFor(this, it) }?.takeIf { it.isFile && it.canRead() }
    }

    /** The folder the movie was opened from, whose files it can load. */
    private val folderAccess: FolderAccess? by lazy {
        val folder = intent.getStringExtra(EXTRA_FOLDER)?.let(Uri::parse)
            ?: swfFile?.parentFile?.let(Uri::fromFile)
        folder?.let { FolderAccess(contentResolver, it) }
    }

    @Suppress("unused")
    // Used by Rust
    private val folderSwfPath: String?
        get() {
            val path = if (intent.hasExtra(EXTRA_FOLDER)) {
                intent.getStringExtra(EXTRA_FOLDER_SWF_PATH)
            } else {
                swfFile?.name
            }
            return path?.takeIf { folderAccess != null }
        }

    @Suppress("unused")
    // Used by Rust
    private fun openFolderFile(path: String): Int = try {
        folderAccess?.openFile(path)?.detachFd() ?: -1
    } catch (e: Exception) {
        Log.w("ruffle", "Couldn't open $path in the folder", e)
        -1
    }

    @Suppress("unused")
    // Used by Rust
    private fun navigateToUrl(url: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private var loc = IntArray(2)

    @Suppress("unused")
    // Handle of an EventLoopProxy over in rust-land
    private val eventLoopHandle: Long = 0

    @Suppress("unused")
    // Used by Rust
    private val locInWindow: IntArray
        get() {
            mSurfaceView.getLocationInWindow(loc)
            return loc
        }

    @Suppress("unused")
    // Used by Rust
    private val surfaceWidth: Int
        get() = mSurfaceView.width

    @Suppress("unused")
    // Used by Rust
    private val surfaceHeight: Int
        get() = mSurfaceView.height

    private external fun keydown(keyTag: String)
    private external fun keyup(keyTag: String)
    private external fun requestContextMenu()
    private external fun runContextMenuCallback(index: Int)
    private external fun clearContextMenu()
    private external fun gotoRootFrame(frame: Int)
    private external fun gotoGameScene(index: Int)
    private external fun requestSceneMenu()

    @Suppress("unused")
    // Used by Rust
    private fun showContextMenu(
        items: Array<String>,
        timeline: Array<String>,
        currentFrame: Int,
        framesLoaded: Int,
        totalFrames: Int
    ) {
        runOnUiThread {
            val popup = PopupMenu(this, findViewById(R.id.button_cm))
            val menu = popup.menu
            if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true)
            }
            var group = 1
            for (i in items.indices) {
                val elements = items[i].split(" ".toRegex(), limit = 4).toTypedArray()
                val enabled = elements[0].toBoolean()
                val separatorBefore = elements[1].toBoolean()
                val checked = elements[2].toBoolean()
                val caption = elements[3]
                if (separatorBefore) group += 1
                val item = menu.add(group, i, Menu.NONE, caption)
                item.setEnabled(enabled)
                if (checked) {
                    item.setCheckable(true)
                    item.setChecked(true)
                }
            }
            val exitItemId: Int = items.size

            // Items after the Exit item jump around the root timeline, or between the
            // scenes of a game that keeps its own list of them.
            val timelineItems = TimelineItems(currentFrame, framesLoaded, totalFrames)
            if (totalFrames > 0) {
                group += 1
                timelineItems.add(menu, group, timeline, exitItemId + 1, scenesAtTop = false)
                group += 1
            }

            menu.add(group, exitItemId, Menu.NONE, "Exit")
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when {
                    item.itemId == exitItemId -> finish()

                    item.itemId < exitItemId -> runContextMenuCallback(item.itemId)

                    timelineItems.click(item.itemId) -> Unit

                    // A submenu header: let the submenu open.
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            popup.setOnDismissListener { clearContextMenu() }
            popup.show()
        }
    }

    @Suppress("unused")
    // Used by Rust
    private fun showSceneMenu(
        timeline: Array<String>,
        currentFrame: Int,
        framesLoaded: Int,
        totalFrames: Int
    ) {
        runOnUiThread {
            val popup = PopupMenu(this, findViewById(R.id.button_scenes))
            val menu = popup.menu
            if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true)
            }
            val timelineItems = TimelineItems(currentFrame, framesLoaded, totalFrames)
            timelineItems.add(menu, 1, timeline, 0, scenesAtTop = true)
            popup.setOnMenuItemClickListener { item: MenuItem -> timelineItems.click(item.itemId) }
            popup.show()
        }
    }

    /**
     * The items of a menu that jump around the root timeline, or between the scenes of a
     * game that keeps its own list of them. [timeline] has the entries that Rust sends,
     * "<scene|label|game> <enabled> <checked> <frame> <caption>", where the frame of a
     * game scene is its index in the game's scene list.
     */
    private inner class TimelineItems(
        private val currentFrame: Int,
        private val framesLoaded: Int,
        private val totalFrames: Int
    ) {
        private val frames = HashMap<Int, Int>()
        private val gameScenes = HashMap<Int, Int>()
        private var gotoFrameItemId = -1

        /**
         * Adds the scenes, frame labels and "Go to frame" to [menu] in [group], with item
         * IDs from [firstItemId], and returns the next free item ID. The scenes go in a
         * submenu, or straight into [menu] with [scenesAtTop], before the rest.
         */
        fun add(
            menu: Menu,
            group: Int,
            timeline: Array<String>,
            firstItemId: Int,
            scenesAtTop: Boolean
        ): Int {
            var nextItemId = firstItemId
            val (labels, scenes) = timeline
                .map { it.split(" ".toRegex(), limit = 5) }
                .partition { it[0] == "label" }

            val scenesMenu = if (scenesAtTop) {
                menu
            } else {
                menu.addSubMenu(group, nextItemId++, Menu.NONE, R.string.scenes)
            }
            val scenesGroup = if (scenesAtTop) group else Menu.NONE
            for (entry in scenes) {
                addEntry(scenesMenu, scenesGroup, entry, nextItemId++)
            }
            // Say why a list is empty, as many movies have no scenes or labels.
            if (scenes.isEmpty()) {
                scenesMenu.add(scenesGroup, nextItemId++, Menu.NONE, R.string.no_scenes)
                    .setEnabled(false)
            }

            val restGroup = if (scenesAtTop) group + 1 else group
            val labelsMenu =
                menu.addSubMenu(restGroup, nextItemId++, Menu.NONE, R.string.frame_labels)
            for (entry in labels) {
                addEntry(labelsMenu, Menu.NONE, entry, nextItemId++)
            }
            if (labels.isEmpty()) {
                labelsMenu.add(Menu.NONE, nextItemId++, Menu.NONE, R.string.no_frame_labels)
                    .setEnabled(false)
            }
            gotoFrameItemId = nextItemId++
            menu.add(restGroup, gotoFrameItemId, Menu.NONE, R.string.go_to_frame)
            return nextItemId
        }

        private fun addEntry(menu: Menu, group: Int, entry: List<String>, itemId: Int) {
            val item = menu.add(group, itemId, Menu.NONE, entry[4])
            item.setEnabled(entry[1].toBoolean())
            if (entry[2].toBoolean()) {
                item.setCheckable(true)
                item.setChecked(true)
            }
            val targets = if (entry[0] == "game") gameScenes else frames
            targets[itemId] = entry[3].toInt()
        }

        /** Does what the item with [itemId] does, and returns whether it's one of these. */
        fun click(itemId: Int): Boolean {
            frames[itemId]?.let {
                gotoRootFrame(it)
                return true
            }
            gameScenes[itemId]?.let {
                gotoGameScene(it)
                return true
            }
            if (itemId == gotoFrameItemId) {
                showGotoFrame(currentFrame, framesLoaded, totalFrames)
                return true
            }
            return false
        }
    }

    private fun showGotoFrame(currentFrame: Int, framesLoaded: Int, totalFrames: Int) {
        val lastFrame = maxOf(framesLoaded, 1)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = getString(R.string.go_to_frame_hint, lastFrame)
        input.setText(currentFrame.coerceIn(1, lastFrame).toString())
        input.setSelectAllOnFocus(true)
        val padding = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        container.setPadding(padding, 0, padding, 0)
        container.addView(input)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.go_to_frame_title, totalFrames))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                input.text.toString().toIntOrNull()?.let {
                    gotoRootFrame(it.coerceIn(1, lastFrame))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @Suppress("unused")
    // Used by Rust
    private fun getAndroidDataStorageDir(): String {
        // TODO It can also be placed in an external storage path in the future to share archived content
        val storageDirPath = "${filesDir.absolutePath}/ruffle/shared_objects"
        val storageDir = File(storageDirPath)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return storageDirPath
    }

    override fun onCreateSurfaceView() {
        val inflater = layoutInflater

        @SuppressLint("InflateParams")
        val layout = inflater.inflate(R.layout.keyboard, null) as ConstraintLayout

        contentViewId = View.generateViewId()
        layout.id = contentViewId
        setContentView(layout)
        mSurfaceView = InputEnabledSurfaceView(this)

        mSurfaceView.contentDescription = "Ruffle Player"

        val placeholder = findViewById<View>(R.id.placeholder)
        val pars = placeholder.layoutParams as ConstraintLayout.LayoutParams
        val parent = placeholder.parent as ViewGroup
        val index = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        parent.addView(mSurfaceView, index)
        mSurfaceView.setLayoutParams(pars)
        val keys = gatherAllDescendantsOfType<Button>(
            layout.getViewById(R.id.keyboard),
            Button::class.java
        )
        for (b in keys) {
            b.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                val tag = view.tag as String
                if (motionEvent.action == MotionEvent.ACTION_DOWN) keydown(tag)
                if (motionEvent.action == MotionEvent.ACTION_UP) keyup(tag)
                view.performClick()
                false
            }
        }
        layout.findViewById<View>(R.id.button_kb).setOnClickListener {
            val keyboard = layout.getViewById(R.id.keyboard)
            if (keyboard.visibility == View.VISIBLE) {
                keyboard.visibility = View.GONE
            } else {
                keyboard.visibility = View.VISIBLE
            }
        }
        layout.findViewById<View>(R.id.button_cm)
            .setOnClickListener { requestContextMenu() }
        layout.findViewById<View>(R.id.button_scenes)
            .setOnClickListener { requestSceneMenu() }
        makeToolbarMovable(layout)
        layout.requestLayout()
        layout.requestFocus()
        mSurfaceView.holder.addCallback(this)
        ViewCompat.setOnApplyWindowInsetsListener(mSurfaceView, this)
    }

    /**
     * Lets the bar of buttons at the bottom left be moved anywhere on the screen: holding one
     * of its buttons still for [MOVE_HOLD_MILLIS] lets the whole bar be dragged, which the
     * bar shows by growing a little (and the device by vibrating), while tapping a button
     * still does what it does. It stays opaque, as a translucent bar over the game's surface
     * can't be seen. Where the bar is left is kept, as a share of the room it can move in, so
     * that it fits the screen in either orientation.
     */
    @SuppressLint("ClickableViewAccessibility") // Taps go on to the buttons' own handling.
    private fun makeToolbarMovable(layout: ConstraintLayout) {
        val toolbar = layout.findViewById<View>(R.id.toolbar)
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val spaceX = { (layout.width - toolbar.width).coerceAtLeast(1).toFloat() }
        val spaceY = { (layout.height - toolbar.height).coerceAtLeast(1).toFloat() }
        fun moveTo(x: Float, y: Float) {
            toolbar.translationX = x.coerceIn(0f, spaceX()) - toolbar.left
            toolbar.translationY = y.coerceIn(0f, spaceY()) - toolbar.top
        }
        layout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            moveTo(
                preferences.getFloat(TOOLBAR_X, 0f) * spaceX(),
                preferences.getFloat(TOOLBAR_Y, 1f) * spaceY()
            )
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var startRawX = 0f
        var startRawY = 0f
        var startX = 0f
        var startY = 0f
        var moving = false
        var held: Runnable? = null
        val holdToMove = View.OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startX = toolbar.left + toolbar.translationX
                    startY = toolbar.top + toolbar.translationY
                    moving = false
                    val hold = Runnable {
                        // Held long enough: the button lets go of the touch, which moves the bar.
                        moving = true
                        val now = SystemClock.uptimeMillis()
                        val cancel =
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                        view.onTouchEvent(cancel)
                        cancel.recycle()
                        toolbar.scaleX = MOVING_SCALE
                        toolbar.scaleY = MOVING_SCALE
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    held = hold
                    view.postDelayed(hold, MOVE_HOLD_MILLIS)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (moving) {
                        moveTo(startX + dx, startY + dy)
                    } else if (hypot(dx, dy) > touchSlop) {
                        // It isn't held still, so it doesn't move the bar.
                        view.removeCallbacks(held)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(held)
                    if (moving) {
                        moving = false
                        toolbar.scaleX = 1f
                        toolbar.scaleY = 1f
                        preferences.edit()
                            .putFloat(TOOLBAR_X, (toolbar.left + toolbar.translationX) / spaceX())
                            .putFloat(TOOLBAR_Y, (toolbar.top + toolbar.translationY) / spaceY())
                            .apply()
                        return@OnTouchListener true
                    }
                }
            }
            moving
        }
        for (id in listOf(R.id.button_kb, R.id.button_cm, R.id.button_scenes)) {
            layout.findViewById<View>(id).setOnTouchListener(holdToMove)
        }
    }

    private fun hideSystemUI() {
        // This will put the game behind any cutouts and waterfalls on devices which have
        // them, so the corresponding insets will be non-zero.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        // From API 30 onwards, this is the recommended way to hide the system UI, rather than
        // using View.setSystemUiVisibility.
        val decorView = window.decorView
        val controller = WindowInsetsControllerCompat(
            window,
            decorView
        )
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        nativeInit { message ->
            Log.e("ruffle", "Handling panic: $message")
            startActivity(
                Intent(this, PanicActivity::class.java).apply {
                    putExtra("message", message)
                }
            )
        }
        // When true, the app will fit inside any system UI windows.
        // When false, we render behind any system UI windows.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()
        // You can set IME fields here or in native code using GameActivity_setImeEditorInfoFields.
        // We set the fields in native_engine.cpp.
        // super.setImeEditorInfoFields(InputType.TYPE_CLASS_TEXT,
        //     IME_ACTION_NONE, IME_FLAG_NO_FULLSCREEN );
        requestNoStatusBarFeature()
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
    }

    // Used by Rust
    @Suppress("unused")
    val isGooglePlayGames: Boolean
        get() {
            val pm = packageManager
            return pm.hasSystemFeature("com.google.android.play.feature.HPE_EXPERIENCE")
        }

    private fun requestNoStatusBarFeature() {
        // Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowInsetsControllerCompat(
            window,
            mSurfaceView
        ).hide(WindowInsetsCompat.Type.statusBars())
    }

    companion object {
        /** The folder (a document tree or `file://` URI) that the movie can load files from. */
        const val EXTRA_FOLDER = "folder"

        /** The path of the movie within [EXTRA_FOLDER]. */
        const val EXTRA_FOLDER_SWF_PATH = "folderSwfPath"

        /** The player's preferences, and the keys of where its buttons were moved to. */
        const val PREFERENCES = "player"
        const val TOOLBAR_X = "toolbarX"
        const val TOOLBAR_Y = "toolbarY"

        /** How long a button of the bar must be held still before dragging moves the bar. */
        const val MOVE_HOLD_MILLIS = 500L

        /** How much bigger the bar is while it can be moved. */
        const val MOVING_SCALE = 1.15f

        init {
            // load the native activity
            System.loadLibrary("ruffle_android")
        }

        @JvmStatic
        private external fun nativeInit(crashCallback: CrashCallback)

        private fun <T> gatherAllDescendantsOfType(v: View, t: Class<*>): List<T> {
            val result: MutableList<T> = ArrayList()
            @Suppress("UNCHECKED_CAST")
            if (t.isInstance(v)) result.add(v as T)
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    result.addAll(gatherAllDescendantsOfType(v.getChildAt(i), t))
                }
            }
            return result
        }
    }

    fun interface CrashCallback {
        fun onCrash(message: String)
    }
}
