package myau.ui.impl.clickgui.raven.components;

import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.enums.ChatColors;
import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.raven.Component;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ModeComponent implements Component {
    private final ModeProperty property;
    private final ModuleComponent parentModule;
    private int x;
    private int y;
    private int offsetY;

    public ModeComponent(ModeProperty desc, ModuleComponent parentModule, int offsetY) {
        this.property = desc;
        this.parentModule = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.yPos;
        this.offsetY = offsetY;
    }

    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        String mode = this.property.getModeString();
        mode = mode.replace("_", " ");
        int bruhWidth = (int) (Myau.fontManagers.getFont(24).getStringWidth(this.property.getName() + ": ") * 0.5);
        Myau.fontManagers.getFont(24).drawString(this.property.getName() + ": ", (float) ((this.parentModule.category.getX() + 4) * 2), (float) ((this.parentModule.category.getY() + this.offsetY + 4) * 2), 0xffffffff, true);
        Myau.fontManagers.getFont(24).drawString(ChatColors.formatColor("&9" + mode.substring(0, 1).toUpperCase() + mode.substring(1).toLowerCase()), (float) ((this.parentModule.category.getX() + 4 + bruhWidth) * 2), (float) ((this.parentModule.category.getY() + this.offsetY + 4) * 2), -1, true);
        GL11.glPopMatrix();
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getModuleY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public void onClick(int x, int y, int mouse) {
        if (isHovered(x, y) && this.parentModule.isOpened) {
            int oldHeight = this.parentModule.getModuleHeight();
            if (mouse == 0) {
                this.property.nextMode();
            } else if (mouse == 1) {
                this.property.previousMode();
            }
            int newHeight = this.parentModule.getModuleHeight();
            // If height changed, trigger smooth animation
            if (oldHeight != newHeight) {
                this.parentModule.startHeightAnimation(oldHeight, newHeight);
            }
        }
    }

    public void mouseDown(int x, int y, int button) {
    }

    @Override
    public void mouseReleased(int x, int y, int button) {

    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {

    }

    private boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 11;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void render() {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        String mode = this.property.getModeString();
        mode = mode.replace("_", " ");
        int bruhWidth = (int) (Myau.fontManagers.getFont(24).getStringWidth(this.property.getName() + ": ") * 0.5);
        Myau.fontManagers.getFont(24).drawString(this.property.getName() + ": ",
                (float) ((this.parentModule.category.getX() + 4) * 2),
                (float) ((this.parentModule.category.getModuleY() + this.offsetY + 4) * 2), 0xffffffff, true);
        Myau.fontManagers.getFont(24).drawString(
                mode.substring(0, 1).toUpperCase() + mode.substring(1).toLowerCase(),
                (float) ((this.parentModule.category.getX() + 4 + bruhWidth) * 2),
                (float) ((this.parentModule.category.getModuleY() + this.offsetY + 4) * 2),
                new Color(100, 200, 255).getRGB(), true);
        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.parentModule.category.getModuleY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    @Override
    public void updateHeight(int y) {
        this.offsetY = y;
    }

    @Override
    public void onScroll(int scroll) {
    }

    @Override
    public void onGuiClosed() {
    }
}
