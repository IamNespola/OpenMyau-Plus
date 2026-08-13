package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class CategoryComponent {
    public List<ModuleComponent> modules = new CopyOnWriteArrayList<>();
    public Module.category category;
    public boolean opened;
    public int width;
    public int y;
    public int x;
    public int titleHeight;
    public boolean dragging;
    public int xx;
    public int yy;
    public boolean hovering = false;
    public boolean hoveringOverCategory = false;
    public Timer smoothTimer;
    private Timer textTimer;
    public Timer smoothScrollTimer;
    public ScaledResolution scale;
    public float big;
    private float bigSettings;
    private final int translucentBackground = new Color(0, 0, 0, 110).getRGB();
    private final int regularOutline = new Color(81, 99, 149).getRGB();
    private final int regularOutline2 = new Color(97, 67, 133).getRGB();
    private final int categoryNameColor = new Color(220, 220, 220).getRGB();
    private float lastHeight;
    public int moduleY;
    private int lastModuleY;
    private int screenHeight;
    private boolean scrolled;
    private int targetModuleY;
    private float closedHeight;

    public CategoryComponent(Module.category category) {
        this.category = category;
        this.width = 92;
        this.x = 5;
        this.moduleY = this.y = 5;
        this.titleHeight = 13;
        this.smoothTimer = null;
        this.textTimer = null;
        this.xx = 0;
        this.opened = false;
        this.dragging = false;
        int moduleRenderY = this.titleHeight + 3;
        this.scale = new ScaledResolution(Minecraft.getMinecraft());
        this.targetModuleY = this.moduleY;

        for (Module mod : Raven.getModuleManager().inCategory(this.category)) {
            ModuleComponent b = new ModuleComponent(mod, this, moduleRenderY);
            this.modules.add(b);
            moduleRenderY += 16;
        }
    }

    public List<ModuleComponent> getModules() {
        return this.modules;
    }

    public void reloadModules(boolean isProfile) {
        this.modules.clear();
        this.titleHeight = 13;
        int moduleRenderY = this.titleHeight + 3;

        if ((this.category == Module.category.profiles && isProfile)) {
            ModuleComponent manager = new ModuleComponent(new Manager(), this, moduleRenderY);
            this.modules.add(manager);

            if (Raven.profileManager == null) {
                return;
            }

            for (Profile profile : Raven.profileManager.profiles) {
                moduleRenderY += 16;
                ModuleComponent b = new ModuleComponent(profile.getModule(), this, moduleRenderY);
                this.modules.add(b);
            }
        } else {
            for (Module mod : Raven.getModuleManager().inCategory(this.category)) {
                ModuleComponent b = new ModuleComponent(mod, this, moduleRenderY);
                this.modules.add(b);
                moduleRenderY += 16;
            }
        }
    }

    public void setX(int n, boolean limit) {
        if (limit) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int screenW = sr.getScaledWidth();
            n = Math.max(n, 2);
            n = Math.min(n, screenW - this.width - 4);
        }
        this.x = n;
    }

    public void setY(int y, boolean limit) {
        if (limit) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int screenH = sr.getScaledHeight();
            float catHeight = this.titleHeight;

            y = Math.max(y, 1);
            int maxY = (int) (screenH - catHeight - 5);
            y = Math.min(y, maxY);
        }
        this.moduleY = this.y = y;
        this.targetModuleY = y;
    }

    public void overTitle(boolean d) {
        this.dragging = d;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void mouseClicked(boolean on) {
        this.opened = on;
        (this.smoothTimer = new Timer(500)).start();
        (this.textTimer = new Timer(200)).start();
    }

    public void openModule(ModuleComponent component) {
        if (!component.isOpened) {
            closedHeight = this.y + this.titleHeight + big + 4;
        }
        (this.smoothTimer = new Timer(200)).start();
    }

    public void onScroll(int mouseScrollInput) {
        for (Component component : this.modules) {
            component.onScroll(mouseScrollInput);
        }
        if (!hoveringOverCategory || !this.opened) {
            return;
        }
        int scrollSpeed = (int) Gui.scrollSpeed.getInput();
        if (mouseScrollInput > 0) {
            this.targetModuleY += scrollSpeed;
        } else if (mouseScrollInput < 0) {
            this.targetModuleY -= scrollSpeed;
        }
        scrolled = true;

        (smoothScrollTimer = new Timer(200)).start();
    }

    public void render(FontRenderer renderer) {
        this.targetModuleY = Math.min(this.targetModuleY, this.y);
        if (this.targetModuleY + this.bigSettings < this.y + this.big + this.titleHeight) {
            this.targetModuleY = (int) (this.y + this.big - this.bigSettings);
        }

        this.width = 92;
        int modulesHeight = 0;
        int settingsHeight = 0;
        if (!this.modules.isEmpty() && this.opened) {
            for (ModuleComponent c : this.modules) {
                settingsHeight += c.getHeight();
                int height = !c.isOpened ? 16 : c.getModuleHeight();
                if (modulesHeight + height > this.screenHeight * 0.9d) {
                    modulesHeight = (int) (this.screenHeight * 0.9d);
                    continue;
                }
                modulesHeight += c.getHeight();
            }
            big = modulesHeight;
            bigSettings = settingsHeight;
        }

        float middlePos = (float) (this.x + this.width / 2 - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.category.name()) / 2);
        float xPos = opened ? middlePos : this.x + 12;
        float extra = this.y + this.titleHeight + modulesHeight + 4;

        if (smoothTimer != null && System.currentTimeMillis() - smoothTimer.last >= 330) {
            smoothTimer = null;
        }

        if (extra != lastHeight && smoothTimer != null) {
            double diff = lastHeight - extra;
            if (diff < 0) {
                extra = smoothTimer.getValueFloat(lastHeight, this.y + this.titleHeight + modulesHeight + 4, 1);
            }
            else if (diff > 0) {
                extra = smoothTimer.getValueFloat(this.opened ? closedHeight : lastHeight, this.y + this.titleHeight + modulesHeight + 4, 1);
            }
        }

        float namePos = textTimer == null ? xPos : textTimer.getValueFloat(this.x + 12, middlePos, 1);
        if (!this.opened) {
            namePos = textTimer == null ? xPos : middlePos - textTimer.getValueFloat(0, this.width / 2 - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.category.name()) / 2 - 12, 1);
        }

        if (scrolled && smoothScrollTimer != null) {
            if (System.currentTimeMillis() - smoothScrollTimer.last <= 200) {
                float interpolated = smoothScrollTimer.getValueFloat(lastModuleY, targetModuleY, 4);
                moduleY = (int) interpolated;
            }
            else {
                moduleY = targetModuleY;
                scrolled = false;
                smoothScrollTimer = null;
            }
        }
        else {
            moduleY = targetModuleY;
        }
        lastModuleY = moduleY;

        lastHeight = extra;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(0, this.y - 2, this.x + this.width + 4, extra - this.y + 4);
        RenderUtils.drawRoundedGradientOutlinedRectangle(this.x - 2, this.y, this.x + this.width + 2, extra, 10, translucentBackground,
                ((opened || hovering) && Gui.rainBowOutlines.isToggled()) ? RenderUtils.setAlpha(Utils.getChroma(2, 0), 0.5) : regularOutline, ((opened || hovering) && Gui.rainBowOutlines.isToggled()) ? RenderUtils.setAlpha(Utils.getChroma(2, 700), 0.5) : regularOutline2);
        renderItemForCategory(this.category, this.x + 1, this.y + 4, opened || hovering);
        renderer.drawString(this.category.name(), namePos, (float) (this.y + 4), categoryNameColor, false);
        RenderUtils.scissor(0, this.y + this.titleHeight + 3, this.x + this.width + 4, extra - this.y - 4 - this.titleHeight);

        int prevY = this.y;
        this.y = this.moduleY;

        if (this.opened || smoothTimer != null) {
            for (Component c2 : this.modules) {
                c2.render();
            }
        }
        this.y = prevY;
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    public void updateHeight() {
        int y = this.titleHeight + 3;

        for (Component component : this.modules) {
            component.updateHeight(y);
            y += component.getHeight();
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getModuleY() {
        return this.moduleY;
    }

    public int getWidth() {
        return this.width;
    }

    public void mousePosition(int mouseX, int mouseY) {
        if (this.dragging) {
            int newX = mouseX - this.xx;
            int newY = mouseY - this.yy;

            if (Gui.limitToScreen.isToggled()) {
                ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
                int screenW = sr.getScaledWidth();
                int screenH = sr.getScaledHeight();

                float catHeight = this.titleHeight;

                newX = Math.max(newX, 2);
                newX = Math.min(newX, screenW - this.width - 4);

                newY = Math.max(newY, 1);
                int maxY = (int) (screenH - catHeight - 5);
                newY = Math.min(newY, maxY);
            }

            this.setX(newX, false);
            this.setY(newY, false);
        }

        hoveringOverCategory = overCategory(mouseX, mouseY);
        hovering = overTitle(mouseX, mouseY);
    }

    public boolean overTitle(int x, int y) {
        return x >= this.x && x <= this.x + this.width && (float) y >= (float) this.y + 2.0F && y <= this.y + this.titleHeight + 1;
    }

    public boolean overCategory(int x, int y) {
        return x >= this.x - 2 && x <= this.x + this.width + 2 && (float) y >= (float) this.y + 2.0F && y <= this.y + this.titleHeight + big + 1;
    }

    public boolean draggable(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.titleHeight;
    }

    public boolean overRect(int x, int y) {
        return x >= this.x - 2 && x <= this.x + this.width + 2 && y >= this.y && y <= lastHeight;
    }

    private void renderItemForCategory(Module.category category, int x, int y, boolean enchant) {
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        double scale = 0.55;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        ItemStack itemStack = null;
        switch (category) {
            case combat:
                itemStack = new ItemStack(Items.diamond_sword);
                break;
            case movement:
                itemStack = new ItemStack(Items.diamond_boots);
                break;
            case player:
                itemStack = new ItemStack(Items.golden_apple);
                break;
            case world:
                itemStack = new ItemStack(Items.map);
                break;
            case render:
                itemStack = new ItemStack(Items.ender_eye);
                break;
            case minigames:
                itemStack = new ItemStack(Items.gold_ingot);
                break;
            case fun:
                itemStack = new ItemStack(Items.slime_ball);
                break;
            case other:
                itemStack = new ItemStack(Items.clock);
                break;
            case client:
                itemStack = new ItemStack(Items.compass);
                break;
            case profiles:
                itemStack = new ItemStack(Items.book);
                break;
            case scripts:
                itemStack = new ItemStack(Items.redstone);
                break;
        }
        if (itemStack != null) {
            if (enchant) {
                if (category != Module.category.player) {
                    itemStack.addEnchantment(Enchantment.unbreaking, 2);
                } else {
                    itemStack.setItemDamage(1);
                }
            }
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.disableBlend();
            renderItem.renderItemAndEffectIntoGUI(itemStack, (int) (x / scale), (int) (y / scale));
            GlStateManager.enableBlend();
            RenderHelper.disableStandardItemLighting();
        }
        GlStateManager.scale(1, 1, 1);
        GlStateManager.popMatrix();
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public void limitPositions() {
        setX(this.x, true);
        setY(this.y, true);
    }
}
