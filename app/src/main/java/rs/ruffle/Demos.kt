package rs.ruffle

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri

/** A demo SWF, hosted for the web demo on ruffle.rs. */
data class Demo(val url: String, val title: String, val author: String, @StringRes val kind: Int) {
    val uri: Uri
        get() = url.toUri()
}

object Demos {
    val all = listOf(
        Demo(
            "https://ruffle.rs/demo/swfs/alien_hominid.swf",
            "Alien Hominid",
            "Tom Fulp and Dan Paladin",
            R.string.game_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/flyguy.swf",
            "FlyGuy",
            "Trevor van Meter",
            R.string.game_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/marvin_spectrum.swf",
            "Marvin Spectrum",
            "Bryan Singh",
            R.string.game_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/wasted_sky.swf",
            "Wasted Sky",
            "Tom Fulp",
            R.string.game_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/bitey1.swf",
            "Bitey of Brackenwood",
            "Adam Phillips",
            R.string.animation_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/saturday_morning_watchmen.swf",
            "Saturday Morning Watchmen",
            "Harry Partridge",
            R.string.animation_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/synj1.swf",
            "Synj vs. Horrid Part 1",
            "Dan Paladin",
            R.string.animation_by
        ),
        Demo(
            "https://ruffle.rs/demo/swfs/synj2.swf",
            "Synj vs. Horrid Part 2",
            "Dan Paladin",
            R.string.animation_by
        )
    )

    fun find(uri: Uri): Demo? = all.find { it.uri == uri }
}
