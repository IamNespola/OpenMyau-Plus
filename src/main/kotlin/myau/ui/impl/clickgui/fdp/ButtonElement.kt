/*
 * Ported from FDPClient's clickgui elements/ButtonElement.kt. FDPClient's "auto settings" cloud
 * panel (the only thing that ever constructed a bare ButtonElement) has no Myau+ equivalent and
 * was removed - this now exists purely as ModuleElement's base class, as in the original.
 */
package myau.ui.impl.clickgui.fdp

open class ButtonElement(
    open val displayName: String,
    val stateDependingColor: () -> Int = { Int.MAX_VALUE }
) : Element() {

    val color
        get() = stateDependingColor()

    open var hoverText: String = ""

    var hoverTime = 0
        set(value) {
            field = value.coerceIn(0, 7)
        }

    override val height = 16

    override fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int?): Boolean {
        FDPClickGui.style.drawButtonElement(mouseX, mouseY, this)
        return false
    }
}
