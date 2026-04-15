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

public class ClickGuiScreen extends GuiScreen {
    private static final long ANIMATION_DURATION = 250L;
    private static ClickGuiScreen instance;
    
    private final ArrayList<Frame> frames;
    private Frame draggingFrame = null;
    private float dragX, dragY;
    
    private boolean isClosing = false;
    private long openTime = 0L;
    private long lastFrameTime;

    public ClickGuiScreen() {
        this.frames = new ArrayList<>();
        int currentX = 20;
        int currentY = 20;
        int frameWidth = 110;
        int headerHeight = 24;

        for (Category category : Category.values()) {
            List<Module> modules = Myau.moduleManager.modules.values().stream()
                    .filter(m -> m.getCategory() == category)
                    .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                    .collect(Collectors.toList());

            if (!modules.isEmpty()) {
                frames.add(new Frame(category.getName(), modules, currentX, currentY, frameWidth, headerHeight));
                currentX += (frameWidth + 15);
            }
        }
    }

    public static ClickGuiScreen getInstance() {
        if (instance == null) instance = new ClickGuiScreen();
        return instance;
    }

    public static void resetInstance() { instance = null; }

    @Override
    public void initGui() {
        super.initGui();
        this.isClosing = false;
        this.openTime = System.currentTimeMillis();
        this.lastFrameTime = System.nanoTime();
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

        long elapsedTime = System.currentTimeMillis() - openTime;
        if (isClosing && elapsedTime > ANIMATION_DURATION) {
            mc.displayGuiScreen(null);
            return;
        }

        float alpha = isClosing ? 
            (1.0f - Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION)) : 
            Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION);
        alpha = (float) (1.0 - Math.pow(1.0 - alpha, 3));

        if (draggingFrame != null) {
            draggingFrame.setX(mouseX - (int) dragX);
            draggingFrame.setY(mouseY - (int) dragY);
        }

        if (alpha > 0.01f) {
            for (Frame frame : frames) {
                frame.render(mouseX, mouseY, partialTicks, alpha, false, 0, deltaTime);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (System.currentTimeMillis() - openTime < 100 || isClosing) return;

        for (int i = frames.size() - 1; i >= 0; i--) {
            Frame frame = frames.get(i);
            
            if (mouseX >= frame.getX() && mouseX <= frame.getX() + frame.getWidth() && 
                mouseY >= frame.getY() && mouseY <= frame.getY() + 24) {
                if (mouseButton == 0) {
                    draggingFrame = frame;
                    dragX = mouseX - frame.getX();
                    dragY = mouseY - frame.getY();
                    frames.remove(i);
                    frames.add(frame);
                    return;
                }
            }

            if (frame.mouseClicked(mouseX, mouseY, mouseButton, 0)) {
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            for (Frame frame : frames) {
                if (mouseX >= frame.getX() && mouseX <= frame.getX() + frame.getWidth() &&
                    mouseY >= frame.getY() && mouseY <= frame.getY() + frame.getCurrentHeight()) {
                    
                    frame.handleScroll(wheel);
                    break; 
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingFrame = null;
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state, 0);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (System.currentTimeMillis() - openTime < 100 || isClosing) return;

        if (frames.stream().anyMatch(Frame::isAnyComponentBinding)) {
            for (Frame frame : frames) frame.keyTyped(typedChar, keyCode);
            return;
        }

        Module clickGuiModule = Myau.moduleManager.getModule("ClickGUI");
        int bind = (clickGuiModule != null) ? clickGuiModule.getKey() : Keyboard.KEY_RSHIFT;

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == bind) {
            close();
            return;
        }
        
        for (Frame frame : frames) frame.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Module guiModule = Myau.moduleManager.getModule("ClickGUI");
        if (guiModule != null && guiModule.isEnabled()) {
            guiModule.setEnabled(false);
        }
    }
}