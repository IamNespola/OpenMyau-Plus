package myau.ui.impl.clickgui.rise;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.RiseClickGUIModule;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiseClickGUI extends GuiScreen {

    private enum Tab {
        SEARCH("Search", false),
        COMBAT("Combat", true),
        MOVEMENT("Movement", true),
        PLAYER("Player", true),
        RENDER("Render", true),
        EXPLOIT("Exploit", true),
        GHOST("Ghost", true),
        MISC("Misc", true),
        CAS("CaS", false),
        THEMES("Themes", false),
        LANGUAGE("Language", false);

        private final String label;
        private final boolean moduleTab;

        Tab(String label, boolean moduleTab) {
            this.label = label;
            this.moduleTab = moduleTab;
        }
    }

    private static RiseClickGUI instance;
    private static final float WINDOW_W = 400f;
    private static final float WINDOW_H = 300f;
    private static final float SIDEBAR_W = 100f;
    private static final float ROUND = 7f;
    private static final long ANIM_DURATION = 300L;
    private static final Map<String, Tab> MODULE_TABS = new HashMap<String, Tab>();

    static {
        map(Tab.GHOST, "AimAssist", "AutoClicker", "Reach", "Velocity", "WTap", "Wtap", "HitBox",
                "HitSelect", "BackTrack", "Hitflick", "MoreKB", "KnockbackDelay", "ClickAssits", "SprintReset", "BlockHit");
        map(Tab.COMBAT, "AntiBot", "KillAura", "TargetStrafe", "NoHitDelay", "AntiFireball", "LagRange", "Refill",
                "Criticals", "Displace", "ServerLag", "Teams");
        map(Tab.MOVEMENT, "AntiAFK", "Fly", "FastBow", "Speed", "LongJump", "Sprint", "SafeWalk",
                "Jesus", "NoFall", "NoSlow", "KeepSprint", "Eagle", "NoJumpDelay");
        map(Tab.PLAYER, "AutoHeal", "AutoTool", "ChestStealer", "InvManager", "InvWalk", "Scaffold",
                "AutoBlockIn", "AutoSwap", "SpeedMine", "FastPlace", "MCF", "AntiDebuff", "FlagDetector",
                "AutoGapple", "Gapple", "ThrowAura", "InventoryClicker", "PacketConsume");
        map(Tab.RENDER, "ESP", "Chams", "FullBright", "Fullbright", "Tracers", "NameTags", "Xray",
                "TargetESP", "TargetHUD", "Indicators", "BedESP", "ItemESP", "ViewClip", "NoHurtCam",
                "HUD", "ChestESP", "Trajectories", "Radar", "FPScounter", "Fpscounter", "WaterMark",
                "HitParticleEffects", "DynamicIsland", "ESP2D", "SeasonDisplay",
                "Animations", "RenderFixes", "ClickGUI", "ClickGui");
        map(Tab.EXPLOIT, "Disabler", "ClientSpoofer", "NoRotate", "AntiObfuscate", "Blink", "Timer",
                "TimerRangev999", "AntiVoid", "FakeLag", "GhostHand", "BedNuker", "BedTracker", "TeamDisplay");
    }

    private float windowX = -1f;
    private float windowY = -1f;
    private boolean firstOpen = true;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean closing;
    private float scaleAnimation;
    private float opacityAnimation;
    private long animationStart;
    private long openedAt;
    private long lastFrameTime;

    private Tab selectedTab = Tab.SEARCH;
    private Tab lastTab = selectedTab;
    private float transitionAlpha;
    private final float[] sidebarAnimations = new float[Tab.values().length];

    private final Map<Tab, List<RiseModuleCard>> moduleCards = new EnumMap<Tab, List<RiseModuleCard>>(Tab.class);
    private final List<RiseModuleCard> allCards = new ArrayList<RiseModuleCard>();
    private final List<RiseModuleCard> searchResults = new ArrayList<RiseModuleCard>();
    private String searchText = "";

    private float scrollOffset;
    private float targetScroll;
    private float screenScrollOffset;
    private float screenTargetScroll;
    private float screenContentHeight;

    private final RiseCaSScreen configScreen = new RiseCaSScreen();
    private final String[] languages = new String[]{
            "English", "Vietnamese", "Spanish", "French", "German", "Polish", "Chinese", "Japanese"
    };
    private int selectedLanguage;

    public RiseClickGUI() {
        rebuildModuleCache();
    }

    public static RiseClickGUI getInstance() {
        if (instance == null) instance = new RiseClickGUI();
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    public static Color accent() {
        if (Myau.moduleManager == null) return new Color(0x4FC3F7);
        RiseClickGUIModule module = (RiseClickGUIModule) Myau.moduleManager.getModule("RiseClickGUI");
        return module != null ? module.getAccentColor() : new Color(0x4FC3F7);
    }

    public static String getModuleCategoryName(Module module) {
        return resolveModuleTab(module).label;
    }

    private static void map(Tab tab, String... moduleNames) {
        for (String moduleName : moduleNames) {
            MODULE_TABS.put(key(moduleName), tab);
        }
    }

    private static Tab resolveModuleTab(Module module) {
        Tab tab = MODULE_TABS.get(key(module.getName()));
        return tab == null ? Tab.MISC : tab;
    }

    private static String key(String input) {
        return input == null ? "" : input.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private void rebuildModuleCache() {
        allCards.clear();
        moduleCards.clear();
        for (Tab tab : Tab.values()) {
            if (tab.moduleTab) moduleCards.put(tab, new ArrayList<RiseModuleCard>());
        }

        List<Module> modules = new ArrayList<Module>(Myau.moduleManager.modules.values());
        Collections.sort(modules, new Comparator<Module>() {
            private final Collator collator = Collator.getInstance();

            @Override
            public int compare(Module first, Module second) {
                return collator.compare(first.getName(), second.getName());
            }
        });

        for (Module module : modules) {
            RiseModuleCard card = new RiseModuleCard(module);
            allCards.add(card);
            Tab tab = resolveModuleTab(module);
            List<RiseModuleCard> list = moduleCards.get(tab);
            if (list == null) {
                list = moduleCards.get(Tab.MISC);
            }
            list.add(card);
        }
        updateSearchResults();
    }

    @Override
    public void initGui() {
        super.initGui();
        FontManager.initializeFonts();
        Keyboard.enableRepeatEvents(true);

        closing = false;
        animationStart = System.currentTimeMillis();
        openedAt = animationStart;
        lastFrameTime = System.nanoTime();

        ScaledResolution sr = new ScaledResolution(mc);
        if (firstOpen || windowX < 0 || windowY < 0 || windowX + WINDOW_W > sr.getScaledWidth()
                || windowY + WINDOW_H > sr.getScaledHeight()) {
            windowX = sr.getScaledWidth() / 2f - WINDOW_W / 2f;
            windowY = sr.getScaledHeight() / 2f - WINDOW_H / 2f;
            firstOpen = false;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        dragging = false;
        Module gui = Myau.moduleManager.getModule("RiseClickGUI");
        if (gui != null) gui.setEnabled(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float deltaTime = frameDelta();
        updateOpenCloseAnimation();
        if (closing && scaleAnimation <= 0.001f) {
            mc.displayGuiScreen(null);
            return;
        }

        if (dragging) {
            windowX = mouseX + dragOffsetX;
            windowY = mouseY + dragOffsetY;
        }

        updateScroll(deltaTime);
        updateScreenScroll(deltaTime);

        GlStateManager.pushMatrix();
        if (scaleAnimation < 0.999f) {
            float centerX = windowX + WINDOW_W / 2f;
            float centerY = windowY + WINDOW_H / 2f;
            GlStateManager.translate(centerX * (1f - scaleAnimation), centerY * (1f - scaleAnimation), 0);
            GlStateManager.scale(scaleAnimation, scaleAnimation, 1);
        }

        RenderUtil.drawRoundedRect(windowX, windowY, WINDOW_W, WINDOW_H, ROUND,
                RiseColors.withAlpha(RiseColors.BACKGROUND, (int) (254 * opacityAnimation)).getRGB(),
                true, true, true, true);
        drawWindowChrome();

        RenderUtil.scissor(windowX, windowY, WINDOW_W, WINDOW_H);
        drawSidebarGlow(accent());
        drawSidebar(mouseX, mouseY, deltaTime);

        float contentX = windowX + SIDEBAR_W + 8f;
        float contentY = windowY + 7f;
        float contentW = WINDOW_W - SIDEBAR_W - 16f;
        float contentH = WINDOW_H - 14f;

        RenderUtil.scissor(contentX, contentY, contentW, contentH);
        drawSelectedScreen(contentX, contentY, contentW, contentH, mouseX, mouseY, partialTicks, deltaTime);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        drawTransition(deltaTime);
        GlStateManager.popMatrix();
        handleInvWalk();
    }

    private float frameDelta() {
        long now = System.nanoTime();
        float deltaTime = Math.min((now - lastFrameTime) / 1_000_000_000.0f, 0.05f);
        lastFrameTime = now;
        return deltaTime;
    }

    private void updateOpenCloseAnimation() {
        float progress = Math.min(1f, (System.currentTimeMillis() - animationStart) / (float) ANIM_DURATION);
        if (closing) {
            scaleAnimation = 1f - easeOutExpo(progress);
            opacityAnimation = 1f - progress;
        } else {
            scaleAnimation = easeOutExpo(progress);
            opacityAnimation = Math.min(1f, progress * 1.5f);
        }
    }

    private void drawTransition(float deltaTime) {
        if (transitionAlpha <= 0.01f) return;
        transitionAlpha = AnimationUtil.animateSmooth(0f, transitionAlpha, 8f, deltaTime);
        RenderUtil.drawRoundedRect(windowX, windowY, WINDOW_W, WINDOW_H, ROUND,
                RiseColors.withAlpha(RiseColors.BACKGROUND, (int) (transitionAlpha * 210)).getRGB(),
                true, true, true, true);
    }

    private void drawWindowChrome() {
        int alpha = (int) (70 * opacityAnimation);
        RenderUtil.drawRoundedRectOutline(windowX + 0.5f, windowY + 0.5f, WINDOW_W - 1f, WINDOW_H - 1f,
                ROUND, 1f, new Color(255, 255, 255, alpha).getRGB(), true, true, true, true);
        RenderUtil.drawRect(windowX + SIDEBAR_W, windowY + 1f, windowX + SIDEBAR_W + 1f,
                windowY + WINDOW_H - 1f, new Color(255, 255, 255, (int) (18 * opacityAnimation)).getRGB());
    }

    private void drawSidebarGlow(Color accent) {
        Color glow = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 1);
        float centerX = windowX + SIDEBAR_W;
        float centerY = windowY + WINDOW_H / 2f;
        for (int i = 1; i <= 8; i++) {
            float radius = i * 50f;
            RenderUtil.drawRoundedRect(centerX - radius / 2f, centerY - radius / 2f, radius, radius, radius / 2f,
                    glow.getRGB(), true, true, true, true);
        }
    }

    private void drawSidebar(int mouseX, int mouseY, float deltaTime) {
        Color accent = accent();
        RenderUtil.drawRoundedRect(windowX, windowY, SIDEBAR_W, WINDOW_H, ROUND,
                RiseColors.withAlpha(RiseColors.SECONDARY, (int) (255 * opacityAnimation)).getRGB(),
                true, false, false, true);

        if (FontManager.productSans24 != null) {
            FontManager.productSans24.drawString("Myau", windowX + 14, windowY + 12,
                    RiseColors.withAlphaRGB(Color.WHITE, (int) (255 * opacityAnimation)));
        }
        if (FontManager.productSans12 != null) {
            float nameWidth = FontManager.productSans24 != null
                    ? (float) FontManager.productSans24.getStringWidth("Myau") : 38f;
            FontManager.productSans12.drawString("Rise", windowX + 18 + nameWidth, windowY + 14,
                    RiseColors.withAlpha(accent, (int) Math.min(200, 255 * opacityAnimation)).getRGB());
        }

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            float itemX = windowX + 6f;
            float itemY = tabY(i);
            float itemW = SIDEBAR_W - 12f;
            float itemH = 16f;
            boolean selected = selectedTab == tab;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + itemH;

            sidebarAnimations[i] = AnimationUtil.animateSmooth(selected ? 255f : 0f, sidebarAnimations[i], 10f, deltaTime);
            if (sidebarAnimations[i] > 1f) {
                RenderUtil.drawRoundedRect(itemX, itemY, itemW, itemH, 5,
                        new Color(accent.darker().getRed(), accent.darker().getGreen(), accent.darker().getBlue(),
                                (int) (sidebarAnimations[i] * opacityAnimation)).getRGB(),
                        true, true, true, true);
            } else if (hovered) {
                RenderUtil.drawRoundedRect(itemX, itemY, itemW, itemH, 5,
                        new Color(255, 255, 255, 10).getRGB(), true, true, true, true);
            }

            int textAlpha = (int) ((selected ? 255 : hovered ? 220 : 185) * opacityAnimation);
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(tab.label, itemX + 11,
                        centeredTextY(itemY, itemH, (float) FontManager.productSans16.getHeight()),
                        RiseColors.withAlphaRGB(Color.WHITE, textAlpha));
            }
            if (tab.moduleTab && FontManager.productSans12 != null) {
                List<RiseModuleCard> cards = moduleCards.get(tab);
                String count = String.valueOf(cards == null ? 0 : cards.size());
                float countW = (float) FontManager.productSans12.getStringWidth(count);
                FontManager.productSans12.drawString(count, itemX + itemW - countW - 8,
                        centeredTextY(itemY, itemH, (float) FontManager.productSans12.getHeight()),
                        RiseColors.withAlphaRGB(RiseColors.TEXT_TRINARY, textAlpha));
            }
        }
    }

    private float centeredTextY(float y, float height, float textHeight) {
        return y + (height - textHeight) / 2f + 0.5f;
    }

    private float tabY(int index) {
        return windowY + 31f + index * 17.2f;
    }

    private void drawSelectedScreen(float x, float y, float w, float h, int mouseX, int mouseY,
                                    float partialTicks, float deltaTime) {
        if (selectedTab == Tab.SEARCH) {
            drawSearchScreen(x, y, w, h, mouseX, mouseY, partialTicks, deltaTime);
        } else if (selectedTab.moduleTab) {
            drawModuleScreen(selectedTab, x, y, w, h, mouseX, mouseY, partialTicks, deltaTime);
        } else if (selectedTab == Tab.CAS) {
            configScreen.draw(x, y, w, h, mouseX, mouseY, deltaTime);
            drawScrollBar(x + w - 2, y, 2, h, configScreen.getScrollOffset(),
                    Math.max(0, configScreen.getLastContentHeight() - h));
        } else if (selectedTab == Tab.THEMES) {
            drawThemeScreen(x, y, w, h, mouseX, mouseY);
        } else if (selectedTab == Tab.LANGUAGE) {
            drawLanguageScreen(x, y, w, h, mouseX, mouseY);
        }
    }

    private void drawSearchScreen(float x, float y, float w, float h, int mouseX, int mouseY,
                                  float partialTicks, float deltaTime) {
        String display = searchText.isEmpty() ? "Start typing to search..." : searchText + "_";
        int color = searchText.isEmpty() ? RiseColors.TEXT_TRINARY.getRGB() : RiseColors.TEXT.getRGB();
        if (FontManager.productSans16 != null) {
            float textW = (float) FontManager.productSans16.getStringWidth(display);
            FontManager.productSans16.drawString(display, x + w / 2f - textW / 2f, y + 11, color);
        }
        drawModuleList(searchText.isEmpty() ? allCards : searchResults, x, y + 28, w, h - 28,
                mouseX, mouseY, partialTicks, deltaTime, true);
        drawScrollBar(x + w - 2, y + 28, 2, h - 28, scrollOffset, getMaxScroll());
    }

    private void drawModuleScreen(Tab tab, float x, float y, float w, float h, int mouseX, int mouseY,
                                  float partialTicks, float deltaTime) {
        drawModuleList(moduleCards.get(tab), x, y, w, h, mouseX, mouseY, partialTicks, deltaTime, false);
        drawScrollBar(x + w - 2, y, 2, h, scrollOffset, getMaxScroll());
    }

    private void drawModuleList(List<RiseModuleCard> cards, float x, float y, float w, float h, int mouseX, int mouseY,
                                float partialTicks, float deltaTime, boolean searchMode) {
        if (cards == null || cards.isEmpty()) {
            drawEmptyState(x, y, w, h, selectedTab == Tab.SEARCH ? "No modules found" : "No modules");
            return;
        }

        float cardY = y + 7f - scrollOffset;
        for (RiseModuleCard card : cards) {
            card.setX(x);
            card.setY(cardY);
            card.setCardWidth(w - 6f);
            card.draw(mouseX, mouseY, partialTicks, deltaTime, x, y, w, h, searchMode);
            cardY += card.getTotalHeight() + 7f;
        }
    }

    private void drawThemeScreen(float x, float y, float w, float h, int mouseX, int mouseY) {
        screenContentHeight = 190f;
        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawString("Themes", x + 7, y + 7, RiseColors.TEXT.getRGB());
        }
        if (FontManager.productSans12 != null) {
            FontManager.productSans12.drawString("Accent palette", x + 7, y + 25, RiseColors.TEXT_TRINARY.getRGB());
        }

        RiseClickGUIModule module = (RiseClickGUIModule) Myau.moduleManager.getModule("RiseClickGUI");
        int selected = module == null ? 0 : module.accentColor.getValue();
        float cardW = (w - 21f) / 2f;
        float cardH = 42f;
        for (int i = 0; i < RiseClickGUIModule.COLOR_NAMES.length; i++) {
            int col = i % 2;
            int row = i / 2;
            float cardX = x + 7 + col * (cardW + 7);
            float cardY = y + 47 + row * (cardH + 7);
            boolean hovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;
            Color color = new Color(RiseClickGUIModule.COLORS[i], true);

            RenderUtil.drawRoundedRect(cardX, cardY, cardW, cardH, 6,
                    new Color(0, 0, 0, hovered ? 72 : 45).getRGB(), true, true, true, true);
            RenderUtil.drawRoundedRect(cardX + 8, cardY + 10, 22, 22, 5, color.getRGB(), true, true, true, true);
            if (i == selected) {
                RenderUtil.drawRoundedRect(cardX, cardY, cardW, cardH, 6,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 45).getRGB(),
                        true, true, true, true);
            }
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(RiseClickGUIModule.COLOR_NAMES[i], cardX + 38, cardY + 13,
                        RiseColors.TEXT.getRGB());
            }
        }
    }

    private void drawLanguageScreen(float x, float y, float w, float h, int mouseX, int mouseY) {
        screenContentHeight = 56 + languages.length * 31f;
        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawString("Language", x + 7, y + 7 - screenScrollOffset, RiseColors.TEXT.getRGB());
        }
        if (FontManager.productSans12 != null) {
            FontManager.productSans12.drawString("Visual selector", x + 7, y + 25 - screenScrollOffset,
                    RiseColors.TEXT_TRINARY.getRGB());
        }

        for (int i = 0; i < languages.length; i++) {
            float rowY = y + 48 + i * 31f - screenScrollOffset;
            if (rowY + 25 < y || rowY > y + h) continue;
            boolean hovered = mouseX >= x + 7 && mouseX <= x + w - 7 && mouseY >= rowY && mouseY <= rowY + 25;
            RenderUtil.drawRoundedRect(x + 7, rowY, w - 14, 25, 6,
                    new Color(0, 0, 0, hovered || selectedLanguage == i ? 64 : 35).getRGB(),
                    true, true, true, true);
            if (selectedLanguage == i) {
                Color accent = accent();
                RenderUtil.drawRoundedRect(x + 11, rowY + 9, 7, 7, 3.5f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220).getRGB(),
                        true, true, true, true);
            }
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(languages[i], x + 25, rowY + 7, RiseColors.TEXT.getRGB());
            }
        }
        drawScrollBar(x + w - 2, y, 2, h, screenScrollOffset, Math.max(0, screenContentHeight - h));
    }

    private void drawEmptyState(float x, float y, float w, float h, String text) {
        if (FontManager.productSans16 == null) return;
        float textW = (float) FontManager.productSans16.getStringWidth(text);
        FontManager.productSans16.drawString(text, x + w / 2f - textW / 2f, y + Math.max(20, h / 2f),
                RiseColors.TEXT_TRINARY.getRGB());
    }

    private void drawScrollBar(float x, float y, float w, float h, float scroll, float maxScroll) {
        if (maxScroll <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, scroll / maxScroll));
        float barH = Math.max(20f, h * (h / (h + maxScroll)));
        float barY = y + ratio * (h - barH);
        RenderUtil.drawRoundedRect(x, barY, w, barH, 1,
                new Color(255, 255, 255, 42).getRGB(), true, true, true, true);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        if (mouseButton == 0 && mouseX >= windowX && mouseX <= windowX + WINDOW_W
                && mouseY >= windowY && mouseY <= windowY + 15) {
            dragging = true;
            dragOffsetX = windowX - mouseX;
            dragOffsetY = windowY - mouseY;
            return;
        }

        if (mouseX >= windowX && mouseX <= windowX + SIDEBAR_W && mouseY >= windowY && mouseY <= windowY + WINDOW_H) {
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (mouseY >= tabY(i) && mouseY <= tabY(i) + 16f) {
                    switchTab(tabs[i]);
                    return;
                }
            }
            return;
        }

        float contentX = windowX + SIDEBAR_W + 8f;
        float contentY = windowY + 7f;
        float contentW = WINDOW_W - SIDEBAR_W - 16f;
        float contentH = WINDOW_H - 14f;
        if (mouseX < contentX || mouseX > contentX + contentW || mouseY < contentY || mouseY > contentY + contentH) {
            return;
        }

        if (selectedTab == Tab.CAS) {
            configScreen.click(contentX, contentY, contentW, contentH, mouseX, mouseY, mouseButton);
            return;
        }
        if (selectedTab == Tab.THEMES && clickTheme(contentX, contentY, contentW, mouseX, mouseY, mouseButton)) return;
        if (selectedTab == Tab.LANGUAGE && clickLanguage(contentX, contentY, contentW, mouseX, mouseY, mouseButton)) return;

        List<RiseModuleCard> cards = currentCards();
        if (cards != null) {
            for (RiseModuleCard card : cards) {
                if (card.click(mouseX, mouseY, mouseButton)) return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        for (RiseModuleCard card : allCards) {
            card.released();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (closing) return;

        if (selectedTab == Tab.CAS && configScreen.isTyping()) {
            configScreen.key(typedChar, keyCode);
            return;
        }

        for (RiseModuleCard card : allCards) {
            if (card.isBindingKey()) {
                card.key(typedChar, keyCode);
                return;
            }
        }

        boolean activeEditor = activeEditor();
        if (activeEditor) {
            for (RiseModuleCard card : allCards) {
                card.key(typedChar, keyCode);
            }
            return;
        }

        Module clickGUIModule = Myau.moduleManager.getModule("RiseClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGUIModule != null && keyCode == clickGUIModule.getKey())) {
            if (keyCode != Keyboard.KEY_ESCAPE && System.currentTimeMillis() - openedAt < 250L) {
                return;
            }
            if (selectedTab == Tab.SEARCH && !searchText.isEmpty()) {
                searchText = "";
                updateSearchResults();
                resetModuleScroll();
            } else {
                close();
            }
            return;
        }

        if (keyCode == Keyboard.KEY_BACK && selectedTab == Tab.SEARCH && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            updateSearchResults();
            resetModuleScroll();
            return;
        }

        if (isSearchCharacter(typedChar)) {
            if (selectedTab != Tab.SEARCH) {
                searchText = "";
                switchTab(Tab.SEARCH);
            }
            searchText += typedChar;
            updateSearchResults();
            resetModuleScroll();
            return;
        }

        List<RiseModuleCard> cards = currentCards();
        if (cards != null) {
            for (RiseModuleCard card : cards) {
                card.key(typedChar, keyCode);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        float contentH = WINDOW_H - 14f;
        if (selectedTab == Tab.CAS) {
            configScreen.scroll(wheel, contentH);
        } else if (selectedTab == Tab.SEARCH || selectedTab.moduleTab) {
            targetScroll += wheel > 0 ? -22f : 22f;
            targetScroll = Math.max(0, Math.min(targetScroll, getMaxScroll()));
        } else {
            screenTargetScroll += wheel > 0 ? -22f : 22f;
            screenTargetScroll = Math.max(0, Math.min(screenTargetScroll, Math.max(0, screenContentHeight - contentH)));
        }
    }

    private boolean clickTheme(float x, float y, float w, int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        RiseClickGUIModule module = (RiseClickGUIModule) Myau.moduleManager.getModule("RiseClickGUI");
        if (module == null) return false;

        float cardW = (w - 21f) / 2f;
        float cardH = 42f;
        for (int i = 0; i < RiseClickGUIModule.COLOR_NAMES.length; i++) {
            int col = i % 2;
            int row = i / 2;
            float cardX = x + 7 + col * (cardW + 7);
            float cardY = y + 47 + row * (cardH + 7);
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                module.accentColor.setValue(i);
                return true;
            }
        }
        return false;
    }

    private boolean clickLanguage(float x, float y, float w, int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        for (int i = 0; i < languages.length; i++) {
            float rowY = y + 48 + i * 31f - screenScrollOffset;
            if (mouseX >= x + 7 && mouseX <= x + w - 7 && mouseY >= rowY && mouseY <= rowY + 25) {
                selectedLanguage = i;
                return true;
            }
        }
        return false;
    }

    private void switchTab(Tab tab) {
        if (selectedTab == tab) return;
        lastTab = selectedTab;
        selectedTab = tab;
        transitionAlpha = 1f;
        resetModuleScroll();
        screenScrollOffset = 0;
        screenTargetScroll = 0;
        if (tab == Tab.CAS) configScreen.resetScroll();
    }

    private List<RiseModuleCard> currentCards() {
        if (selectedTab == Tab.SEARCH) return searchText.isEmpty() ? allCards : searchResults;
        if (selectedTab.moduleTab) return moduleCards.get(selectedTab);
        return null;
    }

    private void updateSearchResults() {
        searchResults.clear();
        String query = key(searchText);
        if (query.isEmpty()) return;

        for (RiseModuleCard card : allCards) {
            Module module = card.getModule();
            String haystack = key(module.getName() + " " + module.getDescription() + " " + getModuleCategoryName(module));
            if (haystack.contains(query)) {
                searchResults.add(card);
            }
        }
    }

    private boolean activeEditor() {
        for (RiseModuleCard card : allCards) {
            if (card.isTyping()) return true;
        }
        return false;
    }

    private boolean isSearchCharacter(char typedChar) {
        return "abcdefghijklmnopqrstuvwxyz1234567890 ".contains(String.valueOf(typedChar).toLowerCase(Locale.ROOT));
    }

    private void close() {
        closing = true;
        animationStart = System.currentTimeMillis();
    }

    private void resetModuleScroll() {
        scrollOffset = 0;
        targetScroll = 0;
    }

    private void updateScroll(float deltaTime) {
        targetScroll = Math.max(0, Math.min(targetScroll, getMaxScroll()));
        scrollOffset = AnimationUtil.animateSmooth(targetScroll, scrollOffset, 12f, deltaTime);
    }

    private void updateScreenScroll(float deltaTime) {
        screenTargetScroll = Math.max(0, Math.min(screenTargetScroll, Math.max(0, screenContentHeight - (WINDOW_H - 14f))));
        screenScrollOffset = AnimationUtil.animateSmooth(screenTargetScroll, screenScrollOffset, 12f, deltaTime);
    }

    private float getMaxScroll() {
        List<RiseModuleCard> cards = currentCards();
        if (cards == null || cards.isEmpty()) return 0;

        float total = 7f;
        for (RiseModuleCard card : cards) {
            total += card.getTotalHeight() + 7f;
        }

        float available = selectedTab == Tab.SEARCH ? WINDOW_H - 42f : WINDOW_H - 14f;
        return Math.max(0, total - available);
    }

    private float easeOutExpo(float value) {
        return value >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * value);
    }

    private void handleInvWalk() {
        try {
            Module invWalk = Myau.moduleManager.getModule("InvWalk");
            if (invWalk != null && invWalk.isEnabled()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),
                        Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
            }
        } catch (Exception ignored) {
        }
    }
}
