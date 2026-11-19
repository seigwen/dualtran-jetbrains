package dev.seigwen.dualtran

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings, stored in `dualtran.xml` in the IDE config directory.
 * Edited through Settings → Tools → DualTran.
 */
@State(name = "DualTranSettings", storages = [Storage("dualtran.xml")])
class DualTranSettings : PersistentStateComponent<DualTranSettings.Data> {

    data class Data(
        var engine: String = "google",
        var target: String = "zh-CN",
        var endpoint: String = "",
        var apiKey: String = "",
        var model: String = "",
        var aiProvider: String = "",
        var reasoning: String = "lowest",
    )

    private var state = Data()

    override fun getState(): Data = state

    override fun loadState(state: Data) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    /** A copy for the settings form; editing it must not mutate live state. */
    fun snapshot(): Data = state.copy()

    /** Called from the settings form's apply(). */
    fun update(next: Data) {
        state = next.copy()
    }

    fun core(): DualTranCore.Settings = DualTranCore.Settings(
        engine = state.engine,
        target = state.target,
        endpoint = state.endpoint,
        apiKey = state.apiKey,
        model = state.model,
        provider = state.aiProvider,
        reasoning = state.reasoning,
    )

    companion object {
        fun getInstance(): DualTranSettings =
            ApplicationManager.getApplication().getService(DualTranSettings::class.java)
    }
}
