/*
 * Ported from FDPClient's (LiquidBounce-derived) classic clickgui, rewired onto Myau+'s own
 * module/property system. See UPSTREAM.md in this package for provenance and what was cut.
 */
package myau.ui.impl.clickgui.fdp

abstract class Element {

    var x = 0
    var y = 0
    var width = 0
    abstract val height: Int
    var isVisible = true

    fun setLocation(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    abstract fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int? = null): Boolean
    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) = isHovered(mouseX, mouseY)

    open fun mouseReleased(mouseX: Int, mouseY: Int, button: Int) = isHovered(mouseX, mouseY)

    fun isHovered(mouseX: Int, mouseY: Int) = isVisible && mouseX in x..x + width && mouseY in y..y + height
}
