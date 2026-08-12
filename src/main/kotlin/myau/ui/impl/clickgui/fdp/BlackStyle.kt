/*
 * Ported from FDPClient's clickgui/style/styles/BlackStyle.kt - panel/hover-text/module-row
 * rendering and layout math kept faithful to the original. The settings `when` block is
 * trimmed to the property types Myau+ actually has:
 *   BoolValue -> BooleanProperty, FloatValue/IntValue -> FloatProperty/IntProperty/LongProperty/
 *   PercentProperty (one shared numeric-slider renderer), ListValue -> ModeProperty (kept as
 *   FDP's real expandable dropdown, not the cycle-arrows every other Myau+ skin uses),
 *   ColorValue -> ColorProperty (ColorProperty is RGB-only with no alpha/rainbow, so the picker
 *   is FDP's swatch + a compact hue/saturation/brightness bar trio instead of its 2D
 *   texture-cached picker), FileValue -> FileProperty (opens via Desktop, like FDP's dialog),
 *   generic text fallback -> TextProperty, using Myau+'s own GuiInput.prompt text editor
 *   instead of porting FDP's EditableText cursor/selection system.
 * Removed entirely (no Myau+ Property maps to these): MultiSelectValue, Vec2/Vec3/MutableList,
 * BlockValue, IntRangeValue/FloatRangeValue, FontValue, CurveValue, KeyBindValue-as-a-setting
 * (Myau+ binds per-module, not per-property - see the Bind row on every other skin, added the
 * same way here), and Configurable nested setting groups (Myau+'s property list is flat).
 */
package myau.ui.impl.clickgui.fdp

import myau.Myau
import myau.font.impl.UFontRenderer
import myau.module.modules.ClickGUIModule
import myau.property.Property
import myau.property.properties.*
import myau.ui.callback.GuiInput
import myau.util.RenderUtil
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object BlackStyle : Style() {

    private var bindingModule: ModuleElement? = null

    /** Returns true when the key was consumed by an in-progress module keybind capture. */
    fun captureModuleKeyBind(keyCode: Int): Boolean {
        val target = bindingModule ?: return false
        target.module.key = if (keyCode == Keyboard.KEY_ESCAPE) 0 else keyCode
        bindingModule = null
        return true
    }

    private fun font(): UFontRenderer = Myau.fontManagers.getFont(16)

    private fun accent(): Int {
        val module = Myau.moduleManager.getModule("ClickGUI")
        return if (module is ClickGUIModule) module.getAccentColor().rgb else Color(80, 150, 255).rgb
    }

    private fun drawBorderedRect(x1: Number, y1: Number, x2: Number, y2: Number, border: Double, borderColor: Int, fillColor: Int) {
        val bx1 = x1.toDouble(); val by1 = y1.toDouble(); val bx2 = x2.toDouble(); val by2 = y2.toDouble()
        RenderUtil.drawRect(bx1, by1, bx2, by2, borderColor)
        RenderUtil.drawRect(bx1 + border, by1 + border, bx2 - border, by2 - border, fillColor)
    }

    private fun drawFilledCircle(x: Number, y: Number, radius: Float, color: Color) {
        RenderUtil.fillCircle(x.toDouble(), y.toDouble(), radius.toDouble(), 12, color.rgb)
    }

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        drawBorderedRect(
            panel.x, panel.y - 3, panel.x + panel.width, panel.y + 17, 3.0, Color(20, 20, 20).rgb, Color(20, 20, 20).rgb
        )

        if (panel.fade > 0) {
            drawBorderedRect(
                panel.x, panel.y + 17, panel.x + panel.width, panel.y + 19 + panel.fade,
                3.0, Color(40, 40, 40).rgb, Color(40, 40, 40).rgb
            )
            drawBorderedRect(
                panel.x, panel.y + 17 + panel.fade, panel.x + panel.width, panel.y + 24 + panel.fade,
                3.0, Color(20, 20, 20).rgb, Color(20, 20, 20).rgb
            )
        }

        val xPos = panel.x - (font().getStringWidth("§f" + StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font().drawString(panel.name, xPos.toFloat(), (panel.y + 2).toFloat(), Color.WHITE.rgb)
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width = lines.maxOfOrNull { font().getStringWidth(it) + 14 } ?: return
        val height = (font().getHeight() * lines.size) + 3

        val sr = ScaledResolution(net.minecraft.client.Minecraft.getMinecraft())
        val x = mouseX.clamp(0, sr.scaledWidth - width)
        val y = mouseY.clamp(0, sr.scaledHeight - height)

        drawBorderedRect(x + 9, y, x + width, y + height, 3.0, Color(40, 40, 40).rgb, Color(40, 40, 40).rgb)

        lines.forEachIndexed { index, line ->
            font().drawString(line, (x + 12).toFloat(), (y + 3 + font().getHeight() * index).toFloat(), Color.WHITE.rgb)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        RenderUtil.drawRect(
            (buttonElement.x - 1).toDouble(), (buttonElement.y - 1).toDouble(),
            (buttonElement.x + buttonElement.width + 1).toDouble(), (buttonElement.y + buttonElement.height + 1).toDouble(),
            getHoverColor(if (buttonElement.color != Int.MAX_VALUE) Color(20, 20, 20) else Color(40, 40, 40), buttonElement.hoverTime)
        )

        font().drawString(buttonElement.displayName, (buttonElement.x + 5).toFloat(), (buttonElement.y + 5).toFloat(), Color.WHITE.rgb)
    }

    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?
    ): Boolean {
        RenderUtil.drawRect(
            (moduleElement.x - 1).toDouble(), (moduleElement.y - 1).toDouble(),
            (moduleElement.x + moduleElement.width + 1).toDouble(), (moduleElement.y + moduleElement.height + 1).toDouble(),
            getHoverColor(Color(40, 40, 40), moduleElement.hoverTime)
        )
        RenderUtil.drawRect(
            (moduleElement.x - 1).toDouble(), (moduleElement.y - 1).toDouble(),
            (moduleElement.x + moduleElement.width + 1).toDouble(), (moduleElement.y + moduleElement.height + 1).toDouble(),
            getHoverColor(Color(20, 20, 20, moduleElement.slowlyFade), moduleElement.hoverTime, !moduleElement.module.isEnabled)
        )

        font().drawString(
            moduleElement.displayName, (moduleElement.x + 5).toFloat(), (moduleElement.y + 5).toFloat(), Color.WHITE.rgb
        )

        val properties = (Myau.propertyManager.properties[moduleElement.module] ?: emptyList())
            .filter { it.isVisible }
        run {
            // A Bind row is always appended below, so the settings arrow always has something to show.
            font().drawString(
                if (moduleElement.showSettings) "<" else ">",
                (moduleElement.x + moduleElement.width - 8).toFloat(), (moduleElement.y + 5).toFloat(), Color.WHITE.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 6

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                if (moduleElement.settingsWidth > 0 && moduleElement.settingsHeight > 0) drawBorderedRect(
                    minX, yPos, maxX, yPos + moduleElement.settingsHeight, 3.0, Color(20, 20, 20).rgb, Color(40, 40, 40).rgb
                )

                fun clickedNumeric(x1: Int, y1: Int): Boolean =
                    mouseButton == 0 && mouseX in minX..maxX && mouseY in y1 - 2..y1 + 5

                fun renderNumeric(prop: Property<*>, current: Double, min: Double, max: Double, text: String, apply: (Double) -> Unit): Boolean {
                    moduleElement.settingsWidth = font().getStringWidth(text) + 8

                    val x = minX + 4
                    val y = yPos + 14
                    val width = moduleElement.settingsWidth - 12
                    val color = Color(20, 20, 20)

                    val displayValue = current.coerceIn(min, max)
                    val sliderValue = (x + width * (displayValue - min) / (max - min)).roundToInt()

                    if (clickedNumeric(x, y) || sliderValueHeld === prop) {
                        val percentage = ((mouseX - x).toDouble() / width).coerceIn(0.0, 1.0)
                        apply(min + (max - min) * percentage)
                        sliderValueHeld = prop
                        if (mouseButton == 0) return true
                    }

                    RenderUtil.drawRect(x.toDouble(), y.toDouble(), (x + width).toDouble(), (y + 2).toDouble(), Int.MAX_VALUE)
                    RenderUtil.drawRect(x.toDouble(), y.toDouble(), sliderValue.toDouble(), (y + 2).toDouble(), color.rgb)
                    drawFilledCircle(sliderValue, y + 1, 3f, color)

                    font().drawString(text, (minX + 2).toFloat(), (yPos + 3).toFloat(), Color.WHITE.rgb)

                    yPos += 19
                    return false
                }

                fun renderLeaf(value: Property<*>): Boolean {
                    when (value) {
                        is BooleanProperty -> {
                            val text = value.name

                            moduleElement.settingsWidth = font().getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 12) {
                                value.setValue(!value.value)
                                clickSound()
                                return true
                            }

                            font().drawString(
                                text, (minX + 2).toFloat(), (yPos + 2).toFloat(),
                                if (value.value) Color.WHITE.rgb else Int.MAX_VALUE
                            )

                            yPos += 11
                        }

                        is ModeProperty -> {
                            val text = value.name
                            val modes = value.modes

                            moduleElement.settingsWidth = font().getStringWidth(text) + 16

                            var openList = openModeLists.contains(value)
                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + font().getHeight()) {
                                if (openList) openModeLists.remove(value) else openModeLists.add(value)
                                openList = !openList
                                clickSound()
                                return true
                            }

                            font().drawString(text, (minX + 2).toFloat(), (yPos + 2).toFloat(), Color.WHITE.rgb)
                            font().drawString(
                                if (openList) "-" else "+",
                                (maxX - if (openList) 5 else 6).toFloat(), (yPos + 2).toFloat(), Color.WHITE.rgb
                            )

                            yPos += font().getHeight() + 1

                            if (openList) {
                                for (i in modes.indices) {
                                    val label = "> ${modes[i]}"
                                    moduleElement.settingsWidth = font().getStringWidth(label) + 12

                                    if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 9) {
                                        value.setValue(i)
                                        clickSound()
                                        return true
                                    }

                                    font().drawString(
                                        label, (minX + 2).toFloat(), (yPos + 2).toFloat(),
                                        if (value.value == i) Color.WHITE.rgb else Int.MAX_VALUE
                                    )

                                    yPos += font().getHeight() + 1
                                }
                            } else {
                                yPos += 1
                            }
                        }

                        is FloatProperty -> {
                            val text = "${value.name}§f: ${round(value.value)}"
                            if (renderNumeric(value, value.value.toDouble(), value.minimum.toDouble(), value.maximum.toDouble(), text) {
                                    value.setValue(round(it.toFloat()))
                                }) return true
                        }

                        is IntProperty -> {
                            val text = "${value.name}§f: ${value.value}"
                            if (renderNumeric(value, value.value.toDouble(), value.minimum.toDouble(), value.maximum.toDouble(), text) {
                                    value.setValue(it.roundToInt())
                                }) return true
                        }

                        is LongProperty -> {
                            val text = "${value.name}§f: ${value.value}"
                            if (renderNumeric(value, value.value.toDouble(), value.minimum.toDouble(), value.maximum.toDouble(), text) {
                                    value.setValue(it.roundToLong())
                                }) return true
                        }

                        is PercentProperty -> {
                            val text = "${value.name}§f: ${value.value}%"
                            if (renderNumeric(value, value.value.toDouble(), value.minimum.toDouble(), value.maximum.toDouble(), text) {
                                    value.setValue(it.roundToInt())
                                }) return true
                        }

                        is ColorProperty -> {
                            val current = Color(value.value)

                            val spacing = 12
                            val startX = moduleElement.x + moduleElement.width + 4
                            val startY = yPos - 1

                            val previewSize = 9
                            val previewX2 = maxX - previewSize
                            val previewX1 = previewX2 - previewSize
                            val previewY1 = startY + 1
                            val previewY2 = previewY1 + previewSize

                            val textX = startX + 2
                            val textY = startY + 3

                            val barWidth = 60
                            val barHeight = 5
                            val barGap = 2
                            val barsStartY = previewY2 + spacing / 3

                            val startText = "${value.name}: "
                            val valueText = "#%06X".format(current.rgb and 0xFFFFFF)
                            moduleElement.settingsWidth = maxOf(
                                font().getStringWidth(startText + valueText), barWidth + spacing
                            ) + spacing * 2

                            font().drawString(startText + valueText, textX.toFloat(), textY.toFloat(), Color.WHITE.rgb)

                            val hsb = Color.RGBtoHSB(current.red, current.green, current.blue, null)
                            var hue = hsb[0]; var sat = hsb[1]; var bri = hsb[2]

                            if (mouseButton == 0 && mouseX in previewX1..previewX2 && mouseY in previewY1..previewY2) {
                                colorPickersOpen.let { if (!it.remove(value)) it.add(value) }
                                clickSound()
                                return true
                            }

                            drawBorderedRect(previewX1, previewY1, previewX2, previewY2, 1.0, Color.BLUE.rgb, current.rgb)

                            if (colorPickersOpen.contains(value)) {
                                val barX = textX
                                var barY = barsStartY

                                fun colorBar(label: String, v: Float, drawColorAt: (Float) -> Int): Float {
                                    font().drawString(label, (barX - 8).toFloat(), (barY - 1).toFloat(), Color.LIGHT_GRAY.rgb)
                                    for (i in 0 until barWidth) {
                                        val t = i.toFloat() / barWidth
                                        RenderUtil.drawRect(
                                            (barX + i).toDouble(), barY.toDouble(),
                                            (barX + i + 1).toDouble(), (barY + barHeight).toDouble(),
                                            drawColorAt(t)
                                        )
                                    }
                                    val pointerX = barX + (barWidth * v).roundToInt()
                                    RenderUtil.drawRect(
                                        (pointerX - 1).toDouble(), (barY - 1).toDouble(),
                                        (pointerX + 1).toDouble(), (barY + barHeight + 1).toDouble(), Color.WHITE.rgb
                                    )

                                    if (mouseButton == 0 && mouseX in barX..barX + barWidth && mouseY in barY - 1..barY + barHeight + 1 || sliderValueHeld === value) {
                                        sliderValueHeld = value
                                        return ((mouseX - barX).toFloat() / barWidth).coerceIn(0f, 1f)
                                    }
                                    barY += barHeight + barGap
                                    return v
                                }

                                val newHue = colorBar("H", hue) { t -> Color.HSBtoRGB(t, 1f, 1f) }
                                val newSat = colorBar("S", sat) { t -> Color.HSBtoRGB(hue, t, bri) }
                                val newBri = colorBar("B", bri) { t -> Color.HSBtoRGB(hue, sat, t) }

                                if (newHue != hue || newSat != sat || newBri != bri) {
                                    hue = newHue; sat = newSat; bri = newBri
                                    value.setValue(Color.HSBtoRGB(hue, sat, bri) and 0xFFFFFF)
                                    if (mouseButton == 0) return true
                                }

                                yPos = barY + barGap
                            }

                            yPos += spacing
                        }

                        is FileProperty -> {
                            val text = "${value.name}§f: ${value.file.name}"

                            moduleElement.settingsWidth = font().getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 12) {
                                value.openFile()
                                clickSound()
                                return true
                            }

                            font().drawString(text, (minX + 2).toFloat(), (yPos + 2).toFloat(), Color.WHITE.rgb)

                            yPos += 12
                        }

                        else -> {
                            val startText = "${value.name}§f: "
                            val valueText = "${value.value}"

                            moduleElement.settingsWidth = font().getStringWidth(startText + valueText) + 8

                            val textY = yPos + 4
                            val startX = minX + 2

                            if (mouseButton == 0 && mouseX in startX..maxX && mouseY in textY - 2..textY + 6 && value is TextProperty) {
                                GuiInput.prompt(value.name, value.value, { newValue -> value.setValue(newValue) }, FDPClickGui)
                                return true
                            }

                            font().drawString(startText, startX.toFloat(), textY.toFloat(), Color.WHITE.rgb)
                            font().drawString(
                                valueText, (startX + font().getStringWidth(startText)).toFloat(), textY.toFloat(), Color.WHITE.rgb
                            )

                            yPos += 12
                        }
                    }

                    return false
                }

                for (value in properties) {
                    if (renderLeaf(value)) return true
                }

                // Myau+ binds per-module, not per-property (unlike FDPClient's KeyBindValue) -
                // this row mirrors the Bind row every other Myau+ ClickGui skin appends.
                run {
                    val listening = bindingModule === moduleElement
                    val keyName = if (moduleElement.module.key == 0) "NONE" else Keyboard.getKeyName(moduleElement.module.key)
                    val text = "Bind§f: ${if (listening) "Press a key..." else keyName}"
                    moduleElement.settingsWidth = maxOf(moduleElement.settingsWidth, font().getStringWidth(text) + 8)

                    if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 12) {
                        bindingModule = if (listening) null else moduleElement
                        clickSound()
                        return true
                    }

                    font().drawString(text, (minX + 2).toFloat(), (yPos + 2).toFloat(), if (listening) Color.YELLOW.rgb else Color.WHITE.rgb)
                    yPos += 12
                }

                moduleElement.adjustWidth()
                moduleElement.settingsHeight = yPos - moduleElement.y - 6

                if (mouseButton != null && mouseX in minX..maxX && mouseY in moduleElement.y + 6..yPos + 2) return true
            }
        }

        if (mouseButton == -1) {
            sliderValueHeld = null
        }

        return false
    }

    private val openModeLists = HashSet<ModeProperty>()
    private val colorPickersOpen = HashSet<ColorProperty>()
}
