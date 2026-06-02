package myau.ui.impl.clickgui.clean;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.*;
import myau.module.modules.Timer;
import myau.util.KeyBindUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CleanClickGuiScreen extends GuiScreen {
    private static final double FRICTION = 0.85D;
    private static final double SNAP_STRENGTH = 0.15D;
    private static final long ANIMATION_DURATION = 250L;
    private static final int SEARCH_WIDTH = 116;
    private static final int SEARCH_HEIGHT = 11;
    private static CleanClickGuiScreen instance;

    private final ArrayList<CleanFrame> frames = new ArrayList<>();
    private CleanFrame draggingComponent;
    private int scrollY;
    private int targetScrollY;
    private double velocity;
    private boolean isClosing;
    private boolean searchFocused;
    private String searchQuery = "";
    private long openTime;
    private long lastFrameTime;

    public CleanClickGuiScreen() {
        rebuildFrames();
    }

    private void rebuildFrames() {
        frames.clear();

        List<Module> combatModules = Arrays.asList(
                Myau.moduleManager.getModule(AimAssist.class),
                Myau.moduleManager.getModule(AntiBot.class),
                Myau.moduleManager.getModule(AutoClicker.class),
                Myau.moduleManager.getModule(KillAura.class),
                Myau.moduleManager.getModule(Wtap.class),
                Myau.moduleManager.getModule(Velocity.class),
                Myau.moduleManager.getModule(ServerLag.class),
                Myau.moduleManager.getModule(Reach.class),
                Myau.moduleManager.getModule(TargetStrafe.class),
                Myau.moduleManager.getModule(NoHitDelay.class),
                Myau.moduleManager.getModule(AntiFireball.class),
                Myau.moduleManager.getModule(KnockbackDelay.class),
                Myau.moduleManager.getModule(LagRange.class),
                Myau.moduleManager.getModule(HitBox.class),
                Myau.moduleManager.getModule(MoreKB.class),
                Myau.moduleManager.getModule(Refill.class),
                Myau.moduleManager.getModule(HitSelect.class),
                Myau.moduleManager.getModule(BackTrack.class),
                Myau.moduleManager.getModule(Hitflick.class),
                Myau.moduleManager.getModule(TimerRangev999.class),
                Myau.moduleManager.getModule(ClickAssits.class),
                Myau.moduleManager.getModule(Criticals.class),
                Myau.moduleManager.getModule(BlockHit.class),
                Myau.moduleManager.getModule(SprintReset.class),
                Myau.moduleManager.getModule(Displace.class),
                Myau.moduleManager.getModule(Teams.class)
        );

        List<Module> movementModules = Arrays.asList(
                Myau.moduleManager.getModule(AntiAFK.class),
                Myau.moduleManager.getModule(Fly.class),
                Myau.moduleManager.getModule(FastBow.class),
                Myau.moduleManager.getModule(Timer.class),
                Myau.moduleManager.getModule(Speed.class),
                Myau.moduleManager.getModule(LongJump.class),
                Myau.moduleManager.getModule(Sprint.class),
                Myau.moduleManager.getModule(SafeWalk.class),
                Myau.moduleManager.getModule(Jesus.class),
                Myau.moduleManager.getModule(BHop.class),
                Myau.moduleManager.getModule(Blink.class),
                Myau.moduleManager.getModule(NoFall.class),
                Myau.moduleManager.getModule(NoSlow.class),
                Myau.moduleManager.getModule(KeepSprint.class),
                Myau.moduleManager.getModule(Eagle.class),
                Myau.moduleManager.getModule(NoJumpDelay.class),
                Myau.moduleManager.getModule(AntiVoid.class)
        );

        List<Module> renderModules = Arrays.asList(
                Myau.moduleManager.getModule(ESP.class),
                Myau.moduleManager.getModule(Chams.class),
                Myau.moduleManager.getModule(FullBright.class),
                Myau.moduleManager.getModule(Tracers.class),
                Myau.moduleManager.getModule(NameTags.class),
                Myau.moduleManager.getModule(Xray.class),
                Myau.moduleManager.getModule(TargetESP.class),
                Myau.moduleManager.getModule(TargetHUD.class),
                Myau.moduleManager.getModule(Indicators.class),
                Myau.moduleManager.getModule(BedESP.class),
                Myau.moduleManager.getModule(ItemESP.class),
                Myau.moduleManager.getModule(ViewClip.class),
                Myau.moduleManager.getModule(NoHurtCam.class),
                Myau.moduleManager.getModule(HUD.class),
                Myau.moduleManager.getModule(ChestESP.class),
                Myau.moduleManager.getModule(Trajectories.class),
                Myau.moduleManager.getModule(Radar.class),
                Myau.moduleManager.getModule(FPScounter.class),
                Myau.moduleManager.getModule(WaterMark.class),
                Myau.moduleManager.getModule(WaterMark2.class),
                Myau.moduleManager.getModule(HitParticleEffects.class),
                Myau.moduleManager.getModule(DynamicIsland.class),
                Myau.moduleManager.getModule(ESP2D.class),
                Myau.moduleManager.getModule(RiseClickGUIModule.class),
                Myau.moduleManager.getModule(SeasonDisplay.class),
                Myau.moduleManager.getModule(Animations.class),
                Myau.moduleManager.getModule(HudEditor.class),
                Myau.moduleManager.getModule(ClickGUIModule.class)
        );

        List<Module> playerModules = Arrays.asList(
                Myau.moduleManager.getModule(AutoHeal.class),
                Myau.moduleManager.getModule(FakeLag.class),
                Myau.moduleManager.getModule(AutoTool.class),
                Myau.moduleManager.getModule(ChestStealer.class),
                Myau.moduleManager.getModule(InvManager.class),
                Myau.moduleManager.getModule(InvWalk.class),
                Myau.moduleManager.getModule(Scaffold.class),
                Myau.moduleManager.getModule(AutoBlockIn.class),
                Myau.moduleManager.getModule(AutoSwap.class),
                Myau.moduleManager.getModule(SpeedMine.class),
                Myau.moduleManager.getModule(FastPlace.class),
                Myau.moduleManager.getModule(GhostHand.class),
                Myau.moduleManager.getModule(MCF.class),
                Myau.moduleManager.getModule(AntiDebuff.class),
                Myau.moduleManager.getModule(FlagDetector.class),
                Myau.moduleManager.getModule(AutoGapple.class),
                Myau.moduleManager.getModule(ThrowAura.class)
        );

        List<Module> miscModules = Arrays.asList(
                Myau.moduleManager.getModule(Spammer.class),
                Myau.moduleManager.getModule(BedNuker.class),
                Myau.moduleManager.getModule(BedTracker.class),
                Myau.moduleManager.getModule(TeamDisplay.class),
                Myau.moduleManager.getModule(LightningTracker.class),
                Myau.moduleManager.getModule(NoRotate.class),
                Myau.moduleManager.getModule(NickHider.class),
                Myau.moduleManager.getModule(AntiObbyTrap.class),
                Myau.moduleManager.getModule(AntiObfuscate.class),
                Myau.moduleManager.getModule(AutoAnduril.class),
                Myau.moduleManager.getModule(InventoryClicker.class),
                Myau.moduleManager.getModule(Disabler.class),
                Myau.moduleManager.getModule(ClientSpoofer.class),
                Myau.moduleManager.getModule(MurderDetector.class),
                Myau.moduleManager.getModule(AutoHypixel.class)
        );

        Comparator<Module> comparator = Comparator.comparing(module -> module.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        int currentX = 12;
        int currentY = 26;
        int frameWidth = 92;
        int frameHeight = 12;

        currentX = addFrame("Combat", combatModules, currentX, currentY, frameWidth, frameHeight);
        currentX = addFrame("Movement", movementModules, currentX, currentY, frameWidth, frameHeight);
        currentX = addFrame("Render", renderModules, currentX, currentY, frameWidth, frameHeight);
        currentX = addFrame("Player", playerModules, currentX, currentY, frameWidth, frameHeight);
        currentX = addFrame("Misc", miscModules, currentX, currentY, frameWidth, frameHeight);
        addConfigFrame("Configs", getConfigs(), currentX, currentY, frameWidth, frameHeight);
    }

    private int addFrame(String name, List<Module> modules, int x, int y, int width, int height) {
        List<Module> filtered = new ArrayList<>(modules);
        filtered.removeIf(module -> module == null);
        if (!filtered.isEmpty()) {
            frames.add(new CleanFrame(name, filtered, x, y, width, height));
            return x + width + 6;
        }
        return x;
    }

    private List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File folder = new File("./config/Myau/");
        File[] files = folder.listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".json"));
        if (files == null) return configs;
        for (File file : files) {
            String name = file.getName();
            if (name.equalsIgnoreCase("menu.json") || name.equalsIgnoreCase("hud.json")) continue;
            configs.add(name.substring(0, name.length() - 5));
        }
        configs.sort(String.CASE_INSENSITIVE_ORDER);
        return configs;
    }

    private int addConfigFrame(String name, List<String> configs, int x, int y, int width, int height) {
        if (!configs.isEmpty()) {
            frames.add(new CleanFrame(name, configs, x, y, width, height, true));
            return x + width + 6;
        }
        return x;
    }

    public static CleanClickGuiScreen getInstance() {
        if (instance == null) instance = new CleanClickGuiScreen();
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    @Override
    public void initGui() {
        super.initGui();
        myau.util.font.FontManager.initializeFonts();
        this.isClosing = false;
        this.searchFocused = false;
        this.openTime = System.currentTimeMillis();
        this.lastFrameTime = System.nanoTime();
        this.scrollY = 0;
        this.targetScrollY = 0;
        this.velocity = 0.0D;
    }

    public void close() {
        if (isClosing) return;
        this.isClosing = true;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long currentFrameTime = System.nanoTime();
        float deltaTime = (currentFrameTime - lastFrameTime) / 1_000_000_000.0F;
        lastFrameTime = currentFrameTime;
        updateScroll();
        long elapsedTime = System.currentTimeMillis() - openTime;
        if (isClosing && elapsedTime > ANIMATION_DURATION) {
            mc.displayGuiScreen(null);
            return;
        }
        float screenAlpha = isClosing ? (1.0F - Math.min(1.0F, (float) elapsedTime / ANIMATION_DURATION)) : Math.min(1.0F, (float) elapsedTime / ANIMATION_DURATION);
        screenAlpha = (float) (1.0D - Math.pow(1.0D - screenAlpha, 3));
        Gui.drawRect(0, 0, this.width, this.height, ((int) (245 * screenAlpha) << 24));
        if (screenAlpha > 0.01F) {
            drawSearchBox(screenAlpha);
            for (CleanFrame frame : frames) {
                frame.render(mouseX, mouseY, partialTicks, screenAlpha, false, scrollY, deltaTime, searchQuery);
            }
            drawKeybinds(screenAlpha);
        }
        try {
            Module invWalkModule = Myau.moduleManager.getModule("InvWalk");
            if (invWalkModule != null && invWalkModule.isEnabled()) handleInvWalk();
        } catch (Exception ignored) {
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSearchBox(float animationProgress) {
        int alpha = (int) (255 * animationProgress);
        int x = getSearchX();
        int y = 6;
        Gui.drawRect(x, y, x + SEARCH_WIDTH, y + SEARCH_HEIGHT, withAlpha(CleanTheme.PANEL_DARK, alpha));
        Gui.drawRect(x, y + SEARCH_HEIGHT - 1, x + SEARCH_WIDTH, y + SEARCH_HEIGHT, withAlpha(searchFocused ? CleanTheme.ACCENT : 0xFF3A3A3A, alpha));
        String text = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        int color = searchQuery.isEmpty() ? CleanTheme.MUTED : CleanTheme.TEXT;
        this.fontRendererObj.drawStringWithShadow(text, x + 4, y + 2, withAlpha(color, alpha));
        if (searchFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = x + 4 + this.fontRendererObj.getStringWidth(searchQuery);
            Gui.drawRect(cursorX, y + 2, cursorX + 1, y + SEARCH_HEIGHT - 2, withAlpha(CleanTheme.ACCENT, alpha));
        }
    }

    private int getSearchX() {
        return this.width / 2 - SEARCH_WIDTH / 2;
    }

    private boolean isMouseOverSearch(int mouseX, int mouseY) {
        int x = getSearchX();
        int y = 6;
        return mouseX >= x && mouseX <= x + SEARCH_WIDTH && mouseY >= y && mouseY <= y + SEARCH_HEIGHT;
    }

    private void handleInvWalk() {
        KeyBinding[] keys = {mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint, mc.gameSettings.keyBindSneak};
        for (KeyBinding key : keys) KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
    }

    private void drawKeybinds(float animationProgress) {
        List<Module> bound = new ArrayList<>();
        for (Module module : Myau.moduleManager.modules.values()) if (module.getKey() != 0) bound.add(module);
        int alpha = (int) (255 * animationProgress);
        int boxWidth = 78;
        int boxHeight = 12 + bound.size() * 12;
        int x = this.width - boxWidth - 4;
        int y = this.height - boxHeight - 4;
        Gui.drawRect(x, y, x + boxWidth, y + boxHeight, withAlpha(CleanTheme.PANEL, alpha));
        Gui.drawRect(x, y, x + 2, y + 12, withAlpha(CleanTheme.ACCENT, alpha));
        Gui.drawRect(x, y, x + boxWidth, y + 12, withAlpha(CleanTheme.PANEL_DARK, alpha));
        this.fontRendererObj.drawStringWithShadow("Keybinds", x + 5, y + 3, withAlpha(CleanTheme.TEXT, alpha));
        int rowY = y + 12;
        for (Module module : bound) {
            this.fontRendererObj.drawStringWithShadow(module.getName(), x + 5, rowY + 3, withAlpha(0xFFBDBDBD, alpha));
            String key = KeyBindUtil.getKeyName(module.getKey());
            this.fontRendererObj.drawStringWithShadow(key, x + boxWidth - 5 - this.fontRendererObj.getStringWidth(key), rowY + 3, withAlpha(CleanTheme.MUTED, alpha));
            rowY += 12;
        }
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (isClosing) return;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) velocity += wheel > 0 ? -30.0D : 30.0D;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isClosing) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && isMouseOverSearch(mouseX, mouseY)) {
            searchFocused = true;
            return;
        }
        searchFocused = false;
        for (int i = frames.size() - 1; i >= 0; i--) {
            CleanFrame frame = frames.get(i);
            if (frame.mouseClicked(mouseX, mouseY, mouseButton, scrollY, searchQuery)) {
                draggingComponent = frame;
                frames.remove(i);
                frames.add(frame);
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (isClosing) return;
        super.mouseReleased(mouseX, mouseY, state);
        if (draggingComponent != null) {
            draggingComponent.mouseReleased(mouseX, mouseY, state, scrollY);
            draggingComponent = null;
        }
        for (CleanFrame frame : frames) frame.mouseReleased(mouseX, mouseY, state, scrollY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isClosing) return;
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingComponent != null) draggingComponent.updatePosition(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isClosing) return;
        if (System.currentTimeMillis() - this.openTime < 100L) return;
        boolean isBindingKey = false;
        for (CleanFrame frame : frames) {
            if (frame.isAnyComponentBinding()) {
                isBindingKey = true;
                break;
            }
        }
        if (isBindingKey) {
            for (CleanFrame frame : frames) frame.keyTyped(typedChar, keyCode);
            return;
        }
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && keyCode == Keyboard.KEY_A)) {
                searchQuery = "";
                return;
            }
            if (isAllowedSearchChar(typedChar) && this.fontRendererObj.getStringWidth(searchQuery + typedChar) < SEARCH_WIDTH - 10) {
                searchQuery += typedChar;
                return;
            }
            return;
        }
        Module clickGUIModule = Myau.moduleManager.getModule("ClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGUIModule != null && keyCode == clickGUIModule.getKey())) {
            close();
            return;
        }
        for (CleanFrame frame : frames) frame.keyTyped(typedChar, keyCode);
    }

    private boolean isAllowedSearchChar(char typedChar) {
        return typedChar >= 32 && typedChar < 127;
    }

    private void updateScroll() {
        targetScrollY += (int) velocity;
        velocity *= FRICTION;
        targetScrollY = Math.max(0, Math.min(targetScrollY, getMaxScroll()));
        int delta = targetScrollY - scrollY;
        scrollY += (int) (delta * SNAP_STRENGTH);
        if (Math.abs(velocity) < 0.5D) velocity = 0.0D;
        if (Math.abs(delta) < 1 && Math.abs(velocity) < 0.5D) scrollY = targetScrollY;
    }

    private int getMaxScroll() {
        int max = 0;
        for (CleanFrame frame : frames) max = Math.max(max, frame.getY() + (int) frame.getCurrentHeight());
        ScaledResolution sr = new ScaledResolution(mc);
        return Math.max(0, max - sr.getScaledHeight() + 20);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Module guiModule = Myau.moduleManager.getModule("ClickGUI");
        if (guiModule != null) guiModule.setEnabled(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

