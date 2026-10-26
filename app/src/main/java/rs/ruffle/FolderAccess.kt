package rs.ruffle

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import java.io.File

/**
 * Reads files from a folder the user picked, so that a SWF in it can load the
 * files next to it (like `parts/level1.swf`), as it can on desktop.
 *
 * The folder is either a document tree from the system folder picker, or a
 * `file://` directory. Paths are '/'-separated and relative to the folder.
 * Names that don't match exactly are matched ignoring case, since many SWFs
 * were made on Windows, where file names are case-insensitive.
 */
class FolderAccess(private val contentResolver: ContentResolver, private val folder: Uri) {
    /** A file or directory in the folder. [id] is a document ID or an absolute path. */
    data class Entry(val name: String, val id: String, val isDirectory: Boolean)

    private val isFileFolder = folder.scheme == ContentResolver.SCHEME_FILE

    private val rootId: String =
        if (isFileFolder) {
            folder.path ?: "/"
        } else {
            DocumentsContract.getTreeDocumentId(folder)
        }

    /** Directory listings by ID, since a movie often loads many files from one directory. */
    private val listings = HashMap<String, List<Entry>>()

    /** The SWF files directly inside the folder, sorted by name. */
    fun listSwfFiles(): List<Entry> = list(rootId)
        .filter { !it.isDirectory && it.name.endsWith(".swf", ignoreCase = true) }
        .sortedBy { it.name.lowercase() }

    /** The URI to open the given file with. */
    fun uriOf(entry: Entry): Uri = if (isFileFolder) {
        Uri.fromFile(File(entry.id))
    } else {
        DocumentsContract.buildDocumentUriUsingTree(folder, entry.id)
    }

    /** Opens the file at [path] for reading, or returns `null` if there is none. */
    fun openFile(path: String): ParcelFileDescriptor? {
        val entry = find(path) ?: return null
        if (entry.isDirectory) {
            return null
        }
        return if (isFileFolder) {
            ParcelFileDescriptor.open(File(entry.id), ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            contentResolver.openFileDescriptor(uriOf(entry), "r")
        }
    }

    private fun find(path: String): Entry? {
        var current = Entry("", rootId, true)
        for (segment in path.split('/')) {
            if (segment.isEmpty() || segment == ".") {
                continue
            }
            if (!current.isDirectory) {
                return null
            }
            val children = list(current.id)
            current = children.firstOrNull { it.name == segment }
                ?: children.firstOrNull { it.name.equals(segment, ignoreCase = true) }
                ?: return null
        }
        return current
    }

    private fun list(id: String): List<Entry> = listings.getOrPut(id) {
        if (isFileFolder) {
            File(id).listFiles().orEmpty().map { Entry(it.name, it.path, it.isDirectory) }
        } else {
            listDocuments(id)
        }
    }

    private fun listDocuments(parentId: String): List<Entry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val entries = ArrayList<Entry>()
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(2)
                entries.add(
                    Entry(
                        name = cursor.getString(1),
                        id = cursor.getString(0),
                        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    )
                )
            }
        }
        return entries
    }
}
