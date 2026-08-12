/*
 * Ported from FDPClient's clickgui/ClickGui.kt (the default "BlackStyle" screen). Removed:
 * the "Auto Settings" cloud-download panel (ClientApi/SettingsUtils - no Myau+ equivalent
 * backend), the HUD-designer icon button (Myau+ has no HUD designer), the bloom glow effect
 * (no bloom-shader utility in Myau+), and kotlinx.coroutines (only used by the removed
 * auto-settings panel). Category grouping is swapped from FDPClient's own Category enum to the
 * same normalized-name-set scheme every other Myau+ skin uses. Panel-position persistence uses
 * the same GSON-file pattern as every other Myau+ skin instead of FDPClient's FileManager.
 */
package myau.ui.impl.clickgui.fdp

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import myau.Myau
import myau.module.Module
import myau.module.modules.ClickGUIModule
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

object FDPClickGui : GuiScreen() {

    val panels = linkedSetOf<Panel>()
    val style: Style = BlackStyle
    val search = ModuleSearch()

    private val configFile = File("./config/Myau/", "clickgui_fdp1to1.txt")

    private var mouseX = 0
    private var mouseY = 0
    private var autoScrollY: Int? = null

    // Used when closing ClickGUI using its key bind: prevents keyTyped (which fires right after
    // the KeyEvent that opened this screen) from closing it again on the same press.
    private var ignoreClosing = false

    // ── Category name sets (normalized), matching every other Myau+ skin ──────────────────────
    private val COMBAT = setOf(
        "aimassist", "autoclicker", "killaura", "wtap", "velocity", "reach", "targetstrafe", "nohitdelay",
        "antifireball", "lagrange", "movefix", "serverlag", "knockbackdelay", "hitbox", "morekb", "refill",
        "hitselect", "backtrack", "hitflick", "timerrange", "clickassits", "criticals", "blockhit",
        "sprintreset", "displace", "tickbase", "piercing", "stasis"
    )
    private val MOVEMENT = setOf(
        "antiafk", "fly", "fastbow", "speed", "longjump", "sprint", "safewalk", "jesus", "blink", "nofall",
        "noslow", "keepsprint", "eagle", "nojumpdelay", "antivoid", "timer"
    )
    private val RENDER = setOf(
        "esp", "chams", "fullbright", "tracers", "nametags", "xray", "targetesp", "targethud", "indicators",
        "bedesp", "itemesp", "breakprogress", "viewclip", "nohurtcam", "hud", "guimodule", "riseclickgui",
        "clickgui", "chestesp", "trajectories", "radar", "renderfixes", "fpscounter", "watermark", "watermark2",
        "hitparticleeffects", "dynamicisland", "esp2d", "teamhealthdisplay", "sessiondisplay", "animations",
        "blockoverlay", "ambience", "capes"
    )
    private val PLAYER = setOf(
        "autoheal", "faklag", "fakelag", "autotool", "cheststealer", "autobeddef", "invmanager", "invwalk",
        "scaffold", "autoblockin", "autoswap", "speedmine", "fastplace", "ghosthand", "mcf", "antidebuff",
        "flagdetector", "autogapple", "chestaura", "autoheadhitter", "throwaura", "autoauth"
    )

    private fun norm(name: String): String = name.replace(Regex("[^A-Za-z0-9]"), "").lowercase(Locale.ROOT)

    fun rebuild() {
        panels.clear()

        val combat = ArrayList<Module>()
        val movement = ArrayList<Module>()
        val render = ArrayList<Module>()
        val player = ArrayList<Module>()
        val misc = ArrayList<Module>()
        val scripts = ArrayList<Module>()

        for (module in Myau.moduleManager.allModules()) {
            when {
                module is myau.module.modules.ScriptModule -> scripts += module
                norm(module.name) in COMBAT -> combat += module
                norm(module.name) in MOVEMENT -> movement += module
                norm(module.name) in RENDER -> render += module
                norm(module.name) in PLAYER -> player += module
                else -> misc += module
            }
        }

        val comparator = compareBy<Module> { it.name.lowercase(Locale.ROOT) }
        combat.sortWith(comparator)
        movement.sortWith(comparator)
        render.sortWith(comparator)
        player.sortWith(comparator)
        misc.sortWith(comparator)
        scripts.sortWith(comparator)

        val width = 100
        val height = 18
        var yPos = 5

        for ((label, list) in listOf(
            "Combat" to combat, "Movement" to movement, "Render" to render,
            "Player" to player, "Misc" to misc, "Scripts" to scripts
        )) {
            panels += Panel(label, 100, yPos, width, height, false, list.map(::ModuleElement))
            yPos += 20
        }

        loadPositions()
    }

    private fun accentColor(): Int {
        val module = Myau.moduleManager.getModule("ClickGUI")
        return if (module is ClickGUIModule) module.getAccentColor().rgb else java.awt.Color(80, 150, 255).rgb
    }

    override fun drawScreen(x: Int, y: Int, partialTicks: Float) {
        mouseX = x
        mouseY = y

        drawDefaultBackground()

        for (panel in panels) {
            panel.updateFade(1)
            panel.drawScreenAndClick(mouseX, mouseY)
        }

        descriptions@ for (panel in panels.reversed()) {
            // Don't draw hover text when hovering over a panel header.
            if (panel.isHovered(mouseX, mouseY)) break

            for (element in panel.elements) {
                if (element is ButtonElement && element.isVisible && element.hoverText.isNotBlank() &&
                    element.isHovered(mouseX, mouseY) && element.y <= panel.y + panel.fade
                ) {
                    style.drawHoverText(mouseX, mouseY, element.hoverText)
                    break@descriptions
                }
            }
        }

        if (Mouse.hasWheel()) {
            val wheel = autoScrollY?.let { it - y } ?: Mouse.getDWheel()
            if (wheel != 0) {
                var handled = false
                for (panel in panels.reversed()) {
                    if (panel.handleScroll(mouseX, mouseY, wheel)) {
                        handled = true
                        break
                    }
                }
                if (!handled && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    // No global scale to adjust in this port - scrolling outside a panel is a no-op.
                }
            }
        }

        search.draw(fontRendererObj, width, accentColor())

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    public override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        if (search.mouseClicked(x, y, mouseButton)) return

        if (mouseButton == 2) {
            autoScrollY = y
        }

        mouseX = x
        mouseY = y

        panels.reversed().forEach { panel ->
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) return

            panel.drag = false

            if (mouseButton == 0 && panel.isHovered(mouseX, mouseY)) {
                panel.x2 = panel.x - mouseX
                panel.y2 = panel.y - mouseY
                panel.drag = true

                // Move dragged panel to top.
                panels.remove(panel)
                panels += panel
                return
            }
        }
    }

    public override fun mouseReleased(x: Int, y: Int, button: Int) {
        mouseX = x
        mouseY = y

        if (button == 2) {
            autoScrollY = null
        }

        for (panel in panels) panel.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (BlackStyle.captureModuleKeyBind(keyCode)) return

        if (search.keyTyped(typedChar, keyCode, isCtrlKeyDown())) return

        val guiModule = Myau.moduleManager.getModule("ClickGUI")
        if (keyCode == (guiModule?.key ?: 0) || keyCode == Keyboard.KEY_ESCAPE) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                if (ignoreClosing) {
                    ignoreClosing = false
                } else {
                    mc.displayGuiScreen(null)
                }
                return
            }
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        autoScrollY = null
        search.unfocus()
        savePositions()
        Keyboard.enableRepeatEvents(false)
        for (panel in panels) panel.fade = 0

        val guiModule = Myau.moduleManager.getModule("ClickGUI")
        if (guiModule is ClickGUIModule && guiModule.isSwitchingGuiStyle) {
            return
        }
        guiModule?.setEnabled(false)
    }

    override fun initGui() {
        ignoreClosing = true
        Keyboard.enableRepeatEvents(true)
        if (panels.isEmpty()) rebuild()
    }

    override fun doesGuiPauseGame() = false

    private fun savePositions() {
        val json = JsonObject()
        for (panel in panels) {
            val pos = JsonObject()
            pos.addProperty("x", panel.x)
            pos.addProperty("y", panel.y)
            pos.addProperty("open", panel.open)
            json.add(panel.name, pos)
        }
        try {
            FileWriter(configFile).use { GsonBuilder().setPrettyPrinting().create().toJson(json, it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadPositions() {
        if (!configFile.exists()) return
        try {
            FileReader(configFile).use { reader ->
                val json = JsonParser().parse(reader).asJsonObject
                for (panel in panels) {
                    if (json.has(panel.name)) {
                        val pos = json.getAsJsonObject(panel.name)
                        panel.x = pos.get("x").asInt
                        panel.y = pos.get("y").asInt
                        panel.open = pos.get("open").asBoolean
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
