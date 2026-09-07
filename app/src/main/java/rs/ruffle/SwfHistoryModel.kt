package rs.ruffle

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SwfHistoryEntry(
    val uri: String,
    val displayName: String,
    val timestamp: Long
) {
    constructor(uri: Uri, displayName: String) : this(
        uri.toString(),
        displayName,
        System.currentTimeMillis()
    )
}

/**
 * Manages history of accessed SWF files
 */
object SwfHistoryManager {
    private const val MAX_HISTORY_ENTRIES = 10
    private const val HISTORY_FILE_NAME = "swf_history.json"
    private val historyEntries = mutableStateListOf<SwfHistoryEntry>()
    private val json = Json { prettyPrint = true }

    fun getHistoryEntries(): List<SwfHistoryEntry> = historyEntries

    fun addToHistory(context: Context, uri: Uri, displayName: String) {
        // Remove existing entry with the same URI if present
        historyEntries.removeAll { it.uri == uri.toString() }

        // Add new entry at the beginning
        historyEntries.add(0, SwfHistoryEntry(uri, displayName))

        // Trim list if too long
        while (historyEntries.size > MAX_HISTORY_ENTRIES) {
            historyEntries.removeAt(historyEntries.size - 1)
        }

        // Save to storage
        saveHistory(context)
    }

    fun loadHistory(context: Context) {
        try {
            val file = File(context.filesDir, HISTORY_FILE_NAME)
            if (file.exists()) {
                val jsonString = file.readText()
                val loadedEntries = json.decodeFromString<List<SwfHistoryEntry>>(jsonString)

                historyEntries.clear()
                historyEntries.addAll(loadedEntries)
            }
        } catch (e: Exception) {
            Log.e("SwfHistoryManager", "Error loading history", e)
        }
    }

    private fun saveHistory(context: Context) {
        try {
            val file = File(context.filesDir, HISTORY_FILE_NAME)
            val jsonString = json.encodeToString(historyEntries)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e("SwfHistoryManager", "Error saving history", e)
        }
    }

    fun clearHistory(context: Context) {
        historyEntries.clear()
        saveHistory(context)
    }
}
