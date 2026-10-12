package rs.ruffle

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class SwfEntry(val uri: Uri, val name: String, val time: Long = System.currentTimeMillis())

/**
 * A list of SWFs, the most recently added one first, which is persisted
 * in the shared preferences, and can be observed by composables.
 */
class SwfList(private val key: String, private val maxSize: Int = Int.MAX_VALUE) {
    private val entries = mutableStateListOf<SwfEntry>()
    private var loaded = false

    val items: List<SwfEntry>
        get() = entries

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val json = prefs(context).getString(key, null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entries.add(
                    SwfEntry(
                        obj.getString("uri").toUri(),
                        obj.getString("name"),
                        obj.getLong("time")
                    )
                )
            }
        } catch (e: JSONException) {
            Log.w("ruffle", "Failed to load the $key list", e)
        }
    }

    operator fun contains(uri: Uri): Boolean = entries.any { it.uri == uri }

    fun add(context: Context, entry: SwfEntry) {
        entries.removeAll { it.uri == entry.uri }
        entries.add(0, entry)
        while (entries.size > maxSize) {
            entries.removeAt(entries.lastIndex)
        }
        save(context)
    }

    fun remove(context: Context, uri: Uri) {
        if (entries.removeAll { it.uri == uri }) {
            save(context)
        }
    }

    fun clear(context: Context) {
        entries.clear()
        save(context)
    }

    private fun save(context: Context) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(
                JSONObject()
                    .put("uri", entry.uri.toString())
                    .put("name", entry.name)
                    .put("time", entry.time)
            )
        }
        prefs(context).edit { putString(key, array.toString()) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("swf_lists", Context.MODE_PRIVATE)
}

object SwfLists {
    val history = SwfList("history", maxSize = 10)

    fun load(context: Context) {
        history.load(context)
    }
}
