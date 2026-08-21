# Upstream

This directory is a vendored copy of **Raven Script Loader** (RSL), a standalone Forge
1.8.9 mod that hosts the JavaScript scripting API. It is not part of the Myau+ source
tree — it builds into its own jar and loads as a separate mod.

| | |
|---|---|
| Upstream | <https://codeberg.org/monster-energy/raven-script-loader> |
| Vendored at commit | `1be742369356a05c9f288b31b2c3d453ca59403a` |
| Commit date | 2025-09-06 |
| Commit subject | `added potions to isConsuming for marko.` |
| Mod id | `rsl` (Myau+ is `myau`) |
| Root package | `keystrokesmod` (Myau+ is `myau`) |

The upstream `.git` directory was removed when vendoring, so local edits here are tracked
by the OpenMyau-Plus repository like any other source.

## Pulling upstream changes

There is no submodule wiring, so updates are a manual merge:

```sh
git clone https://codeberg.org/monster-energy/raven-script-loader.git /tmp/rsl-upstream
diff -ru rsl /tmp/rsl-upstream --exclude=.git --exclude=build --exclude=.gradle --exclude=run
```

Apply the hunks you want, then bump the commit hash recorded above.

## changes from upstream:
raven b4 script compatibility. all of the missing stuff now uses myau modules (killaura/bedaura stuff)

## How it talks to Myau+

RSL has **no compile-time dependency on Myau+**. The `myau.*` script API in
`src/main/java/keystrokesmod/script/ScriptDefaults.java` drives Myau+ entirely at runtime
by issuing chat commands (`.t <module>`, `.<module> <property> <value>`) and intercepting
the suppressed chat output. That is why the two stay separate mods rather than being
merged into one jar — they only need to be installed side by side.
