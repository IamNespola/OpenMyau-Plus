package myau.ui.impl.clickgui.normal;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.*;
import myau.module.modules.Timer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ClickGuiScreen extends GuiScreen {
    private static final double FRICTION = 0.85;
    private static final double SNAP_STRENGTH = 0.15;
    private static final long ANIMATION_DURATION = 250L;
    private static ClickGuiScreen instance;
    private final ArrayList<Frame> frames;
    private Frame draggingComponent = null;
    private int scrollY = 0;
    private int targetScrollY = 0;
    private double velocity = 0;
    private boolean isClosing = false;
    private long openTime = 0L;
    private long lastFrameTime;

    public ClickGuiScreen() {
        this.frames = new ArrayList<>();

        List<Module> combatModules = Arrays.asList(
                Myau.moduleManager.getModule(AimAssist.class),
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
                Myau.moduleManager.getModule(TimerRangev999.class),
                Myau.moduleManager.getModule(ClickAssits.class),
                Myau.moduleManager.getModule(Criticals.class),
                Myau.moduleManager.getModule(BlockHit.class),
                Myau.moduleManager.getModule(SprintReset.class),
                Myau.moduleManager.getModule(Displace.class)
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
                Myau.moduleManager.getModule(HitParticleEffects.class),
                Myau.moduleManager.getModule(DynamicIsland.class),
                Myau.moduleManager.getModule(ESP2D.class),
                Myau.moduleManager.getModule(TeamHealthDisplay.class),
                Myau.moduleManager.getModule(SeasonDisplay.class),
                Myau.moduleManager.getModule(Animations.class)
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
                Myau.moduleManager.getModule(LightningTracker.class),
                Myau.moduleManager.getModule(NoRotate.class),
                Myau.moduleManager.getModule(NickHider.class),
                Myau.moduleManager.getModule(AntiObbyTrap.class),
                Myau.moduleManager.getModule(AntiObfuscate.class),
                Myau.moduleManager.getModule(AutoAnduril.class),
                Myau.moduleManager.getModule(InventoryClicker.class),
                Myau.moduleManager.getModule(Disabler.class),
                Myau.moduleManager.getModule(ClientSpoofer.class),
                Myau.moduleManager.getModule(AutoHypixel.class)
        );

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        int currentX = 20;
        int currentY = 20;
        int frameWidth = 110;
        int frameHeight = 24;

        List<Module> combat = new ArrayList<>(combatModules);
        combat.removeIf(m -> m == null);
        if (!combat.isEmpty()) {
            frames.add(new Frame("Combat", combat, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> movement = new ArrayList<>(movementModules);
        movement.removeIf(m -> m == null);
        if (!movement.isEmpty()) {
            frames.add(new Frame("Movement", movement, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> render = new ArrayList<>(renderModules);
        render.removeIf(m -> m == null);
        if (!render.isEmpty()) {
            frames.add(new Frame("Render", render, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> player = new ArrayList<>(playerModules);
        player.removeIf(m -> m == null);
        if (!player.isEmpty()) {
            frames.add(new Frame("Player", player, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> misc = new ArrayList<>(miscModules);
        misc.removeIf(m -> m == null);
        if (!misc.isEmpty()) {
            frames.add(new Frame("Misc", misc, currentX, currentY, frameWidth, frameHeight));
        }
    }

    public static ClickGuiScreen getInstance() {
        if (instance == null) {
            instance = new ClickGuiScreen();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.isClosing = false;
        this.openTime = System.currentTimeMillis();
        this.lastFrameTime = System.nanoTime();
        this.scrollY = 0;
        this.targetScrollY = 0;
        this.velocity = 0;
    }

    public void close() {
        if (isClosing) return;
        this.isClosing = true;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long currentFrameTime = System.nanoTime();
        float deltaTime = (currentFrameTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentFrameTime;
        updateScroll();
        long elapsedTime = System.currentTimeMillis() - openTime;
        if (isClosing && elapsedTime > ANIMATION_DURATION) {
            mc.displayGuiScreen(null);
            return;
        }
        float screenAlpha = isClosing ? (1.0f - Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION)) : Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION);
        screenAlpha = (float) (1.0 - Math.pow(1.0 - screenAlpha, 3));
        if (screenAlpha > 0.01f) {
            for (Frame frame : frames) {
                frame.render(mouseX, mouseY, partialTicks, screenAlpha, false, scrollY, deltaTime);
            }
        }
        try {
            Module invWalkModule = Myau.moduleManager.getModule("InvWalk");
            if (invWalkModule != null && invWalkModule.isEnabled()) {
                handleInvWalk();
            }
        } catch (Exception ignored) {
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleInvWalk() {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint,
                mc.gameSettings.keyBindSneak
        };
        for (KeyBinding key : keys) {
            KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (isClosing) return;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            velocity += wheel > 0 ? -30 : 30;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isClosing) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (int i = frames.size() - 1; i >= 0; i--) {
            Frame frame = frames.get(i);
            if (frame.mouseClicked(mouseX, mouseY, mouseButton, scrollY)) {
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
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state, scrollY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isClosing) return;
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingComponent != null) {
            draggingComponent.updatePosition(mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isClosing) return;
        if (System.currentTimeMillis() - this.openTime < 100) return;
        boolean isBindingKey = false;
        for (Frame frame : frames) {
            if (frame.isAnyComponentBinding()) {
                isBindingKey = true;
                break;
            }
        }
        if (isBindingKey) {
            for (Frame frame : frames) {
                frame.keyTyped(typedChar, keyCode);
            }
            return;
        }
        Module clickGUIModule = Myau.moduleManager.getModule("ClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGUIModule != null && keyCode == clickGUIModule.getKey())) {
            close();
            return;
        }
        for (Frame frame : frames) {
            frame.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateScroll() {
        targetScrollY += (int) velocity;
        velocity *= FRICTION;
        int maxScroll = getMaxScroll();
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));
        int delta = targetScrollY - scrollY;
        scrollY += (int) (delta * SNAP_STRENGTH);
        if (Math.abs(velocity) < 0.5) velocity = 0;
        if (Math.abs(delta) < 1 && Math.abs(velocity) < 0.5) scrollY = targetScrollY;
    }

    private int getMaxScroll() {
        int max = 0;
        for (Frame frame : frames) {
            int bottom = frame.getY() + (int) frame.getCurrentHeight();
            if (bottom > max) max = bottom;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return Math.max(0, max - sr.getScaledHeight() + 20);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Module guiModule = Myau.moduleManager.getModule("ClickGUI");
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }
}
