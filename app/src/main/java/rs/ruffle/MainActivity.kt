package rs.ruffle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        val intent = Intent(
            this@MainActivity,
            FullscreenNativeActivity::class.java
        ).apply {
            data = uri
        }
        startActivity(intent)
    }
}
