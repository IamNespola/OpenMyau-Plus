package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.ColorProperty;
import myau.util.RenderUtil;
import myau.util.shader.Shader2D;
import org.lwjgl.input.Mouse;
import java.awt.*;

public class ColorPicker extends Component {
    private final ColorProperty colorProperty;
    private boolean draggingHue, draggingSV;
    private float hue, saturation, brightness;
    private int cachedColor;

    public ColorPicker(ColorProperty colorProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.colorProperty = colorProperty;
        this.cachedColor = colorProperty.getValue();
        updateHSB();
    }

    private void updateHSB() {
        int color = colorProperty.getValue();
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        // Mantenemos el alpha en 255 para evitar errores de renderizado en el picker
        int finalColor = (255 << 24) | (rgb & 0x00FFFFFF);
        colorProperty.setValue(finalColor);
        this.cachedColor = finalColor;
    }
    
    public ColorProperty getProperty() {
        return this.colorProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!colorProperty.isVisible()) return;

        float scrolledY = y - scrollOffset;
        float padding = 5;
        float pickerX = x + padding;
        float pickerY = scrolledY + padding;
        float pickerW = width - (padding * 2);
        float pickerH = height - (padding * 2);
        
        float hueHeight = 8;
        float svHeight = pickerH - hueHeight - 6;

        // Lógica de arrastre
        if (!Mouse.isButtonDown(0)) {
            draggingSV = false;
            draggingHue = false;
        }

        if (draggingSV) {
            saturation = Math.min(1, Math.max(0, (mouseX - pickerX) / pickerW));
            brightness = Math.min(1, Math.max(0, 1.0f - ((mouseY - pickerY) / svHeight)));
            updateColor();
        } else if (draggingHue) {
            hue = Math.min(1, Math.max(0, (mouseX - pickerX) / pickerW));
            updateColor();
        }

        // --- SOLUCIÓN PARA EL NEGRO ---
        
        // 1. Capa de Saturación (Horizontal): Blanco -> Color Puro (Hue)
        Color pureHue = Color.getHSBColor(hue, 1, 1);
        Shader2D.drawGradient(pickerX, pickerY, pickerW, svHeight, 2, 
            Color.WHITE, 
            pureHue, 
            pureHue, 
            false);
        
        // 2. Capa de Brillo (Vertical): Transparente -> NEGRO ABSOLUTO
        // IMPORTANTE: color2 y color3 deben ser negros para que la mitad inferior se oscurezca bien
        Shader2D.drawGradient(pickerX, pickerY, pickerW, svHeight, 2, 
            new Color(0, 0, 0, 0),    // Arriba: Transparente (deja ver el color)
            new Color(0, 0, 0, 150),  // Centro: Sombra media
            Color.BLACK,              // Abajo: Negro Puro (0, 0, 0)
            true);

        // Indicador SV
        float indX = pickerX + (saturation * pickerW);
        float indY = pickerY + ((1 - brightness) * svHeight);
        RenderUtil.drawCircleOutline(indX, indY, 3.5f, 2.0f, new Color(0, 0, 0, 100).getRGB());
        RenderUtil.drawCircleOutline(indX, indY, 3.5f, 1.0f, Color.WHITE.getRGB());

        // Barra de Hue
        float hueY = pickerY + svHeight + 6;
        drawHueBar(pickerX, hueY, pickerW, hueHeight);

        // Indicador de Hue
        float hIndX = pickerX + (hue * pickerW);
        Shader2D.drawRoundedRect(hIndX - 1, hueY - 1, 2, hueHeight + 2, 1, Color.WHITE);
        
        // Outline sutil
        Shader2D.drawOutline(pickerX - 1, pickerY - 1, pickerW + 2, pickerH + 2, 2, 0.5f, new Color(255, 255, 255, 25));
    }

    private void drawHueBar(float x, float y, float width, float height) {
        // Dividimos en 6 segmentos para cubrir el círculo cromático HSB completo
        int segments = 6;
        float segmentW = width / segments;
        for (int i = 0; i < segments; i++) {
            float h1 = (float) i / segments;
            float h2 = (float) (i + 1) / segments;
            Shader2D.drawGradient(x + (i * segmentW), y, segmentW, height, 0, 
                Color.getHSBColor(h1, 1, 1), 
                Color.getHSBColor((h1 + h2) / 2, 1, 1), 
                Color.getHSBColor(h2, 1, 1), 
                false);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (mouseButton != 0) return false;

        float scrolledY = y - scrollOffset;
        float pX = x + 5;
        float pY = scrolledY + 5;
        float pW = width - 10;
        float pH = height - 10;
        float hueH = 8;
        float svH = pH - hueH - 6;

        // Detección área SV
        if (RenderUtil.isHovered(pX, pY, pW, svH, mouseX, mouseY)) {
            draggingSV = true;
            return true;
        }

        // Detección área Hue
        if (RenderUtil.isHovered(pX, pY + svH + 6, pW, hueH, mouseX, mouseY)) {
            draggingHue = true;
            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        draggingSV = false;
        draggingHue = false;
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}
    @Override public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }
    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
}