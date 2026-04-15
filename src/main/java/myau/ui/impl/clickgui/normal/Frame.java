package myau.ui.impl.clickgui.normal;

import lombok.Getter;
import myau.module.Module;
import myau.module.modules.render.ClickGUIModule;
import myau.ui.impl.clickgui.normal.component.Component;
import myau.ui.impl.clickgui.normal.component.ModuleEntry;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;
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

    private float scrollY = 0;
    private float targetScrollY = 0;
    private final int MAX_VISIBLE_HEIGHT = 280;

    public Frame(String categoryName, List<Module> modules, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.categoryName = categoryName;
        this.dragging = false;
        this.expanded = true;
        this.moduleEntries = new ArrayList<>();
        this.currentHeight = height;
        for (Module module : modules) {
            this.moduleEntries.add(new ModuleEntry(module, x, 0, width, 22, y));
        }
    }

    public void handleScroll(int wheel) {
        if (expanded) {
            this.targetScrollY += (wheel > 0 ? -35 : 35);
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

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        float headerHeight = this.height;
        float totalListHeight = 0;
        for (ModuleEntry entry : moduleEntries) {
            totalListHeight += entry.getCurrentHeight();
        }

        this.currentHeight = expanded ? Math.min(headerHeight + totalListHeight, MAX_VISIBLE_HEIGHT) : headerHeight;

        float maxScroll = Math.max(0, totalListHeight - (MAX_VISIBLE_HEIGHT - headerHeight));
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));
        scrollY += (targetScrollY - scrollY) * 0.15f;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        ClickGUIModule clickGUIModule = (ClickGUIModule) myau.Myau.moduleManager.getModule("ClickGUI");
        
        boolean shadowEnabled = clickGUIModule != null && clickGUIModule.shadow.getValue();
        if (shadowEnabled) {
            ShadowShader.drawShadow(x, scrolledY, width, currentHeight, MaterialTheme.CORNER_RADIUS_FRAME, 12.0f, new Color(0, 0, 0, (int)(alpha * 0.45)).getRGB());
        }

        float radius = MaterialTheme.CORNER_RADIUS_FRAME;
        Shader2D.drawRoundedRect(x, scrolledY, width, currentHeight, radius, new Color(20, 20, 20, alpha));

        if (expanded) {
            float contentH = currentHeight - headerHeight;
            Shader2D.drawRoundedRect(x, scrolledY + headerHeight, width, contentH, radius, new Color(28, 28, 28, alpha));
            Shader2D.drawRoundedRect(x, scrolledY + headerHeight - 1, width, 1, 0, new Color(255, 255, 255, (int)(alpha * 0.1)));

            RenderUtil.scissor(x, scrolledY + (int)headerHeight, width, (int)contentH);
            
            float currentModuleY = scrolledY + headerHeight - scrollY;
            for (int i = 0; i < moduleEntries.size(); i++) {
                ModuleEntry entry = moduleEntries.get(i);
                entry.setX(x);
                entry.setY((int) currentModuleY);
                entry.setWidth(width);
                
                if (currentModuleY + entry.getCurrentHeight() > scrolledY + headerHeight && currentModuleY < scrolledY + currentHeight) {
                    entry.render(mouseX, mouseY, partialTicks, animationProgress, i == moduleEntries.size() - 1, 0, deltaTime);
                }
                currentModuleY += entry.getCurrentHeight();
            }
            RenderUtil.releaseScissor();
        }

        int textColor = new Color(255, 255, 255, alpha).getRGB();
        if (FontManager.productSans20 != null) {
            float textY = (float) (scrolledY + (headerHeight - FontManager.productSans20.getHeight()) / 2f + 1);
            FontManager.productSans20.drawString(categoryName, x + 8, textY, textColor);
            
            String displayArrow = expanded ? "-" : "+";
            float arrowW = (float) FontManager.productSans20.getStringWidth(displayArrow);
            FontManager.productSans20.drawString(displayArrow, x + width - arrowW - 8, textY, textColor);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        render(mouseX, mouseY, partialTicks, animationProgress, isLast, scrollOffset, 0.016f);
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
                if (!expanded) {
                    targetScrollY = 0;
                    scrollY = 0;
                }
                return true;
            }
        }
        
        if (expanded) {
            int scrolledY = this.y - scrollOffset;
            if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY + height && mouseY <= scrolledY + currentHeight) {
                for (ModuleEntry entry : moduleEntries) {
                    if (entry.mouseClicked(mouseX, mouseY, mouseButton, 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return mouseClicked(mouseX, mouseY, mouseButton, 0);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        this.dragging = false;
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                entry.mouseReleased(mouseX, mouseY, mouseButton, 0);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        mouseReleased(mouseX, mouseY, mouseButton, 0);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                entry.keyTyped(typedChar, keyCode);
            }
        }
    }

    public boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    public void updatePosition(int mouseX, int mouseY) {
        if (this.dragging) {
            this.x = mouseX - this.dragX;
            this.y = mouseY - this.dragY;
        }
    }
}