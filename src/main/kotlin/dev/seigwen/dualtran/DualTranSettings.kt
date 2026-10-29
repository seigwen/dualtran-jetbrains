package dev.seigwen.dualtran

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings, stored in `dualtran.xml` in the IDE config directory.
 * Deliberately not a full settings UI yet: the action reads these values and the defaults are
 * good enough for a first release.
 */
@State(name = "DualTranSettings", storages = [Storage("dualtran.xml")])
class DualTranSettings : PersistentStateComponent<DualTranSettings.Data> {

    data class Data(
        var engine: String = "google",
        var target: String = "zh-CN",
        var endpoint: String = "https://api.openai.com/v1",
        var apiKey: String = "",
        var model: String = "gpt-4o-mini",
    )

    private var state = Data()

    override fun getState(): Data = state

    override fun loadState(state: Data) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun core(): DualTranCore.Settings = DualTranCore.Settings(
        engine = state.engine,
        target = state.target,
        endpoint = state.endpoint,
        apiKey = state.apiKey,
        model = state.model,
    )

    companion object {
        fun getInstance(): DualTranSettings =
            ApplicationManager.getApplication().getService(DualTranSettings::class.java)
    }
}
