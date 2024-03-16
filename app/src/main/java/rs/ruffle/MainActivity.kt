package rs.ruffle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.io.IOException
import rs.ruffle.ui.theme.RuffleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            RuffleTheme {
                RuffleNavHost(openSwf = { openSwf(it) })
            }
        }
    }

    private fun openSwf(uri: Uri) {
        val resolver = contentResolver
        try {
            val inputStream = resolver.openInputStream(uri)
            inputStream.use {
                val available = inputStream!!.available()
                val bytes = ByteArray(available)
                // assuming the whole contents will be available at once
                inputStream.read(bytes)
                FullscreenNativeActivity.SWF_BYTES = bytes
            }
        } catch (_: IOException) {
        }

        val intent = Intent(
            this@MainActivity,
            FullscreenNativeActivity::class.java
        )
        startActivity(intent)
    }
}
