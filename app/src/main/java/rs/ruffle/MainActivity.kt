package rs.ruffle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import rs.ruffle.ui.theme.RuffleTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Verify demos directory exists in assets or cache
        verifyDemosDirectory()
        
        // Load SWF history and favorites data
        SwfHistoryManager.loadHistory(this)
        SwfFavoritesManager.loadFavorites(this)
        
        // Load bundled demo SWF files
        DemosManager.loadDemos(this)

        setContent {
            RuffleTheme {
                RuffleNavHost(openSwf = { openSwf(it) })
            }
        }
    }

    private fun verifyDemosDirectory() {
        try {
            // Check if demos directory exists
            val demoFiles = assets.list("demos")
            
            if (demoFiles == null || demoFiles.isEmpty()) {
                Log.w("MainActivity", "No demos directory or it's empty. Creating cache directory for demos.")
                
                // Create a demos directory in the cache folder so we can still show the demos UI section
                val demosDir = File(cacheDir, "demos")
                if (!demosDir.exists()) {
                    demosDir.mkdirs()
                }
                
                // Inform the user about missing demos
                Toast.makeText(
                    this, 
                    "No demo Flash files found. The demo section will be empty.", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Log.d("MainActivity", "Found ${demoFiles.size} files in demos directory")
                
                // Verify that at least one .swf file exists
                var hasSWF = false
                for (file in demoFiles) {
                    if (file.endsWith(".swf", ignoreCase = true)) {
                        try {
                            assets.open("demos/$file").close()
                            hasSWF = true
                            break
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error verifying demo file demos/$file", e)
                        }
                    }
                }
                
                if (!hasSWF) {
                    Log.w("MainActivity", "No .swf files found in demos directory")
                    
                    // Inform the user about no SWF files
                    Toast.makeText(
                        this, 
                        "Demo section will not work: no valid Flash files found.", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.d("MainActivity", "Found SWF files in demos directory - demo section will work")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error verifying demos directory", e)
            
            // Inform user about the error
            Toast.makeText(
                this, 
                "Error checking for demo files: ${e.localizedMessage}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openSwf(uri: Uri) {
        val intent = Intent(
            this@MainActivity,
            PlayerActivity::class.java
        ).apply {
            data = uri
        }
        startActivity(intent)
    }
}
