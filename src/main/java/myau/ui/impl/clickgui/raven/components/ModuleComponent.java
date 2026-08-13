package myau.ui.impl.clickgui.raven.components;

import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.raven.Component;
import myau.ui.impl.clickgui.raven.dataset.impl.FloatSlider;
import myau.ui.impl.clickgui.raven.dataset.impl.IntSlider;
import myau.ui.impl.clickgui.raven.dataset.impl.PercentageSlider;
import myau.util.RenderUtil;
import myau.util.Timer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public final ArrayList<Component> settings;
    private final int enabledColor = new Color(24, 154, 255).getRGB();
    private final int disabledColor = new Color(192, 192, 192).getRGB();
    private final int originalHoverAlpha = 120;
    private final int hoverColor = (new Color(0, 0, 0, originalHoverAlpha)).getRGB();
    public Module mod;
    public CategoryComponent category;
    public int yPos;
    public boolean isOpened;
    private boolean hovering;
    private Timer hoverTimer;
    private boolean hoverStarted;
    private Timer smoothTimer;
    private int smoothingY = 16;
    private int targetHeight = 16;
    private boolean isAnimatingHeight = false;

    public ModuleComponent(Module mod, CategoryComponent category, int yPos) {
        this.mod = mod;
        this.category = category;
        this.yPos = yPos;
        this.settings = new ArrayList<>();
        this.isOpened = false;
        int y = yPos + 12;

        if (!Myau.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> baseProperty : Myau.propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    BooleanProperty property = (BooleanProperty) baseProperty;
                    CheckBoxComponent c = new CheckBoxComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    FloatProperty property = (FloatProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new FloatSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    IntProperty property = (IntProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new IntSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    PercentProperty property = (PercentProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new PercentageSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeProperty property = (ModeProperty) baseProperty;
                    ModeComponent c = new ModeComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorProperty property = (ColorProperty) baseProperty;
                    ColorSliderComponent c = new ColorSliderComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextProperty property = (TextProperty) baseProperty;
                    TextComponent c = new TextComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }

        this.settings.add(new BindComponent(this, y));
    }

    public void updateHeight(int newY) {
        this.yPos = newY;
        int y = this.yPos + 16;
        Iterator var3 = this.settings.iterator();

        while (true) {
            while (var3.hasNext()) {
                Component co = (Component) var3.next();
                if (!isVisible(co)) {
                    continue;
                }
                co.updateHeight(y);
                y += co.getHeight();
            }

            return;
        }
    }

    public void render() {
        if (hovering || hoverTimer != null) {
            double hoverAlpha = (hovering && hoverTimer != null) ? hoverTimer.getValueFloat(0, originalHoverAlpha, 1) : (hoverTimer != null && !hovering) ? originalHoverAlpha - hoverTimer.getValueFloat(0, originalHoverAlpha, 1) : originalHoverAlpha;
            if (hoverAlpha == 0) {
                hoverTimer = null;
            }
            RenderUtil.drawRoundedRectangle(this.category.getX(), this.category.getY() + yPos, this.category.getX() + this.category.getWidth(), this.category.getY() + 16 + this.yPos, 8, mergeAlpha(hoverColor, (int) hoverAlpha));
        }
        int button_rgb = this.mod.isEnabled() ? enabledColor : disabledColor;

        if (smoothTimer != null && System.currentTimeMillis() - smoothTimer.last >= 300) {
            smoothTimer = null;
            isAnimatingHeight = false;
        }
        if (smoothTimer != null) {
            if (isAnimatingHeight) {
                // Height change animation (for mode switches)
                if (targetHeight > smoothingY) {
                    smoothingY = smoothTimer.getValueInt(smoothingY, targetHeight, 1);
                } else {
                    smoothingY = smoothTimer.getValueInt(targetHeight, smoothingY, 1);
                }
                if (smoothingY == targetHeight) {
                    smoothTimer = null;
                    isAnimatingHeight = false;
                }
            } else if (isOpened) {
                smoothingY = smoothTimer.getValueInt(16, getModuleHeight(), 1);
                if (smoothingY == getModuleHeight()) {
                    smoothTimer = null;
                }
            } else {
                smoothingY = smoothTimer.getValueInt(getModuleHeight(), 16, 1);
                if (smoothingY == 16) {
                    smoothTimer = null;
                }
            }
            this.category.updateHeight();
        }

        Myau.fontManagers.getFont(20).drawString(this.mod.getName(), (float) (this.category.getX() + this.category.getWidth() / 2 - Myau.fontManagers.getFont(20).getStringWidth(this.mod.getName()) / 2), (float) (this.category.getY() + this.yPos + 2), button_rgb);
        boolean scissorRequired = smoothTimer != null;
        if (scissorRequired) {
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.scissor(this.category.getX() - 2, this.category.getY() + this.yPos + 4, this.category.getWidth() + 4, smoothingY + 4);
        }

        if (this.isOpened || smoothTimer != null) {
            for (Component settingComponent : this.settings) {
                if (!isVisible(settingComponent)) {
                    continue;
                }
                settingComponent.render();
            }
        }

        if (scissorRequired) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
        }
    }

    public int getHeight() {
        if (smoothTimer != null) {
            return smoothingY;
        }
        if (!this.isOpened) {
            return 16;
        } else {
            int h = 16;
            Iterator var2 = this.settings.iterator();

            while (true) {
                while (var2.hasNext()) {
                    Component c = (Component) var2.next();
                    if (!isVisible(c)) {
                        continue;
                    }
                    h += c.getHeight();
                }

                return h;
            }
        }
    }

    public void startHeightAnimation(int fromHeight, int toHeight) {
        this.smoothingY = fromHeight;
        this.targetHeight = toHeight;
        this.isAnimatingHeight = true;
        (this.smoothTimer = new Timer(200)).start();
        this.category.updateHeight();
    }

    public void onSliderChange() {
        for (Component c : this.settings) {
            if (c instanceof SliderComponent) {
                ((SliderComponent) c).onSliderChange();
            }
        }
    }

    public int getModuleHeight() {
        int h = 16;
        Iterator var2 = this.settings.iterator();

        while (true) {
            while (var2.hasNext()) {
                Component c = (Component) var2.next();
                if (!isVisible(c)) {
                    continue;
                }
                h += c.getHeight();
            }

            return h;
        }
    }

    public void drawScreen(int x, int y) {
        for (Component c : this.settings) {
            c.drawScreen(x, y);
        }
        if (overModuleName(x, y) && this.category.opened) {
            hovering = true;
            if (hoverTimer == null) {
                (hoverTimer = new Timer(75)).start();
                hoverStarted = true;
            }
        } else {
            if (hovering && hoverStarted) {
                (hoverTimer = new Timer(75)).start();
            }
            hoverStarted = false;
            hovering = false;
        }
    }

    public String getName() {
        return mod.getName();
    }

    public void onClick(int x, int y, int mouse) {
        if (this.overModuleName(x, y) && mouse == 0) {
            this.mod.toggle();
        }

        if (this.overModuleName(x, y) && mouse == 1) {
            this.isOpened = !this.isOpened;
            (this.smoothTimer = new Timer(200)).start();
            this.category.updateHeight();
        }

        for (Component settingComponent : this.settings) {
            settingComponent.onClick(x, y, mouse);
        }
    }

    public void mouseReleased(int x, int y, int m) {
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }

    }

    public void keyTyped(char t, int k) {
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    public void onScroll(int scroll) {
        for (Component component : this.settings) {
            component.onScroll(scroll);
        }
    }

    public void onGuiClosed() {
        for (Component c : this.settings) {
            c.onGuiClosed();
        }
        smoothTimer = null;
        hoverTimer = null;
        smoothingY = getHeight();
    }

    public boolean overModuleName(int x, int y) {
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() && y > this.category.getModuleY() + this.yPos && y < this.category.getModuleY() + 16 + this.yPos;
    }

    public boolean isVisible(Component component) {
        return component.isVisible();
    }

    private int mergeAlpha(int color, int alpha) {
        int newAlpha = (alpha & 0xFF) << 24;
        return (color & 0x00FFFFFF) | newAlpha;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.yPos = newOffsetY;
        int y = this.yPos + 16;

        for (Component c : this.settings) {
            c.setComponentStartAt(y);
            if (c.isVisible()) {
                y += c.getHeight();
            }
        }
    }

    @Override
    public void draw(AtomicInteger offset) {
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
    }

    @Override
    public void mouseDown(int x, int y, int button) {
    }
}
