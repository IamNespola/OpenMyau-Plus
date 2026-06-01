package myau.ui.impl.clickgui.clean;

import myau.module.Module;
import myau.ui.impl.clickgui.normal.component.Component;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.List;

public class CleanFrame extends Component {
    private final String categoryName;
    private final ArrayList<CleanModuleEntry> moduleEntries = new ArrayList<>();
    private final ArrayList<CleanConfigEntry> configEntries = new ArrayList<>();
    private int dragX;
    private int dragY;
    private boolean dragging;
    private boolean expanded = true;
    private float currentHeight;

    public CleanFrame(String categoryName, List<Module> modules, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.currentHeight = height;
        for (Module module : modules) moduleEntries.add(new CleanModuleEntry(module, x, 0, width, 12));
    }

    public CleanFrame(String categoryName, List<String> configs, int x, int y, int width, int height, boolean configFrame) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.currentHeight = height;
        for (String config : configs) configEntries.add(new CleanConfigEntry(config, x, 0, width, 12));
    }

    public boolean isAnyComponentBinding() {
        if (!expanded) return false;
        for (CleanModuleEntry entry : moduleEntries) if (entry.isBinding()) return true;
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        currentHeight = height;
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) currentHeight += entry.getCurrentHeight();
            for (CleanConfigEntry entry : configEntries) currentHeight += entry.getCurrentHeight();
        }
        int alpha = (int) (255 * animationProgress);
        Gui.drawRect(x, scrolledY, x + width, scrolledY + (int) currentHeight, withAlpha(CleanTheme.PANEL, alpha));
        Gui.drawRect(x, scrolledY, x + width, scrolledY + height, withAlpha(CleanTheme.PANEL_DARK, alpha));
        mc.fontRendererObj.drawStringWithShadow(categoryName, x + 5, scrolledY + 3, withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(expanded ? "-" : "+", x + width - 9, scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));
        if (!expanded) return;
        int currentY = y + height;
        for (CleanModuleEntry entry : moduleEntries) {
            entry.setX(x);
            entry.setY(currentY);
            entry.setWidth(width);
            entry.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
            currentY += (int) entry.getCurrentHeight();
        }
        for (CleanConfigEntry entry : configEntries) {
            entry.setX(x);
            entry.setY(currentY);
            entry.setWidth(width);
            entry.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
            currentY += (int) entry.getCurrentHeight();
        }
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
                return true;
            } else if (mouseButton == 1) {
                expanded = !expanded;
                return true;
            }
        }
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) if (entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
            for (CleanConfigEntry entry : configEntries) if (entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
        }
        return false;
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        dragging = false;
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) entry.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
            for (CleanConfigEntry entry : configEntries) entry.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
        }
    }

    public void updatePosition(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) entry.keyTyped(typedChar, keyCode);
            for (CleanConfigEntry entry : configEntries) entry.keyTyped(typedChar, keyCode);
        }
    }

    public float getCurrentHeight() {
        return currentHeight;
    }
}

