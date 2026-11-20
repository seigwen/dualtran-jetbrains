# DualTran Translate for JetBrains IDEs

Select text in any editor — code, comments, Markdown, a commit message — and hit
**Ctrl+Alt+T** (or right-click → *Translate selection*). The translation appears in a balloon and
lands on the clipboard, because that is almost always what you do with it next.

- **Google Translate** — no key, no configuration
- **AI** — any OpenAI-compatible endpoint with your own key (OpenAI, DeepSeek, OpenRouter, a local
  Ollama at `http://127.0.0.1:11434/v1`)

Runs on the IntelliJ Platform 2023.3+ (IDEA, PyCharm, WebStorm, GoLand, CLion, Rider, RustRover…).

## Build

```bash
./gradlew buildPlugin        # -> build/distributions/dualtran-jetbrains-0.2.0.zip
./gradlew test               # JUnit over the translation core
./gradlew runIde             # sandbox IDE with the plugin installed
```

## Install

**Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the ZIP from `build/distributions`.
Until it is on the Marketplace, that is the only route.

## Configuration

Open **Settings → Tools → DualTran**. The form edits Engine, Target language, API endpoint,
API key, Model and Reasoning effort; Apply writes them to `dualtran.xml` and the next translation
uses them without an IDE restart. Values are also readable/editable in that file:

```xml
<application>
  <component name="DualTranSettings">
    <option name="engine" value="ai" />
    <option name="endpoint" value="https://api.openai.com/v1" />
    <option name="model" value="gpt-4o-mini" />
    <option name="apiKey" value="sk-..." />
    <option name="target" value="zh-CN" />
    <option name="reasoning" value="lowest" />
  </component>
</application>
```

## Conformance (SPEC 2.1)

The plugin owns a real SPEC 2.1 gate, not a hand-written payload:

```bash
npm run conformance          # Kotlin catalogue + 38-locale bundle -> validate.mjs
npm run conformance:negative # proves R2/R4/R5 can fail
./gradlew test buildPlugin
```

The gate covers the provider `source` split (built-in + models.dev), the
non-selectable empty provider placeholder, model-level reasoning resolution
(`none → minimal → low`), and the 38-locale message matrix. CI runs it as the
`Conformance` workflow. The settings form and translation action use the same
`DualTranMessages` bundle, so the gate and the rendered UI share one source.

## Status

- JUnit tests over the translation core and the catalogue/i18n invariants.
- `buildPlugin` compiles and packages against IntelliJ Platform 2023.3; the settings form is
  registered as an application configurable under Tools.
- Marketplace publication needs a JetBrains vendor account.

## Licence

MPL-2.0.
