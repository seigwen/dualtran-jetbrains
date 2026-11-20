package dev.seigwen.dualtran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader
import java.util.Properties

class DualTranCatalogTest {

    @Test
    fun `catalogue contains built-in and models dev sources`() {
        val providers = DualTranCatalog.providers
        assertTrue("no built-in provider", providers.any { it.effectiveSource == "built-in" })
        assertTrue("no models.dev provider", providers.any { it.effectiveSource == "models.dev" })
        assertEquals("custom provider must stay last", DualTranCatalog.CUSTOM_ID, providers.last().id)
        assertTrue("a built-in provider has no models", providers.filter { it.effectiveSource == "built-in" }.all { it.modelList.isNotEmpty() })
    }

    @Test
    fun `built-in models carry reasoning options`() {
        val openai = DualTranCatalog.provider("openai")
        assertNotNull(openai)
        val reasoning = DualTranCatalog.model("openai", "o4-mini")?.reasoningOptions.orEmpty()
        assertTrue(reasoning.containsAll(listOf("none", "minimal", "low")))
    }

    @Test
    fun `reasoning lowest resolves to none then minimal then low`() {
        assertEquals("none", DualTranCore.resolveReasoning("lowest", listOf("none", "minimal", "low")))
        assertEquals("minimal", DualTranCore.resolveReasoning("lowest", listOf("minimal", "low")))
        assertEquals("low", DualTranCore.resolveReasoning("lowest", listOf("low", "medium")))
        assertEquals("", DualTranCore.resolveReasoning("lowest", emptyList()))
        assertEquals("high", DualTranCore.resolveReasoning("high", emptyList()))
    }

    @Test
    fun `all 38 locale bundles have every key and no CJK outside Chinese and Japanese`() {
        val locales = listOf(
            "af", "ar", "ca", "cs", "da", "de", "el", "en", "es", "fa", "fi", "fr",
            "he", "hi", "hr", "hu", "is", "it", "ja", "ko", "lv", "nl", "no", "pl",
            "pt_BR", "pt_PT", "ro", "ru", "sl", "sr", "sv", "th", "tr", "ug", "uk",
            "vi", "zh_CN", "zh_TW",
        )
        val base = load("en")
        assertEquals(38, locales.size)
        locales.forEach { locale ->
            val messages = load(locale)
            base.stringPropertyNames().forEach { key ->
                assertTrue("$locale missing $key", messages.getProperty(key).orEmpty().isNotBlank())
            }
            if (locale !in setOf("zh_CN", "zh_TW", "ja")) {
                val cjk = messages.stringPropertyNames().firstOrNull { key ->
                    messages.getProperty(key).orEmpty().any { it in '\u3400'..'\u9fff' }
                }
                assertEquals("$locale has CJK in $cjk", null, cjk)
            }
        }
    }

    private fun load(locale: String): Properties {
        val resource = if (locale == "en") {
            "/messages/DualTranBundle.properties"
        } else {
            "/messages/DualTranBundle_$locale.properties"
        }
        val stream = javaClass.getResourceAsStream(resource)
            ?: error("missing $resource")
        val properties = Properties()
        InputStreamReader(stream, Charsets.UTF_8).use { properties.load(it) }
        return properties
    }
}
