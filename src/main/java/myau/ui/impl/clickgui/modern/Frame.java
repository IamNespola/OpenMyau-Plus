package myau.ui.impl.clickgui.modern;

import lombok.Getter;
import myau.module.Module;
import myau.module.modules.ClickGUIModule;
import myau.ui.impl.clickgui.modern.component.Component;
import myau.ui.impl.clickgui.modern.component.ModuleEntry;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import myau.util.shader.ShadowShader;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Frame extends Component {
    private final String categoryName;
    private final ArrayList<ModuleEntry> moduleEntries;
    private int dragX, dragY;
    private boolean dragging;
    private boolean expanded;
    @Getter
    private float currentHeight;

    public Frame(String categoryName, List<Module> modules, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.dragging = false;
        this.expanded = true;
        this.moduleEntries = new ArrayList<>();
        this.currentHeight = height;
        for (Module module : modules) {
            this.moduleEntries.add(new ModuleEntry(module, x, 0, width, 22));
        }
    }

    public boolean isAnyComponentBinding() {
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                if (entry.isBinding()) return true;
            }
        }
        return false;
    }

    public void renderBlurMask(int scrollOffset) {
        float headerHeight = this.height;
        float listHeight = 0;
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                listHeight += entry.getCurrentHeight();
            }
        }
        float currentH = expanded ? (headerHeight + listHeight) : headerHeight;
        int scrolledY = y - scrollOffset;
        RenderUtil.drawRoundedRect(x, scrolledY, width, currentH, MaterialTheme.CORNER_RADIUS_FRAME, 0xFFFFFFFF, true, true, true, true);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        float headerHeight = this.height;
        float listHeight = 0;
        for (ModuleEntry entry : moduleEntries) {
            listHeight += entry.getCurrentHeight();
        }

        this.currentHeight = expanded ? (headerHeight + listHeight) : headerHeight;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        ClickGUIModule clickGUIModule = (ClickGUIModule) myau.Myau.moduleManager.getModule("ClickGUI");
        boolean shadowEnabled = clickGUIModule != null && clickGUIModule.shadow.getValue();
        if (shadowEnabled) {
            int shadowAlpha = Math.min(120, (int) (alpha * 0.5));
            int shadowColor = new Color(0, 0, 0, shadowAlpha).getRGB();
            ShadowShader.drawShadow(x, scrolledY, width, currentHeight, MaterialTheme.CORNER_RADIUS_FRAME, 12.0f, shadowColor);
        }

        boolean useGlass = clickGUIModule != null && clickGUIModule.glass.getValue();

        // Glassmorphism background
        int alphaFrame = useGlass ? 150 : 220;
        int frameBgColor = new Color(15, 15, 15, (int)(alphaFrame * animationProgress)).getRGB();
        int outlineColor = new Color(255, 255, 255, (int)(40 * animationProgress)).getRGB();

        // Draw translucent background
        RenderUtil.drawRoundedRect(x, scrolledY, width, currentHeight, MaterialTheme.CORNER_RADIUS_FRAME, frameBgColor, true, true, true, true);
        
        // Draw subtle outline
        RenderUtil.drawRoundedRectOutline(x, scrolledY, width, currentHeight, MaterialTheme.CORNER_RADIUS_FRAME, 1.5f, outlineColor, true, true, true, true);

        int textColor = new Color(255, 255, 255, alpha).getRGB();
        if (FontManager.googleSans20 != null) {
            float textY = (float) (scrolledY + (headerHeight - FontManager.googleSans20.getHeight()) / 2f + 1);
            FontManager.googleSans20.drawString(categoryName, x + 8, textY, textColor);
            String displayArrow = expanded ? "-" : "+";
            float arrowW = (float) FontManager.googleSans20.getStringWidth(displayArrow);
            FontManager.googleSans20.drawString(displayArrow, x + width - arrowW - 8, textY, textColor);
        } else {
            mc.fontRendererObj.drawStringWithShadow(categoryName, x + 6, scrolledY + 6, textColor);
        }

        if (expanded) {
            RenderUtil.scissor(x, scrolledY + headerHeight, width, currentHeight - headerHeight);
            int currentModuleY = y + (int) headerHeight;
            for (int i = 0; i < moduleEntries.size(); i++) {
                ModuleEntry entry = moduleEntries.get(i);
                entry.setX(x);
                entry.setY(currentModuleY);
                entry.setWidth(width);
                entry.render(mouseX, mouseY, partialTicks, animationProgress, i == moduleEntries.size() - 1, scrollOffset, deltaTime);
                currentModuleY += (int) entry.getCurrentHeight();
            }
            RenderUtil.releaseScissor();
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        render(mouseX, mouseY, partialTicks, animationProgress, isLast, scrollOffset, 0.016f);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return mouseClicked(mouseX, mouseY, mouseButton, 0);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                this.dragging = true;
                this.dragX = mouseX - this.x;
                this.dragY = mouseY - this.y;
                return true;
            } else if (mouseButton == 1) {
                expanded = !expanded;
                return true;
            }
        }
        if (expanded) {
            if (currentHeight <= height) return false;
            for (ModuleEntry entry : moduleEntries) {
                if (entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        mouseReleased(mouseX, mouseY, mouseButton, 0);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        this.dragging = false;
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                entry.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
            }
        }
    }

    public void updatePosition(int mouseX, int mouseY) {
        if (this.dragging) {
            this.x = mouseX - this.dragX;
            this.y = mouseY - this.dragY;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                entry.keyTyped(typedChar, keyCode);
            }
        }
    }
}
