package myau.ui.impl.clickgui.raven.components;

import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.enums.ChatColors;
import myau.property.properties.ColorProperty;
import myau.ui.impl.clickgui.raven.Component;

import java.awt.*;

public class ColorSliderComponent implements Component {
    private final ModuleComponent parentModule;
    private final ColorProperty property;
    private int offsetY;
    private boolean draggingHue, draggingSat, draggingBri;
    private float hue, saturation, brightness;

    public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.offsetY = offsetY;
        this.property = property;
        initColorValues();
    }

    private void initColorValues() {
        Color c = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    @Override
    public void draw(java.util.concurrent.atomic.AtomicInteger offset) {
        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 8;

        drawLabel(x, y);
        updateColorValues();
        drawColorPreview(x, y, width);
        drawSliders(x, y, width);
    }

    private void drawLabel(int x, int y) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        String text = property.getName().replace("-", " ") + ": " + ChatColors.formatColor(property.formatValue());
        Myau.fontManagers.getFont(24).drawString(text, x * 2, (parentModule.category.getY() + offsetY + 3) * 2, -1);
        GL11.glPopMatrix();
    }

    private void updateColorValues() {
        Color color = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        if (!draggingHue) hue = hsb[0];
        if (!draggingSat) saturation = hsb[1];
        if (!draggingBri) brightness = hsb[2];
    }

    private void drawColorPreview(int x, int y, int width) {
        int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
        Gui.drawRect(x + width - 12, y + 2, x + width - 6, y + 8, previewColor);
    }

    private void drawSliders(int x, int y, int width) {
        int hueY = y + 10;
        int satY = hueY + 6;
        int briY = satY + 6;

        drawHueSlider(x, hueY, width);
        drawSaturationSlider(x, satY, width);
        drawBrightnessSlider(x, briY, width);
    }

    private void drawHueSlider(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float h = (float) i / width;
            Gui.drawRect(x + i, y, x + i + 1, y + 4, Color.HSBtoRGB(h, 1f, 1f));
        }
        drawSliderPointer(x, y, width, hue);
    }

    private void drawSaturationSlider(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float s = (float) i / width;
            Gui.drawRect(x + i, y, x + i + 1, y + 4, Color.HSBtoRGB(hue, s, 1f));
        }
        drawSliderPointer(x, y, width, saturation);
    }

    private void drawBrightnessSlider(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float b = (float) i / width;
            Gui.drawRect(x + i, y, x + i + 1, y + 4, Color.HSBtoRGB(hue, saturation, b));
        }
        drawSliderPointer(x, y, width, brightness);
    }

    private void drawSliderPointer(int x, int y, int width, float value) {
        int posX = x + (int) (width * Math.max(0, Math.min(1, value)));
        Gui.drawRect(posX - 1, y, posX, y + 4, new Color(0, 0, 0, 200).getRGB());
    }

    @Override
    public void update(int mouseX, int mouseY) {
        int baseX = parentModule.category.getX() + 4;
        int width = parentModule.category.getWidth() - 8;

        if (draggingHue) {
            hue = clampValue(mouseX, baseX, width);
            updateColor();
        }
        if (draggingSat) {
            saturation = clampValue(mouseX, baseX, width);
            updateColor();
        }
        if (draggingBri) {
            brightness = clampValue(mouseX, baseX, width);
            updateColor();
        }
    }

    private float clampValue(int mouseX, int baseX, int width) {
        float value = (float) (mouseX - baseX) / width;
        return Math.max(0, Math.min(1, value));
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        property.setValue(rgb);
    }

    @Override
    public void onClick(int x, int y, int mouse) {
        if (mouse != 0 || !parentModule.isOpened) return;
        int baseY = parentModule.category.getModuleY() + offsetY + 10;
        if (isSliderHovered(x, y, baseY)) {
            draggingHue = true;
        } else if (isSliderHovered(x, y, baseY + 6)) {
            draggingSat = true;
        } else if (isSliderHovered(x, y, baseY + 12)) {
            draggingBri = true;
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
    }

    private boolean isSliderHovered(int mouseX, int mouseY, int sliderY) {
        int startX = parentModule.category.getX() + 4;
        int endX = startX + parentModule.category.getWidth() - 8;
        return mouseX >= startX && mouseX <= endX && mouseY >= sliderY && mouseY <= sliderY + 4;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        draggingHue = false;
        draggingSat = false;
        draggingBri = false;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public void render() {
        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getModuleY() + offsetY;
        int width = parentModule.category.getWidth() - 8;

        // Draw label
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        Myau.fontManagers.getFont(24).drawString(property.getName(), x * 2, (y + 3) * 2, -1);
        GL11.glPopMatrix();

        // Update color values from property
        updateColorValues();

        // Draw color preview
        int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
        Gui.drawRect(x + width - 12, y + 2, x + width - 6, y + 8, previewColor);

        // Draw sliders
        int hueY = y + 10;
        int satY = hueY + 6;
        int briY = satY + 6;

        // Hue slider
        for (int i = 0; i < width; i++) {
            float h = (float) i / width;
            Gui.drawRect(x + i, hueY, x + i + 1, hueY + 4, Color.HSBtoRGB(h, 1f, 1f));
        }
        Gui.drawRect(x + (int) (hue * width) - 1, hueY - 1, x + (int) (hue * width) + 2, hueY + 5, -1);

        // Saturation slider
        for (int i = 0; i < width; i++) {
            float s = (float) i / width;
            Gui.drawRect(x + i, satY, x + i + 1, satY + 4, Color.HSBtoRGB(hue, s, brightness));
        }
        Gui.drawRect(x + (int) (saturation * width) - 1, satY - 1, x + (int) (saturation * width) + 2, satY + 5, -1);

        // Brightness slider
        for (int i = 0; i < width; i++) {
            float b = (float) i / width;
            Gui.drawRect(x + i, briY, x + i + 1, briY + 4, Color.HSBtoRGB(hue, saturation, b));
        }
        Gui.drawRect(x + (int) (brightness * width) - 1, briY - 1, x + (int) (brightness * width) + 2, briY + 5, -1);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        int x = parentModule.category.getX() + 4;
        int baseY = parentModule.category.getModuleY() + offsetY + 10;
        int width = parentModule.category.getWidth() - 8;

        if (draggingHue) {
            float newHue = Math.max(0, Math.min(1, (mouseX - x) / (float) width));
            hue = newHue;
            updateColor();
        } else if (draggingSat) {
            float newSat = Math.max(0, Math.min(1, (mouseX - x) / (float) width));
            saturation = newSat;
            updateColor();
        } else if (draggingBri) {
            float newBri = Math.max(0, Math.min(1, (mouseX - x) / (float) width));
            brightness = newBri;
            updateColor();
        }
    }

    @Override
    public void updateHeight(int y) {
        this.offsetY = y;
    }

    @Override
    public void onScroll(int scroll) {
    }

    @Override
    public void onGuiClosed() {
    }
}
