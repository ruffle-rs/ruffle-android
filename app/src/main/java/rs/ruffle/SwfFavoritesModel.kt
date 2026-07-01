package rs.ruffle

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents a favorite SWF file entry
 */
@Serializable
data class SwfFavoriteEntry(
    val uri: String,
    val displayName: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor(uri: Uri, displayName: String, notes: String = "") : this(
        uri.toString(),
        displayName,
        notes,
        System.currentTimeMillis()
    )
}

/**
 * Manages favorites of SWF files
 */
object SwfFavoritesManager {
    private const val MAX_FAVORITES = 50
    private const val FAVORITES_FILE_NAME = "swf_favorites.json"
    private val favoriteEntries = mutableStateListOf<SwfFavoriteEntry>()
    private val json = Json { prettyPrint = true }

    fun getFavoriteEntries(): List<SwfFavoriteEntry> = favoriteEntries

    fun addToFavorites(context: Context, uri: Uri, displayName: String, notes: String = "") {
        // If already exists, remove it first (will be re-added)
        favoriteEntries.removeAll { it.uri == uri.toString() }

        // Add new entry
        favoriteEntries.add(0, SwfFavoriteEntry(uri, displayName, notes))

        // Make sure we don't exceed our limit
        while (favoriteEntries.size > MAX_FAVORITES) {
            favoriteEntries.removeAt(favoriteEntries.size - 1)
        }

        // Save to storage
        saveFavorites(context)
    }

    fun removeFromFavorites(context: Context, uri: String) {
        val entryRemoved = favoriteEntries.removeAll { it.uri == uri }
        if (entryRemoved) {
            saveFavorites(context)
        }
    }

    fun isFavorite(uri: String): Boolean {
        return favoriteEntries.any { it.uri == uri }
    }

    fun loadFavorites(context: Context) {
        try {
            val file = File(context.filesDir, FAVORITES_FILE_NAME)
            if (file.exists()) {
                val jsonString = file.readText()
                val loadedEntries = json.decodeFromString<List<SwfFavoriteEntry>>(jsonString)

                favoriteEntries.clear()
                favoriteEntries.addAll(loadedEntries)
            }
        } catch (e: Exception) {
            Log.e("SwfFavoritesManager", "Error loading favorites", e)
        }
    }

    private fun saveFavorites(context: Context) {
        try {
            val file = File(context.filesDir, FAVORITES_FILE_NAME)
            val jsonString = json.encodeToString(favoriteEntries)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e("SwfFavoritesManager", "Error saving favorites", e)
        }
    }

    fun clearFavorites(context: Context) {
        favoriteEntries.clear()
        saveFavorites(context)
    }
}
