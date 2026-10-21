package rs.ruffle

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
                RuffleNavHost(openSwf = { openSwf(it) }, openFolder = { openFolder(it) })
            }
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

    private fun openFolder(folder: Uri) {
        val folderAccess = FolderAccess(contentResolver, folder)
        val swfs = folderAccess.listSwfFiles()
        when (swfs.size) {
            0 -> Toast.makeText(this, R.string.no_swf_in_folder, Toast.LENGTH_LONG).show()

            1 -> openSwfInFolder(folder, folderAccess, swfs[0])

            else -> AlertDialog.Builder(this)
                .setTitle(R.string.choose_a_swf)
                .setItems(swfs.map { it.name }.toTypedArray()) { _, which ->
                    openSwfInFolder(folder, folderAccess, swfs[which])
                }
                .show()
        }
    }

    private fun openSwfInFolder(folder: Uri, folderAccess: FolderAccess, swf: FolderAccess.Entry) {
        val intent = Intent(
            this@MainActivity,
            PlayerActivity::class.java
        ).apply {
            data = folderAccess.uriOf(swf)
            putExtra(PlayerActivity.EXTRA_FOLDER, folder.toString())
            putExtra(PlayerActivity.EXTRA_FOLDER_SWF_PATH, swf.name)
        }
        startActivity(intent)
    }
}
