package dev.seigwen.dualtran

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import java.net.HttpURLConnection
import java.net.URL

/**
 * Translates the editor selection and shows the result in a balloon; the translation also lands on
 * the clipboard, because the next step is almost always pasting it somewhere.
 */
class TranslateAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = editor?.selectionModel?.hasSelection() == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val text = editor.selectionModel.selectedText?.trim().orEmpty()
        if (text.isEmpty()) return

        val settings = DualTranSettings.getInstance().core()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { translate(text, settings) }
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { translation ->
                        CopyPasteManager.getInstance().setContents(StringSelection(translation))
                        notify("DualTran · ${settings.target}", escape(translation), NotificationType.INFORMATION)
                    },
                    onFailure = { error ->
                        notify("DualTran failed", escape(error.message ?: error.toString()), NotificationType.ERROR)
                    },
                )
            }
        }
    }

    private fun translate(text: String, settings: DualTranCore.Settings): String {
        val separator = "\n\n"
        return DualTranCore.chunkText(text).joinToString(separator) { chunk ->
            if (settings.engine == "ai") translateWithAi(chunk, settings) else translateWithGoogle(chunk, settings)
        }
    }

    private fun translateWithGoogle(text: String, settings: DualTranCore.Settings): String =
        DualTranCore.parseGoogleResponse(request("GET", DualTranCore.googleUrl(text, settings.target), null, null))

    private fun translateWithAi(text: String, settings: DualTranCore.Settings): String {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (settings.apiKey.isNotEmpty()) headers["Authorization"] = "Bearer ${settings.apiKey}"
        val body = DualTranCore.buildChatPayload(settings.model, text, settings.target)
        return DualTranCore.parseChatResponse(
            request("POST", DualTranCore.chatCompletionsUrl(settings.endpoint), body, headers),
        )
    }

    private fun request(method: String, url: String, body: String?, headers: Map<String, String>?): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        headers?.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val payload = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code: ${payload.take(160)}")
        }
        return payload
    }

    private fun notify(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("DualTran")
            .createNotification(title, content, type)
            .notify(null)
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br/>")
}
