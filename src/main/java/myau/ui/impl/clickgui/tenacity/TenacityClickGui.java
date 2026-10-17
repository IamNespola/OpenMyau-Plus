package myau.ui.impl.clickgui.tenacity;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.ClickGUIModule;
import myau.module.modules.InvWalk;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.rise.RiseClickGUI;
import myau.ui.impl.clickgui.rise.RiseValueEditor;
import myau.util.KeyBindUtil;
import myau.util.RenderUtil;
import myau.util.AnimationUtil;
import myau.module.ModuleCatalog;
import java.util.IdentityHashMap;
import java.util.Map;
import myau.util.font.FontManager;
import myau.util.font.impl.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Three-panel GUI inspired by the Tenacity reference, using the client's own modules. */
public class TenacityClickGui extends GuiScreen {
    private static final float WINDOW_W = 720, WINDOW_H = 400;
    private static final float SIDEBAR_W = 134, SETTINGS_W = 220;
    private static final float ROW_H = 52, ROW_STEP = 60;
    private static final int BACKGROUND = 0xFF202124, PANEL = 0xFF303136;
    private static final int MUTED = 0xFF888A92, WHITE = 0xFFF7F7FA;
    private static final int CYAN = 0xFF29B8DB, PINK = 0xFFE38FD5;
    private static final String[] CATEGORIES = ModuleCatalog.CATEGORIES;
    private static TenacityClickGui instance;

    private final List<Module> modules = new ArrayList<>();
    private final List<RiseValueEditor> editors = new ArrayList<>();
    private String category = "Movement";
    private Module selected;
    private Module binding;
    private float windowX = -1, windowY = -1, scale = 1;
    private float moduleScroll, settingsScroll, settingsHeight;
    private boolean dragging;
    private float dragX, dragY;
    private long lastFrame;
    private float moduleTargetScroll, settingsTargetScroll;
    private float openProgress = 1, visualScale = 1, categoryHighlight = 1;
    private boolean closing;
    private String searchText = "";
    private boolean searching;
    private final Map<Module, Float> toggleAnimations = new IdentityHashMap<>();
    private final Map<Module, Float> hoverAnimations = new IdentityHashMap<>();

    public static TenacityClickGui getInstance() {
        if (instance == null) instance = new TenacityClickGui();
        return instance;
    }

    @Override
    public void initGui() {
        FontManager.initializeFonts();
        Keyboard.enableRepeatEvents(true);
        scale = Math.min(1, Math.min((width - 12f) / WINDOW_W, (height - 12f) / WINDOW_H));
        scale = Math.max(0.01f, scale);
        ClickGUIModule gui = guiModule();
        if (windowX < 0 || gui == null || !gui.saveGuiState.getValue()) {
            windowX = (width / scale - WINDOW_W) / 2;
            windowY = (height / scale - WINDOW_H) / 2;
        }
        clampWindow();
        rebuildModules();
        if (selected == null || !modules.contains(selected)) {
            Module noSlow = Myau.moduleManager.getModule("NoSlow");
            select(modules.contains(noSlow) ? noSlow : modules.isEmpty() ? null : modules.get(0));
        } else {
            select(selected);
        }
        openProgress = 0;
        closing = false;
        lastFrame = System.nanoTime();
    }

    private ClickGUIModule guiModule() {
        return (ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");
    }

    private void rebuildModules() {
        modules.clear();
        for (Module module : Myau.moduleManager.allModules()) {
            String tab = ModuleCatalog.category(module);
            String query = searchText.toLowerCase(java.util.Locale.ROOT);
            if (query.isEmpty() ? category.equals(tab) :
                    (module.getName() + " " + module.getDescription()).toLowerCase(java.util.Locale.ROOT).contains(query))
                modules.add(module);
        }
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        moduleScroll = clamp(moduleScroll, 0, moduleScrollMax());
        moduleTargetScroll = moduleScroll;
    }

    private void select(Module module) {
        releaseEditors();
        editors.clear();
        selected = module;
        binding = null;
        settingsScroll = settingsTargetScroll = 0;
        List<Property<?>> properties = module == null ? null : Myau.propertyManager.properties.get(module);
        if (properties != null) for (Property<?> property : properties) {
            if (property instanceof ModeProperty) editors.add(new PillModeEditor((ModeProperty) property));
            else if (property instanceof BooleanProperty) editors.add(new RiseValueEditor.BooleanEditor((BooleanProperty) property));
            else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty)
                editors.add(new RiseValueEditor.SliderEditor(property));
            else if (property instanceof ColorProperty) editors.add(new RiseValueEditor.ColorEditor((ColorProperty) property));
            else if (property instanceof TextProperty) editors.add(new RiseValueEditor.TextEditor((TextProperty) property));
        }
        updateSettingsHeight();
    }

    private void updateSettingsHeight() {
        settingsHeight = 32;
        for (RiseValueEditor editor : editors) if (editor.isVisible()) settingsHeight += editor.getHeight() + 6;
        settingsScroll = clamp(settingsScroll, 0, settingsScrollMax());
        settingsTargetScroll = clamp(settingsTargetScroll, 0, settingsScrollMax());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float mx = localX(mouseX), my = localY(mouseY);
        if (dragging) {
            windowX = mx - dragX;
            windowY = my - dragY;
            clampWindow();
        }
        float delta = Math.min(0.25f, (System.nanoTime() - lastFrame) / 1_000_000_000f);
        lastFrame = System.nanoTime();
        openProgress = AnimationUtil.animateSmooth(closing ? 0 : 1, openProgress, 18, delta);
        if (closing && openProgress < 0.02f) { mc.displayGuiScreen(null); return; }
        visualScale = 0.96f + openProgress * 0.04f;
        moduleScroll = AnimationUtil.animateSmooth(moduleTargetScroll, moduleScroll, 16, delta);
        settingsScroll = AnimationUtil.animateSmooth(settingsTargetScroll, settingsScroll, 16, delta);
        int categoryIndex = java.util.Arrays.asList(CATEGORIES).indexOf(category);
        categoryHighlight = AnimationUtil.animateSmooth(categoryIndex, categoryHighlight, 16, delta);
        updateSettingsHeight();
        net.minecraft.client.gui.Gui.drawRect(0, 0, width, height, ((int) (90 * openProgress) << 24) | 0x090C12);
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(visualScale, visualScale, 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);
        GlStateManager.scale(scale, scale, 1);
        try {
            rounded(windowX, windowY, WINDOW_W, WINDOW_H, 14, BACKGROUND);
            rounded(windowX, windowY, SIDEBAR_W, WINDOW_H, 14, PANEL);
            drawSidebar(mx, my);
            drawModules(mx, my, delta);
            drawSettings(mx, my, partialTicks, delta);
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
        updateMovement();
    }

    private void drawSidebar(float mx, float my) {
        rounded(windowX + 9, windowY + 10, 26, 26, 13, WHITE);
        rounded(windowX + 12, windowY + 13, 20, 20, 10, PANEL);
        text(FontManager.productSans24, "M", windowX + 16, windowY + 13, WHITE);
        text(FontManager.productSans24, "Myau+", windowX + 43, windowY + 15, WHITE);
        text(FontManager.productSans12, fit(FontManager.productSans12, Myau.version == null ? "dev" : Myau.version, 28),
                windowX + 94, windowY + 20, MUTED);
        rounded(windowX + 14, windowY + 47, SIDEBAR_W - 28, 1, 0, 0xFF42444B);
        rounded(windowX + 7, categoryY(0) + categoryHighlight * 38, SIDEBAR_W - 14, 30, 5, 0xFF373D49);
        rounded(windowX + 7, categoryY(0) + categoryHighlight * 38 + 6, 2, 18, 1, CYAN);
        for (int i = 0; i < CATEGORIES.length; i++) {
            float y = categoryY(i);
            boolean active = category.equals(CATEGORIES[i]);
            boolean hover = over(mx, my, windowX + 8, y, SIDEBAR_W - 16, 30);
            int color = active ? WHITE : hover ? 0xFFBFC1C8 : MUTED;
            if (hover) rounded(windowX + 7, y, SIDEBAR_W - 14, 30, 5, 0xFF37383E);
            drawCategoryIcon(i, windowX + 21, y + 15, color);
            text(FontManager.productSans24, CATEGORIES[i], windowX + 46, y + 7, color);
        }
    }

    private void drawCategoryIcon(int icon, float x, float y, int color) {
        if (icon == 5) {
            for (int i = -1; i <= 1; i++) {
                rounded(x - 9, y + i * 5 - 1, 3, 2, 1, color);
                rounded(x - 3, y + i * 5 - 1, 13, 2, 1, color);
            }
        } else if (icon == 2) {
            RenderUtil.drawRoundedRectOutline(x - 10, y - 6, 20, 12, 6, 1.5f, color, true, true, true, true);
            rounded(x - 3, y - 3, 6, 6, 3, color);
        } else if (icon == 1 || icon == 3) {
            rounded(x - 2, y - 12, 6, 6, 3, color);
            line(x, y - 4, x - 3, y + 3, 3, color);
            line(x - 3, y + 3, x - 7, y + 10, 3, color);
            line(x - 3, y + 3, x + 6, y + 10, 3, color);
            line(x - 1, y - 2, x + 7, y + (icon == 1 ? -4 : 3), 3, color);
            line(x - 1, y - 2, x - 8, y + 1, 3, color);
        } else if (icon == 6) {
            RenderUtil.drawRoundedRectOutline(x - 8, y - 11, 16, 22, 2, 1.5f, color, true, true, true, true);
            line(x - 3, y - 4, x - 6, y, 1.5f, color);
            line(x - 6, y, x - 3, y + 4, 1.5f, color);
            line(x + 3, y - 4, x + 6, y, 1.5f, color);
            line(x + 6, y, x + 3, y + 4, 1.5f, color);
        } else if (icon == 4) {
            rounded(x - 5, y - 8, 10, 17, 5, color);
            for (int i = -1; i <= 1; i++) line(x - 10, y + i * 6, x + 10, y + i * 6, 1.5f, color);
        } else {
            line(x - 8, y - 9, x + 8, y + 9, 3, color);
            line(x + 8, y - 9, x - 8, y + 9, 3, color);
            line(x - 10, y + 4, x - 4, y + 10, 2, color);
            line(x + 4, y + 10, x + 10, y + 4, 2, color);
        }
    }

    private void drawModules(float mx, float my, float delta) {
        text(FontManager.productSans24, category, listX() + 2, windowY + 14, WHITE);
        rounded(listX() + listWidth() - 164, windowY + 10, 164, 24, 5, searching ? 0xFF394352 : PANEL);
        String query = searchText.isEmpty() ? "Search modules..." : searchText + (searching ? "_" : "");
        text(FontManager.productSans16, fit(FontManager.productSans16, query, 144),
                listX() + listWidth() - 154, windowY + 17, searchText.isEmpty() ? MUTED : WHITE);
        clip(listX(), listTop(), listWidth(), listViewport());
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            float y = rowY(i);
            boolean hover = over(mx, my, listX(), y, listWidth(), ROW_H) && over(mx, my, listX(), listTop(), listWidth(), listViewport());
            float hovered = AnimationUtil.animateSmooth(hover ? 1 : 0, hoverAnimations.getOrDefault(module, 0f), 14, delta);
            float enabled = AnimationUtil.animateSmooth(module.isEnabled() ? 1 : 0,
                    toggleAnimations.getOrDefault(module, module.isEnabled() ? 1f : 0f), 16, delta);
            hoverAnimations.put(module, hovered);
            toggleAnimations.put(module, enabled);
            if (y + ROW_H < listTop() || y > listTop() + listViewport()) continue;
            rounded(listX(), y, listWidth(), ROW_H, 7,
                    AnimationUtil.interpolateColor(PANEL, 0xFF3C414C, hovered));
            if (selected == module) rounded(listX(), y + 9, 2, ROW_H - 18, 1, CYAN);
            text(FontManager.productSans24, fit(FontManager.productSans24, module.getName(), listWidth() - 76),
                    listX() + 12, y + 11, WHITE);
            text(FontManager.productSans16, fit(FontManager.productSans16, module.getDescription(), listWidth() - 24),
                    listX() + 12, y + 33, MUTED);
            float switchX = listX() + listWidth() - 60;
            rounded(switchX, y + 12, 28, 14, 7, AnimationUtil.interpolateColor(0xFF4A4F59, CYAN, enabled));
            rounded(switchX + 2 + enabled * 14, y + 14, 10, 10, 5, WHITE);
            for (int dot = 0; dot < 3; dot++) rounded(listX() + listWidth() - 16, y + 12 + dot * 5, 3, 3, 1.5f, MUTED);
        }
        if (modules.isEmpty()) text(FontManager.productSans16, "No modules in this category", listX() + 10, listTop() + 12, MUTED);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        scrollbar(listX() + listWidth() + 3, listTop(), listViewport(), moduleScroll, moduleScrollMax());
    }

    private void drawSettings(float mx, float my, float partialTicks, float delta) {
        rounded(settingsX(), windowY, SETTINGS_W, WINDOW_H, 14, PANEL);
        String title = selected == null ? "Settings" : selected.getName();
        title = fit(FontManager.productSans24, title, SETTINGS_W - 24);
        text(FontManager.productSans24, title, settingsX() + (SETTINGS_W - textWidth(FontManager.productSans24, title)) / 2,
                windowY + 7, WHITE);
        if (selected != null) {
            String description = selected.getDescription();
            List<String> lines = wrap(description, SETTINGS_W - 24);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                text(FontManager.productSans16, lines.get(i), settingsX() + 12, windowY + 31 + i * 12, MUTED);
        }
        rounded(settingsX() + 12, windowY + 58, SETTINGS_W - 24, 1, 0, 0xFF424650);
        clip(settingsX() + 5, settingsTop(), SETTINGS_W - 10, settingsViewport());
        float y = settingsTop() + 4 - settingsScroll;
        for (RiseValueEditor editor : editors) {
            if (!editor.isVisible()) continue;
            // Keep active drag updates even when an editor scrolls out of view.
            editor.draw(settingsX() + 10, y, SETTINGS_W - 20, (int) mx, (int) my, partialTicks, delta, 255);
            y += editor.getHeight() + 6;
        }
        if (selected != null) {
            text(FontManager.productSans16, "Keybind", settingsX() + 10, y + 5, WHITE);
            String key = binding == selected ? "Press a key..." : selected.getKey() == 0 ? "None" : KeyBindUtil.getKeyName(selected.getKey());
            text(FontManager.productSans12, key, settingsX() + SETTINGS_W - 10 - textWidth(FontManager.productSans12, key), y + 7, MUTED);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        scrollbar(settingsX() + SETTINGS_W - 4, settingsTop(), settingsViewport(), settingsScroll, settingsScrollMax());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (closing) return;
        float mx = localX(mouseX), my = localY(mouseY);
        if (!over(mx, my, windowX, windowY, WINDOW_W, WINDOW_H)) { searching = false; return; }
        searching = button == 0 && over(mx, my, listX() + listWidth() - 164, windowY + 10, 164, 24);
        if (searching) { releaseEditors(); return; }
        for (int i = 0; i < CATEGORIES.length; i++) if (button == 0 && over(mx, my, windowX + 8, categoryY(i), SIDEBAR_W - 16, 30)) {
            category = CATEGORIES[i];
            searchText = "";
            moduleScroll = moduleTargetScroll = 0;
            rebuildModules();
            select(modules.isEmpty() ? null : modules.get(0));
            return;
        }
        if (over(mx, my, listX(), listTop(), listWidth(), listViewport())) {
            for (int i = 0; i < modules.size(); i++) if (over(mx, my, listX(), rowY(i), listWidth(), ROW_H)) {
                Module module = modules.get(i);
                if (button == 2) { select(module); binding = module; }
                else if (button == 1 || (button == 0 && mx >= listX() + listWidth() - 22)) select(module);
                else if (button == 0) module.toggle();
                return;
            }
        }
        if (over(mx, my, settingsX() + 5, settingsTop(), SETTINGS_W - 10, settingsViewport())) {
            float y = settingsTop() + 4 - settingsScroll;
            for (RiseValueEditor editor : editors) if (editor.isVisible()) {
                if (editor.click((int) mx, (int) my, button, settingsX() + 10, y, SETTINGS_W - 20)) return;
                y += editor.getHeight() + 6;
            }
            if (selected != null && button == 0 && over(mx, my, settingsX() + 10, y, SETTINGS_W - 20, 24)) binding = selected;
            return;
        }
        if (button == 0 && over(mx, my, windowX, windowY, SIDEBAR_W, 47)) {
            dragging = true;
            dragX = mx - windowX;
            dragY = my - windowY;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        float mx = localX(Mouse.getEventX() * width / (float) mc.displayWidth);
        float my = localY(height - Mouse.getEventY() * height / (float) mc.displayHeight - 1);
        float amount = wheel > 0 ? -28 : 28;
        releaseEditors();
        if (over(mx, my, listX(), listTop(), listWidth(), listViewport())) moduleTargetScroll = clamp(moduleTargetScroll + amount, 0, moduleScrollMax());
        else if (over(mx, my, settingsX(), settingsTop(), SETTINGS_W, settingsViewport())) settingsTargetScroll = clamp(settingsTargetScroll + amount, 0, settingsScrollMax());
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        releaseEditors();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searching) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) { searching = false; return; }
            if (keyCode == Keyboard.KEY_BACK && !searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
            else if (net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(typedChar) && searchText.length() < 64) searchText += typedChar;
            moduleScroll = moduleTargetScroll = 0;
            rebuildModules();
            select(modules.isEmpty() ? null : modules.get(0));
            return;
        }
        if (binding != null) {
            if (keyCode != Keyboard.KEY_ESCAPE) binding.setKey(keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK ? 0 : keyCode);
            binding = null;
            return;
        }
        if (isTyping()) {
            for (RiseValueEditor editor : editors) editor.key(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) { closing = true; releaseEditors(); }
    }

    private boolean isTyping() {
        if (binding != null || searching) return true;
        for (RiseValueEditor editor : editors) if (editor.isTyping()) return true;
        return false;
    }

    private void updateMovement() {
        InvWalk invWalk = (InvWalk) Myau.moduleManager.getModule("InvWalk");
        if (invWalk != null && invWalk.isEnabled() && invWalk.guiEnabled.getValue()) {
            if (isTyping()) KeyBinding.unPressAllKeys();
            else invWalk.pressMovementKeys(true);
        }
    }

    private void releaseEditors() {
        for (RiseValueEditor editor : editors) editor.released();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        dragging = false;
        binding = null;
        releaseEditors();
        KeyBinding.unPressAllKeys();
        ClickGUIModule gui = guiModule();
        if (gui != null && !gui.isSwitchingGuiStyle()) gui.setEnabled(false);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private float listX() { return windowX + SIDEBAR_W + 12; }
    private float listWidth() { return WINDOW_W - SIDEBAR_W - SETTINGS_W - 30; }
    private float settingsX() { return windowX + WINDOW_W - SETTINGS_W; }
    private float settingsTop() { return windowY + 66; }
    private float settingsViewport() { return WINDOW_H - 76; }
    private float categoryY(int index) { return windowY + 60 + index * 38; }
    private float rowY(int index) { return listTop() + index * ROW_STEP - moduleScroll; }
    private float listTop() { return windowY + 46; }
    private float listViewport() { return WINDOW_H - 58; }
    private float moduleScrollMax() { return Math.max(0, modules.size() * ROW_STEP - (ROW_STEP - ROW_H) + 8 - listViewport()); }
    private float settingsScrollMax() { return Math.max(0, settingsHeight + 8 - settingsViewport()); }
    private void clampWindow() {
        windowX = clamp(windowX, 0, width / scale - WINDOW_W);
        windowY = clamp(windowY, 0, height / scale - WINDOW_H);
    }
    private float localX(float x) { return ((x - width / 2f) / visualScale + width / 2f) / scale; }
    private float localY(float y) { return ((y - height / 2f) / visualScale + height / 2f) / scale; }
    private void clip(float x, float y, float w, float h) {
        RenderUtil.scissor(width / 2f + (x * scale - width / 2f) * visualScale,
                height / 2f + (y * scale - height / 2f) * visualScale,
                w * scale * visualScale, h * scale * visualScale);
    }
    private static List<String> wrap(String description, float width) {
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String word : description.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && textWidth(FontManager.productSans16, next) > width) {
                lines.add(current); current = word;
            } else current = next;
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, Math.max(min, max))); }
    private static boolean over(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static void rounded(float x, float y, float w, float h, float radius, int color) { RenderUtil.drawRoundedRect(x, y, w, h, radius, color, true, true, true, true); }
    private static void line(float x, float y, float x2, float y2, float width, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.drawLine(x, y, x2, y2, width, color);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    private static float textWidth(FontRenderer font, String text) { return font == null ? net.minecraft.client.Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) : (float) font.getStringWidth(text); }
    private static void text(FontRenderer font, String text, float x, float y, int color) {
        if (font != null) font.drawString(text, x, y, color);
        else net.minecraft.client.Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color, false);
    }
    private static String fit(FontRenderer font, String text, float width) {
        if (textWidth(font, text) <= width) return text;
        while (!text.isEmpty() && textWidth(font, text + "...") > width) text = text.substring(0, text.length() - 1);
        return text + "...";
    }
    private static void scrollbar(float x, float y, float viewport, float scroll, float max) {
        if (max <= 0) return;
        float thumb = Math.max(18, viewport * viewport / (viewport + max));
        rounded(x, y + (viewport - thumb) * scroll / max, 2, thumb, 1, 0xFF777983);
    }

    /** Mode values live in outlined pills, matching the settings panel in the reference. */
    private static class PillModeEditor extends RiseValueEditor {
        private final ModeProperty mode;
        PillModeEditor(ModeProperty mode) { super(mode); this.mode = mode; }
        @Override public float getHeight() { return 22; }
        @Override public void draw(float x, float y, float w, int mx, int my, float partialTicks, float delta, int opacity) {
            float pillW = Math.min(w * 0.5f, Math.max(58, textWidth(FontManager.productSans16, mode.getModeString()) + 14));
            text(FontManager.productSans16, fit(FontManager.productSans16, mode.getName(), w - pillW - 6), x, y + 5, WHITE);
            float pillX = x + w - pillW;
            rounded(pillX, y + 2, pillW, 18, 6, 0xFF484A52);
            rounded(pillX + 2, y + 4, pillW - 4, 14, 4, BACKGROUND);
            String value = fit(FontManager.productSans16, mode.getModeString(), pillW - 10);
            text(FontManager.productSans16, value, pillX + (pillW - textWidth(FontManager.productSans16, value)) / 2, y + 5, WHITE);
        }
        @Override public boolean click(int mx, int my, int button, float x, float y, float w) {
            if ((button == 0 || button == 1) && over(x, y, w, getHeight(), mx, my)) {
                if (button == 0) mode.nextMode(); else mode.previousMode();
                return true;
            }
            return false;
        }
    }
}
