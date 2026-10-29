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
./gradlew buildPlugin        # -> build/distributions/dualtran-jetbrains-0.1.0.zip
./gradlew test               # JUnit over the translation core
./gradlew runIde             # sandbox IDE with the plugin installed
```

## Install

**Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the ZIP from `build/distributions`.
Until it is on the Marketplace, that is the only route.

## Configuration

Settings live in `dualtran.xml` in the IDE config directory; defaults are Google / `zh-CN`. To use
an AI endpoint, edit that file (a settings UI is the next milestone):

```xml
<application>
  <component name="DualTranSettings">
    <option name="engine" value="ai" />
    <option name="endpoint" value="https://api.openai.com/v1" />
    <option name="model" value="gpt-4o-mini" />
    <option name="apiKey" value="sk-..." />
    <option name="target" value="zh-CN" />
  </component>
</application>
```

## Status

- JUnit tests over the translation core: endpoint normalisation, payload escaping, response
  parsing (including `\uXXXX`), Google URL encoding, chunking.
- **Not yet run inside a real IDE** — `buildPlugin` proves it compiles and packages against the
  platform SDK; the action itself still needs a manual smoke test.
- Marketplace publication needs a JetBrains vendor account.

## Licence

MPL-2.0.
