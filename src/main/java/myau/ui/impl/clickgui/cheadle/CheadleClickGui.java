package myau.ui.impl.clickgui.cheadle;

import com.google.gson.GsonBuilder;
import myau.module.modules.ClickGUIModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.Myau;
import myau.font.impl.UFontRenderer;
import myau.module.Module;
import myau.module.modules.HUD;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.callback.GuiInput;
import myau.ui.impl.clickgui.raven.dataset.Slider;
import myau.ui.impl.clickgui.raven.dataset.impl.FloatSlider;
import myau.ui.impl.clickgui.raven.dataset.impl.IntSlider;
import myau.ui.impl.clickgui.raven.dataset.impl.PercentageSlider;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CheadleClickGui extends GuiScreen {
    private static CheadleClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui_cheadle.txt");
    private final ArrayList<CategoryComponent> categoryList;

    // ── Category name sets (normalized) ───────────────────────────────────────
    private static final Set<String> COMBAT = set(
            "AimAssist", "AutoClicker", "KillAura", "Wtap", "Velocity", "Reach", "TargetStrafe", "NoHitDelay",
            "AntiFireball", "LagRange", "MoveFix", "ServerLag", "KnockbackDelay", "HitBox", "MoreKB", "Refill",
            "HitSelect", "BackTrack", "Hitflick", "TimerRange", "ClickAssits", "Criticals", "BlockHit",
            "SprintReset", "Displace", "TickBase", "Piercing", "Stasis");
    private static final Set<String> MOVEMENT = set(
            "AntiAFK", "Fly", "FastBow", "Speed", "LongJump", "Sprint", "SafeWalk", "Jesus", "Blink", "NoFall",
            "NoSlow", "KeepSprint", "Eagle", "NoJumpDelay", "AntiVoid", "Timer");
    private static final Set<String> RENDER = set(
            "ESP", "Chams", "FullBright", "Tracers", "NameTags", "Xray", "TargetESP", "TargetHUD", "Indicators",
            "BedESP", "ItemESP", "BreakProgress", "ViewClip", "NoHurtCam", "HUD", "RiseClickGUI",
            "ClickGUI", "ChestESP", "Trajectories", "Radar", "RenderFixes", "FPScounter", "WaterMark", "WaterMark2",
            "HitParticleEffects", "DynamicIsland", "ESP2D", "TeamHealthDisplay", "Statistics", "Animations",
            "BlockOverlay", "Ambience", "Capes", "FreeLook", "ItemPhysics");
    private static final Set<String> PLAYER = set(
            "AutoHeal", "FakeLag", "AutoTool", "ChestStealer", "AutoBedDef", "InvManager", "InvWalk", "Scaffold",
            "AutoBlockIn", "AutoSwap", "SpeedMine", "FastPlace", "GhostHand", "MCF", "AntiDebuff", "FlagDetector",
            "AutoGapple", "ChestAura", "AutoHeadHitter", "ThrowAura", "AutoAuth");

    private static Set<String> set(String... names) {
        Set<String> s = new HashSet<>();
        for (String n : names) s.add(norm(n));
        return s;
    }

    private static String norm(String s) {
        return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static UFontRenderer font() {
        return Myau.fontManagers.getFont(16);
    }

    public CheadleClickGui() {
        instance = this;

        List<Module> combatModules = new ArrayList<>();
        List<Module> movementModules = new ArrayList<>();
        List<Module> renderModules = new ArrayList<>();
        List<Module> playerModules = new ArrayList<>();
        List<Module> miscModules = new ArrayList<>();

        for (Module module : Myau.moduleManager.modules.values()) {
            String n = norm(module.getName());
            if (COMBAT.contains(n)) {
                combatModules.add(module);
            } else if (MOVEMENT.contains(n)) {
                movementModules.add(module);
            } else if (RENDER.contains(n)) {
                renderModules.add(module);
            } else if (PLAYER.contains(n)) {
                playerModules.add(module);
            } else {
                miscModules.add(module);
            }
        }

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());

        this.categoryList = new ArrayList<>();
        int xOffset = 105;
        int spacing = 105;

        List<Module> combat = new ArrayList<>(combatModules);
        combat.removeIf(m -> m == null);
        combat.sort(comparator);
        CategoryComponent combatCat = new CategoryComponent("combat", combat);
        combatCat.setX(xOffset);
        combatCat.setY(25);
        categoryList.add(combatCat);
        xOffset += spacing;

        List<Module> movement = new ArrayList<>(movementModules);
        movement.removeIf(m -> m == null);
        movement.sort(comparator);
        CategoryComponent movementCat = new CategoryComponent("movement", movement);
        movementCat.setX(xOffset);
        movementCat.setY(25);
        categoryList.add(movementCat);
        xOffset += spacing;

        List<Module> render = new ArrayList<>(renderModules);
        render.removeIf(m -> m == null);
        render.sort(comparator);
        CategoryComponent renderCat = new CategoryComponent("render", render);
        renderCat.setX(xOffset);
        renderCat.setY(25);
        categoryList.add(renderCat);
        xOffset += spacing;

        List<Module> player = new ArrayList<>(playerModules);
        player.removeIf(m -> m == null);
        player.sort(comparator);
        CategoryComponent playerCat = new CategoryComponent("player", player);
        playerCat.setX(xOffset);
        playerCat.setY(25);
        categoryList.add(playerCat);
        xOffset += spacing;

        List<Module> misc = new ArrayList<>(miscModules);
        misc.removeIf(m -> m == null);
        misc.sort(comparator);
        CategoryComponent miscCat = new CategoryComponent("misc", misc);
        miscCat.setX(xOffset);
        miscCat.setY(25);
        categoryList.add(miscCat);

        loadPositions();
    }

    public static CheadleClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> btnCat = categoryList.iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                c.mouseDown(x, y, mouseButton);
            }
        }
    }

    public void mouseReleased(int x, int y, int s) {
        if (s == 0) {
            Iterator<CategoryComponent> iterator = categoryList.iterator();

            CategoryComponent categoryComponent;
            while (iterator.hasNext()) {
                categoryComponent = iterator.next();
                categoryComponent.mousePressed(false);
            }

            iterator = categoryList.iterator();

            while (true) {
                do {
                    do {
                        if (!iterator.hasNext()) {
                            return;
                        }

                        categoryComponent = iterator.next();
                    } while (!categoryComponent.isOpened());
                } while (categoryComponent.getModules().isEmpty());

                for (Component component : categoryComponent.getModules()) {
                    component.mouseReleased(x, y, s);
                }
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = categoryList.iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
        Module guiModule = Myau.moduleManager.getModule("ClickGUI");
        if (guiModule instanceof myau.module.modules.ClickGUIModule && ((myau.module.modules.ClickGUIModule) guiModule).isSwitchingGuiStyle()) {
            return;
        }
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== INNER COMPONENT INTERFACE ====================

    private interface Component {
        void draw(AtomicInteger offset);

        void update(int mousePosX, int mousePosY);

        void mouseDown(int x, int y, int button);

        void mouseReleased(int x, int y, int button);

        void keyTyped(char chatTyped, int keyCode);

        void setComponentStartAt(int newOffsetY);

        int getHeight();

        boolean isVisible();
    }

    // ==================== INNER COMPONENT CLASSES ====================

    private class CategoryComponent {
        private final int MAX_HEIGHT = 320;
        public ArrayList<Component> modulesInCategory = new ArrayList<>();
        public String categoryName;
        private boolean categoryOpened;
        private int width;
        private int y;
        private int x;
        private final int bh;
        public boolean dragging;
        public int xx;
        public int yy;
        public boolean pin = false;
        private int scroll = 0;
        private double animScroll = 0;
        private int height = 0;
        private float openAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public CategoryComponent(String category, List<Module> modules) {
            this.categoryName = category;
            this.width = 95;
            this.x = 5;
            this.y = 5;
            this.bh = 16;
            this.xx = 0;
            this.categoryOpened = true;
            this.dragging = false;
            int tY = this.bh;
            for (Module mod : modules) {
                ModuleComponent b = new ModuleComponent(mod, this, tY);
                this.modulesInCategory.add(b);
                tY += 16;
            }
        }

        public ArrayList<Component> getModules() {
            return this.modulesInCategory;
        }

        public void setX(int n) {
            this.x = n;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void mousePressed(boolean d) {
            this.dragging = d;
        }

        public boolean isPin() {
            return this.pin;
        }

        public void setPin(boolean on) {
            this.pin = on;
        }

        public boolean isOpened() {
            return this.categoryOpened;
        }

        public void setOpened(boolean on) {
            this.categoryOpened = on;
        }

        public void render(FontRenderer renderer) {
            this.width = 95;
            update();

            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            float targetAnim = categoryOpened ? 1F : 0F;
            openAnimation += (targetAnim - openAnimation) * Math.min(deltaTime * 8F, 1F);

            height = 0;
            for (Component moduleRenderManager : this.modulesInCategory) {
                height += moduleRenderManager.getHeight();
            }
            int maxScroll = Math.max(0, height - MAX_HEIGHT);
            if (scroll > maxScroll) scroll = maxScroll;
            if (animScroll > maxScroll) animScroll = maxScroll;
            animScroll += (scroll - animScroll) * 0.2;

            if (!this.modulesInCategory.isEmpty() && openAnimation > 0.01F) {
                int displayHeight = Math.min(height, MAX_HEIGHT);
                int animatedHeight = (int) (displayHeight * openAnimation);

                Gui.drawRect(this.x, this.y + this.bh,
                        this.x + this.width, this.y + this.bh + animatedHeight,
                        new Color(20, 20, 25, 250).getRGB());
            }

            RenderUtil.drawGradientRect(this.x, this.y, this.x + this.width, this.y + this.bh,
                    new Color(30, 30, 35, 255).getRGB(), new Color(25, 25, 30, 255).getRGB());

            GlStateManager.pushMatrix();
            String displayName = categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
            float textX = this.x + 4;
            font().drawString(displayName, textX, this.y + 5, new Color(200, 200, 200).getRGB());

            String closeButton = "X";
            float closeX = this.x + this.width - font().getStringWidth(closeButton) - 5;
            font().drawString(closeButton, closeX, this.y + 5, new Color(180, 180, 180).getRGB());

            GlStateManager.popMatrix();

            if (openAnimation > 0.01F && !this.modulesInCategory.isEmpty()) {
                int renderHeight = 0;
                ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
                double scale = sr.getScaleFactor();
                int displayHeight = Math.min(height, MAX_HEIGHT);
                int animatedHeight = (int) (displayHeight * openAnimation);
                int bottom = this.y + this.bh + animatedHeight;

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor((int) (this.x * scale), (int) ((sr.getScaledHeight() - bottom) * scale),
                        (int) (this.width * scale), (int) (animatedHeight * scale));

                GlStateManager.pushMatrix();
                GlStateManager.color(1F, 1F, 1F, openAnimation);

                for (Component c2 : this.modulesInCategory) {
                    int compHeight = c2.getHeight();
                    if (renderHeight + compHeight > animScroll &&
                            renderHeight < animScroll + MAX_HEIGHT) {
                        int drawY = (int) (renderHeight - animScroll);
                        c2.setComponentStartAt(this.bh + drawY);
                        c2.draw(new AtomicInteger(0));
                    }
                    renderHeight += compHeight;
                }

                GlStateManager.popMatrix();
                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                if (height > MAX_HEIGHT) {
                    float scrollbarHeight = ((float) MAX_HEIGHT * MAX_HEIGHT / height);
                    float scrollY = (float) this.y + this.bh + (float) (animScroll * MAX_HEIGHT / height);

                    Gui.drawRect(this.x + this.width - 2, this.y + this.bh,
                            this.x + this.width, this.y + this.bh + animatedHeight,
                            new Color(15, 15, 20, 180).getRGB());

                    Gui.drawRect(this.x + this.width - 2, (int) scrollY,
                            this.x + this.width, (int) (scrollY + scrollbarHeight),
                            new Color(100, 60, 180, 200).getRGB());
                }
            }
        }

        public void update() {
            int offset = this.bh;
            for (Component component : this.modulesInCategory) {
                component.setComponentStartAt(offset);
                offset += component.getHeight();
            }
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getWidth() {
            return this.width;
        }

        public void handleDrag(int x, int y) {
            if (this.dragging) {
                this.setX(x - this.xx);
                this.setY(y - this.yy);
            }
        }

        public boolean isHovered(int x, int y) {
            return x >= this.x + this.width - 20 && x <= this.x + this.width && y >= this.y && y <= this.y + this.bh;
        }

        public boolean insideArea(int x, int y) {
            return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.bh;
        }

        public String getName() {
            return categoryName;
        }

        public void setLocation(int parseInt, int parseInt1) {
            this.x = parseInt;
            this.y = parseInt1;
        }

        public void onScroll(int mouseX, int mouseY, int scrollAmount) {
            if (!categoryOpened || height <= MAX_HEIGHT) return;

            int areaTop = this.y + this.bh;
            int areaBottom = this.y + this.bh + MAX_HEIGHT;

            if (mouseX >= this.x && mouseX <= this.x + width && mouseY >= areaTop && mouseY <= areaBottom) {
                scroll -= scrollAmount * 12;
                scroll = Math.max(0, Math.min(scroll, height - MAX_HEIGHT));
            }
        }
    }

    private class ModuleComponent implements Component {
        public Module mod;
        public CategoryComponent category;
        public int offsetY;
        private final ArrayList<Component> settings;
        public boolean panelExpand;
        private float hoverAnimation = 0F;
        private float enableAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
            this.mod = mod;
            this.category = category;
            this.offsetY = offsetY;
            this.settings = new ArrayList<>();
            this.panelExpand = false;
            int y = offsetY + 10;
            if (!Myau.propertyManager.properties.get(mod.getClass()).isEmpty()) {
                for (Property<?> baseProperty : Myau.propertyManager.properties.get(mod.getClass())) {
                    if (baseProperty instanceof BooleanProperty) {
                        CheckBoxComponent c = new CheckBoxComponent((BooleanProperty) baseProperty, this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof FloatProperty) {
                        SliderComponent c = new SliderComponent(new FloatSlider((FloatProperty) baseProperty), this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof IntProperty) {
                        SliderComponent c = new SliderComponent(new IntSlider((IntProperty) baseProperty), this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof PercentProperty) {
                        SliderComponent c = new SliderComponent(new PercentageSlider((PercentProperty) baseProperty), this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof ModeProperty) {
                        ModeComponent c = new ModeComponent((ModeProperty) baseProperty, this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof ColorProperty) {
                        ColorSliderComponent c = new ColorSliderComponent((ColorProperty) baseProperty, this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    } else if (baseProperty instanceof TextProperty) {
                        TextComponent c = new TextComponent((TextProperty) baseProperty, this, y);
                        this.settings.add(c);
                        y += c.getHeight();
                    }
                }
            }
            this.settings.add(new BindComponent(this, y));
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
            int y = this.offsetY + 16;
            for (Component c : this.settings) {
                c.setComponentStartAt(y);
                if (c.isVisible()) {
                    y += c.getHeight();
                }
            }
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            float targetEnable = this.mod.isEnabled() ? 1F : 0F;
            enableAnimation += (targetEnable - enableAnimation) * Math.min(deltaTime * 8F, 1F);

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = category.getWidth() - Mouse.getY() * category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            boolean hovered = isHovered(mouseX, mouseY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            int startX = this.category.getX();
            int startY = this.category.getY() + this.offsetY;
            int endX = this.category.getX() + this.category.getWidth();

            if (enableAnimation > 0.01F) {
                int alpha = (int) (25 * enableAnimation);
                Gui.drawRect(startX + 1, startY, endX - 1, startY + 16,
                        new Color(100, 60, 180, alpha).getRGB());
            }

            if (hoverAnimation > 0.01F && !this.mod.isEnabled()) {
                int alpha = (int) (20 * hoverAnimation);
                Gui.drawRect(startX + 1, startY, endX - 1, startY + 16,
                        new Color(60, 60, 70, alpha).getRGB());
            }

            int textColor;
            if (this.mod.isEnabled()) {
                textColor = new Color(220, 220, 220).getRGB();
            } else {
                int gray = 140 + (int) (30 * hoverAnimation);
                textColor = new Color(gray, gray, gray).getRGB();
            }

            float textX = this.category.getX() + 6;
            font().drawString(this.mod.getName(), textX, this.category.getY() + this.offsetY + 5, textColor);

            if (!this.settings.isEmpty()) {
                String arrow = this.panelExpand ? "-" : "+";
                float arrowX = this.category.getX() + this.category.getWidth() - 10;
                font().drawString(arrow, arrowX, this.category.getY() + this.offsetY + 5, new Color(150, 150, 150).getRGB());
            }

            if (this.panelExpand && !this.settings.isEmpty()) {
                for (Component c : this.settings) {
                    if (c.isVisible()) {
                        c.draw(offset);
                        offset.incrementAndGet();
                    }
                }
            }
        }

        public int getHeight() {
            if (!this.panelExpand) {
                return 16;
            } else {
                int h = 16;
                for (Component c : this.settings) {
                    if (c.isVisible()) {
                        h += c.getHeight();
                    }
                }
                return h;
            }
        }

        public void update(int mousePosX, int mousePosY) {
            if (!panelExpand) return;
            if (!this.settings.isEmpty()) {
                for (Component c : this.settings) {
                    if (c.isVisible()) {
                        c.update(mousePosX, mousePosY);
                    }
                }
            }
        }

        public void mouseDown(int x, int y, int button) {
            if (this.isHovered(x, y) && button == 0) {
                this.mod.toggle();
            }
            if (this.isHovered(x, y) && button == 1) {
                this.panelExpand = !this.panelExpand;
            }
            if (!panelExpand) return;
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.mouseDown(x, y, button);
                }
            }
        }

        public void mouseReleased(int x, int y, int button) {
            if (!panelExpand) return;
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.mouseReleased(x, y, button);
                }
            }
        }

        public void keyTyped(char chatTyped, int keyCode) {
            if (!panelExpand) return;
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.keyTyped(chatTyped, keyCode);
                }
            }
        }

        public boolean isHovered(int x, int y) {
            return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() &&
                    y > this.category.getY() + this.offsetY && y < this.category.getY() + 16 + this.offsetY;
        }

        public boolean isVisible() {
            return true;
        }
    }

    private class BindComponent implements Component {
        private boolean isBinding;
        private final ModuleComponent parentModule;
        private int offsetY;
        private int x;
        private int y;

        public BindComponent(ModuleComponent b, int offsetY) {
            this.parentModule = b;
            this.x = b.category.getX() + b.category.getWidth();
            this.y = b.category.getY() + b.offsetY;
            this.offsetY = offsetY;
        }

        public void draw(AtomicInteger offset) {
            GL11.glPushMatrix();
            GL11.glScaled(0.5D, 0.5D, 0.5D);
            this.renderText(this.isBinding ? "Press a key..." : "Bind" + ": " + Keyboard.getKeyName(this.parentModule.mod.getKey()),
                    ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get()));
            GL11.glPopMatrix();
        }

        public void update(int mousePosX, int mousePosY) {
            this.y = this.parentModule.category.getY() + this.offsetY;
            this.x = this.parentModule.category.getX();
        }

        public void mouseDown(int x, int y, int button) {
            if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
                this.isBinding = !this.isBinding;
            }
        }

        public void mouseReleased(int x, int y, int button) {
        }

        public void keyTyped(char chatTyped, int keyCode) {
            if (this.isBinding) {
                if (keyCode == 11) {
                    if (this.parentModule.mod instanceof ClickGUIModule) {
                        this.parentModule.mod.setKey(54);
                    } else {
                        this.parentModule.mod.setKey(0);
                    }
                } else {
                    this.parentModule.mod.setKey(keyCode);
                }
                this.isBinding = false;
            }
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
        }

        public boolean isHovered(int x, int y) {
            return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y - 1 && y < this.y + 12;
        }

        public int getHeight() {
            return 12;
        }

        public boolean isVisible() {
            return true;
        }

        private void renderText(String s, int color) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(s, (float) ((this.parentModule.category.getX() + 4) * 2),
                    (float) ((this.parentModule.category.getY() + this.offsetY + 3) * 2), color);
        }
    }

    private class CheckBoxComponent implements Component {
        private final BooleanProperty property;
        private final ModuleComponent module;
        private int offsetY;
        private int x;
        private int y;
        private float hoverAnimation = 0F;
        private float checkAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public CheckBoxComponent(BooleanProperty property, ModuleComponent parentModule, int offsetY) {
            this.property = property;
            this.module = parentModule;
            this.x = parentModule.category.getX() + parentModule.category.getWidth();
            this.y = parentModule.category.getY() + parentModule.offsetY;
            this.offsetY = offsetY;
            this.checkAnimation = property.getValue() ? 1F : 0F;
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * module.category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = module.category.getWidth() - Mouse.getY() * module.category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            boolean hovered = isHovered(mouseX, mouseY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            float targetCheck = property.getValue() ? 1F : 0F;
            checkAnimation += (targetCheck - checkAnimation) * Math.min(deltaTime * 10F, 1F);

            int boxSize = 10;
            int boxX = this.module.category.getX() + this.module.category.getWidth() - boxSize - 5;
            int boxY = this.module.category.getY() + this.offsetY + 2;

            if (hoverAnimation > 0.01F) {
                int hoverAlpha = (int) (40 * hoverAnimation);
                Gui.drawRect(this.module.category.getX() + 2, this.module.category.getY() + this.offsetY,
                        this.module.category.getX() + this.module.category.getWidth() - 2,
                        this.module.category.getY() + this.offsetY + 14,
                        new Color(60, 60, 70, hoverAlpha).getRGB());
            }

            Gui.drawRect(boxX, boxY, boxX + boxSize, boxY + boxSize, new Color(20, 20, 25, 220).getRGB());

            Color borderColor = checkAnimation > 0.01F ? new Color(80, 150, 255) : new Color(60, 60, 70);
            drawHollowRect(boxX, boxY, boxX + boxSize, boxY + boxSize, borderColor.getRGB());

            if (checkAnimation > 0.01F) {
                int fillSize = (int) ((boxSize - 2) * checkAnimation);
                int fillOffset = (boxSize - fillSize) / 2;
                Gui.drawRect(boxX + fillOffset, boxY + fillOffset, boxX + fillOffset + fillSize, boxY + fillOffset + fillSize,
                        new Color(80, 150, 255, (int) (200 * checkAnimation)).getRGB());
            }

            if (checkAnimation > 0.4F) {
                float checkAlpha = Math.min(1F, (checkAnimation - 0.4F) / 0.6F);
                GlStateManager.pushMatrix();
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GL11.glLineWidth(1.5F);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glColor4f(1F, 1F, 1F, checkAlpha);
                float progress = Math.min(1F, (checkAnimation - 0.4F) / 0.6F);
                GL11.glVertex2f(boxX + 2, boxY + 5);
                if (progress > 0.3F) {
                    GL11.glVertex2f(boxX + 4, boxY + 7);
                }
                if (progress > 0.6F) {
                    GL11.glVertex2f(boxX + 8, boxY + 3);
                }
                GL11.glEnd();
                GlStateManager.enableTexture2D();
                GlStateManager.popMatrix();
            }

            String displayName = this.property.getName().replace("-", " ");
            Color textColor;
            if (checkAnimation > 0.01F) {
                textColor = new Color(220, 220, 220);
            } else {
                int gray = 170 + (int) (30 * hoverAnimation);
                textColor = new Color(gray, gray, gray);
            }
            font().drawString(displayName, this.module.category.getX() + 5,
                    this.module.category.getY() + this.offsetY + 4, textColor.getRGB());
        }

        private void drawHollowRect(int left, int top, int right, int bottom, int color) {
            Gui.drawRect(left, top, right, top + 1, color);
            Gui.drawRect(left, bottom - 1, right, bottom, color);
            Gui.drawRect(left, top, left + 1, bottom, color);
            Gui.drawRect(right - 1, top, right, bottom, color);
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
        }

        public int getHeight() {
            return 14;
        }

        public void update(int mousePosX, int mousePosY) {
            this.y = this.module.category.getY() + this.offsetY;
            this.x = this.module.category.getX();
        }

        public void mouseDown(int x, int y, int button) {
            if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
                this.property.setValue(!this.property.getValue());
            }
        }

        public void mouseReleased(int x, int y, int button) {
        }

        public void keyTyped(char chatTyped, int keyCode) {
        }

        public boolean isHovered(int x, int y) {
            return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + 14;
        }

        public boolean isVisible() {
            return property.isVisible();
        }
    }

    private class ColorSliderComponent implements Component {
        private final ModuleComponent parentModule;
        private final ColorProperty property;
        private int offsetY;
        private boolean draggingHue, draggingSat, draggingBri;
        private float hue, saturation, brightness;
        private float hoverAnimation = 0F;
        private float dragAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
            this.parentModule = parentModule;
            this.offsetY = offsetY;
            this.property = property;
            Color c = new Color(property.getValue());
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = parentModule.category.getWidth() - Mouse.getY() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            int baseY = parentModule.category.getY() + offsetY + 10;
            int satY = baseY + 6;
            int briY = satY + 6;

            boolean hovered = isHovered(mouseX, mouseY, baseY) || isHovered(mouseX, mouseY, satY) || isHovered(mouseX, mouseY, briY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            boolean isDragging = draggingHue || draggingSat || draggingBri;
            float targetDrag = isDragging ? 1F : 0F;
            dragAnimation += (targetDrag - dragAnimation) * Math.min(deltaTime * 10F, 1F);

            int x = parentModule.category.getX() + 5;
            int y = parentModule.category.getY() + offsetY;
            int width = parentModule.category.getWidth() - 10;

            String label = property.getName().replace("-", " ") + ": ";
            font().drawString(label, x, y + 3, new Color(170, 170, 170).getRGB());

            if (!draggingHue && !draggingSat && !draggingBri) {
                Color color = new Color(property.getValue());
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                hue = hsb[0];
                saturation = hsb[1];
                brightness = hsb[2];
            }

            int colorPreviewSize = 8;
            int colorPreviewX = x + width - colorPreviewSize;
            int colorPreviewY = y + 2;
            int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
            Gui.drawRect(colorPreviewX, colorPreviewY, colorPreviewX + colorPreviewSize, colorPreviewY + colorPreviewSize, previewColor);

            drawHueBar(x, baseY, width);
            drawPointer(x, baseY, width, hue, draggingHue);

            drawGradientBar(x, satY, width, Color.WHITE.getRGB(), Color.getHSBColor(hue, 1f, 1f).getRGB());
            drawPointer(x, satY, width, saturation, draggingSat);

            drawGradientBar(x, briY, width, Color.BLACK.getRGB(), Color.getHSBColor(hue, saturation, 1f).getRGB());
            drawPointer(x, briY, width, brightness, draggingBri);
        }

        private void drawHueBar(int x, int y, int width) {
            for (int i = 0; i < width; i++) {
                float h = (float) i / (float) width;
                int color = Color.HSBtoRGB(h, 1f, 1f);
                Gui.drawRect(x + i, y, x + i + 1, y + 4, color);
            }
        }

        private void drawGradientBar(int x, int y, int width, int startColor, int endColor) {
            RenderUtil.drawGradientRect(x, y, x + width, y + 4, startColor, endColor);
        }

        private void drawPointer(int x, int y, int width, float value, boolean isDragging) {
            int posX = x + (int) (width * value);
            Gui.drawRect(posX - 1, y, posX + 1, y + 4, new Color(255, 255, 255, 240).getRGB());
            if (isDragging) {
                Gui.drawRect(posX - 2, y - 1, posX + 2, y + 5, new Color(255, 255, 255, 100).getRGB());
            }
        }

        public void update(int mouseX, int mouseY) {
            int baseX = parentModule.category.getX() + 5;
            int width = parentModule.category.getWidth() - 10;
            boolean changed = false;

            if (draggingHue) {
                hue = getSliderValue(mouseX, baseX, width);
                changed = true;
            }
            if (draggingSat) {
                saturation = getSliderValue(mouseX, baseX, width);
                changed = true;
            }
            if (draggingBri) {
                brightness = getSliderValue(mouseX, baseX, width);
                changed = true;
            }

            if (changed) {
                int signed = Color.HSBtoRGB(hue, saturation, brightness);
                property.setValue(new Color(signed).getRGB() & 0xFFFFFF);
            }
        }

        private float getSliderValue(int mouseX, int startX, int width) {
            double d = Math.min(width, Math.max(0, mouseX - startX));
            return (float) roundToPrecision(d / width, 3);
        }

        private double roundToPrecision(double v, int precision) {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(precision, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }

        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button != 0 || !parentModule.panelExpand) return;
            int baseY = parentModule.category.getY() + offsetY + 10;
            if (isHovered(mouseX, mouseY, baseY)) draggingHue = true;
            else if (isHovered(mouseX, mouseY, baseY + 6)) draggingSat = true;
            else if (isHovered(mouseX, mouseY, baseY + 12)) draggingBri = true;
        }

        public void mouseReleased(int x, int y, int button) {
            draggingHue = draggingSat = draggingBri = false;
        }

        private boolean isHovered(int mx, int my, int sliderY) {
            int startX = parentModule.category.getX() + 5;
            int endX = startX + parentModule.category.getWidth() - 10;
            return mx >= startX && mx <= endX && my >= sliderY && my <= sliderY + 4;
        }

        public boolean isVisible() {
            return property.isVisible();
        }

        public void keyTyped(char chatTyped, int keyCode) {
        }

        public void setComponentStartAt(int newOffsetY) {
            offsetY = newOffsetY;
        }

        public int getHeight() {
            return 28;
        }
    }

    private class ModeComponent implements Component {
        private final ModeProperty property;
        private final ModuleComponent parentModule;
        private int x;
        private int y;
        private int offsetY;
        private float hoverAnimation = 0F;
        private float changeAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public ModeComponent(ModeProperty desc, ModuleComponent parentModule, int offsetY) {
            this.property = desc;
            this.parentModule = parentModule;
            this.x = parentModule.category.getX() + parentModule.category.getWidth();
            this.y = parentModule.category.getY() + parentModule.offsetY;
            this.offsetY = offsetY;
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = parentModule.category.getWidth() - Mouse.getY() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            boolean hovered = isHovered(mouseX, mouseY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            if (changeAnimation > 0F) {
                changeAnimation = Math.max(0F, changeAnimation - deltaTime * 4F);
            }

            if (hoverAnimation > 0.01F) {
                int hoverAlpha = (int) (40 * hoverAnimation);
                Gui.drawRect(this.parentModule.category.getX() + 2, this.parentModule.category.getY() + this.offsetY,
                        this.parentModule.category.getX() + this.parentModule.category.getWidth() - 2,
                        this.parentModule.category.getY() + this.offsetY + 14,
                        new Color(60, 60, 70, hoverAlpha).getRGB());
            }

            if (changeAnimation > 0.01F) {
                int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))
                        .getColor(System.currentTimeMillis(), offset.get());
                Color accentColor = new Color(accentColorInt, true);
                int flashAlpha = (int) (50 * changeAnimation);
                Gui.drawRect(this.parentModule.category.getX() + 2, this.parentModule.category.getY() + this.offsetY,
                        this.parentModule.category.getX() + this.parentModule.category.getWidth() - 2,
                        this.parentModule.category.getY() + this.offsetY + 14,
                        new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), flashAlpha).getRGB());
            }

            String mode = this.property.getModeString();
            mode = mode.replace("_", " ");
            if (!mode.isEmpty()) {
                mode = mode.substring(0, 1).toUpperCase() + mode.substring(1).toLowerCase();
            }

            String label = this.property.getName() + ": ";
            Color labelColor = new Color(170, 170, 170);
            font().drawString(label, this.parentModule.category.getX() + 5,
                    this.parentModule.category.getY() + this.offsetY + 4, labelColor.getRGB());

            int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))
                    .getColor(System.currentTimeMillis(), offset.get());
            Color accentColor = new Color(accentColorInt, true);

            int modeR = accentColor.getRed();
            int modeG = accentColor.getGreen();
            int modeB = accentColor.getBlue();

            if (hoverAnimation > 0.01F) {
                modeR = Math.min(255, (int) (modeR + 50 * hoverAnimation));
                modeG = Math.min(255, (int) (modeG + 50 * hoverAnimation));
                modeB = Math.min(255, (int) (modeB + 50 * hoverAnimation));
            }

            if (changeAnimation > 0.01F) {
                modeR = Math.min(255, (int) (modeR + 80 * changeAnimation));
                modeG = Math.min(255, (int) (modeG + 80 * changeAnimation));
                modeB = Math.min(255, (int) (modeB + 80 * changeAnimation));
            }

            float modeX = this.parentModule.category.getX() + 5 + font().getStringWidth(label);
            font().drawString(mode, modeX, this.parentModule.category.getY() + this.offsetY + 4,
                    new Color(modeR, modeG, modeB).getRGB());

            if (hoverAnimation > 0.3F) {
                int arrowAlpha = (int) (180 * hoverAnimation);
                font().drawString("<", this.parentModule.category.getX() + this.parentModule.category.getWidth() - 20,
                        this.parentModule.category.getY() + this.offsetY + 4, new Color(150, 150, 150, arrowAlpha).getRGB());
                font().drawString(">", this.parentModule.category.getX() + this.parentModule.category.getWidth() - 10,
                        this.parentModule.category.getY() + this.offsetY + 4, new Color(150, 150, 150, arrowAlpha).getRGB());
            }
        }

        public void update(int mousePosX, int mousePosY) {
            this.y = this.parentModule.category.getY() + this.offsetY;
            this.x = this.parentModule.category.getX();
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
        }

        public int getHeight() {
            return 14;
        }

        public void mouseDown(int x, int y, int button) {
            if (isHovered(x, y)) {
                if (button == 0) {
                    this.property.nextMode();
                    changeAnimation = 1F;
                } else if (button == 1) {
                    this.property.previousMode();
                    changeAnimation = 1F;
                }
            }
        }

        public void mouseReleased(int x, int y, int button) {
        }

        public void keyTyped(char chatTyped, int keyCode) {
        }

        private boolean isHovered(int x, int y) {
            return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 14;
        }

        public boolean isVisible() {
            return property.isVisible();
        }
    }

    private class SliderComponent implements Component {
        private final Slider slider;
        private final ModuleComponent parentModule;
        private int offsetY;
        private int x;
        private int y;
        private boolean dragging = false;
        private double sliderWidth;
        private float hoverAnimation = 0F;
        private float dragAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public SliderComponent(Slider slider, ModuleComponent parentModule, int offsetY) {
            this.slider = slider;
            this.parentModule = parentModule;
            this.x = parentModule.category.getX() + parentModule.category.getWidth();
            this.y = parentModule.category.getY() + parentModule.offsetY;
            this.offsetY = offsetY;
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = parentModule.category.getWidth() - Mouse.getY() * parentModule.category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            boolean hovered = isLeftHalfHovered(mouseX, mouseY) || isRightHalfHovered(mouseX, mouseY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            float targetDrag = dragging ? 1F : 0F;
            dragAnimation += (targetDrag - dragAnimation) * Math.min(deltaTime * 10F, 1F);

            if (hoverAnimation > 0.01F) {
                int hoverAlpha = (int) (40 * hoverAnimation);
                Gui.drawRect(this.parentModule.category.getX() + 2, this.parentModule.category.getY() + this.offsetY,
                        this.parentModule.category.getX() + this.parentModule.category.getWidth() - 2,
                        this.parentModule.category.getY() + this.offsetY + 18,
                        new Color(60, 60, 70, hoverAlpha).getRGB());
            }

            String label = this.slider.getName() + ": ";
            String value = this.slider.getValueString();

            Color labelColor = new Color(170, 170, 170);
            font().drawString(label, this.parentModule.category.getX() + 5,
                    this.parentModule.category.getY() + this.offsetY + 3, labelColor.getRGB());

            Color valueColor = new Color(200, 200, 200);
            float valueX = this.parentModule.category.getX() + 5 + font().getStringWidth(label);
            font().drawString(value, valueX, this.parentModule.category.getY() + this.offsetY + 3, valueColor.getRGB());

            int sliderY = this.parentModule.category.getY() + this.offsetY + 12;
            int sliderLeft = this.parentModule.category.getX() + 5;
            int sliderRight = this.parentModule.category.getX() + this.parentModule.category.getWidth() - 5;

            Gui.drawRect(sliderLeft, sliderY, sliderRight, sliderY + 3, new Color(20, 20, 25, 200).getRGB());

            int sliderStart = sliderLeft;
            int sliderEnd = sliderLeft + (int) this.sliderWidth;
            if (sliderEnd - sliderStart > this.parentModule.category.getWidth() - 10) {
                sliderEnd = sliderStart + this.parentModule.category.getWidth() - 10;
            }

            if (sliderEnd > sliderStart) {
                int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))
                        .getColor(System.currentTimeMillis(), offset.get());
                Color accentColor = new Color(accentColorInt, true);

                int brightBoost = (int) (30 * Math.max(hoverAnimation, dragAnimation));
                Color brightColor = new Color(
                        Math.min(255, accentColor.getRed() + brightBoost),
                        Math.min(255, accentColor.getGreen() + brightBoost),
                        Math.min(255, accentColor.getBlue() + brightBoost), 230);

                Gui.drawRect(sliderStart, sliderY, sliderEnd, sliderY + 3, brightColor.getRGB());
            }

            if (sliderEnd > sliderStart) {
                int thumbSize = 3 + (int) (2 * dragAnimation);
                int thumbX = sliderEnd - thumbSize / 2;
                int thumbY = sliderY - 1;
                Gui.drawRect(thumbX, thumbY, thumbX + thumbSize, thumbY + 5, new Color(255, 255, 255, 240).getRGB());
            }
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
        }

        public int getHeight() {
            return 18;
        }

        public void update(int mousePosX, int mousePosY) {
            this.y = this.parentModule.category.getY() + this.offsetY;
            this.x = this.parentModule.category.getX();

            double d = Math.min(this.parentModule.category.getWidth() - 10, Math.max(0, mousePosX - this.x - 5));
            this.sliderWidth = (double) (this.parentModule.category.getWidth() - 10) *
                    (this.slider.getInput() - this.slider.getMin()) / (this.slider.getMax() - this.slider.getMin());

            if (this.dragging) {
                if (d == 0.0D) {
                    this.slider.setValue(this.slider.getMin());
                } else {
                    double rawValue = d / (double) (this.parentModule.category.getWidth() - 10) *
                            (this.slider.getMax() - this.slider.getMin()) + this.slider.getMin();

                    double increment = this.slider.getIncrement();
                    if (increment > 0) {
                        rawValue = Math.round(rawValue / increment) * increment;
                    }
                    double n = roundToPrecision(rawValue, 2);
                    n = Math.max(this.slider.getMin(), Math.min(this.slider.getMax(), n));
                    this.slider.setValue(n);
                }
            }
        }

        private double roundToPrecision(double v, int precision) {
            if (precision < 0) {
                return 0.0D;
            } else {
                BigDecimal bd = new BigDecimal(v);
                bd = bd.setScale(precision, RoundingMode.HALF_UP);
                return bd.doubleValue();
            }
        }

        public void mouseDown(int x, int y, int button) {
            if (this.isLeftHalfHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
                this.dragging = true;
            }
            if (this.isRightHalfHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
                this.dragging = true;
            }
        }

        public void mouseReleased(int x, int y, int button) {
            this.dragging = false;
        }

        public void keyTyped(char chatTyped, int keyCode) {
        }

        public boolean isLeftHalfHovered(int x, int y) {
            return x > this.x && x < this.x + this.parentModule.category.getWidth() / 2 + 1 && y > this.y && y < this.y + 18;
        }

        public boolean isRightHalfHovered(int x, int y) {
            return x > this.x + this.parentModule.category.getWidth() / 2 &&
                    x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 18;
        }

        public boolean isVisible() {
            return slider.isVisible();
        }
    }

    private class TextComponent implements Component {
        private final TextProperty property;
        private final ModuleComponent module;
        private int offsetY;
        private int x;
        private int y;
        private float hoverAnimation = 0F;
        private long lastFrameTime = System.currentTimeMillis();

        public TextComponent(TextProperty property, ModuleComponent parentModule, int offsetY) {
            this.property = property;
            this.module = parentModule;
            this.x = parentModule.category.getX() + parentModule.category.getWidth();
            this.y = parentModule.category.getY() + parentModule.offsetY;
            this.offsetY = offsetY;
        }

        public void draw(AtomicInteger offset) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastFrameTime) / 1000F;
            lastFrameTime = currentTime;

            int mouseX = 0, mouseY = 0;
            try {
                mouseX = Mouse.getX() * module.category.getWidth() / Minecraft.getMinecraft().displayWidth;
                mouseY = module.category.getWidth() - Mouse.getY() * module.category.getWidth() / Minecraft.getMinecraft().displayHeight - 1;
            } catch (Exception e) {
            }

            boolean hovered = isHovered(mouseX, mouseY);
            float targetHover = hovered ? 1F : 0F;
            hoverAnimation += (targetHover - hoverAnimation) * Math.min(deltaTime * 12F, 1F);

            if (hoverAnimation > 0.01F) {
                int hoverAlpha = (int) (40 * hoverAnimation);
                Gui.drawRect(this.module.category.getX() + 2, this.module.category.getY() + this.offsetY,
                        this.module.category.getX() + this.module.category.getWidth() - 2,
                        this.module.category.getY() + this.offsetY + 14,
                        new Color(60, 60, 70, hoverAlpha).getRGB());
            }

            int iconX = this.module.category.getX() + this.module.category.getWidth() - 12;
            int iconY = this.module.category.getY() + this.offsetY + 3;
            int iconSize = 8;

            int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))
                    .getColor(System.currentTimeMillis(), offset.get());
            Color accentColor = new Color(accentColorInt, true);

            int iconAlpha = 150 + (int) (50 * hoverAnimation);
            Gui.drawRect(iconX, iconY, iconX + iconSize, iconY + iconSize, new Color(40, 40, 50, iconAlpha).getRGB());

            if (hoverAnimation > 0.01F) {
                int pencilAlpha = (int) (200 * hoverAnimation);
                Gui.drawRect(iconX + 3, iconY + 2, iconX + 4, iconY + 3,
                        new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), pencilAlpha).getRGB());
                Gui.drawRect(iconX + 2, iconY + 3, iconX + 3, iconY + 4, new Color(180, 180, 180, pencilAlpha).getRGB());
                Gui.drawRect(iconX + 1, iconY + 4, iconX + 2, iconY + 5, new Color(160, 160, 160, pencilAlpha).getRGB());
            }

            String displayName = this.property.getName().replace("-", " ");
            String value = this.property.getValue();

            String label = displayName + ": ";
            font().drawString(label, this.module.category.getX() + 5,
                    this.module.category.getY() + this.offsetY + 4, new Color(170, 170, 170).getRGB());

            int valueR = accentColor.getRed();
            int valueG = accentColor.getGreen();
            int valueB = accentColor.getBlue();

            if (hoverAnimation > 0.01F) {
                valueR = Math.min(255, (int) (valueR + 40 * hoverAnimation));
                valueG = Math.min(255, (int) (valueG + 40 * hoverAnimation));
                valueB = Math.min(255, (int) (valueB + 40 * hoverAnimation));
            }

            String displayValue = value;
            float maxWidth = this.module.category.getWidth() - 30;
            if (font().getStringWidth(displayValue) > maxWidth) {
                while (font().getStringWidth(displayValue + "...") > maxWidth && displayValue.length() > 0) {
                    displayValue = displayValue.substring(0, displayValue.length() - 1);
                }
                displayValue += "...";
            }

            float valueX = this.module.category.getX() + 5 + font().getStringWidth(label);
            font().drawString(displayValue, valueX, this.module.category.getY() + this.offsetY + 4,
                    new Color(valueR, valueG, valueB).getRGB());
        }

        public void setComponentStartAt(int newOffsetY) {
            this.offsetY = newOffsetY;
        }

        public int getHeight() {
            return 14;
        }

        public void update(int mousePosX, int mousePosY) {
            this.y = this.module.category.getY() + this.offsetY;
            this.x = this.module.category.getX();
        }

        public void mouseDown(int x, int y, int button) {
            if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
                GuiInput.prompt(property.getName().replace("-", " "), property.getValue(), property::setValue, CheadleClickGui.getInstance());
            }
        }

        public void mouseReleased(int x, int y, int button) {
        }

        public void keyTyped(char chatTyped, int keyCode) {
        }

        public boolean isHovered(int x, int y) {
            return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + 14;
        }

        public boolean isVisible() {
            return property.isVisible();
        }
    }
}
