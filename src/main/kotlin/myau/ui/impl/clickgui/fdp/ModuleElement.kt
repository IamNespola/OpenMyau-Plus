/*
 * Ported from FDPClient's clickgui elements/ModuleElement.kt, rewired from FDPClient's own
 * Module/ValueDispatcher onto myau.module.Module and Myau.propertyManager.
 */
package myau.ui.impl.clickgui.fdp

import myau.Myau
import myau.module.Module

class ModuleElement(val module: Module) : ButtonElement(module.name) {
    override val displayName
        get() = module.name

    override var hoverText = ""
        get() = module.description ?: ""

    var showSettings = false
    var disableFiltering = false

    var supposedWidth = 0
    var settingsWidth = 0
        set(value) {
            if (value > settingsWidth || disableFiltering) {
                disableFiltering = false
                field = value
            }

            if (value > supposedWidth) {
                supposedWidth = value
            }
        }

    var settingsHeight = 0

    var slowlyFade = 0
        set(value) {
            field = value.coerceIn(0, 255)
        }

    private fun hasVisibleSettings(): Boolean =
        Myau.propertyManager.properties[module]?.any { it.isVisible } == true

    override fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int?): Boolean {
        this.supposedWidth = 0

        return FDPClickGui.style.drawModuleElementAndClick(mouseX, mouseY, this, mouseButton)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) {
            return false
        }

        when (mouseButton) {
            0 -> {
                module.toggle()
                FDPClickGui.style.clickSound()
            }
            1 -> {
                if (hasVisibleSettings()) {
                    showSettings = !showSettings
                    FDPClickGui.style.showSettingsSound()
                }
            }
        }

        return true
    }

    fun adjustWidth() {
        if (settingsWidth - supposedWidth > 16) {
            disableFiltering = true
            settingsWidth = supposedWidth
        }
    }

}
