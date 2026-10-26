package rs.ruffle

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Direct access to the files in shared storage, which lets a SWF opened by itself
 * load the files next to it, as it can on desktop.
 */
object StorageAccess {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents"
    private const val MEDIA_AUTHORITY = "com.android.providers.media.documents"

    /** Whether Ruffle may read any file in shared storage. */
    fun isGranted(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** The file on disk that [uri] refers to, or `null` if it can't be found out. */
    fun fileFor(context: Context, uri: Uri): File? = try {
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path?.let(::File)

            ContentResolver.SCHEME_CONTENT -> documentFile(context, uri)
                ?: dataColumnFile(context, uri)

            else -> null
        }
    } catch (e: Exception) {
        Log.w("ruffle", "Couldn't find the file for $uri", e)
        null
    }

    /** The file of a document from one of the system's own document providers. */
    private fun documentFile(context: Context, uri: Uri): File? {
        if (!DocumentsContract.isDocumentUri(context, uri)) {
            return null
        }
        val id = DocumentsContract.getDocumentId(uri)
        return when (uri.authority) {
            // "primary:Download/game/game.swf", or "1234-ABCD:game/game.swf" on an SD card
            EXTERNAL_STORAGE_AUTHORITY -> {
                val volume = id.substringBefore(':', missingDelimiterValue = "")
                val path = id.substringAfter(':')
                when {
                    volume.isEmpty() -> null

                    volume.equals("primary", ignoreCase = true) ->
                        File(Environment.getExternalStorageDirectory(), path)

                    else -> File(File("/storage", volume), path)
                }
            }

            // "raw:/storage/emulated/0/Download/game.swf", or "msf:1234" for a MediaStore ID
            DOWNLOADS_AUTHORITY -> when {
                id.startsWith("raw:") -> File(id.removePrefix("raw:"))

                id.startsWith("msf:") -> id.removePrefix("msf:").toLongOrNull()?.let {
                    mediaStoreFile(context, it)
                }

                else -> null
            }

            // "document:1234", "image:1234", ...
            MEDIA_AUTHORITY -> id.substringAfter(':').toLongOrNull()?.let {
                mediaStoreFile(context, it)
            }

            else -> null
        }
    }

    private fun mediaStoreFile(context: Context, id: Long): File? = dataColumnFile(
        context,
        MediaStore.Files.getContentUri("external"),
        "${BaseColumns._ID}=?",
        arrayOf(id.toString())
    )

    /** Many providers, including file manager apps, give the file's path in the `_data` column. */
    @Suppress("DEPRECATION")
    private fun dataColumnFile(
        context: Context,
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): File? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)?.let(::File)
            }
        }
        return null
    }
}
