package myau.ui.impl.clickgui.clean;

import myau.module.Module;
import myau.ui.impl.clickgui.normal.component.Component;
import myau.util.RenderUtil;
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
    private boolean headerClicked;
    private float currentHeight;

    public CleanFrame(String categoryName, List<Module> modules, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.currentHeight = height;
        for (Module module : modules) moduleEntries.add(new CleanModuleEntry(module, x, 0, width, 12));
        applySavedState();
    }

    public CleanFrame(String categoryName, List<String> configs, int x, int y, int width, int height, boolean configFrame) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.currentHeight = height;
        for (String config : configs) configEntries.add(new CleanConfigEntry(config, x, 0, width, 12));
        applySavedState();
    }

    private void applySavedState() {
        CleanGuiState.FrameState state = CleanGuiState.get(categoryName);
        if (state != null) {
            this.x = state.x;
            this.y = state.y;
            this.expanded = state.expanded;
        }
    }

    public void saveState() {
        CleanGuiState.put(categoryName, x, y, expanded);
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean wasHeaderClicked() {
        return headerClicked;
    }

    public boolean isAnyComponentBinding() {
        if (!expanded) return false;
        for (CleanModuleEntry entry : moduleEntries) if (entry.isBinding()) return true;
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        render(mouseX, mouseY, partialTicks, animationProgress, isLast, scrollOffset, deltaTime, "");
    }

    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime, String searchQuery) {
        boolean searching = searchQuery != null && !searchQuery.trim().isEmpty();
        if (searching && !hasMatches(searchQuery)) {
            currentHeight = 0.0F;
            return;
        }
        int scrolledY = y - scrollOffset;
        currentHeight = height;
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) if (entry.matches(searchQuery)) currentHeight += entry.getCurrentHeight();
            for (CleanConfigEntry entry : configEntries) if (entry.matches(searchQuery)) currentHeight += entry.getCurrentHeight();
        }
        int alpha = (int) (255 * animationProgress);
        int panel = CleanTheme.withAlpha(CleanTheme.PANEL, alpha);
        int header = CleanTheme.withAlpha(CleanTheme.PANEL_DARK, alpha);
        RenderUtil.drawRoundedRect(x, scrolledY, width, Math.max(height, currentHeight), 4.0F, panel, true, true, true, true);
        RenderUtil.drawRoundedRect(x, scrolledY, width, height, 4.0F, header, true, true, !expanded, !expanded);
        Gui.drawRect(x, scrolledY + 2, x + 2, scrolledY + height - 2, CleanTheme.withAlpha(CleanTheme.ACCENT, alpha));
        mc.fontRendererObj.drawStringWithShadow(categoryName, x + 7, scrolledY + 3, CleanTheme.withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(expanded ? "-" : "+", x + width - 10, scrolledY + 3, CleanTheme.withAlpha(CleanTheme.MUTED, alpha));
        if (!expanded) return;
        int currentY = y + height;
        for (CleanModuleEntry entry : moduleEntries) {
            if (!entry.matches(searchQuery)) continue;
            entry.setX(x + 2);
            entry.setY(currentY);
            entry.setWidth(width - 4);
            entry.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
            currentY += (int) entry.getCurrentHeight();
        }
        for (CleanConfigEntry entry : configEntries) {
            if (!entry.matches(searchQuery)) continue;
            entry.setX(x + 2);
            entry.setY(currentY);
            entry.setWidth(width - 4);
            entry.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
            currentY += (int) entry.getCurrentHeight();
        }
    }

    private boolean hasMatches(String searchQuery) {
        for (CleanModuleEntry entry : moduleEntries) if (entry.matches(searchQuery)) return true;
        for (CleanConfigEntry entry : configEntries) if (entry.matches(searchQuery)) return true;
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        return mouseClicked(mouseX, mouseY, mouseButton, scrollOffset, "");
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset, String searchQuery) {
        headerClicked = false;
        if (searchQuery != null && !searchQuery.trim().isEmpty() && !hasMatches(searchQuery)) return false;
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            headerClicked = true;
            if (mouseButton == 0) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
                return true;
            } else if (mouseButton == 1) {
                expanded = !expanded;
                saveState();
                CleanGuiState.save();
                return true;
            }
        }
        if (expanded) {
            for (CleanModuleEntry entry : moduleEntries) if (entry.matches(searchQuery) && entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
            for (CleanConfigEntry entry : configEntries) if (entry.matches(searchQuery) && entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
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
        if (dragging) {
            dragging = false;
            saveState();
            CleanGuiState.save();
        }
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
