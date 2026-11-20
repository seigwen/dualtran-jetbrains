package dev.seigwen.dualtran

/**
 * Pure translation helpers.
 *
 * No IntelliJ classes here on purpose: the whole file is exercised by plain JUnit tests, so the
 * plugin's fragile part (endpoint handling and response parsing) is covered without booting an IDE.
 */
object DualTranCore {

    data class Settings(
        val engine: String = "google",
        val target: String = "zh-CN",
        val endpoint: String = "",
        val apiKey: String = "",
        val model: String = "",
        val provider: String = "",
        val reasoning: String = "lowest",
    )

    const val MAX_QUERY = 1800

    fun normalizeEndpoint(endpoint: String): String {
        val trimmed = endpoint.trim().trimEnd('/')
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "Endpoint must start with http:// or https:// (got \"$endpoint\")"
        }
        return trimmed
    }

    fun chatCompletionsUrl(endpoint: String): String {
        val base = normalizeEndpoint(endpoint)
        return when {
            base.endsWith("/chat/completions", ignoreCase = true) -> base
            Regex("/v\\d+$").containsMatchIn(base) -> "$base/chat/completions"
            else -> "$base/v1/chat/completions"
        }
    }

    private fun escapeJson(value: String): String {
        val out = StringBuilder(value.length + 16)
        for (character in value) {
            when (character) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (character < ' ') out.append(String.format("\\u%04x", character.code)) else out.append(character)
            }
        }
        return out.toString()
    }

    /**
     * Resolve the persisted `lowest` sentinel against the selected model's declared options.
     * Returns "" when nothing should be sent; explicit values pass through unchanged.
     */
    fun resolveReasoning(setting: String, options: List<String>): String {
        if (setting != "lowest") return setting
        for (candidate in listOf("none", "minimal", "low")) {
            if (candidate in options) return candidate
        }
        return ""
    }

    fun buildChatPayload(model: String, text: String, target: String, reasoning: String = "lowest"): String {
        val system = "You translate into $target. Return only the translation, no commentary and " +
            "no quotes. Keep names, numbers and inline code exactly as they appear. " +
            "If the text is already in $target, return it unchanged."
        val effort = if (reasoning.isNotBlank() && reasoning != "lowest") {
            ",\"reasoning_effort\":\"${escapeJson(reasoning)}\""
        } else {
            ""
        }
        return """{"model":"${escapeJson(model)}","temperature":0,"stream":false$effort,"messages":[""" +
            """{"role":"system","content":"${escapeJson(system)}"},""" +
            """{"role":"user","content":"${escapeJson(text)}"}]}"""
    }

    /** Minimal extractor for the `content` field of the first choice; enough for a chat reply. */
    fun parseChatResponse(body: String): String {
        val errorMatch = Regex("\"error\"\\s*:\\s*\\{[^}]*\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(body)
        if (errorMatch != null) throw IllegalStateException(unescape(errorMatch.groupValues[1]))

        val contentMatch = Regex("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(body)
            ?: throw IllegalStateException("The provider returned an unexpected payload")
        val content = unescape(contentMatch.groupValues[1]).trim()
        if (content.isEmpty()) throw IllegalStateException("The provider returned an empty translation")
        return content
    }

    fun googleUrl(text: String, target: String, source: String = "auto"): String {
        val query = buildString {
            append("client=gtx&sl=").append(encode(source))
            append("&tl=").append(encode(target))
            append("&dt=t&q=").append(encode(text))
        }
        return "https://translate.googleapis.com/translate_a/single?$query"
    }

    /** Google answers with nested arrays; every sentence piece sits at `[0][i][0]`. */
    fun parseGoogleResponse(body: String): String {
        val text = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(body)
            .map { unescape(it.groupValues[1]) }
            .filter { it.isNotBlank() && !it.startsWith("en") && !it.startsWith("zh") }
            .joinToString("")
            .trim()
        if (text.isEmpty()) throw IllegalStateException("Google Translate returned an empty translation")
        return text
    }

    fun chunkText(text: String, limit: Int = MAX_QUERY): List<String> {
        val clean = text.replace("\r\n", "\n").trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= limit) return listOf(clean)

        val parts = mutableListOf<String>()
        var current = ""
        for (paragraph in clean.split(Regex("\n{2,}"))) {
            if ((current + "\n\n" + paragraph).trim().length <= limit) {
                current = if (current.isEmpty()) paragraph else "$current\n\n$paragraph"
                continue
            }
            if (current.isNotEmpty()) parts.add(current)
            if (paragraph.length <= limit) {
                current = paragraph
            } else {
                var index = 0
                while (index < paragraph.length) {
                    parts.add(paragraph.substring(index, minOf(index + limit, paragraph.length)))
                    index += limit
                }
                current = ""
            }
        }
        if (current.isNotEmpty()) parts.add(current)
        return parts
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character != '\\' || index == value.length - 1) {
                out.append(character)
                index += 1
                continue
            }
            when (val next = value[index + 1]) {
                'n' -> { out.append('\n'); index += 2 }
                'r' -> { out.append('\r'); index += 2 }
                't' -> { out.append('\t'); index += 2 }
                '"' -> { out.append('"'); index += 2 }
                '\\' -> { out.append('\\'); index += 2 }
                'u' -> {
                    val hex = value.substring(index + 2, minOf(index + 6, value.length))
                    out.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                    index += 6
                }
                else -> { out.append(next); index += 2 }
            }
        }
        return out.toString()
    }
}
