package rs.ruffle

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.androidgamesdk.GameActivity
import java.io.DataInputStream
import java.io.File

class PlayerActivity : GameActivity() {
    @Suppress("unused")
    // Used by Rust
    private val swfBytes: ByteArray?
        get() {
            val uri = intent.data
            if (uri == null) {
                Log.e("PlayerActivity", "No URI provided in intent")
                return null
            }

            try {
                Log.d(
                    "PlayerActivity",
                    "Attempting to load SWF from URI: $uri (scheme: ${uri.scheme})"
                )

                when (uri.scheme) {
                    "content" -> {
                        try {
                            // For content:// URIs (including those from FileProvider)
                            contentResolver.openInputStream(uri).use { inputStream ->
                                if (inputStream == null) {
                                    Log.e("PlayerActivity", "Failed to open content URI: $uri")
                                    return null
                                }

                                // Get the size first
                                val size = inputStream.available()
                                Log.d(
                                    "PlayerActivity",
                                    "Content URI input stream available size: $size bytes"
                                )

                                // Creating byte array of exact size
                                val bytes = ByteArray(size)
                                val dataInputStream = DataInputStream(inputStream)
                                val bytesRead = dataInputStream.read(bytes)

                                Log.d(
                                    "PlayerActivity",
                                    "Successfully read content URI: $uri (read $bytesRead of $size bytes)"
                                )

                                if (bytesRead <= 0 || bytesRead < size) {
                                    Log.e(
                                        "PlayerActivity",
                                        "Failed to read complete content from URI: $uri ($bytesRead of $size bytes)"
                                    )
                                    // Try to continue anyway with what we have
                                }

                                // Add to history
                                swfUri = uri
                                addToHistory(uri)

                                return bytes
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerActivity", "Error reading content URI: $uri", e)
                            return null
                        }
                    }
                    "file" -> {
                        try {
                            // Handle file:// URIs (although these should be avoided in favor of content:// URIs)
                            val filePath = uri.path
                            if (filePath == null) {
                                Log.e("PlayerActivity", "File URI has no path: $uri")
                                return null
                            }

                            val file = File(filePath)
                            if (!file.exists()) {
                                Log.e("PlayerActivity", "File doesn't exist: $filePath")
                                return null
                            }

                            if (!file.canRead()) {
                                Log.e("PlayerActivity", "File isn't readable: $filePath")
                                // Try to make it readable
                                file.setReadable(true, false)
                                if (!file.canRead()) {
                                    Log.e(
                                        "PlayerActivity",
                                        "Failed to make file readable: $filePath"
                                    )
                                    return null
                                }
                            }

                            Log.d(
                                "PlayerActivity",
                                "Reading file directly: $filePath (${file.length()} bytes)"
                            )

                            file.inputStream().use { inputStream ->
                                val size = inputStream.available()
                                val bytes = ByteArray(size)
                                val dataInputStream = DataInputStream(inputStream)
                                val bytesRead = dataInputStream.read(bytes)

                                Log.d(
                                    "PlayerActivity",
                                    "Successfully read file: $filePath (read $bytesRead of $size bytes)"
                                )

                                // Add to history
                                swfUri = uri
                                addToHistory(uri)

                                return bytes
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerActivity", "Error reading file URI: $uri", e)
                            return null
                        }
                    }
                    else -> {
                        Log.e("PlayerActivity", "Unsupported URI scheme: ${uri.scheme}")
                        return null
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Unexpected error loading SWF", e)
                return null
            }

            return null
        }

    @Suppress("unused")
    // Used by Rust
    private fun getSwfUriString(): String? {
        val uri = intent.data
        if (uri != null) {
            // Add to history
            swfUri = uri
            addToHistory(uri)
        }
        return intent.dataString
    }

    @JvmName("getSwfUri")
    @Suppress("unused")
    // Used by Rust
    public fun getSwfUri(): String? {
        Log.d("PlayerActivity", "getSwfUri() called from native code")
        if (swfUri == null) {
            swfUri = intent.data
            Log.d("PlayerActivity", "getSwfUri() initialized from intent: $swfUri")
        } else {
            Log.d("PlayerActivity", "getSwfUri() returning existing uri: $swfUri")
        }
        return swfUri?.toString()
    }

    @JvmName("getSwfUriObject")
    @Suppress("unused")
    // Used by Rust (alternative method if needed)
    public fun getSwfUriObject(): Uri? {
        Log.d("PlayerActivity", "getSwfUriObject() called")
        if (swfUri == null) {
            swfUri = intent.data
        }
        return swfUri
    }

    @Suppress("unused")
    // Used by Rust
    private val traceOutput: String?
        get() {
            return intent.getStringExtra("traceOutput")
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

    private external fun keydown(keyCode: Byte, keyChar: Char)
    private external fun keyup(keyCode: Byte, keyChar: Char)
    private external fun requestContextMenu()
    private external fun runContextMenuCallback(index: Int)
    private external fun clearContextMenu()

    @Suppress("unused")
    // Used by Rust
    private fun showContextMenu(items: Array<String>) {
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
            menu.add(group, exitItemId, Menu.NONE, "Exit")
            popup.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == exitItemId) {
                    finish()
                } else {
                    runContextMenuCallback(item.itemId)
                }
                true
            }
            popup.setOnDismissListener { clearContextMenu() }
            popup.show()
        }
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

    private lateinit var loadingSpinner: ProgressBar
    private var isContentLoaded = false
    private lateinit var favoriteButton: ImageButton
    private var isFavorite = false
    private var swfUri: Uri? = null

    override fun onCreateSurfaceView() {
        val inflater = layoutInflater

        @SuppressLint("InflateParams")
        val layout = inflater.inflate(R.layout.keyboard, null) as ConstraintLayout

        contentViewId = View.generateViewId()
        layout.id = contentViewId
        setContentView(layout)
        mSurfaceView = InputEnabledSurfaceView(this)

        mSurfaceView.contentDescription = "Ruffle Player"

        // Create and add loading spinner with improved styling
        loadingSpinner = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        loadingSpinner.isIndeterminate = true

        // Set up an overlay background for the spinner to make it stand out
        val overlayBackground = View(this)
        overlayBackground.setBackgroundColor(Color.BLACK)
        overlayBackground.alpha = 0.5f

        val spinnerParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        spinnerParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        spinnerParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        spinnerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        loadingSpinner.layoutParams = spinnerParams

        val overlayParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        overlayBackground.layoutParams = overlayParams

        layout.addView(overlayBackground)
        layout.addView(loadingSpinner)

        // Add favorite button
        favoriteButton = ImageButton(this)
        favoriteButton.setImageResource(android.R.drawable.btn_star_big_off)
        favoriteButton.setBackgroundResource(android.R.drawable.btn_default)

        val favoriteParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        favoriteParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        favoriteParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        favoriteParams.setMargins(0, 16, 16, 0) // left, top, right, bottom
        favoriteButton.layoutParams = favoriteParams

        favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        layout.addView(favoriteButton)

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
                val spl = tag.split(" ".toRegex(), limit = 2).toTypedArray()
                val by = spl[0].toByte()
                val c: Char = if (spl.size > 1) spl[1][0] else Char.MIN_VALUE
                if (motionEvent.action == MotionEvent.ACTION_DOWN) keydown(by, c)
                if (motionEvent.action == MotionEvent.ACTION_UP) keyup(by, c)
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

        // Set navigation bar transparency for better immersive experience
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
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

        // For devices with display cutouts, ensure content renders properly
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.onApplyWindowInsets(insets)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load history data
        SwfHistoryManager.loadHistory(this)
        SwfFavoritesManager.loadFavorites(this)

        // Get intent data and log it for debugging
        swfUri = intent.data
        Log.d(
            "PlayerActivity",
            "onCreate: intent.data = ${intent.data}, intent.dataString = ${intent.dataString}"
        )

        if (swfUri != null) {
            Log.d("PlayerActivity", "Found SWF URI in intent: $swfUri (scheme: ${swfUri!!.scheme})")
            isFavorite = SwfFavoritesManager.isFavorite(swfUri.toString())
        } else {
            Log.e("PlayerActivity", "No SWF URI found in intent!")
        }

        // Set a fallback timer to hide the loading spinner after a timeout
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isContentLoaded) {
                Log.d("PlayerActivity", "Fallback timer: hiding loading spinner after timeout")
                hideLoadingSpinner()
            }
        }, 10000) // 10 seconds timeout

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

    // New method to be called from Rust when content is ready
    @JvmName("onContentReady")
    @Suppress("unused")
    // Used by Rust
    public fun onContentReady() {
        Log.d("PlayerActivity", "onContentReady() called from native code")
        runOnUiThread {
            isContentLoaded = true
            hideLoadingSpinner()
        }
    }

    private fun hideLoadingSpinner() {
        // Update favorite button with current status
        if (swfUri != null) {
            isFavorite = SwfFavoritesManager.isFavorite(swfUri.toString())
            updateFavoriteButton()
        }

        // Find and remove the overlay background too
        val parent = loadingSpinner.parent as? ViewGroup
        parent?.let {
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                if (child is View && child.background != null && child.alpha < 1.0f) {
                    Log.d("PlayerActivity", "Hiding overlay background")
                    child.visibility = View.GONE
                }
            }
        }

        Log.d("PlayerActivity", "Hiding loading spinner")
        loadingSpinner.visibility = View.GONE
    }

    private fun toggleFavorite() {
        val uri = swfUri ?: return
        val displayName = getDisplayNameFromUri(uri) ?: uri.lastPathSegment ?: "Unknown SWF"

        isFavorite = if (SwfFavoritesManager.isFavorite(uri.toString())) {
            // Remove from favorites
            SwfFavoritesManager.removeFromFavorites(this, uri.toString())
            false
        } else {
            // Add to favorites
            SwfFavoritesManager.addToFavorites(this, uri, displayName)
            true
        }

        // Update button appearance
        updateFavoriteButton()
    }

    private fun updateFavoriteButton() {
        val iconResource = if (isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        favoriteButton.setImageResource(iconResource)
    }

    companion object {
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

    // Record SWF file access in history
    private fun addToHistory(uri: Uri) {
        val displayName = getDisplayNameFromUri(uri) ?: uri.lastPathSegment ?: "Unknown SWF"
        SwfHistoryManager.addToHistory(this, uri, displayName)
    }

    // Get a friendly display name for the SWF file
    private fun getDisplayNameFromUri(uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
        }
        return uri.lastPathSegment
    }
}
