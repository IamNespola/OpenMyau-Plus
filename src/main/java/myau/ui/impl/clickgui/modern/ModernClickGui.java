package myau.ui.impl.clickgui.modern;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.*;
import myau.module.modules.Timer;
import myau.util.shader.BlurUtils;
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

public class ModernClickGui extends GuiScreen {
    private static final double FRICTION = 0.85;
    private static final double SNAP_STRENGTH = 0.15;
    private static final long ANIMATION_DURATION = 250L;
    private static ModernClickGui instance;
    private final ArrayList<Frame> frames;
    private Frame draggingComponent = null;
    private int scrollY = 0;
    private int targetScrollY = 0;
    private double velocity = 0;
    private boolean isClosing = false;
    private long openTime = 0L;
    private long lastFrameTime;

    public ModernClickGui() {
        this.frames = new ArrayList<>();
        int x = 20;
        for (String category : myau.module.ModuleCatalog.CATEGORIES) {
            List<Module> modules = myau.module.ModuleCatalog.modules(category);
            if (!modules.isEmpty()) {
                frames.add(new Frame(category, modules, x, 20, 110, 24));
                x += 125;
            }
        }
    }

    public static ModernClickGui getInstance() {
        if (instance == null) {
            instance = new ModernClickGui();
        }
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
        this.openTime = System.currentTimeMillis();
        this.lastFrameTime = System.nanoTime();
        this.scrollY = 0;
        this.targetScrollY = 0;
        this.velocity = 0;
        // Wrap category panels onto scrollable rows on smaller GUI scales.
        int columns = Math.max(1, (width - 30) / 125);
        int rowY = 20, rowHeight = 0;
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0 && i % columns == 0) { rowY += rowHeight + 24; rowHeight = 0; }
            Frame frame = frames.get(i);
            frame.setX(20 + (i % columns) * 125);
            frame.setY(rowY);
            String category = frame.getCategoryName();
            rowHeight = Math.max(rowHeight, 24 + myau.module.ModuleCatalog.modules(category).size() * 22);
        }
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
            Module clickGUI = Myau.moduleManager.getModule("ClickGUI");
            boolean useGlass = clickGUI instanceof ClickGUIModule && ((ClickGUIModule) clickGUI).glass.getValue();

            // Pass 1: Blur Mask
            if (useGlass) {
                BlurUtils.prepareBlur();
                for (Frame frame : frames) {
                    frame.renderBlurMask(scrollY);
                }
                BlurUtils.blurEnd(2, 4.0f);
            }

            // Pass 2: Visuals
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
        if (guiModule instanceof ClickGUIModule && ((ClickGUIModule) guiModule).isSwitchingGuiStyle()) {
            return;
        }
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }
}
