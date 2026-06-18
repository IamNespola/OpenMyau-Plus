package myau.ui.impl.clickgui.raven;

import java.util.concurrent.atomic.AtomicInteger;

public interface Component {
    void draw(AtomicInteger offset);

    void update(int mousePosX, int mousePosY);

    void mouseDown(int x, int y, int button);

    void mouseReleased(int x, int y, int button);

    void keyTyped(char chatTyped, int keyCode);

    void setComponentStartAt(int newOffsetY);

    int getHeight();

    boolean isVisible();

    void render();

    void drawScreen(int x, int y);

    void onClick(int x, int y, int mouse);

    void updateHeight(int y);

    void onScroll(int scroll);

    void onGuiClosed();
}
