package myau.ui.impl.clickgui.clean;

import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.normal.component.ColorPicker;
import myau.ui.impl.clickgui.normal.component.Component;
import myau.ui.impl.clickgui.normal.component.Dropdown;
import myau.ui.impl.clickgui.normal.component.KeybindComponent;
import myau.ui.impl.clickgui.normal.component.MultiDropdown;
import myau.ui.impl.clickgui.normal.component.Slider;
import myau.ui.impl.clickgui.normal.component.Switch;
import myau.ui.impl.clickgui.normal.component.TextField;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.List;

public class CleanModuleEntry extends Component {
    private final Module module;
    private final List<Component> propertyComponents = new ArrayList<>();
    private boolean expanded;
    private float currentSettingsHeight;

    public CleanModuleEntry(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        initializePropertyComponents();
    }

    private void initializePropertyComponents() {
        int currentY = y + height;
        KeybindComponent keybind = new KeybindComponent(module, x, currentY, width, 22);
        propertyComponents.add(keybind);
        currentY += keybind.getHeight();
        if (Myau.propertyManager == null) return;
        ArrayList<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties == null) return;
        for (Property<?> property : properties) {
            Component component = null;
            if (property instanceof BooleanProperty) {
                component = new Switch((BooleanProperty) property, x, currentY, width, 22);
            } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                component = new Slider(property, x, currentY, width, 26);
            } else if (property instanceof ModeProperty) {
                component = new Dropdown((ModeProperty) property, x, currentY, width, 22);
            } else if (property instanceof MultiModeProperty) {
                component = new MultiDropdown((MultiModeProperty) property, x, currentY, width, 22);
            } else if (property instanceof ColorProperty) {
                component = new ColorPicker((ColorProperty) property, x, currentY, width, 60);
            } else if (property instanceof TextProperty) {
                component = new TextField((TextProperty) property, x, currentY, width, 22);
            }
            if (component != null) {
                propertyComponents.add(component);
                currentY += component.getHeight();
            }
        }
    }

    private boolean isComponentVisible(Component component) {
        if (component instanceof Switch) return ((Switch) component).getProperty().isVisible();
        if (component instanceof Slider) return ((Slider) component).getProperty().isVisible();
        if (component instanceof Dropdown) return ((Dropdown) component).getProperty().isVisible();
        if (component instanceof MultiDropdown) return ((MultiDropdown) component).getProperty().isVisible();
        if (component instanceof ColorPicker) return ((ColorPicker) component).getProperty().isVisible();
        if (component instanceof TextField) return ((TextField) component).getProperty().isVisible();
        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        boolean hovered = isMouseOverHeader(mouseX, mouseY, scrollOffset);
        if (hovered) RenderUtil.drawRoundedRect(x + 1, scrolledY, width - 2, height, 3.0F, CleanTheme.withAlpha(CleanTheme.ROW_HOVER, alpha), true, true, true, true);
        if (module.isEnabled()) Gui.drawRect(x + 1, scrolledY + 2, x + 3, scrolledY + height - 2, CleanTheme.withAlpha(CleanTheme.ACCENT, alpha));
        String moduleName = trimToWidth(module.getName(), width - (!propertyComponents.isEmpty() ? 18 : 9));
        int textColor = module.isEnabled() ? CleanTheme.TEXT_ACTIVE : 0xFFBDBDBD;
        mc.fontRendererObj.drawStringWithShadow(moduleName, x + 6, scrolledY + 3, CleanTheme.withAlpha(textColor, alpha));
        if (!propertyComponents.isEmpty()) mc.fontRendererObj.drawStringWithShadow(expanded ? "<" : ">", x + width - 10, scrolledY + 3, CleanTheme.withAlpha(CleanTheme.MUTED, alpha));

        float targetSettingsHeight = 0.0F;
        if (expanded) {
            for (Component component : propertyComponents) if (isComponentVisible(component)) targetSettingsHeight += component.getHeight();
        }
        currentSettingsHeight = AnimationUtil.animateSmooth(targetSettingsHeight, currentSettingsHeight, 12.0F, deltaTime);

        if (currentSettingsHeight > 1.0F) {
            RenderUtil.drawRoundedRect(x + 3, scrolledY + height, width - 5, currentSettingsHeight, 3.0F, CleanTheme.withAlpha(CleanTheme.PANEL_ELEVATED, alpha), false, false, true, true);
            float componentY = y + height;
            for (Component component : propertyComponents) {
                if (!isComponentVisible(component)) continue;
                if (componentY - (y + height) < currentSettingsHeight) {
                    component.setX(x + 4);
                    component.setY((int) componentY);
                    component.setWidth(width - 8);
                    component.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
                }
                componentY += component.getHeight();
            }
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0 || mc.fontRendererObj.getStringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = mc.fontRendererObj.getStringWidth(ellipsis);
        if (maxWidth <= ellipsisWidth) return ellipsis;
        String trimmed = text;
        while (!trimmed.isEmpty() && mc.fontRendererObj.getStringWidth(trimmed) + ellipsisWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    public float getCurrentHeight() {
        return height + currentSettingsHeight;
    }

    public boolean matches(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) return true;
        String query = searchQuery.trim().toLowerCase();
        if (module.getName().toLowerCase().contains(query)) return true;
        if (module.getDescription() != null && module.getDescription().toLowerCase().contains(query)) return true;
        if (Myau.propertyManager == null) return false;
        ArrayList<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties == null) return false;
        for (Property<?> property : properties) {
            if (property.getName().toLowerCase().contains(query)) return true;
        }
        return false;
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
                if (!propertyComponents.isEmpty()) expanded = !expanded;
                return true;
            }
        }
        if (expanded && currentSettingsHeight >= 10.0F) {
            for (Component component : propertyComponents) {
                if (!isComponentVisible(component)) continue;
                if (component.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
            }
        }
        return false;
    }

    public boolean isBinding() {
        if (!expanded) return false;
        for (Component component : propertyComponents) {
            if (!isComponentVisible(component)) continue;
            if (component instanceof KeybindComponent && ((KeybindComponent) component).isBinding()) return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (expanded) {
            for (Component component : propertyComponents) {
                if (isComponentVisible(component)) component.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Component component : propertyComponents) {
                if (isComponentVisible(component)) component.keyTyped(typedChar, keyCode);
            }
        }
    }
}
