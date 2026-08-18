package myau.ui.impl.clickgui.rise;

import myau.Myau;
import myau.module.modules.ClickGUIModule;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.TextProperty;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public abstract class RiseValueEditor {

    protected final Property<?> property;

    protected RiseValueEditor(Property<?> property) {
        this.property = property;
    }

    public boolean isVisible() {
        return property.isVisible();
    }

    public boolean isTyping() {
        return false;
    }

    public abstract float getHeight();

    public abstract void draw(float x, float y, float w, int mouseX, int mouseY,
                              float partialTicks, float deltaTime, int opacity);

    public abstract boolean click(int mouseX, int mouseY, int mouseButton,
                                  float x, float y, float w);

    public void released() {
    }

    public void key(char typedChar, int keyCode) {
    }

    protected Color accent() {
        ClickGUIModule clickGUI = (ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");
        return clickGUI != null ? clickGUI.getAccentColor() : new Color(0x4FC3F7);
    }

    protected int alpha(Color color, int opacity) {
        return RiseColors.withAlpha(color, opacity).getRGB();
    }

    protected boolean over(float x, float y, float w, float h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected float centeredY(float y, float height, float contentHeight) {
        return y + (height - contentHeight) / 2f + 0.5f;
    }

    public static class BooleanEditor extends RiseValueEditor {
        private final BooleanProperty prop;
        private float valueAnimation;
        private float hoverAnimation;

        public BooleanEditor(BooleanProperty prop) {
            super(prop);
            this.prop = prop;
            this.valueAnimation = prop.getValue() ? 1f : 0f;
        }

        @Override
        public float getHeight() {
            return 20f;
        }

        @Override
        public void draw(float x, float y, float w, int mouseX, int mouseY,
                         float partialTicks, float deltaTime, int opacity) {
            valueAnimation = AnimationUtil.animateSmooth(prop.getValue() ? 1f : 0f, valueAnimation, 12f, deltaTime);
            hoverAnimation = AnimationUtil.animateSmooth(over(x, y, w, getHeight(), mouseX, mouseY) ? 1f : 0f,
                    hoverAnimation, 10f, deltaTime);

            if (hoverAnimation > 0.01f) {
                RenderUtil.drawRoundedRect(x - 4, y, w + 8, getHeight() - 1, 4,
                        new Color(255, 255, 255, (int) (7 * hoverAnimation * opacity / 255f)).getRGB(),
                        true, true, true, true);
            }

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(prop.getName(), x, y + 4,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
                float textWidth = (float) FontManager.productSans16.getStringWidth(prop.getName());
                float radius = 2.5f + valueAnimation;
                float dotX = x + textWidth + 8;
                float dotY = y + 9.5f;

                RenderUtil.drawRoundedRect(dotX - 2.5f, dotY - 2.5f, 5, 5, 2.5f,
                        new Color(255, 255, 255, (int) (45 * opacity / 255f)).getRGB(),
                        true, true, true, true);
                if (valueAnimation > 0.01f) {
                    Color accent = accent();
                    RenderUtil.drawRoundedRect(dotX - radius, dotY - radius, radius * 2, radius * 2, radius,
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    (int) (opacity * valueAnimation)).getRGB(),
                            true, true, true, true);
                }
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int mouseButton, float x, float y, float w) {
            if (mouseButton == 0 && over(x, y, w, getHeight(), mouseX, mouseY)) {
                prop.setValue(!prop.getValue());
                return true;
            }
            return false;
        }
    }

    public static class ModeEditor extends RiseValueEditor {
        private final ModeProperty prop;
        private float hoverAnimation;

        public ModeEditor(ModeProperty prop) {
            super(prop);
            this.prop = prop;
        }

        @Override
        public float getHeight() {
            return 20f;
        }

        @Override
        public void draw(float x, float y, float w, int mouseX, int mouseY,
                         float partialTicks, float deltaTime, int opacity) {
            hoverAnimation = AnimationUtil.animateSmooth(over(x, y, w, getHeight(), mouseX, mouseY) ? 1f : 0f,
                    hoverAnimation, 10f, deltaTime);

            if (hoverAnimation > 0.01f) {
                RenderUtil.drawRoundedRect(x - 4, y, w + 8, getHeight() - 1, 4,
                        new Color(255, 255, 255, (int) (7 * hoverAnimation * opacity / 255f)).getRGB(),
                        true, true, true, true);
            }

            if (FontManager.productSans16 != null) {
                String prefix = prop.getName() + ":";
                FontManager.productSans16.drawString(prefix, x, y + 4,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
                FontManager.productSans16.drawString(prop.getModeString(),
                        x + (float) FontManager.productSans16.getStringWidth(prefix) + 3, y + 4,
                        alpha(accent(), opacity));
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int mouseButton, float x, float y, float w) {
            if (over(x, y, w, getHeight(), mouseX, mouseY)) {
                if (mouseButton == 0) {
                    prop.nextMode();
                    return true;
                }
                if (mouseButton == 1) {
                    prop.previousMode();
                    return true;
                }
            }
            return false;
        }
    }

    public static class SliderEditor extends RiseValueEditor {
        private boolean dragging;
        private boolean editing;
        private String editText = "";
        private float hoverAnimation;
        private float percentAnimation;

        public SliderEditor(Property<?> prop) {
            super(prop);
            this.percentAnimation = ratio();
        }

        @Override
        public float getHeight() {
            return 22f;
        }

        @Override
        public boolean isTyping() {
            return editing;
        }

        @Override
        public void draw(float x, float y, float w, int mouseX, int mouseY,
                         float partialTicks, float deltaTime, int opacity) {
            hoverAnimation = AnimationUtil.animateSmooth(over(x, y, w, getHeight(), mouseX, mouseY) || dragging ? 1f : 0f,
                    hoverAnimation, 10f, deltaTime);

            if (hoverAnimation > 0.01f) {
                RenderUtil.drawRoundedRect(x - 4, y, w + 8, getHeight(), 4,
                        new Color(255, 255, 255, (int) (6 * hoverAnimation * opacity / 255f)).getRGB(),
                        true, true, true, true);
            }

            if (dragging) {
                setFromMouse(mouseX, x, w);
            }

            String value = editing ? editText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "") : valueText();
            float valueWidth = FontManager.productSans16 != null
                    ? (float) FontManager.productSans16.getStringWidth(value) : 30f;
            float trackX = sliderTrackX(x, w);
            float trackW = sliderTrackWidth(x, w, trackX, valueWidth);
            float trackY = centeredY(y, getHeight(), 4f);
            float textY = FontManager.productSans16 != null
                    ? centeredY(y, getHeight(), (float) FontManager.productSans16.getHeight()) : y + 4f;

            percentAnimation = AnimationUtil.animateSmooth(ratio(), percentAnimation, 14f, deltaTime);

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(property.getName(), x, textY,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
                FontManager.productSans16.drawString(value, x + w - valueWidth, textY,
                        alpha(editing ? Color.WHITE : accent(), opacity));
            }

            RenderUtil.drawRoundedRect(trackX, trackY, trackW, 4, 2,
                    new Color(36, 39, 46, opacity).getRGB(), true, true, true, true);
            RenderUtil.drawRoundedRect(trackX, trackY, Math.max(4, trackW * percentAnimation), 4, 2,
                    alpha(accent(), opacity), true, true, true, true);

            float thumb = 5f + hoverAnimation;
            float thumbX = trackX + trackW * percentAnimation;
            RenderUtil.drawRoundedRect(thumbX - thumb / 2f, trackY + 2 - thumb / 2f, thumb, thumb, thumb / 2f,
                    RiseColors.withAlphaRGB(Color.WHITE, opacity), true, true, true, true);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int mouseButton, float x, float y, float w) {
            if (mouseButton != 0 || !over(x, y, w, getHeight(), mouseX, mouseY)) {
                if (mouseButton == 0) commitEdit();
                return false;
            }

            String value = valueText();
            float valueWidth = FontManager.productSans16 != null
                    ? (float) FontManager.productSans16.getStringWidth(value) : 30f;
            if (mouseX >= x + w - valueWidth - 4) {
                editing = true;
                editText = value.replace("%", "");
                return true;
            }

            dragging = true;
            setFromMouse(mouseX, x, w);
            return true;
        }

        @Override
        public void released() {
            dragging = false;
        }

        @Override
        public void key(char typedChar, int keyCode) {
            if (!editing) return;

            if (keyCode == Keyboard.KEY_ESCAPE) {
                editing = false;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                commitEdit();
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!editText.isEmpty()) editText = editText.substring(0, editText.length() - 1);
                return;
            }
            if ((typedChar >= '0' && typedChar <= '9') || typedChar == '.' || typedChar == '-') {
                editText += typedChar;
            }
        }

        private void commitEdit() {
            if (!editing) return;
            try {
                if (!editText.trim().isEmpty()) {
                    setNumber(Double.parseDouble(editText.trim()));
                }
            } catch (NumberFormatException ignored) {
            }
            editing = false;
        }

        private void setFromMouse(int mouseX, float x, float w) {
            float valueWidth = FontManager.productSans16 != null
                    ? (float) FontManager.productSans16.getStringWidth(valueText()) : 30f;
            float trackX = sliderTrackX(x, w);
            float trackW = sliderTrackWidth(x, w, trackX, valueWidth);
            float nextRatio = Math.max(0f, Math.min(1f, (mouseX - trackX) / trackW));
            setNumber(min() + (max() - min()) * nextRatio);
        }

        private float sliderTrackX(float x, float w) {
            return x + Math.min(92f, Math.max(72f, w * 0.34f));
        }

        private float sliderTrackWidth(float x, float w, float trackX, float valueWidth) {
            return Math.min(100f, Math.max(46f, x + w - valueWidth - 12f - trackX));
        }

        private float ratio() {
            double min = min();
            double max = max();
            if (max <= min) return 0f;
            return (float) Math.max(0, Math.min(1, (current() - min) / (max - min)));
        }

        private double min() {
            if (property instanceof IntProperty) return ((IntProperty) property).getMinimum();
            if (property instanceof FloatProperty) return ((FloatProperty) property).getMinimum();
            if (property instanceof PercentProperty) return ((PercentProperty) property).getMinimum();
            return 0;
        }

        private double max() {
            if (property instanceof IntProperty) return ((IntProperty) property).getMaximum();
            if (property instanceof FloatProperty) return ((FloatProperty) property).getMaximum();
            if (property instanceof PercentProperty) return ((PercentProperty) property).getMaximum();
            return 100;
        }

        private double current() {
            if (property instanceof IntProperty) return ((IntProperty) property).getValue();
            if (property instanceof FloatProperty) return ((FloatProperty) property).getValue();
            if (property instanceof PercentProperty) return ((PercentProperty) property).getValue();
            return 0;
        }

        private void setNumber(double value) {
            value = Math.max(min(), Math.min(max(), value));
            if (property instanceof IntProperty) {
                ((IntProperty) property).setValue((int) Math.round(value));
            } else if (property instanceof FloatProperty) {
                ((FloatProperty) property).setValue((float) (Math.round(value * 100.0) / 100.0));
            } else if (property instanceof PercentProperty) {
                ((PercentProperty) property).setValue((int) Math.round(value));
            }
        }

        private String valueText() {
            if (property instanceof IntProperty) return String.valueOf(((IntProperty) property).getValue());
            if (property instanceof FloatProperty) return String.format("%.2f", ((FloatProperty) property).getValue());
            if (property instanceof PercentProperty) return ((PercentProperty) property).getValue() + "%";
            return "";
        }
    }

    public static class ColorEditor extends RiseValueEditor {
        private final ColorProperty prop;
        private boolean expanded;
        private boolean draggingPicker;
        private boolean draggingHue;
        private float expandAnimation;

        public ColorEditor(ColorProperty prop) {
            super(prop);
            this.prop = prop;
        }

        @Override
        public float getHeight() {
            return 20f + expandAnimation * 98f;
        }

        @Override
        public void draw(float x, float y, float w, int mouseX, int mouseY,
                         float partialTicks, float deltaTime, int opacity) {
            expandAnimation = AnimationUtil.animateSmooth(expanded ? 1f : 0f, expandAnimation, 12f, deltaTime);

            Color color = currentColor();
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(prop.getName(), x, y + 4,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
            }

            float preview = 10f;
            RenderUtil.drawRoundedRect(x + w - preview, y + 5, preview, preview, 3,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity).getRGB(),
                    true, true, true, true);

            if (expandAnimation <= 0.03f) return;

            float pickerX = x;
            float pickerY = y + 24;
            float pickerW = Math.min(92f, w - 42f);
            float pickerH = 54f;
            float hueX = pickerX + pickerW + 8;
            float hueW = 8f;

            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            drawPicker(pickerX, pickerY, pickerW, pickerH, hsb[0], opacity);
            drawHueBar(hueX, pickerY, hueW, pickerH, opacity);

            float pickerDotX = pickerX + hsb[1] * pickerW;
            float pickerDotY = pickerY + (1f - hsb[2]) * pickerH;
            RenderUtil.drawCircleOutline(pickerDotX, pickerDotY, 3.5f, 1.2f, RiseColors.withAlphaRGB(Color.WHITE, opacity));

            float hueDotY = pickerY + hsb[0] * pickerH;
            RenderUtil.drawRoundedRect(hueX - 1.5f, hueDotY - 1.5f, hueW + 3, 3, 1.5f,
                    RiseColors.withAlphaRGB(Color.WHITE, opacity), true, true, true, true);

            if (FontManager.productSans12 != null) {
                String rgb = color.getRed() + " " + color.getGreen() + " " + color.getBlue();
                String hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
                FontManager.productSans12.drawString(rgb, x, pickerY + pickerH + 9,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
                FontManager.productSans12.drawString(hex, x + w - (float) FontManager.productSans12.getStringWidth(hex),
                        pickerY + pickerH + 9, RiseColors.withAlphaRGB(RiseColors.TEXT_TRINARY, opacity));
            }

            if (draggingPicker) updatePicker(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);
            if (draggingHue) updateHue(mouseY, hueX, pickerY, hueW, pickerH);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int mouseButton, float x, float y, float w) {
            float pickerX = x;
            float pickerY = y + 24;
            float pickerW = Math.min(92f, w - 42f);
            float pickerH = 54f;
            float hueX = pickerX + pickerW + 8;
            float hueW = 8f;

            if (expanded && mouseButton == 0 && over(pickerX, pickerY, pickerW, pickerH, mouseX, mouseY)) {
                draggingPicker = true;
                updatePicker(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);
                return true;
            }
            if (expanded && mouseButton == 0 && over(hueX - 3, pickerY, hueW + 6, pickerH, mouseX, mouseY)) {
                draggingHue = true;
                updateHue(mouseY, hueX, pickerY, hueW, pickerH);
                return true;
            }
            if (over(x, y, w, 20, mouseX, mouseY)) {
                expanded = !expanded;
                return true;
            }
            return false;
        }

        @Override
        public void released() {
            draggingPicker = false;
            draggingHue = false;
        }

        private Color currentColor() {
            return new Color(prop.getValue() & 0xFFFFFF);
        }

        private void updatePicker(int mouseX, int mouseY, float x, float y, float w, float h) {
            Color color = currentColor();
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            float saturation = Math.max(0f, Math.min(1f, (mouseX - x) / w));
            float brightness = 1f - Math.max(0f, Math.min(1f, (mouseY - y) / h));
            prop.setValue(Color.HSBtoRGB(hsb[0], saturation, brightness) & 0xFFFFFF);
        }

        private void updateHue(int mouseY, float x, float y, float w, float h) {
            Color color = currentColor();
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            float hue = Math.max(0f, Math.min(1f, (mouseY - y) / h));
            prop.setValue(Color.HSBtoRGB(hue, hsb[1], hsb[2]) & 0xFFFFFF);
        }

        private void drawPicker(float x, float y, float w, float h, float hue, int opacity) {
            int width = Math.max(1, (int) w);
            int height = Math.max(1, (int) h);
            for (int ix = 0; ix < width; ix++) {
                float saturation = ix / (float) Math.max(1, width - 1);
                for (int iy = 0; iy < height; iy++) {
                    float brightness = 1f - iy / (float) Math.max(1, height - 1);
                    int rgb = Color.HSBtoRGB(hue, saturation, brightness);
                    Color color = new Color(rgb);
                    RenderUtil.drawRect(x + ix, y + iy, x + ix + 1, y + iy + 1,
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity).getRGB());
                }
            }
        }

        private void drawHueBar(float x, float y, float w, float h, int opacity) {
            int height = Math.max(1, (int) h);
            for (int iy = 0; iy < height; iy++) {
                Color color = new Color(Color.HSBtoRGB(iy / (float) Math.max(1, height - 1), 1f, 1f));
                RenderUtil.drawRect(x, y + iy, x + w, y + iy + 1,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity).getRGB());
            }
        }
    }

    public static class TextEditor extends RiseValueEditor {
        private final TextProperty prop;
        private boolean editing;

        public TextEditor(TextProperty prop) {
            super(prop);
            this.prop = prop;
        }

        @Override
        public float getHeight() {
            return 22f;
        }

        @Override
        public boolean isTyping() {
            return editing;
        }

        @Override
        public void draw(float x, float y, float w, int mouseX, int mouseY,
                         float partialTicks, float deltaTime, int opacity) {
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(prop.getName(), x, y + 4,
                        RiseColors.withAlphaRGB(RiseColors.TEXT_SECONDARY, opacity));
            }

            float fieldW = Math.max(76f, w * 0.55f);
            float fieldX = x + w - fieldW;
            RenderUtil.drawRoundedRect(fieldX, y + 3, fieldW, 15, 3,
                    new Color(0, 0, 0, editing ? 82 : 45).getRGB(), true, true, true, true);
            if (editing) {
                RenderUtil.drawRoundedRect(fieldX, y + 3, fieldW, 15, 3,
                        new Color(accent().getRed(), accent().getGreen(), accent().getBlue(), 35).getRGB(),
                        true, true, true, true);
            }

            if (FontManager.productSans12 != null) {
                String text = prop.getValue();
                if (editing) text += System.currentTimeMillis() % 1000 < 500 ? "|" : "";
                FontManager.productSans12.drawString(trimToWidth(text, fieldW - 8), fieldX + 4, y + 6,
                        RiseColors.withAlphaRGB(Color.WHITE, opacity));
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int mouseButton, float x, float y, float w) {
            float fieldW = Math.max(76f, w * 0.55f);
            float fieldX = x + w - fieldW;
            if (mouseButton == 0 && over(fieldX, y + 3, fieldW, 15, mouseX, mouseY)) {
                editing = true;
                return true;
            }
            if (mouseButton == 0) editing = false;
            return false;
        }

        @Override
        public void key(char typedChar, int keyCode) {
            if (!editing) return;
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                editing = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                String value = prop.getValue();
                if (!value.isEmpty()) prop.setValue(value.substring(0, value.length() - 1));
                return;
            }
            if (typedChar >= ' ' && typedChar <= '~') {
                prop.setValue(prop.getValue() + typedChar);
            }
        }

        private String trimToWidth(String text, float width) {
            if (FontManager.productSans12 == null) return text;
            String result = text;
            while (result.length() > 1 && FontManager.productSans12.getStringWidth(result) > width) {
                result = result.substring(1);
            }
            return result;
        }
    }
}
