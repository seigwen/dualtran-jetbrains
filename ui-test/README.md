# DualTran JetBrains UI test assets

Phase 4 black-box tests for the IntelliJ IDEA Community host.

## Host / plugin

- IntelliJ IDEA Community **2024.3.5**, build **IC-243.26053.27**, installed from
  the official `ideaIC-2024.3.5.exe` installer under
  `C:/DualTran-node/idea-2024.3.5`.
- Plugin ZIP: `dualtran-jetbrains-0.1.0.zip` from `build/distributions`; installed
  through **Settings → Plugins → ⚙ → Install Plugin from Disk…** (the
  `Install Plugin from Disk` action was also reachable through Find Action).
- The plugin is listed as **DualTran Translate 0.1.0**, author `seigwen`, enabled;
  the third-party-plugin notice was accepted and the IDE was restarted.
- Plugin XML declares `since-build="233"` / `until-build="243.*"`; IDE build 243
  is within range.

## Settings UI limitation

The plugin has **no DualTran settings UI**. Its settings are an application-level
`dualtran.xml` file; the source comment explicitly calls the settings UI a future
milestone. IntelliJ Settings exposes no provider/model/endpoint/reasoning/target
controls for this plugin, so those five settings surfaces are marked `blocked`
("concept does not exist in this host plugin surface"), not fabricated. Runtime
translation and the configured target are tested through the editor action.

## State isolation

The test used an isolated IDE configuration:

```text
-Didea.config.path=C:/DualTran-node/test/idea-phase4/config
-Didea.system.path=C:/DualTran-node/test/idea-phase4/system
-Didea.plugins.path=C:/DualTran-node/test/idea-phase4/plugins
-Didea.log.path=C:/DualTran-node/test/idea-phase4/log
```

The plugin was installed into that test plugin path, and `dualtran.xml` was set to:

```xml
<application>
  <component name="DualTranSettings">
    <option name="engine" value="ai" />
    <option name="target" value="zh-CN" />
    <option name="endpoint" value="http://10.66.66.1:11434/v1" />
    <option name="apiKey" value="" />
    <option name="model" value="glm-5.3-flash:cloud" />
  </component>
</application>
```

The home node reached the VPS Ollama through a temporary TCP forwarder on
`10.66.66.1:11434`; it was stopped afterwards. The test project and test config
were removed; the IDE installation itself remains available.

## Scenarios

| Scenario | Axis | What is asserted |
|---|---|---|
| `jetbrains-plugins-baseline` | baseline | Settings → Plugins → Installed shows DualTran Translate 0.1.0 enabled; no raw error |
| `jetbrains-translation-configured` | configured | `Hello world` selected in `sample.md`; `Ctrl+Alt+T` produces `DualTran · zh-CN` notification with `你好，世界`; no raw error |

The configured scenario uses the IDE notification timeline (opened through the
bell icon) so the result is readable instead of relying on a clipped balloon.

## Run

With the isolated IDE running and project open:

```bash
cd /root/DualTran-manage/host-ui-tests
DT_EVIDENCE_DIR=reports/phase4/jetbrains \
  node --experimental-strip-types src/cli.ts test --host dualtran-jetbrains
npm run ui:audit -- --host dualtran-jetbrains
```

Evidence: `reports/phase4/jetbrains/`. The five settings surfaces are blocked, so
the default audit exits 2; this is intentional and is not counted as coverage.
