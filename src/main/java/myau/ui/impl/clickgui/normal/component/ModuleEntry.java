package myau.ui.impl.clickgui.normal.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import lombok.Getter;
import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class ModuleEntry extends Component {
    @Getter
    private final Module module;
    private final List<Component> propertiesComponents;
    private boolean expanded;
    private float hoverOpacity = 0f;
    private float currentSettingsHeight = 0f;
    private int currentColor;
    private final int parentFrameY;

    public ModuleEntry(Module module, int x, int y, int width, int height, int parentFrameY) {
        super(x, y, width, height);
        this.module = module;
        this.parentFrameY = parentFrameY;
        this.expanded = false;
        this.propertiesComponents = new ArrayList<>();
        this.currentColor = MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        init();
    }

    private void init() {
        int compHeight = 18;
        
        propertiesComponents.add(new KeybindComponent(module, x, y, width, compHeight));
        
        if (Myau.propertyManager != null) {
            List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
            if (properties != null) {
                for (Property<?> property : properties) {
                    Component comp = null;
                    
                    if (property instanceof BooleanProperty) {
                        comp = new Switch((BooleanProperty) property, x, 0, width, compHeight);
                    } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                        comp = new Slider(property, x, 0, width, compHeight + 4);
                    } else if (property instanceof ModeProperty) {
                        comp = new Dropdown((ModeProperty) property, x, 0, width, compHeight, parentFrameY);
                    } else if (property instanceof ColorProperty) {
                        comp = new ColorPicker((ColorProperty) property, x, 0, width, 65);
                    } else if (property instanceof TextProperty) {
                        comp = new TextField((TextProperty) property, x, 0, width, compHeight + 2);
                    }

                    if (comp != null) propertiesComponents.add(comp);
                }
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        boolean hovered = isMouseOverHeader(mouseX, mouseY, scrollOffset);
        int alpha = (int) (255 * animationProgress);

        float targetHover = hovered ? 1.0f : 0.0f;
        this.hoverOpacity = AnimationUtil.animateSmooth(targetHover, this.hoverOpacity, 12.0f, deltaTime);
        
        if (hoverOpacity > 0.01f) {
            int hAlpha = (int) (30 * hoverOpacity * (alpha / 255f));
            Shader2D.drawRoundedRect(x + 2, scrolledY + 1, width - 4, height - 2, 3.0f, new Color(255, 255, 255, hAlpha));
        }

        int targetColor = module.isEnabled() ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        this.currentColor = AnimationUtil.interpolateColor(this.currentColor, targetColor, 12.0f * deltaTime);
        int finalTextColor = (this.currentColor & 0x00FFFFFF) | (alpha << 24);

        if (alpha > 10) {
            float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f);
            FontManager.productSans16.drawString(module.getName(), x + 12, textY, finalTextColor);

            if (module.isEnabled()) {
                Shader2D.drawRoundedRect(x + 5, scrolledY + (height / 2f) - 1.5f, 3, 3, 1.5f, new Color(finalTextColor, true));
            }

            if (!propertiesComponents.isEmpty()) {
                float dotSize = 1.5f;
                float dotSpacing = 3.5f;
                float dotsX = x + width - 12;
                float dotsY = scrolledY + (height / 2f);

                int dotsColor = expanded ? 
                        MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha) : 
                        MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha / 2);

                for (int i = -1; i <= 1; i++) {
                    float drawY = dotsY + (i * dotSpacing) - (dotSize / 2f);
                    Shader2D.drawRoundedRect(dotsX, drawY, dotSize, dotSize, dotSize / 2f, new Color(dotsColor, true));
                }
            }
        }

        float visibleHeightSum = 0;
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) visibleHeightSum += comp.getHeight();
            }
        }

        this.currentSettingsHeight = AnimationUtil.animateSmooth(visibleHeightSum, this.currentSettingsHeight, 14.0f, deltaTime);

        if (currentSettingsHeight > 0.5f) {
            Shader2D.drawRoundedRect(x + 3, scrolledY + height, width - 6, currentSettingsHeight, 2.0f, new Color(20, 20, 23, (int) (alpha * 0.6f)));

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            float clipTop = parentFrameY + 24;
            float clipBottom = parentFrameY + 280;

            float settingsTop = scrolledY + height;
            float settingsBottom = scrolledY + height + currentSettingsHeight;

            float finalTop = Math.max(settingsTop, clipTop);
            float finalBottom = Math.min(settingsBottom, clipBottom);
            float finalHeight = finalBottom - finalTop;

            if (finalHeight > 0) {
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                
                int sX = (int) (x * scale);
                int sY = (int) ((sr.getScaledHeight() - finalBottom) * scale);
                int sW = (int) (width * scale);
                int sH = (int) (finalHeight * scale);

                GL11.glScissor(sX, sY, sW, sH);

                float dynamicY = scrolledY + height;
                for (Component comp : propertiesComponents) {
                    if (!isComponentVisible(comp)) continue;

                    comp.setX(x + 4);
                    comp.setY((int) dynamicY);
                    comp.setWidth(width - 8);
                    
                    if (dynamicY + comp.getHeight() > finalTop && dynamicY < finalBottom) {
                        comp.render(mouseX, mouseY, partialTicks, animationProgress, false, 0, deltaTime);
                    }
                    
                    dynamicY += comp.getHeight();
                }

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }

    private boolean isComponentVisible(Component comp) {
        if (comp instanceof Switch) return ((Switch) comp).getProperty().isVisible();
        if (comp instanceof Slider) return ((Slider) comp).getProperty().isVisible();
        if (comp instanceof Dropdown) return ((Dropdown) comp).getProperty().isVisible();
        if (comp instanceof ColorPicker) return ((ColorPicker) comp).getProperty().isVisible();
        if (comp instanceof TextField) return ((TextField) comp).getProperty().isVisible();
        return true;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                module.toggle();
                return true;
            } else if (mouseButton == 1) {
                if (!propertiesComponents.isEmpty()) expanded = !expanded;
                return true;
            }
        }
        if (expanded && currentSettingsHeight > 1) {
            int scrolledY = this.y - scrollOffset;
            if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY + height && mouseY <= scrolledY + height + currentSettingsHeight) {
                for (Component comp : propertiesComponents) {
                    if (isComponentVisible(comp) && comp.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                }
            }
        }
        return false;
    }

    public float getCurrentHeight() { return height + currentSettingsHeight; }

    private boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
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

    @Override public void keyTyped(char typedChar, int keyCode) {
        if (expanded) propertiesComponents.stream().filter(this::isComponentVisible).forEach(c -> c.keyTyped(typedChar, keyCode));
    }
    
    @Override public void mouseReleased(int mx, int my, int mb, int so) {
        if (expanded) propertiesComponents.stream().filter(this::isComponentVisible).forEach(c -> c.mouseReleased(mx, my, mb, so));
    }

    @Override public boolean mouseClicked(int mx, int my, int mb) { return false; }
    @Override public void mouseReleased(int mx, int my, int mb) {}
}