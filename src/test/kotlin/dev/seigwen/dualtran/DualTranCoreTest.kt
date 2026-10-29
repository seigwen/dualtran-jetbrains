package dev.seigwen.dualtran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DualTranCoreTest {

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a schemeless endpoint`() {
        DualTranCore.normalizeEndpoint("api.openai.com/v1")
    }

    @Test
    fun `completes the chat path`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            DualTranCore.chatCompletionsUrl("https://api.openai.com/v1")
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            DualTranCore.chatCompletionsUrl("https://api.deepseek.com")
        )
        assertEquals(
            "https://x/api/v1/chat/completions",
            DualTranCore.chatCompletionsUrl("https://x/api/v1/chat/completions")
        )
    }

    @Test
    fun `names the target language in the payload`() {
        val payload = DualTranCore.buildChatPayload("gpt-4o-mini", "Hello \"world\"", "zh-CN")
        assertTrue(payload.contains("zh-CN"))
        assertTrue(payload.contains("Hello \\\"world\\\""))
        assertTrue(payload.contains("\"temperature\":0"))
    }

    @Test
    fun `reads the first choice and unescapes it`() {
        val body = "{\"choices\":[{\"message\":{\"content\":\"\\u4f60\\u597d\\nworld\"}}]}"
        assertEquals("你好\nworld", DualTranCore.parseChatResponse(body))
    }

    @Test
    fun `surfaces provider errors`() {
        val body = "{\"error\":{\"message\":\"bad key\",\"type\":\"auth\"}}"
        val error = runCatching { DualTranCore.parseChatResponse(body) }.exceptionOrNull()
        assertEquals("bad key", error?.message)
    }

    @Test(expected = IllegalStateException::class)
    fun `refuses an empty translation`() {
        DualTranCore.parseChatResponse("{\"choices\":[{\"message\":{\"content\":\"   \"}}]}")
    }

    @Test
    fun `encodes the google query`() {
        val url = DualTranCore.googleUrl("a & b", "zh-CN")
        assertTrue(url.contains("q=a%20%26%20b"))
        assertTrue(url.contains("tl=zh-CN"))
    }

    @Test
    fun `chunks long text without losing characters`() {
        val long = "x".repeat(250)
        assertEquals(
            listOf("x".repeat(100), "x".repeat(100), "x".repeat(50)),
            DualTranCore.chunkText(long, 100)
        )
    }

    @Test
    fun `keeps short text whole`() {
        assertEquals(listOf("hi"), DualTranCore.chunkText("hi"))
    }
}
