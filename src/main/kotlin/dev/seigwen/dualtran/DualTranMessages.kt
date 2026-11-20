package dev.seigwen.dualtran

import java.util.Locale
import java.util.ResourceBundle

/**
 * All user-visible chrome text goes through this bundle. Provider/model/language names are data,
 * not UI chrome, so they stay in the catalogue.
 */
object DualTranMessages {

    private val bundle: ResourceBundle by lazy {
        ResourceBundle.getBundle(
            "messages.DualTranBundle",
            Locale.getDefault(),
            DualTranMessages::class.java.classLoader,
        )
    }

    fun text(key: String, fallback: String = key): String =
        try {
            bundle.getString(key).takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
}
