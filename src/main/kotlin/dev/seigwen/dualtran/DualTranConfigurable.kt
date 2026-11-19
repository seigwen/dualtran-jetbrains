package dev.seigwen.dualtran

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

/**
 * Settings → Tools → DualTran.
 *
 * Target language, provider, model and reasoning are all selection controls. The provider list is
 * a single alphabetical models.dev snapshot (plus a disabled-looking placeholder and Custom… last);
 * choosing a provider fills the endpoint only while the user has not typed an override, and the
 * model list is scoped to that provider.
 */
class DualTranConfigurable : Configurable {

    private data class ProviderItem(
        val id: String,
        val label: String,
        val endpoint: String,
        val models: List<DualTranCatalog.Model>,
    ) {
        override fun toString(): String = label
        val isPlaceholder: Boolean get() = id.isEmpty()
    }

    private data class ModelItem(val id: String, val label: String) {
        override fun toString(): String = label
    }

    private data class LanguageItem(val code: String, val label: String) {
        override fun toString(): String = label
    }

    private var panel: JPanel? = null
    private var lastAutoEndpoint = ""

    private val engine = JComboBox(arrayOf("Google", "AI"))
    private val target = JComboBox<LanguageItem>()
    private val provider = JComboBox<ProviderItem>()
    private val endpoint = JBTextField().apply { emptyText.text = "https://" }
    private val apiKey = JBPasswordField()
    private val model = JComboBox<ModelItem>()
    private val reasoning = JComboBox(arrayOf("Lowest", "Low", "Medium", "High"))

    private val engineValues = mapOf("Google" to "google", "AI" to "ai")
    private val reasoningValues = mapOf("Lowest" to "lowest", "Low" to "low", "Medium" to "medium", "High" to "high")

    init {
        target.addItem(LanguageItem("", ""))
        DualTranCatalog.languages.forEach { target.addItem(LanguageItem(it.code, it.label)) }
        target.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, selected, focus)
                if ((value as? LanguageItem)?.code.isNullOrEmpty()) {
                    text = "Select a language"
                    isEnabled = false
                }
                return this
            }
        }

        provider.addItem(ProviderItem("", "Select a provider", "", emptyList()))
        DualTranCatalog.providers.forEach {
            provider.addItem(ProviderItem(it.id, it.label, it.endpoint, it.models))
        }
        provider.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, selected, focus)
                if ((value as? ProviderItem)?.isPlaceholder == true) {
                    isEnabled = false
                }
                return this
            }
        }

        provider.addActionListener {
            if (provider.selectedItem == null) return@addActionListener
            val chosen = provider.selectedItem as ProviderItem
            if (chosen.isPlaceholder) {
                lastAutoEndpoint = ""
                populateModels(emptyList(), "")
            } else {
                if (endpoint.text.isBlank() || endpoint.text.trimEnd('/') == lastAutoEndpoint) {
                    endpoint.text = chosen.endpoint
                    lastAutoEndpoint = chosen.endpoint
                }
                populateModels(chosen.models, "")
            }
            updateEnabledState()
        }
        engine.addActionListener { updateEnabledState() }
    }

    override fun getDisplayName(): String = "DualTran"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Engine:", engine)
                .addLabeledComponent("Target language:", target)
                .addLabeledComponent("AI provider:", provider)
                .addLabeledComponent("API endpoint:", endpoint)
                .addLabeledComponent("API key:", apiKey)
                .addLabeledComponent("Model:", model)
                .addLabeledComponent("Reasoning effort:", reasoning)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        }
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val current = settings().snapshot()
        return engineValue() != current.engine ||
            selectedLanguageCode() != current.target ||
            selectedProviderId() != current.aiProvider ||
            endpoint.text.trim() != current.endpoint ||
            String(apiKey.password) != current.apiKey ||
            selectedModelId() != current.model ||
            reasoningValue() != current.reasoning
    }

    override fun apply() {
        val current = settings().snapshot()
        current.engine = engineValue()
        current.target = selectedLanguageCode()
        current.aiProvider = selectedProviderId()
        current.endpoint = endpoint.text.trim()
        current.apiKey = String(apiKey.password)
        current.model = selectedModelId()
        current.reasoning = reasoningValue()
        settings().update(current)
    }

    override fun reset() {
        val state = settings().snapshot()

        engine.selectedItem = engineValues.entries.firstOrNull { it.value == state.engine }?.key ?: "Google"

        val language = (0 until target.itemCount)
            .map { target.getItemAt(it) }
            .firstOrNull { it.code == state.target }
        if (language != null) {
            target.selectedItem = language
        } else {
            val fallback = LanguageItem(state.target, state.target)
            target.addItem(fallback)
            target.selectedItem = fallback
        }

        val chosenProvider = (0 until provider.itemCount)
            .map { provider.getItemAt(it) }
            .firstOrNull { it.id == state.aiProvider }
        provider.selectedItem = chosenProvider ?: (provider.getItemAt(0) as ProviderItem)
        lastAutoEndpoint = ""
        if (chosenProvider != null && !chosenProvider.isPlaceholder) {
            populateModels(chosenProvider.models, state.model)
            endpoint.text = state.endpoint
            lastAutoEndpoint = chosenProvider.endpoint
        } else {
            populateModels(emptyList(), "")
            endpoint.text = state.endpoint
        }

        apiKey.text = state.apiKey
        reasoning.selectedItem = reasoningValues.entries.firstOrNull { it.value == state.reasoning }?.key ?: "Lowest"
        updateEnabledState()
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun populateModels(models: List<DualTranCatalog.Model>, preferred: String) {
        model.removeAllItems()
        models.forEach { model.addItem(ModelItem(it.id, it.label)) }
        if (models.isEmpty()) {
            model.isEnabled = false
            return
        }
        val selected = (0 until model.itemCount)
            .map { model.getItemAt(it) }
            .firstOrNull { it.id == preferred }
        if (selected != null) {
            model.selectedItem = selected
        } else if (preferred.isNotEmpty()) {
            val current = ModelItem(preferred, preferred)
            model.addItem(current)
            model.selectedItem = current
        } else {
            model.selectedIndex = 0
        }
    }

    private fun updateEnabledState() {
        val ai = engineValue() == "ai"
        val hasProvider = !selectedProviderId().isNullOrEmpty()
        provider.isEnabled = ai
        model.isEnabled = ai && hasProvider
        reasoning.isEnabled = ai && hasProvider
        endpoint.isEnabled = ai
        apiKey.isEnabled = ai
    }

    private fun selectedLanguageCode(): String = (target.selectedItem as? LanguageItem)?.code ?: ""

    private fun selectedProviderId(): String = (provider.selectedItem as? ProviderItem)?.id ?: ""

    private fun selectedModelId(): String = (model.selectedItem as? ModelItem)?.id ?: ""

    private fun engineValue(): String = engineValues[engine.selectedItem] ?: "google"

    private fun reasoningValue(): String = reasoningValues[reasoning.selectedItem] ?: "lowest"

    private fun settings(): DualTranSettings = DualTranSettings.getInstance()
}
