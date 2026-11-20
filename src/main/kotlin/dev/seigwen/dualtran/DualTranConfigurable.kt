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
 * Target language, provider, model and reasoning are all selection controls. The provider combo
 * has **no selectable empty option**: when nothing is configured it has no selection and the
 * renderer shows the localized placeholder, so the R2 empty state is genuinely disabled. The
 * model combo is scoped to the selected provider; the reasoning combo follows that model's
 * declared options and persists the `lowest` sentinel until a request is built.
 */
class DualTranConfigurable : Configurable {

    private data class ProviderItem(
        val id: String,
        val label: String,
        val endpoint: String,
        val models: List<DualTranCatalog.Model>,
    ) {
        override fun toString(): String = label
    }

    private data class ModelItem(
        val id: String,
        val label: String,
        val reasoningOptions: List<String>,
    ) {
        override fun toString(): String = label
    }

    private data class ReasoningItem(val value: String, val label: String) {
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
    private val reasoning = JComboBox<ReasoningItem>()

    private val engineValues = mapOf("Google" to "google", "AI" to "ai")
    private val reasoningOrder = listOf("none", "minimal", "low", "medium", "high", "xhigh", "max")

    init {
        DualTranCatalog.languages.forEach { target.addItem(LanguageItem(it.code, it.label)) }
        target.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, selected, focus)
                if (value == null) {
                    text = DualTranMessages.text("selectLanguage", "Select a language")
                    isEnabled = false
                }
                return this
            }
        }

        DualTranCatalog.providers.forEach {
            provider.addItem(ProviderItem(it.id, it.label, it.endpoint, it.modelList))
        }
        provider.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, selected, focus)
                if (value == null) {
                    text = DualTranMessages.text("selectProvider", "Select a provider")
                    isEnabled = false
                }
                return this
            }
        }

        provider.addActionListener {
            val chosen = provider.selectedItem as? ProviderItem ?: return@addActionListener
            if (endpoint.text.isBlank() || endpoint.text.trimEnd('/') == lastAutoEndpoint) {
                endpoint.text = chosen.endpoint
                lastAutoEndpoint = chosen.endpoint
            }
            populateModels(chosen.models, "")
            updateEnabledState()
        }
        model.addActionListener { updateReasoningOptions() }
        engine.addActionListener { updateEnabledState() }
    }

    override fun getDisplayName(): String = "DualTran"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(DualTranMessages.text("engine", "Engine") + ":", engine)
                .addLabeledComponent(DualTranMessages.text("targetLanguage", "Target language") + ":", target)
                .addLabeledComponent(DualTranMessages.text("provider", "AI provider") + ":", provider)
                .addLabeledComponent(DualTranMessages.text("endpoint", "API endpoint") + ":", endpoint)
                .addLabeledComponent(DualTranMessages.text("apiKey", "API key") + ":", apiKey)
                .addLabeledComponent(DualTranMessages.text("model", "Model") + ":", model)
                .addLabeledComponent(DualTranMessages.text("reasoning", "Reasoning effort") + ":", reasoning)
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
        if (chosenProvider == null) {
            // No selectable placeholder exists; -1 is the genuinely empty state.
            provider.selectedIndex = -1
            populateModels(emptyList(), "")
            endpoint.text = state.endpoint
            populateReasoning(emptyList(), state.reasoning)
        } else {
            provider.selectedItem = chosenProvider
            lastAutoEndpoint = chosenProvider.endpoint
            populateModels(chosenProvider.models, state.model)
            endpoint.text = state.endpoint
            populateReasoning(selectedModelReasoningOptions(), state.reasoning)
        }

        apiKey.text = state.apiKey
        updateEnabledState()
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun populateModels(models: List<DualTranCatalog.Model>, preferred: String) {
        model.removeAllItems()
        models.forEach {
            model.addItem(ModelItem(it.id, it.label, it.reasoningOptions.orEmpty()))
        }
        if (models.isEmpty()) {
            model.isEnabled = false
            populateReasoning(emptyList(), "lowest")
            return
        }
        val selected = (0 until model.itemCount)
            .map { model.getItemAt(it) }
            .firstOrNull { it.id == preferred }
        if (selected != null) {
            model.selectedItem = selected
        } else if (preferred.isNotEmpty()) {
            val current = ModelItem(preferred, preferred, emptyList())
            model.addItem(current)
            model.selectedItem = current
        } else {
            model.selectedIndex = 0
        }
        updateReasoningOptions()
    }

    private fun updateReasoningOptions() {
        populateReasoning(selectedModelReasoningOptions(), reasoningValue())
    }

    private fun populateReasoning(options: List<String>, preferred: String) {
        val values = LinkedHashSet<String>()
        values.addAll(reasoningOrder.filter { it in options })
        if (preferred.isNotEmpty() && preferred != "lowest") values.add(preferred)
        reasoning.removeAllItems()
        reasoning.addItem(ReasoningItem("lowest", DualTranMessages.text("reasoningLowest", "Lowest (automatic)")))
        values.forEach { reasoning.addItem(ReasoningItem(it, reasoningLabel(it))) }
        val selected = (0 until reasoning.itemCount)
            .map { reasoning.getItemAt(it) }
            .firstOrNull { it.value == preferred }
        reasoning.selectedItem = selected ?: reasoning.getItemAt(0)
    }

    private fun reasoningLabel(value: String): String = DualTranMessages.text(
        "reasoning${value.replaceFirstChar { it.uppercase() }}",
        value.replaceFirstChar { it.uppercase() },
    )

    private fun updateEnabledState() {
        val ai = engineValue() == "ai"
        val hasProvider = selectedProviderId().isNotEmpty()
        provider.isEnabled = ai
        model.isEnabled = ai && hasProvider
        reasoning.isEnabled = ai && hasProvider
        endpoint.isEnabled = ai
        apiKey.isEnabled = ai
    }

    private fun selectedLanguageCode(): String = (target.selectedItem as? LanguageItem)?.code ?: ""

    private fun selectedProviderId(): String = (provider.selectedItem as? ProviderItem)?.id ?: ""

    private fun selectedModel(): ModelItem? = model.selectedItem as? ModelItem

    private fun selectedModelId(): String = selectedModel()?.id ?: ""

    private fun selectedModelReasoningOptions(): List<String> = selectedModel()?.reasoningOptions.orEmpty()

    private fun engineValue(): String = engineValues[engine.selectedItem] ?: "google"

    private fun reasoningValue(): String = (reasoning.selectedItem as? ReasoningItem)?.value ?: "lowest"

    private fun settings(): DualTranSettings = DualTranSettings.getInstance()
}
