/*
 * Ported from FDPClient's clickgui/Panel.kt. FDPClient reads its layout knobs (fadeSpeed,
 * maxElements, panelsForcedInBoundaries, scale) from its own ClickGUIModule; those are
 * skin-internal tuning here rather than user-facing Myau+ properties, so they're constants.
 */
package myau.ui.impl.clickgui.fdp

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val FADE_SPEED = 1f
private const val MAX_ELEMENTS = 200
private const val PANELS_FORCED_IN_BOUNDARIES = true
private const val SCALE = 1f

fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max.coerceAtLeast(0))

class Panel(
    val name: String,
    var x: Int,
    var y: Int,
    val width: Int,
    val height: Int,
    var open: Boolean,
    val elements: List<Element>
) {
    private val mc = Minecraft.getMinecraft()

    var x2 = 0
    var y2 = 0

    private var updatePos = false

    fun parseX(value: Int = x): Int {
        if (!PANELS_FORCED_IN_BOUNDARIES)
            return value

        val settingsWidth =
            if (open || FDPClickGui.search.active) displayedElements().filterIsInstance<ModuleElement>()
                .maxOfOrNull { if (it.showSettings) it.settingsWidth else 0 } ?: 0
            else 0

        return value.clamp(0, (ScaledResolution(mc).scaledWidth / SCALE - width - settingsWidth).roundToInt())
    }
    fun parseY(value: Int = y): Int {
        if (!PANELS_FORCED_IN_BOUNDARIES)
            return value

        var yPos = height + 4
        var panelHeight = height + fade

        if (open || FDPClickGui.search.active)
            for (element in displayedElements()) {
                if (element.isVisible) {
                    if (element is ModuleElement && element.showSettings && element.settingsHeight != 0) {
                        val relativeSettingsHeight = yPos + element.settingsHeight
                        if (relativeSettingsHeight > panelHeight) panelHeight = relativeSettingsHeight
                    }
                    yPos += element.height + 1
                }
            }

        return value.clamp(0, (ScaledResolution(mc).scaledHeight / SCALE - panelHeight).roundToInt())
    }

    var drag = false
    val scrollbar
        get() = displayedElements().size > MAX_ELEMENTS

    var isVisible = true
    var fade = 0
        set(value) {
            val parsed = value.clamp(0, elementsHeight)

            if (parsed != field) {
                // Update panel pos not to extend beyond border.
                field = parsed

                x = parseX()
                y = parseY()
            }
        }

    private var elementsHeight = 0

    private var scroll = 0
        set(value) {
            // How many elements should be hidden
            val hiddenCount = displayedElements().size - MAX_ELEMENTS
            // Don't overscroll
            field = if (hiddenCount > 0) min(hiddenCount, value.coerceAtLeast(0)) else 0
        }

    fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int? = null): Boolean {
        if (!isVisible) return false

        val displayedElements = displayedElements()
        if (FDPClickGui.search.active && displayedElements.isEmpty()) {
            elements.forEach { it.isVisible = false }
            return false
        }

        updateElementsHeight()

        // Drag
        if (drag) {
            x = parseX(x2 + mouseX)
            y = parseY(y2 + mouseY)
        }

        FDPClickGui.style.drawPanel(mouseX, mouseY, this)

        var yPos = y + height - 2

        val visibleRange = getVisibleRange()

        elements.forEach { it.isVisible = false }
        displayedElements.forEachIndexed { index, element ->
            if (index in visibleRange) {
                element.isVisible = true
                element.setLocation(x, yPos)
                element.width = width

                // If mouse wasn't hovering above any ButtonElement, drawScreenAndClick got called with mouseButton != null.
                // Mouse was detected to be hovering above a value while rendering it.
                // True was returned to stop any further values from getting clicked.
                if (yPos <= y + fade && element.drawScreenAndClick(mouseX, mouseY, mouseButton)) {
                    // Update panel pos not to extend beyond border.
                    updatePos = true
                    return true
                }

                yPos += element.height + 1
                element.isVisible = true
            } else element.isVisible = false
        }

        // Position is updated on next draw calls because ModuleElement.settingsHeight gets updated on next draw call.
        // Updated to prevent long settings lists from extending beyond window boundaries.
        if (updatePos) {
            x = parseX()
            y = parseY()
            updatePos = false
        }

        return false
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isVisible) return false
        if (FDPClickGui.search.active && displayedElements().isEmpty()) return false

        if (mouseButton == 1 && isHovered(mouseX, mouseY)) {
            open = !open
            FDPClickGui.style.showSettingsSound()
            return true
        }

        if (displayedElements().any { it.y <= y + fade && it.mouseClicked(mouseX, mouseY, mouseButton) }) {
            // Update panel pos not to extend beyond border.
            updatePos = true
            return true
        }

        return drawScreenAndClick(mouseX, mouseY, mouseButton)
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (!isVisible) return false

        drag = false

        if (!open && !FDPClickGui.search.active) return false

        return displayedElements().any { it.y <= y + fade && it.mouseReleased(mouseX, mouseY, button) }
    }

    fun handleScroll(mouseX: Int, mouseY: Int, wheel: Int): Boolean {
        if (FDPClickGui.search.active && displayedElements().isEmpty()) return false
        if (mouseX in x..x + width && mouseY in y..y + height + elementsHeight) {
            if (wheel < 0) scroll++
            else scroll--

            return true
        }
        return false
    }

    fun updateFade(delta: Int) {
        fade += ((if (open || FDPClickGui.search.active) 0.4f else -0.4f) * delta * FADE_SPEED).roundToInt()
    }

    private fun updateElementsHeight() {
        var height = 0

        for ((count, element) in displayedElements().withIndex()) {
            if (count >= MAX_ELEMENTS) break
            height += element.height + 1
        }

        elementsHeight = height
    }

    fun getVisibleRange(): IntRange {
        val size = displayedElements().size
        if (size == 0) return 1..0

        val first = scroll.coerceIn(0, max(size - MAX_ELEMENTS, 0))
        return first..min(size - 1, first + MAX_ELEMENTS - 1)
    }

    private fun displayedElements(): List<Element> {
        if (!FDPClickGui.search.active) return elements
        return elements.filter { element ->
            element is ModuleElement && FDPClickGui.search.matches(element.module)
        }
    }

    fun isHovered(mouseX: Int, mouseY: Int) = mouseX in x..x + width && mouseY in y..y + height

    override fun hashCode(): Int = this.name.hashCode()

    override fun equals(other: Any?): Boolean = other is Panel && other.name == this.name
}
