package myau.ui.impl.clickgui.raven.components;

import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.enums.ChatColors;
import myau.property.properties.BooleanProperty;
import myau.ui.impl.clickgui.raven.Component;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckBoxComponent implements Component {
    private final BooleanProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;

    public CheckBoxComponent(BooleanProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.yPos;
        this.offsetY = offsetY;
    }


    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Myau.fontManagers.getFont(24).drawString(this.property.getName().replace("-", " ") + ": " + ChatColors.formatColor(this.property.formatValue()), (float) ((this.module.category.getX() + 4) * 2), (float) ((this.module.category.getY() + this.offsetY + 5) * 2), -1, false);
        GL11.glPopMatrix();
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
    }

    public void mouseDown(int x, int y, int button) {
    }

    @Override
    public void mouseReleased(int x, int y, int button) {

    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {

    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + 11;
    }


    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void render() {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Myau.fontManagers.getFont(24).drawString(
                (this.property.getValue() ? "[+]  " : "[-]  ") + this.property.getName(),
                (float) ((this.module.category.getX() + 4) * 2),
                (float) ((this.module.category.getModuleY() + this.offsetY + 4) * 2),
                this.property.getValue() ? (new Color(20, 255, 0)).getRGB() : -1,
                false);
        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.module.category.getModuleY() + this.offsetY;
        this.x = this.module.category.getX();
    }

    @Override
    public void onClick(int x, int y, int mouse) {
        if (this.isHovered(x, y) && mouse == 0 && this.module.isOpened) {
            this.property.setValue(!this.property.getValue());
            // Trigger height update to fix sub-options overlapping
            this.module.updateHeight(this.module.yPos);
            this.module.category.updateHeight();
        }
    }

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
