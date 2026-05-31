package myau.ui.hudeditor;

import myau.Myau;
import myau.config.HudConfig;
import myau.module.modules.*;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int COLOR_IDLE = new Color(60, 162, 253, 170).getRGB();
    private static final int COLOR_ACTIVE = new Color(255, 85, 85, 190).getRGB();
    private static final int OVERLAY_COLOR = new Color(0, 0, 0, 45).getRGB();

    private final List<DraggableElement> elements = new ArrayList<>();
    private DraggableElement dragging;
    private ScaledResolution resolution;
    private int dragOffsetX;
    private int dragOffsetY;

    @Override
    public void initGui() {
        HudConfig.load();
        this.resolution = new ScaledResolution(mc);
        this.rebuildElements();
    }

    private void rebuildElements() {
        this.elements.clear();
        addArrayList();
        addTopLeft("TargetHUD", HUD.targetHUDX, HUD.targetHUDY, 150, 48, HUD::setTargetHUDPosition);
        addTopLeft("MurdererDetector", defaulted(MurderDetector.textX, centerX(130)), MurderDetector.textY, 130, 24, MurderDetector::setTextPosition);
        addCentered("Radar", Radar.x, Radar.y, 120, 120, Radar::setPosition);
        addTopLeft("WaterMark", WaterMark.x, WaterMark.y, 150, 18, WaterMark::setPosition);
        addTopLeft("WaterMark2", WaterMark2.x, WaterMark2.y, 80, 20, WaterMark2::setPosition);
        addTopLeft("DynamicIsland", defaulted(DynamicIsland.x, centerX(340)), DynamicIsland.y, 340, 26, DynamicIsland::setPosition);
        addTopLeft("SeasonDisplay", SeasonDisplay.x, SeasonDisplay.y, 145, 48, SeasonDisplay::setPosition);
        addCentered("FPSCounter", defaulted(FPScounter.x, resolution.getScaledWidth() / 2), defaulted(FPScounter.y, resolution.getScaledHeight() / 2), 70, 20, FPScounter::setPosition);
    }

    private void addArrayList() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        float scale = hud == null ? 1.0F : hud.scale.getValue();
        int rowHeight = Math.max(8, Math.round(mc.fontRendererObj.FONT_HEIGHT * scale));
        int height = hud == null ? 18 : Math.max(18, hud.getVisibleArrayListSize() * rowHeight);
        int width = hud == null ? 110 : Math.max(90, Math.round(hud.getVisibleArrayListWidth() * scale));
        addTopLeft("ArrayList", HUD.arrayListX, HUD.arrayListY, width, height, HUD::setArrayListPosition);
    }

    private int defaulted(int value, int fallback) {
        return value < 0 ? fallback : value;
    }

    private int centerX(int width) {
        return resolution.getScaledWidth() / 2 - width / 2;
    }

    private void addTopLeft(String name, int x, int y, int width, int height, PositionApplier applier) {
        this.elements.add(new DraggableElement(name, x, y, width, height, applier, false));
    }

    private void addCentered(String name, int centerX, int centerY, int width, int height, PositionApplier applier) {
        this.elements.add(new DraggableElement(name, centerX - width / 2, centerY - height / 2, width, height, applier, true));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, resolution.getScaledWidth(), resolution.getScaledHeight(), OVERLAY_COLOR);
        drawCenteredString(fontRendererObj, "HudEditor - drag directly on HUD elements | ESC to close", resolution.getScaledWidth() / 2, 8, 0xFFFFFFFF);

        for (DraggableElement element : elements) {
            element.draw(mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;

        for (int i = elements.size() - 1; i >= 0; i--) {
            DraggableElement element = elements.get(i);
            if (element.isHovered(mouseX, mouseY)) {
                dragging = element;
                dragOffsetX = mouseX - element.x;
                dragOffsetY = mouseY - element.y;
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (dragging != null && clickedMouseButton == 0) {
            dragging.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (dragging != null) {
            HudConfig.save();
        }
        dragging = null;
    }

    @Override
    public void onGuiClosed() {
        HudConfig.save();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private interface PositionApplier {
        void apply(int x, int y);
    }

    private class DraggableElement {
        private final String name;
        private final int width;
        private final int height;
        private final PositionApplier applier;
        private final boolean centeredPosition;
        private int x;
        private int y;

        private DraggableElement(String name, int x, int y, int width, int height, PositionApplier applier, boolean centeredPosition) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.applier = applier;
            this.centeredPosition = centeredPosition;
        }

        private boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private void setPosition(int x, int y) {
            this.x = clamp(x, 0, resolution.getScaledWidth() - width);
            this.y = clamp(y, 0, resolution.getScaledHeight() - height);
            if (centeredPosition) {
                applier.apply(this.x + width / 2, this.y + height / 2);
            } else {
                applier.apply(this.x, this.y);
            }
        }

        private void draw(int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY) || this == dragging;
            int color = hovered ? COLOR_ACTIVE : COLOR_IDLE;
            RenderUtil.drawRectOutline(x, y, width, height, 1.0F, color);
            if (hovered) {
                fontRendererObj.drawStringWithShadow(name, x + 3, y - 9, 0xFFFFFFFF);
            }
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
