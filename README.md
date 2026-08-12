# Open Myau Plus

![Preview](/images/image3.png)
 
[Myau Client](https://myau.sell.app/),  OpenMyau But Better. 


Open Myau Plus is an enhanced version of the original OpenMyau client, built with additional features, optimizations, and quality-of-life improvements. This project focuses on expanding the core functionality while maintaining stability and performance.

🔗 Official Client: https://myau.sell.app/

---

# Features & Improvements

- Various bug fixes and performance improvements
- Enhanced overall user experience compared to the base version

---

# About

This project is based on OpenMyau, with the goal of refining and extending its capabilities beyond the original implementation.

---

#  Issues & Suggestions

If you encounter any bugs or have ideas for new features, feel free to open an issue:

 https://github.com/IamNespola/OpenMyau-Plus/issues

---

# Building

To build the project, run:

```bash
git clone https://github.com/IamNespola/OpenMyau-Plus.git
cd OpenMyau-Plus
./gradlew build
```

This produces two jars in `build/libs`:

- `Myau+.jar` — the client itself
- the **Raven Script Loader** jar — the scripting mod (see below)

---

# Scripting (Raven Script Loader)

Script support lives in [`rsl/`](rsl/), a vendored copy of
[Raven Script Loader](https://codeberg.org/monster-energy/raven-script-loader). It is a
**separate Forge mod** (`modid=rsl`), not part of the client jar — the two are installed
side by side in your mods folder. RSL has no compile-time dependency on Myau+; its `myau.*`
script API drives the client at runtime by issuing chat commands and reading back the
suppressed output. See [`rsl/UPSTREAM.md`](rsl/UPSTREAM.md) for provenance and update steps.

Because RSL ships its own Gradle 8.11 wrapper and Loom toolchain (this project uses Gradle
8.8 + essential-loom), it is built out of process through its own wrapper rather than as a
composite build. `./gradlew build` drives it automatically; these tasks are also available
individually:

| Task | Purpose |
|---|---|
| `./gradlew buildRsl` | Build RSL via `rsl/gradlew` |
| `./gradlew copyRslJar` | Build it and copy the jar into `build/libs` |
| `./gradlew installRslToRunMods` | Build it and install it into `run/mods` |

`./gradlew runClient` runs `installRslToRunMods` first, so the dev client launches with both
mods loaded and you can test scripts against live Myau+ modules.

---

#  Contributing

Contributions are welcome! You can:

- Open an issue
- Submit a pull request

---
#  Support

If you like this project, consider giving it a  on GitHub — it really helps!
---

#  Contact

If you're interested in collaborating or have any questions, feel free to reach out:

- Discord: https://dsc.gg/nespola
- Username: @nespola1

---

# © License

This project follows the same licensing terms as the original OpenMyau project unless stated otherwise.
