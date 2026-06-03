package myau.ui.impl.clickgui.rise;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.RiseClickGUIModule;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.TextProperty;
import myau.util.AnimationUtil;
import myau.util.KeyBindUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RiseModuleCard {

    private static final float DEFAULT_HEIGHT = 38f;

    private final Module module;
    private final List<RiseValueEditor> valueEditors = new ArrayList<RiseValueEditor>();

    private double x;
    private double y;
    private double cardWidth = 283;
    private boolean expanded;
    private boolean mouseDown;
    private boolean bindingKey;
    private float hoverAnimation;
    private float enabledAnimation;
    private float expandAnimation = DEFAULT_HEIGHT;
    private float settingOpacity;

    public RiseModuleCard(Module module) {
        this.module = module;
        this.enabledAnimation = module.isEnabled() ? 1f : 0f;
        initValueEditors();
    }

    private void initValueEditors() {
        if (Myau.propertyManager == null || Myau.propertyManager.properties == null) return;

        List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties == null) return;

        for (Property<?> property : properties) {
            if (property instanceof BooleanProperty) {
                valueEditors.add(new RiseValueEditor.BooleanEditor((BooleanProperty) property));
            } else if (property instanceof ModeProperty) {
                valueEditors.add(new RiseValueEditor.ModeEditor((ModeProperty) property));
            } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                valueEditors.add(new RiseValueEditor.SliderEditor(property));
            } else if (property instanceof ColorProperty) {
                valueEditors.add(new RiseValueEditor.ColorEditor((ColorProperty) property));
            } else if (property instanceof TextProperty) {
                valueEditors.add(new RiseValueEditor.TextEditor((TextProperty) property));
            }
        }
    }

    public Module getModule() {
        return module;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setCardWidth(double cardWidth) {
        this.cardWidth = cardWidth;
    }

    public double getTotalHeight() {
        return expandAnimation;
    }

    public boolean isBindingKey() {
        return bindingKey;
    }

    public boolean isTyping() {
        if (bindingKey) return true;
        for (RiseValueEditor editor : valueEditors) {
            if (editor.isTyping()) return true;
        }
        return false;
    }

    public void draw(int mouseX, int mouseY, float partialTicks, float deltaTime,
                     double guiX, double guiY, double guiW, double guiH, boolean searchMode) {
        float targetHeight = DEFAULT_HEIGHT;
        if (expanded) {
            targetHeight = DEFAULT_HEIGHT + 3;
            for (RiseValueEditor editor : valueEditors) {
                if (editor.isVisible()) targetHeight += editor.getHeight();
            }
        }

        expandAnimation = AnimationUtil.animateSmooth(targetHeight, expandAnimation, 12f, deltaTime);
        settingOpacity = AnimationUtil.animateSmooth(expanded ? 255f : 0f, settingOpacity, 10f, deltaTime);
        enabledAnimation = AnimationUtil.animateSmooth(module.isEnabled() ? 1f : 0f, enabledAnimation, 10f, deltaTime);

        boolean visible = !(y + expandAnimation < guiY || y > guiY + guiH);
        if (!visible) return;

        RiseClickGUIModule clickGUI = (RiseClickGUIModule) Myau.moduleManager.getModule("RiseClickGUI");
        Color accent = clickGUI != null ? clickGUI.getAccentColor() : new Color(0x4FC3F7);
        boolean overHeader = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT;
        float hoverTarget = overHeader ? (mouseDown ? 35f : 20f) : 0f;
        hoverAnimation = AnimationUtil.animateSmooth(hoverTarget, hoverAnimation, 18f, deltaTime);

        RenderUtil.drawRoundedRect((float) x, (float) y, (float) cardWidth, expandAnimation, 6,
                RiseColors.OVERLAY.getRGB(), true, true, true, true);
        if (hoverAnimation > 0.5f) {
            RenderUtil.drawRoundedRect((float) x, (float) y, (float) cardWidth, expandAnimation, 6,
                    new Color(0, 0, 0, (int) hoverAnimation).getRGB(), true, true, true, true);
        }

        Color nameColor = mix(RiseColors.TEXT, accent, enabledAnimation);
        int nameAlpha = module.isEnabled() ? 255 : 205;

        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawString(module.getName(), (float) x + 6f, (float) y + 8f,
                    RiseColors.withAlpha(nameColor, nameAlpha).getRGB());
        }

        if (searchMode && FontManager.productSans12 != null) {
            String category = RiseClickGUI.getModuleCategoryName(module);
            float nameWidth = FontManager.productSans20 != null
                    ? (float) FontManager.productSans20.getStringWidth(module.getName()) : 50f;
            FontManager.productSans12.drawString("(" + category + ")", (float) x + 16f + nameWidth,
                    (float) y + 10f, RiseColors.withAlphaRGB(RiseColors.TEXT, 65));
        }

        if (FontManager.productSans12 != null) {
            String description = module.getDescription();
            if (description != null && !description.isEmpty()) {
                FontManager.productSans12.drawString(trim(description, (float) cardWidth - 68f),
                        (float) x + 6f, (float) y + 25f, RiseColors.withAlphaRGB(RiseColors.TEXT, 72));
            }
        }

        drawRightBadges(accent);

        if (expandAnimation > DEFAULT_HEIGHT + 1 && settingOpacity > 1) {
            float editorY = (float) y + DEFAULT_HEIGHT + 2;
            for (RiseValueEditor editor : valueEditors) {
                if (!editor.isVisible()) continue;
                if (editorY + editor.getHeight() >= guiY && editorY <= guiY + guiH) {
                    editor.draw((float) x + 6f, editorY, (float) cardWidth - 12f,
                            mouseX, mouseY, partialTicks, deltaTime, (int) Math.min(255, settingOpacity));
                }
                editorY += editor.getHeight();
            }
        }
    }

    private void drawRightBadges(Color accent) {
        float right = (float) (x + cardWidth - 7);

        if (FontManager.productSans12 != null && bindingKey) {
            String text = "Press a key";
            float width = (float) FontManager.productSans12.getStringWidth(text);
            FontManager.productSans12.drawString(text, right - width, (float) y + 8,
                    System.currentTimeMillis() % 1000 < 500 ? RiseColors.TEXT.getRGB() : RiseColors.TEXT_TRINARY.getRGB());
            return;
        }

        String key = keyName();
        if (!key.isEmpty() && FontManager.productSans12 != null) {
            float textWidth = (float) FontManager.productSans12.getStringWidth(key);
            float badgeWidth = textWidth + 8f;
            right -= badgeWidth;
            RenderUtil.drawRoundedRect(right, (float) y + 6, badgeWidth, 14, 4,
                    new Color(255, 255, 255, 18).getRGB(), true, true, true, true);
            FontManager.productSans12.drawString(key, right + 4, (float) y + 8, RiseColors.TEXT_TRINARY.getRGB());
            right -= 6;
        }

        if (!valueEditors.isEmpty() && FontManager.productSans16 != null) {
            String symbol = expanded ? "-" : "+";
            FontManager.productSans16.drawString(symbol, right - (float) FontManager.productSans16.getStringWidth(symbol),
                    (float) y + 10, RiseColors.withAlpha(accent, expanded ? 240 : 145).getRGB());
        }
    }

    public boolean click(int mouseX, int mouseY, int mouseButton) {
        boolean overHeader = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT;
        if (overHeader) {
            mouseDown = true;
            if (mouseButton == 0) {
                module.toggle();
                return true;
            }
            if (mouseButton == 1 && !valueEditors.isEmpty()) {
                expanded = !expanded;
                return true;
            }
            if (mouseButton == 2) {
                bindingKey = !bindingKey;
                return true;
            }
        }

        if (expanded && expandAnimation > DEFAULT_HEIGHT + 1) {
            float editorY = (float) y + DEFAULT_HEIGHT + 2;
            for (RiseValueEditor editor : valueEditors) {
                if (!editor.isVisible()) continue;
                if (editor.click(mouseX, mouseY, mouseButton, (float) x + 6f, editorY, (float) cardWidth - 12f)) {
                    return true;
                }
                editorY += editor.getHeight();
            }
        }

        return false;
    }

    public void released() {
        mouseDown = false;
        for (RiseValueEditor editor : valueEditors) {
            editor.released();
        }
    }

    public void key(char typedChar, int keyCode) {
        if (bindingKey) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                module.setKey(0);
            } else {
                module.setKey(keyCode);
            }
            bindingKey = false;
            return;
        }

        if (expanded) {
            for (RiseValueEditor editor : valueEditors) {
                editor.key(typedChar, keyCode);
            }
        }
    }

    private String keyName() {
        int key = module.getKey();
        if (key == 0) return "";
        return KeyBindUtil.getKeyName(key);
    }

    private Color mix(Color from, Color to, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * amount);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * amount);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * amount);
        return new Color(r, g, b);
    }

    private String trim(String text, float width) {
        if (FontManager.productSans12 == null || FontManager.productSans12.getStringWidth(text) <= width) return text;
        String ellipsis = "...";
        String result = text;
        while (result.length() > 0 && FontManager.productSans12.getStringWidth(result + ellipsis) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }
}
