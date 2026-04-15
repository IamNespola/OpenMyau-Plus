package myau.ui.impl.clickgui.normal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import myau.Myau;
import myau.module.Category;
import myau.module.Module;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;

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

        int currentX = 20;
        int currentY = 20;
        int frameWidth = 110;
        int frameHeight = 24;

        for (Category category : Category.values()) {
            List<Module> categoryModules = Myau.moduleManager.modules.values().stream()
                    .filter(m -> m.getCategory() == category)
                    .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                    .collect(Collectors.toList());

            if (!categoryModules.isEmpty()) {
                frames.add(new Frame(
                    category.getName(), 
                    categoryModules, 
                    currentX, 
                    currentY, 
                    frameWidth, 
                    frameHeight
                ));
                
                currentX += (frameWidth + 15);
            }
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
