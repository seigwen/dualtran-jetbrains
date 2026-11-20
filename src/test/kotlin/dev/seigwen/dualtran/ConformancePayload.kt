package dev.seigwen.dualtran

import com.google.gson.GsonBuilder
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Generates the SPEC 2.1 payload from the real Kotlin catalogue, settings defaults, and
 * message resources. Invoked by `scripts/conformance/payload.sh`; the same data object is
 * what the settings form renders and what the validator sees.
 */
object ConformancePayload {

    private val localeCodes = listOf(
        "af", "ar", "ca", "cs", "da", "de", "el", "en", "es", "fa", "fi", "fr",
        "he", "hi", "hr", "hu", "is", "it", "ja", "ko", "lv", "nl", "no", "pl",
        "pt_BR", "pt_PT", "ro", "ru", "sl", "sr", "sv", "th", "tr", "ug", "uk",
        "vi", "zh_CN", "zh_TW",
    )

    private val baseKeys = listOf(
        "settingsTitle", "engine", "targetLanguage", "provider", "endpoint",
        "apiKey", "model", "reasoning", "selectLanguage", "selectProvider",
        "selectModel", "reasoningLowest", "reasoningNone", "reasoningMinimal",
        "reasoningLow", "reasoningMedium", "reasoningHigh", "reasoningXhigh",
        "reasoningMax", "translateAction", "translateDescription",
        "translationFailed", "providerRequired", "invalidEndpoint", "networkError",
        "apiKeyMissing",
    )

    private val reasoningOrder = listOf("none", "minimal", "low", "medium", "high", "xhigh", "max")

    fun build(): Map<String, Any> {
        val providers = DualTranCatalog.providers
        val selectedProvider = providers.firstOrNull { it.effectiveSource == "built-in" && it.modelList.isNotEmpty() }
            ?: providers.first { it.modelList.isNotEmpty() }
        val selectedModel = selectedProvider.modelList.firstOrNull { it.reasoningOptions.orEmpty().isNotEmpty() }
            ?: selectedProvider.modelList.first()

        val languageOptions = DualTranCatalog.languages.map { linkedMapOf("value" to it.code, "label" to it.label) }
        val providerOptions = providers.map {
            linkedMapOf(
                "value" to it.id,
                "label" to it.label,
                "source" to it.effectiveSource,
            )
        }
        val modelOptions = selectedProvider.modelList.map { modelOption(it) }
        val byProvider = linkedMapOf<String, Any>()
        providers.forEach { provider ->
            byProvider[provider.id] = provider.modelList.map { modelOption(it) }
        }

        return linkedMapOf(
            "spec" to "2.1.0",
            "host" to "dualtran-jetbrains",
            "language" to linkedMapOf(
                "control" to "dropdown",
                "current" to "zh-CN",
                "options" to languageOptions,
            ),
            "provider" to linkedMapOf(
                "control" to "dropdown",
                "current" to selectedProvider.id,
                "placeholder" to linkedMapOf(
                    "value" to "",
                    "label" to "Select a provider",
                    "disabled" to true,
                    "selected" to false,
                ),
                "fallback" to "",
                "options" to providerOptions,
            ),
            "model" to linkedMapOf(
                "control" to "dropdown",
                "disabled" to false,
                "selectedProvider" to selectedProvider.id,
                "selectedModel" to selectedModel.id,
                "options" to modelOptions,
                "byProvider" to byProvider,
            ),
            "endpoint" to linkedMapOf(
                "control" to "text",
                "current" to selectedProvider.endpoint,
                "placeholder" to "https://",
                "autofill" to linkedMapOf(
                    "provider" to selectedProvider.id,
                    "providerApi" to selectedProvider.endpoint,
                    "before" to "",
                    "after" to selectedProvider.endpoint,
                    "userEdited" to "https://proxy.example/v1",
                    "afterSwitch" to "https://proxy.example/v1",
                ),
            ),
            "reasoning" to linkedMapOf(
                "control" to "dropdown",
                "disabled" to false,
                "default" to "lowest",
                "resolved" to linkedMapOf(
                    "none" to listOf(resolvedCase(listOf("none", "minimal", "low"), "none")),
                    "minimal" to listOf(resolvedCase(listOf("minimal", "low"), "minimal")),
                    "low" to listOf(resolvedCase(listOf("low", "medium"), "low")),
                    "none-declared" to listOf(resolvedCase(emptyList(), "")),
                ),
            ),
            "i18n" to linkedMapOf(
                "locales" to localeCodes,
                "keys" to baseKeys,
                "sourceLocale" to "en",
                "files" to localeCodes.associateWith { readMessages(it) },
                "used" to baseKeys,
            ),
        )
    }

    private fun modelOption(model: DualTranCatalog.Model): Map<String, Any> = linkedMapOf(
        "value" to model.id,
        "label" to model.label,
        "reasoningOptions" to model.reasoningOptions.orEmpty(),
    )

    private fun resolvedCase(options: List<String>, value: String): Map<String, Any> = linkedMapOf(
        "setting" to "lowest",
        "options" to options,
        "value" to value,
    )

    private fun readMessages(locale: String): Map<String, String> {
        val resource = "/messages/DualTranBundle_$locale.properties"
        val stream = ConformancePayload::class.java.getResourceAsStream(resource)
            ?: ConformancePayload::class.java.getResourceAsStream("/messages/DualTranBundle.properties")
            ?: error("message bundle missing for $locale")
        val properties = Properties()
        InputStreamReader(stream, Charsets.UTF_8).use { properties.load(it) }
        return baseKeys.associateWith { properties.getProperty(it, baseFallback(it)) }
    }

    private fun baseFallback(key: String): String = when (key) {
        "settingsTitle" -> "DualTran"
        "engine" -> "Engine"
        "targetLanguage" -> "Target language"
        "provider" -> "AI provider"
        "endpoint" -> "API endpoint"
        "apiKey" -> "API key"
        "model" -> "Model"
        "reasoning" -> "Reasoning effort"
        "selectLanguage" -> "Select a language"
        "selectProvider" -> "Select a provider"
        "selectModel" -> "Select a model"
        "reasoningLowest" -> "Lowest (automatic)"
        "reasoningNone" -> "None"
        "reasoningMinimal" -> "Minimal"
        "reasoningLow" -> "Low"
        "reasoningMedium" -> "Medium"
        "reasoningHigh" -> "High"
        "reasoningXhigh" -> "XHigh"
        "reasoningMax" -> "Max"
        "translateAction" -> "Translate selection (DualTran)"
        "translateDescription" -> "Translate the selected text into the configured language"
        "translationFailed" -> "DualTran failed"
        "providerRequired" -> "Select an AI provider in Settings → Tools → DualTran"
        "invalidEndpoint" -> "Endpoint must start with http:// or https://"
        "networkError" -> "Cannot reach the AI service — check the API endpoint and network"
        "apiKeyMissing" -> "API key is not set"
        else -> key
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val output = Path.of("build", "conformance", "payload.json")
        Files.createDirectories(output.parent)
        Files.writeString(output, GsonBuilder().setPrettyPrinting().create().toJson(build()))
        println(output)
    }
}
