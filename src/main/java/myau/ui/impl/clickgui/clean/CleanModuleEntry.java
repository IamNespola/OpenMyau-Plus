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
        propertyComponents.add(new KeybindComponent(module, x, currentY, width, 20));
        if (Myau.propertyManager == null) return;
        ArrayList<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties == null) return;
        for (Property<?> property : properties) {
            Component component = null;
            if (property instanceof BooleanProperty) {
                component = new Switch((BooleanProperty) property, x, currentY, width, 20);
            } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                component = new Slider(property, x, currentY, width, 20);
            } else if (property instanceof ModeProperty) {
                component = new Dropdown((ModeProperty) property, x, currentY, width, 20);
            } else if (property instanceof MultiModeProperty) {
                component = new MultiDropdown((MultiModeProperty) property, x, currentY, width, 20);
            } else if (property instanceof ColorProperty) {
                component = new ColorPicker((ColorProperty) property, x, currentY, width, 60);
            } else if (property instanceof TextProperty) {
                component = new TextField((TextProperty) property, x, currentY, width, 20);
            }
            if (component != null) propertyComponents.add(component);
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
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + height, withAlpha(CleanTheme.ROW_HOVER, alpha));
        if (module.isEnabled()) Gui.drawRect(x, scrolledY + 1, x + 2, scrolledY + height - 1, withAlpha(CleanTheme.ACCENT, alpha));
        mc.fontRendererObj.drawStringWithShadow(module.getName(), x + 5, scrolledY + 3, module.isEnabled() ? withAlpha(0xFFFFFFFF, alpha) : withAlpha(0xFFBDBDBD, alpha));
        if (!propertyComponents.isEmpty()) mc.fontRendererObj.drawStringWithShadow(expanded ? "<" : ">", x + width - 9, scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));

        float targetSettingsHeight = 0.0F;
        if (expanded) {
            for (Component component : propertyComponents) if (isComponentVisible(component)) targetSettingsHeight += component.getHeight();
        }
        currentSettingsHeight = AnimationUtil.animateSmooth(targetSettingsHeight, currentSettingsHeight, 12.0F, deltaTime);

        if (currentSettingsHeight > 1.0F) {
            Gui.drawRect(x + 2, scrolledY + height, x + width, scrolledY + height + (int) currentSettingsHeight, withAlpha(0xAA0A0A0A, alpha));
            float componentY = y + height;
            for (Component component : propertyComponents) {
                if (!isComponentVisible(component)) continue;
                if (componentY - (y + height) < currentSettingsHeight) {
                    component.setX(x + 2);
                    component.setY((int) componentY);
                    component.setWidth(width - 4);
                    component.render(mouseX, mouseY, partialTicks, animationProgress, false, scrollOffset, deltaTime);
                }
                componentY += component.getHeight();
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public float getCurrentHeight() {
        return height + currentSettingsHeight;
    }

    public boolean matches(String searchQuery) {
        return searchQuery == null || searchQuery.trim().isEmpty() || module.getName().toLowerCase().contains(searchQuery.trim().toLowerCase());
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

