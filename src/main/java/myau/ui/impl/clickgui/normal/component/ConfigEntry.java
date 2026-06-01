package myau.ui.impl.clickgui.normal.component;

import myau.config.Config;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

public class ConfigEntry extends Component {
    private final String configName;

    public ConfigEntry(String configName, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.configName = configName;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        if (isMouseOver(mouseX, mouseY, scrollOffset)) {
            RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, height, 4, MaterialTheme.getRGBWithAlpha(MaterialTheme.SURFACE_CONTAINER_HIGH, alpha), true, true, true, true);
        }

        boolean active = configName.equalsIgnoreCase(Config.lastConfig);
        int textColor = active ? MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha) : MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
        String text = configName;
        if (FontManager.productSans16 != null) {
            float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f + 1);
            FontManager.productSans16.drawString(text, x + 10, textY, textColor);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x + 8, scrolledY + 6, textColor);
        }
    }

    public float getCurrentHeight() {
        return height;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            new Config(configName, false).load();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}
