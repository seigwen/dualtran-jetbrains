package dev.seigwen.dualtran

import com.google.gson.Gson
import java.text.Collator
import java.util.Locale

/** A provider/model snapshot generated from models.dev; shipped as a resource so the form works offline. */
object DualTranCatalog {

    data class Model(val id: String = "", val label: String = "")
    data class Provider(
        val id: String = "",
        val label: String = "",
        val endpoint: String = "",
        val models: List<Model> = emptyList(),
    )
    data class Catalog(val providers: List<Provider> = emptyList())

    private val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }

    val providers: List<Provider> by lazy {
        val stream = DualTranCatalog::class.java.getResourceAsStream("/catalog.json")
            ?: return@lazy listOf(customProvider)
        stream.bufferedReader().use { reader ->
            val loaded = Gson().fromJson(reader, Catalog::class.java).providers
            (loaded.filter { it.id != CUSTOM_ID }.sortedWith { left, right ->
                collator.compare(left.label, right.label)
            } + customProvider)
        }
    }

    const val CUSTOM_ID = "_custom_jetbrains"

    val customProvider = Provider(
        id = CUSTOM_ID,
        label = "Custom…",
        endpoint = "",
        models = listOf(
            Model("gpt-4o-mini", "GPT-4o mini"),
            Model("gpt-4.1", "GPT-4.1"),
            Model("deepseek-chat", "DeepSeek Chat"),
            Model("qwen3", "Qwen3"),
        ),
    )

    data class Language(val code: String, val label: String)

    val languages: List<Language> = listOf(
        Language("en", "English (en)"),
        Language("zh-CN", "Chinese (Simplified) (zh-CN)"),
        Language("zh-TW", "Chinese (Traditional) (zh-TW)"),
        Language("ja", "Japanese (ja)"),
        Language("ko", "Korean (ko)"),
        Language("es", "Spanish (es)"),
        Language("fr", "French (fr)"),
        Language("de", "German (de)"),
        Language("it", "Italian (it)"),
        Language("pt", "Portuguese (pt)"),
        Language("pt-BR", "Portuguese (Brazil) (pt-BR)"),
        Language("ru", "Russian (ru)"),
        Language("ar", "Arabic (ar)"),
        Language("hi", "Hindi (hi)"),
        Language("bn", "Bengali (bn)"),
        Language("ur", "Urdu (ur)"),
        Language("fa", "Persian (fa)"),
        Language("tr", "Turkish (tr)"),
        Language("pl", "Polish (pl)"),
        Language("nl", "Dutch (nl)"),
        Language("sv", "Swedish (sv)"),
        Language("no", "Norwegian (no)"),
        Language("da", "Danish (da)"),
        Language("fi", "Finnish (fi)"),
        Language("cs", "Czech (cs)"),
        Language("sk", "Slovak (sk)"),
        Language("hu", "Hungarian (hu)"),
        Language("ro", "Romanian (ro)"),
        Language("bg", "Bulgarian (bg)"),
        Language("el", "Greek (el)"),
        Language("he", "Hebrew (he)"),
        Language("th", "Thai (th)"),
        Language("vi", "Vietnamese (vi)"),
        Language("id", "Indonesian (id)"),
        Language("ms", "Malay (ms)"),
        Language("uk", "Ukrainian (uk)"),
        Language("sr", "Serbian (sr)"),
        Language("hr", "Croatian (hr)"),
        Language("sl", "Slovenian (sl)"),
        Language("lt", "Lithuanian (lt)"),
        Language("lv", "Latvian (lv)"),
        Language("et", "Estonian (et)"),
        Language("sw", "Swahili (sw)"),
        Language("tl", "Filipino (tl)"),
    )

    fun provider(id: String): Provider? = providers.firstOrNull { it.id == id }
}
