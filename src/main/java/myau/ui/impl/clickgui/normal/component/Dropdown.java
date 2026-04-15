package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import java.awt.*;

public class Dropdown extends Component {
    private static final int ITEM_HEIGHT = 18;
    private final ModeProperty modeProperty;
    private final int headerHeight;
    private final int parentFrameY;
    private boolean expanded;
    private float expandAnim = 0.0f;
    private String[] modesCache;

    public Dropdown(ModeProperty modeProperty, int x, int y, int width, int height, int parentFrameY) {
        super(x, y, width, height);
        this.modeProperty = modeProperty;
        this.parentFrameY = parentFrameY;
        this.headerHeight = height;
        this.modesCache = modeProperty.getValuePrompt().split(", ");
    }

    @Override
    public int getHeight() {
        return (int) (headerHeight + expandAnim + (expanded ? 4 : 0));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!modeProperty.isVisible()) return;

        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0.05f) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * easedProgress);

        Shader2D.drawRoundedRect(x + 2, scrolledY, width - 4, headerHeight, 4.0f, new Color(30, 30, 35, alpha));

        String nameStr = modeProperty.getName().replace("-", " ");
        String text = nameStr + ": " + modeProperty.getModeString();
        int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
        float textY = (float) (scrolledY + (headerHeight - FontManager.productSans16.getHeight()) / 2f);
        
        if (FontManager.productSans16 != null) {
            FontManager.productSans16.drawString(text, x + 6, textY, textColor);
        }

        this.modesCache = modeProperty.getValuePrompt().split(", ");
        float targetAnim = expanded ? modesCache.length * ITEM_HEIGHT : 0f;
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 12.0f, deltaTime);

        if (expandAnim > 1f) {
            int dropdownY = scrolledY + headerHeight + 2;

            Shader2D.drawRoundedRect(x + 2, dropdownY, width - 4, expandAnim, 4.0f, new Color(20, 20, 24, (int)(alpha * 0.94f)));

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            float finalTop = Math.max(dropdownY, parentFrameY + 24);
            float finalBottom = Math.min(dropdownY + expandAnim, parentFrameY + 280);
            float finalHeight = finalBottom - finalTop;

            if (finalHeight > 0) {
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                
                int sX = (int) (x * scale);
                int sY = (int) ((sr.getScaledHeight() - finalBottom) * scale);
                int sW = (int) (width * scale);
                int sH = (int) (finalHeight * scale);

                GL11.glScissor(sX, sY, sW, sH);

                for (int i = 0; i < modesCache.length; i++) {
                    String mode = modesCache[i];
                    int itemY = dropdownY + i * ITEM_HEIGHT;

                    boolean isHovered = mouseX >= x + 2 && mouseX <= x + width - 2 && 
                                      mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT &&
                                      mouseY < dropdownY + expandAnim;

                    int itemColor;
                    if (modeProperty.getModeString().equalsIgnoreCase(mode)) {
                        itemColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
                    } else {
                        itemColor = isHovered ? 
                                   MaterialTheme.getRGBWithAlpha(Color.WHITE, alpha) : 
                                   MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha);
                    }

                    if (FontManager.productSans16 != null) {
                        FontManager.productSans16.drawString(mode, x + 8, itemY + 5, itemColor);
                    }
                }

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (!modeProperty.isVisible()) return false;
        
        int scrolledY = y - scrollOffset;

        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + headerHeight) {
            expanded = !expanded;
            return true;
        }

        if (expanded && expandAnim > (modesCache.length * ITEM_HEIGHT) - 5) {
            int dropdownY = scrolledY + headerHeight + 2;

            if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                for (int i = 0; i < modesCache.length; i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                        modeProperty.setValue(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ModeProperty getProperty() {
        return modeProperty;
    }

    @Override public boolean mouseClicked(int mx, int my, int mb) { return false; }
    @Override public void mouseReleased(int mx, int my, int mb) {}
    @Override public void keyTyped(char tc, int kc) {}
}