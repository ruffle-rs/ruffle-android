package rs.ruffle

import android.util.Patterns

class UrlState(val url: String? = null) :
    TextFieldState(validator = ::isUrlValid, errorFor = ::urlValidationError) {
    init {
        url?.let {
            text = it
        }
    }
}

private fun urlValidationError(url: String): String = "Invalid url: $url"

private fun isUrlValid(url: String): Boolean = Patterns.WEB_URL.matcher(url).matches()

val UrlStateSaver = textFieldStateSaver(UrlState())
