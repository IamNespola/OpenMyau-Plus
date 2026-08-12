/*
 * Ported from FDPClient's clickgui/style/Style.kt. Dropped: TickScheduler-delayed config saves
 * (this port persists panel layout only, like every other Myau+ skin, not per-property state)
 * and EditableText RGBA-index editing (ColorValue's port here has no rainbow/alpha, per
 * ColorProperty - see BlackStyle.kt).
 */
package myau.ui.impl.clickgui.fdp

import myau.property.Property
import myau.util.SoundUtil
import org.lwjgl.input.Mouse
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

abstract class Style {

    var sliderValueHeld: Property<*>? = null
        get() {
            if (!Mouse.isButtonDown(0)) field = null
            return field
        }

    abstract fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
    abstract fun drawHoverText(mouseX: Int, mouseY: Int, text: String)
    abstract fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
    abstract fun drawModuleElementAndClick(
        mouseX: Int,
        mouseY: Int,
        moduleElement: ModuleElement,
        mouseButton: Int?
    ): Boolean

    fun clickSound() {
        SoundUtil.playSound("gui.button.press")
    }

    fun showSettingsSound() {
        SoundUtil.playSound("random.bow")
    }

    protected fun round(v: Float): Float {
        val bigDecimal = BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP)
        return bigDecimal.toFloat()
    }

    protected fun getHoverColor(color: java.awt.Color, hover: Int, inactiveModule: Boolean = false): Int {
        val r = color.red - hover * 2
        val g = color.green - hover * 2
        val b = color.blue - hover * 2
        val alpha = if (inactiveModule) color.alpha.coerceAtMost(128) else color.alpha

        return java.awt.Color(max(r, 0), max(g, 0), max(b, 0), alpha).rgb
    }
}
