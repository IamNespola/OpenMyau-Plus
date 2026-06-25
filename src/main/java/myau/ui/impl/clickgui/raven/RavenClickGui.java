package myau.ui.impl.clickgui.raven;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.config.Config;
import myau.module.Module;
import myau.module.modules.*;
import myau.ui.impl.clickgui.raven.components.BindComponent;
import myau.ui.impl.clickgui.raven.components.CategoryComponent;
import myau.ui.impl.clickgui.raven.components.ModuleComponent;
import myau.util.Timer;
import myau.util.shader.BlurUtils;
import myau.util.shader.RoundedUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class RavenClickGui extends GuiScreen {
    public static ArrayList<CategoryComponent> categories;
    private static RavenClickGui instance;
    private static boolean isNotFirstOpen;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final String clientName = "Myau+";
    private final String clientVersion = Myau.version;
    private final String developer = "dev, Nespola";
    public int originalScale;
    public int previousScale;
    public float updates;
    public long last;
    private Timer logoSmoothWidth;
    private Timer logoSmoothLength;
    private Timer smoothEntity;
    private Timer backgroundFade;
    private Timer blurSmooth;
    private boolean clickGuiOpen = false;
    private long openedTime;
    private int hudColorCached = Color.white.getRGB();
    private long lastHudColorUpdate = 0;
    private float cached;

    public RavenClickGui() {
        instance = this;
        categories = new ArrayList<>();
        int y = 5;

        List<Module> combatModules = collect(
                AimAssist.class, MoveFix.class, AutoClicker.class, KillAura.class, Wtap.class, Velocity.class, ServerLag.class,
                Reach.class, TargetStrafe.class, NoHitDelay.class, AntiFireball.class, KnockbackDelay.class,
                LagRange.class, HitBox.class, MoreKB.class, Refill.class, HitSelect.class, BackTrack.class,
                Hitflick.class, TimerRange.class, ClickAssits.class, Criticals.class, BlockHit.class,
                SprintReset.class, Displace.class, TickBase.class, Piercing.class);
        List<Module> movementModules = collect(
                AntiAFK.class, Fly.class, FastBow.class, myau.module.modules.Timer.class, Speed.class, LongJump.class,
                Sprint.class, SafeWalk.class, Jesus.class, Blink.class, NoFall.class, NoSlow.class, KeepSprint.class,
                Eagle.class, NoJumpDelay.class, AntiVoid.class);
        List<Module> renderModules = collect(
                ESP.class, Chams.class, FullBright.class, Tracers.class, NameTags.class, Xray.class, TargetESP.class,
                TargetHUD.class, Indicators.class, BedESP.class, ItemESP.class, BreakProgress.class, ViewClip.class,
                NoHurtCam.class, HUD.class, GuiModule.class, RiseClickGUIModule.class, ClickGUIModule.class,
                ChestESP.class, Trajectories.class, Radar.class, RenderFixes.class, FPScounter.class, WaterMark.class,
                WaterMark2.class, HitParticleEffects.class, DynamicIsland.class, ESP2D.class, TeamHealthDisplay.class,
                SeasonDisplay.class, Animations.class, BlockOverlay.class);
        List<Module> playerModules = collect(
                AutoHeal.class, FakeLag.class, AutoTool.class, ChestStealer.class, AutoBedDef.class, InvManager.class,
                InvWalk.class, Scaffold.class, AutoBlockIn.class, AutoSwap.class, SpeedMine.class, FastPlace.class,
                GhostHand.class, MCF.class, AntiDebuff.class, FlagDetector.class, AutoGapple.class,
                AutoHeadHitter.class, ThrowAura.class);
        List<Module> miscModules = collect(
                Spammer.class, BedNuker.class, AntiBot.class, BedTracker.class, LightningTracker.class, NoRotate.class,
                NickHider.class, AntiObbyTrap.class, AntiObfuscate.class, AutoAnduril.class, InventoryClicker.class,
                Disabler.class, ClientSpoofer.class, AutoHypixel.class, MouseRawInput.class, BedwarUtils.class);

        String[] names = {"Combat", "Movement", "Render", "Player", "Misc"};
        List<List<Module>> groups = Arrays.asList(combatModules, movementModules, renderModules, playerModules, miscModules);
        for (int i = 0; i < names.length; i++) {
            CategoryComponent categoryComponent = new CategoryComponent(names[i], groups.get(i));
            categoryComponent.setY(y, false);
            categories.add(categoryComponent);
            y += 20;
        }

        loadPositions();
    }

    @SafeVarargs
    private final List<Module> collect(Class<? extends Module>... classes) {
        List<Module> list = new ArrayList<>();
        for (Class<? extends Module> c : classes) {
            Module m = Myau.moduleManager.getModule(c);
            if (m != null) {
                list.add(m);
            }
        }
        list.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        return list;
    }

    public static RavenClickGui getInstance() {
        return instance;
    }

    public void initMain() {
        (this.logoSmoothWidth = new Timer(500.0F)).start();
        (this.smoothEntity = new Timer(500.0F)).start();
        (this.blurSmooth = new Timer(500.0F)).start();
        (this.backgroundFade = new Timer(500.0F)).start();
        (this.logoSmoothLength = new Timer(500.0F)).start();
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!isNotFirstOpen) {
            isNotFirstOpen = true;
            this.previousScale = 2;
        }
        initMain();
        ScaledResolution sr = new ScaledResolution(this.mc);
        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenHeight(sr.getScaledHeight());
        }
        this.previousScale = 2;
    }

    @Override
    public void drawScreen(int x, int y, float p) {
        if (RenderFixes.shouldUseShaders()) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(0, 0, this.width, this.height, 0.0f, true, Color.black);
            float inputToRange = 1.5f;
            BlurUtils.blurEnd(2, this.blurSmooth.getValueFloat(0, inputToRange, 1));
        }

        drawRect(0, 0, this.width, this.height, (int) (this.backgroundFade.getValueFloat(0.0F, 0.7F, 2) * 255.0F) << 24);

        // Update cached HUD color every 50ms
        if (System.currentTimeMillis() - lastHudColorUpdate > 50) {
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            hudColorCached = hud.getColor(System.currentTimeMillis()).getRGB();
            lastHudColorUpdate = System.currentTimeMillis();
        }

        int h = this.height / 4;
        int wd = this.width / 2;
        int w_c = 30 - this.logoSmoothWidth.getValueInt(0, 30, 3);

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        int hudColor1 = hud.getColor(System.currentTimeMillis()).getRGB();
        int hudColor2 = hud.getColor(System.currentTimeMillis() + 300).getRGB();
        int hudColor3 = hud.getColor(System.currentTimeMillis() + 600).getRGB();
        int hudColor4 = hud.getColor(System.currentTimeMillis() + 900).getRGB();
        int hudColor5 = hud.getColor(System.currentTimeMillis() + 1200).getRGB();
        int hudColor6 = hud.getColor(System.currentTimeMillis() + 1500).getRGB();

        this.drawCenteredString(this.fontRendererObj, "M", wd + 1 - w_c, h - 24, hudColor6);
        this.drawCenteredString(this.fontRendererObj, "y", wd - w_c, h - 12, hudColor5);
        this.drawCenteredString(this.fontRendererObj, "a", wd - w_c, h + 0, hudColor4);
        this.drawCenteredString(this.fontRendererObj, "u", wd - w_c, h + 12, hudColor3);
        this.drawCenteredString(this.fontRendererObj, "+", wd + 1 + w_c, h + 26, hudColor1);
        this.drawVerticalLine(wd - 10 - w_c, h - 30, h + 43, Color.white.getRGB());
        this.drawVerticalLine(wd + 10 + w_c, h - 30, h + 43, Color.white.getRGB());
        if (this.logoSmoothLength != null) {
            int r = this.logoSmoothLength.getValueInt(0, 20, 2);
            this.drawHorizontalLine(wd - 10, wd - 10 + r, h - 29, -1);
            this.drawHorizontalLine(wd + 10, wd + 10 - r, h + 42, -1);
        }

        for (CategoryComponent c : categories) {
            c.render();
            c.mousePosition(x, y);

            for (Component m : c.getModules()) {
                m.drawScreen(x, y);
            }
        }

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        if (this.mc.thePlayer != null) {
            GuiInventory.drawEntityOnScreen(this.width + 15 - this.smoothEntity.getValueInt(0, 40, 2), this.height - 10, 40, (float) (this.width - 25 - x), (float) (this.height - 50 - y), this.mc.thePlayer);
        }
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();

        onRenderTick(p);
    }

    private void onRenderTick(float partialTicks) {
        if (!clickGuiOpen && this.mc.currentScreen instanceof RavenClickGui) {
            clickGuiOpen = true;
            initTimer(500.0F);
            startTimer();
            openedTime = System.currentTimeMillis();
        } else if (!(this.mc.currentScreen instanceof RavenClickGui)) {
            clickGuiOpen = false;
        } else {
            int[] displaySize = {this.width, this.height};
            int y = displaySize[1] + (8 - getValueInt(0, 30, 2));

            Myau.fontManagers.getFont(20).drawString(clientName + "-" + clientVersion, 4, y, hudColorCached, true);

            long elapsedTime = System.currentTimeMillis() - openedTime + 50L;
            int characterIndex = (int) (elapsedTime / 200L);
            y += this.fontRendererObj.FONT_HEIGHT + 1;

            if (characterIndex < developer.length()) {
                String obfuscated = "";

                for (int i = 0; i < developer.length(); ++i) {
                    char currentChar = i < characterIndex
                            ? developer.charAt(i)
                            : (char) ((new java.util.Random()).nextInt(26) + 'a');
                    obfuscated += currentChar;
                }

                Myau.fontManagers.getFont(20).drawString(obfuscated, 4, y, hudColorCached, true);
            } else {
                Myau.fontManagers.getFont(20).drawString(developer, 4, y, hudColorCached, true);
            }
        }
    }

    public void initTimer(float updates) {
        this.updates = updates;
    }

    public void startTimer() {
        this.cached = 0.0F;
        this.last = System.currentTimeMillis();
    }

    public float getValueFloat(float begin, float end, int type) {
        if (this.cached == end) {
            return this.cached;
        } else {
            float t = (float) (System.currentTimeMillis() - this.last) / this.updates;
            switch (type) {
                case 1:
                    t = t < 0.5F ? 4.0F * t * t * t : (t - 1.0F) * (2.0F * t - 2.0F) * (2.0F * t - 2.0F) + 1.0F;
                    break;
                case 2:
                    t = (float) (1.0D - Math.pow(1.0F - t, 5.0D));
                    break;
            }

            float value = begin + t * (end - begin);
            if ((end > begin && value > end) || (end < begin && value < end)) {
                value = end;
            }

            if (value == end) {
                this.cached = value;
            }

            return value;
        }
    }

    public int getValueInt(int begin, int end, int type) {
        return Math.round(this.getValueFloat((float) begin, (float) end, type));
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            boolean draggingAssigned = false;
            for (int i = categories.size() - 1; i >= 0; i--) {
                CategoryComponent category = categories.get(i);
                if (!draggingAssigned && category.draggable(mouseX, mouseY)) {
                    category.overTitle(true);
                    category.xx = mouseX - category.getX();
                    category.yy = mouseY - category.getY();
                    category.dragging = true;
                    draggingAssigned = true;
                } else {
                    category.overTitle(false);
                }
            }
        }

        if (mouseButton == 1) {
            boolean toggled = false;
            for (int i = categories.size() - 1; i >= 0; i--) {
                CategoryComponent category = categories.get(i);
                if (!toggled && category.overTitle(mouseX, mouseY)) {
                    category.mouseClicked(!category.isOpened());
                    toggled = true;
                }
            }
        }

        for (CategoryComponent category : categories) {
            if (category.isOpened() && !category.getModules().isEmpty() && category.overRect(mouseX, mouseY)) {
                for (Component component : category.getModules()) {
                    if (component instanceof ModuleComponent) {
                        ModuleComponent moduleComponent = (ModuleComponent) component;
                        moduleComponent.onClick(mouseX, mouseY, mouseButton);
                        category.openModule(moduleComponent);
                    }
                }
            }
        }
    }

    public void mouseReleased(int x, int y, int button) {
        if (button == 0) {
            Iterator<CategoryComponent> iterator = categories.iterator();
            while (iterator.hasNext()) {
                CategoryComponent category = iterator.next();
                category.overTitle(false);
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.mouseReleased(x, y, button);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput);
            }
        }
    }

    @Override
    public void setWorldAndResolution(Minecraft p_setWorldAndResolution_1_, final int p_setWorldAndResolution_2_, final int p_setWorldAndResolution_3_) {
        this.mc = p_setWorldAndResolution_1_;
        originalScale = this.mc.gameSettings.guiScale;
        this.mc.gameSettings.guiScale = 2;
        this.itemRender = p_setWorldAndResolution_1_.getRenderItem();
        this.fontRendererObj = p_setWorldAndResolution_1_.fontRendererObj;
        final ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        this.width = scaledresolution.getScaledWidth();
        this.height = scaledresolution.getScaledHeight();
        this.buttonList.clear();
        this.initGui();
    }

    @Override
    public void keyTyped(char t, int k) {
        if (k == Keyboard.KEY_ESCAPE && !binding()) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> iterator = categories.iterator();
            while (iterator.hasNext()) {
                CategoryComponent category = iterator.next();

                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.keyTyped(t, k);
                    }
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        this.logoSmoothLength = null;
        for (CategoryComponent c : categories) {
            c.dragging = false;
            for (Component m : c.getModules()) {
                m.onGuiClosed();
            }
        }
        this.mc.gameSettings.guiScale = originalScale;
        savePositions();
        saveCurrentConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (Component component : c.getModules()) {
                if (component instanceof ModuleComponent) {
                    ModuleComponent moduleComponent = (ModuleComponent) component;
                    for (Component setting : moduleComponent.settings) {
                        if (setting instanceof BindComponent && ((BindComponent) setting).isBinding) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categories) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            gson.toJson(json, writer);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCurrentConfig() {
        if (Config.lastConfig != null && !Config.lastConfig.isEmpty()) {
            Config config = new Config(Config.lastConfig, false);
            config.save();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
        try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
            com.google.gson.JsonObject json = parser.parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categories) {
                if (json.has(cat.getName())) {
                    com.google.gson.JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
