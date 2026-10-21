package rs.ruffle

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SubMenu
import android.view.View
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

            // Items after the Exit item jump around the root timeline.
            var nextItemId = exitItemId + 1
            val itemFrames = HashMap<Int, Int>()
            var gotoFrameItemId = -1
            if (totalFrames > 0) {
                group += 1
                var scenesMenu: SubMenu? = null
                var labelsMenu: SubMenu? = null
                for (entry in timeline) {
                    val elements = entry.split(" ".toRegex(), limit = 5).toTypedArray()
                    val submenu = if (elements[0] == "scene") {
                        scenesMenu ?: menu.addSubMenu(
                            group,
                            nextItemId++,
                            Menu.NONE,
                            R.string.scenes
                        ).also { scenesMenu = it }
                    } else {
                        labelsMenu ?: menu.addSubMenu(
                            group,
                            nextItemId++,
                            Menu.NONE,
                            R.string.frame_labels
                        ).also { labelsMenu = it }
                    }
                    val item = submenu.add(Menu.NONE, nextItemId, Menu.NONE, elements[4])
                    item.setEnabled(elements[1].toBoolean())
                    if (elements[2].toBoolean()) {
                        item.setCheckable(true)
                        item.setChecked(true)
                    }
                    itemFrames[nextItemId] = elements[3].toInt()
                    nextItemId += 1
                }
                gotoFrameItemId = nextItemId
                menu.add(group, gotoFrameItemId, Menu.NONE, R.string.go_to_frame)
                group += 1
            }

            menu.add(group, exitItemId, Menu.NONE, "Exit")
            popup.setOnMenuItemClickListener { item: MenuItem ->
                val frame = itemFrames[item.itemId]
                when {
                    item.itemId == exitItemId -> finish()

                    item.itemId < exitItemId -> runContextMenuCallback(item.itemId)

                    frame != null -> gotoRootFrame(frame)

                    item.itemId == gotoFrameItemId -> showGotoFrame(
                        currentFrame,
                        framesLoaded,
                        totalFrames
                    )

                    // A submenu header: let the submenu open.
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            popup.setOnDismissListener { clearContextMenu() }
            popup.show()
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
        layout.requestLayout()
        layout.requestFocus()
        mSurfaceView.holder.addCallback(this)
        ViewCompat.setOnApplyWindowInsetsListener(mSurfaceView, this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val keyboard = findViewById<View>(R.id.keyboard)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        keyboard.visibility = if (isLandscape) View.GONE else View.VISIBLE
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
