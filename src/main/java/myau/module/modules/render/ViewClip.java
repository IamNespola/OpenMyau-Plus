package myau.module.modules.render;

import myau.module.Category;
import myau.module.Module;
import net.minecraft.client.Minecraft;

public class ViewClip extends Module {

    public ViewClip() {
        super("ViewClip",Category.RENDER, false);
    }

    @Override
    public void onEnabled() {
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
