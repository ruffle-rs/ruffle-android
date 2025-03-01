package rs.ruffle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Manages bundled demo SWF files
 */
object DemosManager {
    private val demoEntries = mutableStateListOf<DemoEntry>()
    private const val DEMOS_DIRECTORY = "demos"
    
    data class DemoEntry(
        val name: String,
        val assetPath: String,
        val description: String,
        val category: DemoCategory
    )
    
    enum class DemoCategory {
        ANIMATION,
        GAME
    }
    
    // Map of filename to proper display name and category
    private val demoMetadata = mapOf(
        "bitey1.swf" to Pair("Bitey of Brackenwood", DemoCategory.ANIMATION),
        "saturday_morning_watchmen.swf" to Pair("Saturday Morning Watchmen", DemoCategory.ANIMATION),
        "synj1.swf" to Pair("Synj vs. Horrid Part 1", DemoCategory.ANIMATION),
        "synj2.swf" to Pair("Synj vs. Horrid Part 2", DemoCategory.ANIMATION),
        "alien_hominid.swf" to Pair("Alien Hominid", DemoCategory.GAME),
        "flyguy.swf" to Pair("FlyGuy", DemoCategory.GAME),
        "marvin_spectrum.swf" to Pair("Marvin Spectrum", DemoCategory.GAME),
        "wasted_sky.swf" to Pair("Wasted Sky", DemoCategory.GAME)
    )
    
    /**
     * Load demo files from assets
     */
    fun loadDemos(context: Context) {
        try {
            // Clear previous entries
            demoEntries.clear()
            
            // Check if the demos directory exists
            try {
                // Get list of files in the demos directory
                val demoFiles = context.assets.list(DEMOS_DIRECTORY)
                
                if (demoFiles != null && demoFiles.isNotEmpty()) {
                    Log.d("DemosManager", "Found ${demoFiles.size} files in demos directory")
                    
                    // Add each .swf file as a demo entry
                    for (file in demoFiles) {
                        if (file.endsWith(".swf", ignoreCase = true)) {
                            val path = "$DEMOS_DIRECTORY/$file"
                            
                            // Get proper name and category from metadata or generate default
                            val (displayName, category) = demoMetadata[file] ?: Pair(
                                file.substringBeforeLast(".").replace("_", " ").capitalize(),
                                DemoCategory.ANIMATION // Default category if unknown
                            )
                            
                            // Verify file can be opened
                            try {
                                context.assets.open(path).close()
                                
                                // Create appropriate description based on category
                                val description = when (category) {
                                    DemoCategory.ANIMATION -> "Flash Animation"
                                    DemoCategory.GAME -> "Flash Game"
                                }
                                
                                demoEntries.add(
                                    DemoEntry(
                                        displayName,
                                        path,
                                        description,
                                        category
                                    )
                                )
                                Log.d("DemosManager", "Added demo: $displayName (${category.name}) from $path")
                            } catch (e: Exception) {
                                Log.e("DemosManager", "Error verifying demo file $path", e)
                            }
                        }
                    }
                } else {
                    Log.w("DemosManager", "No files found in demos directory or directory doesn't exist")
                }
            } catch (e: Exception) {
                Log.e("DemosManager", "Error listing demos directory", e)
            }
            
            // If no demos found in assets, log this instead of adding placeholder
            if (demoEntries.isEmpty()) {
                Log.w("DemosManager", "No demo SWF files found in assets")
            }
            
            Log.d("DemosManager", "Loaded ${demoEntries.size} demo entries")
        } catch (e: Exception) {
            Log.e("DemosManager", "Error loading demos", e)
        }
    }
    
    fun getDemoEntries(): List<DemoEntry> = demoEntries
    
    fun getAnimationDemos(): List<DemoEntry> = 
        demoEntries.filter { it.category == DemoCategory.ANIMATION }
    
    fun getGameDemos(): List<DemoEntry> = 
        demoEntries.filter { it.category == DemoCategory.GAME }
    
    /**
     * Extracts a demo SWF file to a temporary file and returns a Uri to it
     */
    fun extractDemoToFile(context: Context, assetPath: String): Uri? {
        try {
            // First check if the asset actually exists
            try {
                // This will throw an exception if the asset doesn't exist
                context.assets.open(assetPath).close()
            } catch (e: Exception) {
                Log.e("DemosManager", "Demo file $assetPath doesn't exist in assets", e)
                return null
            }
            
            val tempFile = File(context.cacheDir, "demos/${File(assetPath).name}")
            if (!tempFile.parentFile?.exists()!!) {
                val dirCreated = tempFile.parentFile?.mkdirs()
                if (dirCreated != true) {
                    Log.e("DemosManager", "Failed to create parent directory for $tempFile")
                    return null
                }
            }
            
            // If the file already exists, delete it to avoid issues
            if (tempFile.exists()) {
                tempFile.delete()
            }
            
            try {
                context.assets.open(assetPath).use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        var totalBytes = 0
                        
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            outputStream.write(buffer, 0, length)
                            totalBytes += length
                        }
                        
                        outputStream.flush()
                        Log.d("DemosManager", "Successfully extracted $assetPath to $tempFile ($totalBytes bytes)")
                    }
                }
                
                // Verify the file was created successfully
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    Log.e("DemosManager", "Extraction completed but file is empty or doesn't exist")
                    return null
                }
                
                // Ensure the file is readable
                if (!tempFile.canRead()) {
                    Log.e("DemosManager", "Extracted file is not readable: $tempFile")
                    return null
                }
                
                // Set read permissions for all
                tempFile.setReadable(true, false)
                
                // Use FileProvider to create a content:// URI instead of file:// URI
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                
                // Grant read permission for the content URI
                context.grantUriPermission(
                    context.packageName, 
                    contentUri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                Log.d("DemosManager", "Created content URI: $contentUri for file: $tempFile")
                return contentUri
            } catch (e: Exception) {
                Log.e("DemosManager", "Error extracting demo file $assetPath", e)
                return null
            }
        } catch (e: Exception) {
            Log.e("DemosManager", "Error creating temp file", e)
            return null
        }
    }

    // Helper function to capitalize first letter of words
    private fun String.capitalize(): String {
        return this.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) word[0].uppercase() + word.substring(1) else ""
        }
    }
} 