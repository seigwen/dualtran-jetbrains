package dev.seigwen.dualtran

import com.google.gson.Gson
import java.text.Collator
import java.util.Locale

/**
 * A provider/model snapshot generated from models.dev, plus the curated built-in registry.
 *
 * A provider is either `built-in` (registered here with a known endpoint) or `models.dev`
 * (loaded from the generated catalogue). Both are merged into one alphabetical list; the
 * custom provider stays last. Models may carry the provider's declared reasoning options.
 */
object DualTranCatalog {

    data class Model(
        val id: String = "",
        val label: String = "",
        /** Values declared by the model, e.g. ["none", "minimal", "low"]. Empty means no parameter. */
        val reasoningOptions: List<String>? = null,
    )

    data class Provider(
        val id: String = "",
        val label: String = "",
        val endpoint: String = "",
        /** "built-in" | "models.dev" | "custom". */
        val source: String? = null,
        val models: List<Model>? = null,
    ) {
        val effectiveSource: String
            get() = source?.takeIf { it.isNotBlank() } ?: "models.dev"

        val modelList: List<Model>
            get() = models.orEmpty().filter { it.id.isNotBlank() }
    }

    data class Catalog(val providers: List<Provider> = emptyList())

    private val gson = Gson()
    private val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }

    /** Approximate Intl.Collator("en", {numeric:true, sensitivity:"base"}) for the payload gate. */
    private fun naturalCompare(left: String, right: String): Int {
        val leftTokens = Regex("\\d+|\\D+").findAll(left.lowercase()).map { it.value }.toList()
        val rightTokens = Regex("\\d+|\\D+").findAll(right.lowercase()).map { it.value }.toList()
        var index = 0
        while (index < leftTokens.size && index < rightTokens.size) {
            val a = leftTokens[index]
            val b = rightTokens[index]
            val bothNumeric = a.all(Char::isDigit) && b.all(Char::isDigit)
            val result = if (bothNumeric) {
                a.toLongOrNull()!!.compareTo(b.toLongOrNull()!!)
            } else {
                collator.compare(a, b)
            }
            if (result != 0) return result
            index += 1
        }
        return leftTokens.size.compareTo(rightTokens.size)
    }

    const val CUSTOM_ID = "_custom_jetbrains"

    val customProvider = Provider(
        id = CUSTOM_ID,
        label = "Custom…",
        endpoint = "",
        source = "custom",
        models = listOf(
            Model("gpt-4o-mini", "GPT-4o mini"),
            Model("gpt-4.1", "GPT-4.1"),
            Model("deepseek-chat", "DeepSeek Chat"),
            Model("qwen3", "Qwen3"),
        ),
    )

    val providers: List<Provider> by lazy {
        val builtIns = load("/builtin-providers.json").map { provider ->
            provider.copy(
                source = provider.effectiveSource,
                models = provider.modelList.sortedWith { left, right ->
                    naturalCompare(left.label, right.label)
                },
            )
        }
        val builtInIds = builtIns.map { it.id }.toSet()
        val catalogue = load("/catalog.json")
            .filter { it.id !in builtInIds && it.id != CUSTOM_ID }
            .map { provider ->
                provider.copy(
                    source = "models.dev",
                    models = provider.modelList.sortedWith { left, right ->
                        naturalCompare(left.label, right.label)
                    },
                )
            }
        (builtIns + catalogue + customProvider).sortedWith { left, right ->
            when {
                left.id == CUSTOM_ID -> 1
                right.id == CUSTOM_ID -> -1
                else -> naturalCompare(left.label, right.label)
            }
        }
    }

    private fun load(resource: String): List<Provider> {
        val stream = DualTranCatalog::class.java.getResourceAsStream(resource) ?: return emptyList()
        return stream.bufferedReader().use { reader ->
            gson.fromJson(reader, Catalog::class.java)?.providers.orEmpty()
        }
    }

    fun provider(id: String): Provider? = providers.firstOrNull { it.id == id }

    fun model(providerId: String, modelId: String): Model? =
        provider(providerId)?.modelList?.firstOrNull { it.id == modelId }

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
}
