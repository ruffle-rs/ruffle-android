package rs.ruffle

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import rs.ruffle.ui.theme.RuffleTheme

class MainActivity : ComponentActivity() {
    private val requestReadStorage =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            RuffleTheme {
                RuffleNavHost(openSwf = { openSwf(it) }, openFolder = { openFolder(it) })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!askedForStorageAccess && !StorageAccess.isGranted(this)) {
            askedForStorageAccess = true
            AlertDialog.Builder(this)
                .setTitle(R.string.storage_access_title)
                .setMessage(R.string.storage_access_message)
                .setPositiveButton(R.string.storage_access_allow) { _, _ -> requestStorageAccess() }
                .setNegativeButton(R.string.storage_access_not_now, null)
                .show()
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            requestReadStorage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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

    companion object {
        /** Whether this run of the app asked for storage access, so that it asks only once. */
        private var askedForStorageAccess = false
    }
}
