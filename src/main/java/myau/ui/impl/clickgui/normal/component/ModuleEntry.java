package myau.ui.impl.clickgui.normal.component;

import lombok.Getter;
import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleEntry extends Component {
    @Getter
    private final Module module;
    private final List<Component> propertiesComponents;
    private boolean expanded;
    private float hoverOpacity = 0f;
    private float currentSettingsHeight = 0f;
    private int currentColor;

    public ModuleEntry(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        this.expanded = false;
        this.propertiesComponents = new ArrayList<>();
        this.currentColor = MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        initializePropertiesComponents();
    }

    private void initializePropertiesComponents() {
        int currentY = y + height;

        KeybindComponent keybindComp = new KeybindComponent(module, x, currentY, width, 20);
        propertiesComponents.add(keybindComp);

        if (Myau.propertyManager != null) {
            List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
            if (properties != null) {
                for (Property<?> property : properties) {
                    Component comp = null;
                    int compHeight = 20;

                    if (property instanceof BooleanProperty) {
                        comp = new Switch((BooleanProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                        comp = new Slider(property, x, currentY, width, compHeight);
                    } else if (property instanceof ModeProperty) {
                        comp = new Dropdown((ModeProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof MultiModeProperty) {
                        comp = new MultiDropdown((MultiModeProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof ColorProperty) {
                        comp = new ColorPicker((ColorProperty) property, x, currentY, width, 60);
                    } else if (property instanceof TextProperty) {
                        comp = new TextField((TextProperty) property, x, currentY, width, compHeight);
                    }

                    if (comp != null) {
                        propertiesComponents.add(comp);
                    }
                }
            }
        }
    }

    private boolean isComponentVisible(Component comp) {
        if (comp instanceof Switch) return ((Switch) comp).getProperty().isVisible();
        if (comp instanceof Slider) return ((Slider) comp).getProperty().isVisible();
        if (comp instanceof Dropdown) return ((Dropdown) comp).getProperty().isVisible();
        if (comp instanceof MultiDropdown) return ((MultiDropdown) comp).getProperty().isVisible();
        if (comp instanceof ColorPicker) return ((ColorPicker) comp).getProperty().isVisible();
        if (comp instanceof TextField) return ((TextField) comp).getProperty().isVisible();
        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        boolean hovered = isMouseOverHeader(mouseX, mouseY, scrollOffset);
        int alpha = (int) (255 * animationProgress);

        float targetHover = hovered ? 1.0f : 0.0f;
        this.hoverOpacity = AnimationUtil.animateSmooth(targetHover, this.hoverOpacity, 10.0f, deltaTime);
        if (hoverOpacity > 0.01f) {
            int hoverColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.SURFACE_CONTAINER_HIGH, (int) (alpha * hoverOpacity));
            RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, height, 4, hoverColor, true, true, true, true);
        }

        myau.module.modules.ClickGUIModule clickGUI = (myau.module.modules.ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");
        java.awt.Color accent = (clickGUI != null) ? clickGUI.getAccentColor() : MaterialTheme.PRIMARY_COLOR;
        int targetColor = module.isEnabled() ? MaterialTheme.getRGB(accent) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        this.currentColor = AnimationUtil.interpolateColor(this.currentColor, targetColor, 10.0f * deltaTime);
        int finalTextColor = (this.currentColor & 0x00FFFFFF) | (alpha << 24);

        if (alpha > 5) {
            if (FontManager.productSans16 != null) {
                float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f + 1);
                FontManager.productSans16.drawString(module.getName(), x + 10, textY, finalTextColor);
                if (!propertiesComponents.isEmpty()) {
                    String icon = expanded ? "..." : ":";
                    float iconW = (float) FontManager.productSans16.getStringWidth(icon);
                    FontManager.productSans16.drawString(icon, x + width - iconW - 8, textY, MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha));
                }
            } else {
                mc.fontRendererObj.drawStringWithShadow(module.getName(), x + 8, scrolledY + 6, finalTextColor);
            }
            if (module.isEnabled()) {
                RenderUtil.drawRoundedRect(x + 4, scrolledY + height / 2f - 1.5f, 3, 3, 1.5f, finalTextColor, true, true, true, true);
            }
        }

        float visibleHeightSum = 0;
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    visibleHeightSum += comp.getHeight();
                }
            }
        }

        this.currentSettingsHeight = AnimationUtil.animateSmooth(visibleHeightSum, this.currentSettingsHeight, 12.0f, deltaTime);

        if (currentSettingsHeight > 1.0f) {
            float bgLeft = x + 2;
            float bgTop = scrolledY + height;
            float bgRight = x + width - 2;
            float bgBottom = bgTop + currentSettingsHeight;

            RenderUtil.drawRect(bgLeft, bgTop, bgRight, bgBottom, new Color(10, 10, 12, (int) (100 * (alpha / 255f))).getRGB());

            RenderUtil.scissor(x, scrolledY + height, width, currentSettingsHeight);

            float dynamicY = y + height;

            for (int i = 0; i < propertiesComponents.size(); i++) {
                Component comp = propertiesComponents.get(i);

                if (!isComponentVisible(comp)) continue;

                float relativeY = dynamicY - (y + height);
                if (relativeY < currentSettingsHeight) {
                    comp.setX(x + 4);
                    comp.setY((int) dynamicY);
                    comp.setWidth(width - 8);

                    comp.render(mouseX, mouseY, partialTicks, animationProgress, isLast && (i == propertiesComponents.size() - 1), scrollOffset, deltaTime);
                }

                dynamicY += comp.getHeight();
            }
            RenderUtil.releaseScissor();
        }
    }

    public float getCurrentHeight() {
        float heightSum = height;
        if (expanded || currentSettingsHeight > 0) {
            heightSum += currentSettingsHeight;
        }
        return heightSum;
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                module.toggle();
                return true;
            } else if (mouseButton == 1) {
                if (!propertiesComponents.isEmpty()) {
                    expanded = !expanded;
                }
                return true;
            }
        }

        if (expanded) {
            if (currentSettingsHeight < 10) return false;

            for (Component comp : propertiesComponents) {
                if (!isComponentVisible(comp)) continue;

                if (comp.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBinding() {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (!isComponentVisible(comp)) continue;
                if (comp instanceof KeybindComponent && ((KeybindComponent) comp).isBinding()) return true;
            }
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    comp.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    comp.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
                }
            }
        }
    }
}
