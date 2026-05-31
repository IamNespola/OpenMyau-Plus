package myau.module.modules;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import myau.util.shader.BlurUtils;
import myau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RenderFixes extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DRAG_LIMIT = 5000;

    public final BooleanProperty shader = new BooleanProperty("shader", true);
    public final BooleanProperty chat = new BooleanProperty("chat", true);
    public final BooleanProperty scoreboard = new BooleanProperty("scoreboard", true);
    public final IntProperty chatX = new IntProperty("chat-x", 0, -DRAG_LIMIT, DRAG_LIMIT);
    public final IntProperty chatY = new IntProperty("chat-y", 0, -DRAG_LIMIT, DRAG_LIMIT);
    public final IntProperty scoreboardX = new IntProperty("scoreboard-x", 0, -DRAG_LIMIT, DRAG_LIMIT);
    public final IntProperty scoreboardY = new IntProperty("scoreboard-y", 0, -DRAG_LIMIT, DRAG_LIMIT);

    private enum DragTarget {
        NONE,
        SCOREBOARD
    }

    private DragTarget dragging = DragTarget.NONE;
    private boolean wasMouseDown;
    private float dragOffsetX;
    private float dragOffsetY;
    private static Bounds lastScoreboardBounds;
    private static long lastScoreboardRender;

    public RenderFixes() {
        super("RenderFixes", true, false, "Modern rounded chat and scoreboard rendering");
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        updateDragging();
    }

    public static RenderFixes get() {
        if (Myau.moduleManager == null) {
            return null;
        }
        Module module = Myau.moduleManager.getModule(RenderFixes.class);
        return module instanceof RenderFixes ? (RenderFixes) module : null;
    }

    public static boolean isActive() {
        RenderFixes module = get();
        return module != null && module.isEnabled();
    }

    public static boolean isChatActive() {
        RenderFixes module = get();
        return module != null && module.isEnabled() && module.chat.getValue() && RenderUtil.customHudEffects();
    }

    public static boolean isChatScreenOpen() {
        return mc.currentScreen instanceof GuiChat;
    }

    public static boolean shouldReplaceChatBackground() {
        return isChatActive() && isChatScreenOpen();
    }

    public static boolean isScoreboardActive() {
        RenderFixes module = get();
        return module != null && module.isEnabled() && module.scoreboard.getValue() && RenderUtil.customHudEffects();
    }

    public static int getChatOffsetX() {
        RenderFixes module = get();
        return module == null ? 0 : module.chatX.getValue();
    }

    public static int getChatOffsetY() {
        RenderFixes module = get();
        return module == null ? 0 : module.chatY.getValue();
    }

    private static int getChatRenderOffsetX() {
        return isChatScreenOpen() ? 0 : getChatOffsetX();
    }

    private static int getChatRenderOffsetY() {
        return isChatScreenOpen() ? 0 : getChatOffsetY();
    }

    public static void translateChat() {
        GlStateManager.translate((float) getChatOffsetX(), (float) getChatOffsetY(), 0.0F);
    }

    public static int adjustChatMouseX(int mouseX) {
        if (!isChatActive() || !isChatScreenOpen()) {
            return mouseX;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return mouseX - getChatRenderOffsetX() * sr.getScaleFactor();
    }

    public static int adjustChatMouseY(int mouseY) {
        if (!isChatActive() || !isChatScreenOpen()) {
            return mouseY;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return mouseY + getChatRenderOffsetY() * sr.getScaleFactor();
    }

    public static void renderChatHistoryBackground(GuiNewChat chatGui) {
        if (!isChatActive()) {
            return;
        }

        Bounds bounds = getChatHistoryBounds(chatGui);
        if (bounds == null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -(sr.getScaledHeight() - 48.0F), 0.0F);
        drawGlassPanel(bounds.x, bounds.y, bounds.width, bounds.height, 7.0F, 92);
        GlStateManager.popMatrix();
    }

    public static boolean renderChat(GuiNewChat chatGui, int updateCounter, List<ChatLine> drawnChatLines, int scrollPos, boolean isScrolled) {
        if (!isChatActive() || chatGui == null || mc.gameSettings == null || mc.fontRendererObj == null) {
            return false;
        }
        if (mc.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) {
            return false;
        }

        int lineCount = chatGui.getLineCount();
        int totalLines = drawnChatLines.size();
        if (totalLines <= 0) {
            return true;
        }

        float scale = Math.max(0.1F, chatGui.getChatScale());
        int chatWidth = MathHelper.ceiling_float_int((float) chatGui.getChatWidth() / scale);
        renderChatHistoryBackground(chatGui);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) getChatRenderOffsetX(), (float) getChatRenderOffsetY(), 0.0F);
        GlStateManager.translate(2.0F, 20.0F, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);

        int renderedLines = 0;
        for (int i = 0; i + scrollPos < totalLines && i < lineCount; ++i) {
            ChatLine chatLine = drawnChatLines.get(i + scrollPos);
            if (chatLine != null) {
                ++renderedLines;
                int y = -i * 9;
                String text = chatLine.getChatComponent().getFormattedText();
                GlStateManager.enableBlend();
                mc.fontRendererObj.drawString(text, 0.0F, (float) (y - 8), 16777215 + (255 << 24), RenderUtil.hudShadow());
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
            }
        }

        if (isChatScreenOpen()) {
            int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
            int totalHeight = totalLines * fontHeight + totalLines;
            int visibleHeight = renderedLines * fontHeight + renderedLines;
            int scrollBarY = scrollPos * visibleHeight / totalLines;
            int scrollBarHeight = visibleHeight * visibleHeight / totalHeight;

            if (totalHeight != visibleHeight) {
                int trackAlpha = scrollBarY > 0 ? 170 : 96;
                int railColor = isScrolled ? 13382451 : 3355562;
                Gui.drawRect(0, -scrollBarY, 2, -scrollBarY - scrollBarHeight, railColor + (trackAlpha << 24));
                Gui.drawRect(2, -scrollBarY, 1, -scrollBarY - scrollBarHeight, 13421772 + (trackAlpha << 24));
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public static void renderChatInputBackground(int width, int height) {
        if (!isChatActive()) {
            return;
        }

        float x = 2.0F;
        float y = height - 14.0F;
        float boxWidth = width - 4.0F;
        drawGlassPanel(x, y, boxWidth, 12.0F, 5.0F, 96);
    }

    public static boolean renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes) {
        if (!isScoreboardActive() || objective == null || mc.fontRendererObj == null) {
            return false;
        }

        Scoreboard board = objective.getScoreboard();
        Collection<Score> sortedScores = board.getSortedScores(objective);
        List<Score> visibleScores = new ArrayList<Score>();

        for (Score score : sortedScores) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                visibleScores.add(score);
            }
        }

        Collection<Score> scores;
        if (visibleScores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(visibleScores, visibleScores.size() - 15));
        } else {
            scores = visibleScores;
        }

        if (scores.isEmpty()) {
            lastScoreboardBounds = null;
            return true;
        }

        int textWidth = mc.fontRendererObj.getStringWidth(objective.getDisplayName());
        for (Score score : scores) {
            ScorePlayerTeam team = board.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()) + ": " + EnumChatFormatting.RED + score.getScorePoints();
            textWidth = Math.max(textWidth, mc.fontRendererObj.getStringWidth(line));
        }

        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        float width = textWidth + 10.0F;
        float height = (scores.size() + 1) * fontHeight + 9.0F;
        float defaultX = scaledRes.getScaledWidth() - width - 3.0F;
        float defaultY = scaledRes.getScaledHeight() / 2.0F + scores.size() * fontHeight / 3.0F
                - scores.size() * fontHeight - fontHeight - 4.0F;
        RenderFixes module = get();
        float x = defaultX + (module == null ? 0 : module.scoreboardX.getValue());
        float y = defaultY + (module == null ? 0 : module.scoreboardY.getValue());

        lastScoreboardBounds = new Bounds(x, y, width, height, defaultX, defaultY);
        lastScoreboardRender = System.currentTimeMillis();

        drawGlassPanel(x, y, width, height, 7.0F, 98);

        int textColor = new Color(238, 241, 245, 238).getRGB();
        int scoreColor = new Color(255, 106, 106, 238).getRGB();
        String title = objective.getDisplayName();
        float titleX = x + width / 2.0F - mc.fontRendererObj.getStringWidth(title) / 2.0F;
        mc.fontRendererObj.drawString(title, titleX, y + 4.0F, textColor, RenderUtil.hudShadow());

        List<Score> displayScores = new ArrayList<Score>(scores);
        float lineY = y + fontHeight + 7.0F;
        for (int i = displayScores.size() - 1; i >= 0; i--) {
            Score score = displayScores.get(i);
            ScorePlayerTeam team = board.getPlayersTeam(score.getPlayerName());
            String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String value = String.valueOf(score.getScorePoints());
            mc.fontRendererObj.drawString(name, x + 5.0F, lineY, textColor, RenderUtil.hudShadow());
            mc.fontRendererObj.drawString(value, x + width - 5.0F - mc.fontRendererObj.getStringWidth(value), lineY, scoreColor, RenderUtil.hudShadow());
            lineY += fontHeight;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    private void updateDragging() {
        if (!this.isEnabled() || !(mc.currentScreen instanceof GuiChat)) {
            dragging = DragTarget.NONE;
            wasMouseDown = false;
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float mouseX = Mouse.getX() * sr.getScaledWidth() / (float) mc.displayWidth;
        float mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / (float) mc.displayHeight - 1.0F;
        boolean mouseDown = Mouse.isButtonDown(0);

        if (mouseDown && !wasMouseDown) {
            Bounds scoreboardBounds = this.scoreboard.getValue() ? getLiveScoreboardBounds() : null;

            if (scoreboardBounds != null && scoreboardBounds.contains(mouseX, mouseY)) {
                dragging = DragTarget.SCOREBOARD;
                dragOffsetX = mouseX - scoreboardBounds.x;
                dragOffsetY = mouseY - scoreboardBounds.y;
            }
        }

        if (!mouseDown) {
            dragging = DragTarget.NONE;
        } else if (dragging == DragTarget.SCOREBOARD && this.scoreboard.getValue()) {
            Bounds scoreboardBounds = getLiveScoreboardBounds();
            if (scoreboardBounds != null) {
                setClamped(scoreboardX, Math.round(mouseX - dragOffsetX - scoreboardBounds.defaultX));
                setClamped(scoreboardY, Math.round(mouseY - dragOffsetY - scoreboardBounds.defaultY));
            }
        }

        wasMouseDown = mouseDown;
    }

    private static Bounds getChatHistoryBounds(GuiNewChat chatGui) {
        if (chatGui == null || mc.gameSettings == null || mc.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) {
            return null;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = Math.max(0.1F, chatGui.getChatScale());
        float rawWidth = (float) Math.ceil(chatGui.getChatWidth() / scale) + 4.0F;
        float width = rawWidth * scale;
        float height = chatGui.getLineCount() * 9.0F * scale + 8.0F;
        float defaultX = 2.0F - 3.0F;
        float defaultY = sr.getScaledHeight() - 28.0F - chatGui.getLineCount() * 9.0F * scale - 4.0F;
        float x = defaultX + getChatRenderOffsetX();
        float y = defaultY + getChatRenderOffsetY();
        return new Bounds(x, y, width + 6.0F, height, defaultX, defaultY);
    }

    private static Bounds getLiveScoreboardBounds() {
        if (lastScoreboardBounds == null || System.currentTimeMillis() - lastScoreboardRender > 250L) {
            return null;
        }
        return lastScoreboardBounds;
    }

    private static void setClamped(IntProperty property, int value) {
        int clamped = Math.max(property.getMinimum(), Math.min(property.getMaximum(), value));
        property.setValue(clamped);
    }

    private static void drawGlassPanel(float x, float y, float width, float height, float radius, int alpha) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        RenderUtil.resetColor();
        if (RenderUtil.hudBloom()) {
            BlurUtils.prepareBloom();
            RoundedUtils.drawRound(x, y, width, height, radius, true, new Color(0, 0, 0, Math.min(210, alpha + 82)));
            BlurUtils.bloomEnd(3, 2.0F);
        }
        if (RenderUtil.hudBlur()) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(x, y, width, height, radius, true, new Color(0, 0, 0, Math.min(180, alpha + 48)));
            BlurUtils.blurEnd(2, 3.0F);
        }

        int background = new Color(7, 9, 13, alpha).getRGB();
        int highlight = new Color(255, 255, 255, (RenderUtil.hudBlur() || RenderUtil.hudBloom()) ? 16 : 9).getRGB();
        int outline = new Color(255, 255, 255, (RenderUtil.hudBlur() || RenderUtil.hudBloom()) ? 30 : 18).getRGB();
        RenderUtil.drawRoundedRect(x, y, width, height, radius, background, true, true, true, true);
        RenderUtil.drawRoundedRect(x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, Math.max(0.0F, radius - 1.0F),
                highlight, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(x + 0.5F, y + 0.5F, width - 1.0F, height - 1.0F, radius, 1.0F,
                outline, true, true, true, true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static final class Bounds {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final float defaultX;
        private final float defaultY;

        private Bounds(float x, float y, float width, float height, float defaultX, float defaultY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
        }

        private boolean contains(float mouseX, float mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
