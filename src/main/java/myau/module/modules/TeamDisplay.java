package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamDisplay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty offsetX = new IntProperty("offset-x", 4, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 42, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);

    private final Map<UUID, TeammateInfo> cachedTeammates = new HashMap<>();

    public TeamDisplay() {
        super("TeamDisplay", false, true);
    }

    @EventTarget(Priority.LOW)
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.showDebugInfo) return;
        GuiScreen screen = mc.currentScreen;
        if (screen != null && !(screen instanceof GuiChat)) return;

        List<TeammateInfo> teammates = getTeammates();
        if (teammates.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        GlStateManager.translate(this.offsetX.getValue() / this.scale.getValue(), this.offsetY.getValue() / this.scale.getValue(), 0.0F);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawLine(ChatColors.formatColor("&bTeamDisplay"), 0.0F, 0.0F, Color.CYAN.getRGB());
        int y = mc.fontRendererObj.FONT_HEIGHT + 2;
        for (TeammateInfo player : teammates) {
            Color healthColor = ColorUtil.getHealthBlend(player.health / player.maxHealth);
            String distanceText = player.distance >= 0 ? player.distance + "m" : "far";
            String text = ChatColors.formatColor(String.format("&f%s &7| &c%.1f❤ &7| &b%s", player.name, player.health, distanceText));
            drawLine(text, 0.0F, y, healthColor.getRGB());
            y += mc.fontRendererObj.FONT_HEIGHT + 1;
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private List<TeammateInfo> getTeammates() {
        Teams teams = (Teams) Myau.moduleManager.getModule(Teams.class);
        if (teams == null) return new ArrayList<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player == mc.thePlayer || player.isDead) continue;
            if (teams.isTeam(player)) {
                cachedTeammates.put(player.getUniqueID(), TeammateInfo.from(player));
            }
        }

        cachedTeammates.entrySet().removeIf(entry -> {
            EntityPlayer loaded = mc.theWorld.getPlayerEntityByUUID(entry.getKey());
            return loaded != null && (loaded.isDead || !teams.isTeam(loaded));
        });

        List<TeammateInfo> teammates = new ArrayList<>(cachedTeammates.values());
        teammates.sort(Comparator.comparingInt(player -> player.distance < 0 ? Integer.MAX_VALUE : player.distance));
        return teammates;
    }

    private void drawLine(String text, float x, float y, int color) {
        mc.fontRendererObj.drawString(text, x, y, color, this.shadow.getValue());
    }

    private static class TeammateInfo {
        private final String name;
        private final float health;
        private final float maxHealth;
        private final int distance;

        private TeammateInfo(String name, float health, float maxHealth, int distance) {
            this.name = name;
            this.health = Math.max(0.0F, health);
            this.maxHealth = Math.max(1.0F, maxHealth);
            this.distance = distance;
        }

        private static TeammateInfo from(EntityPlayer player) {
            int distance = mc.thePlayer == null ? -1 : (int) mc.thePlayer.getDistanceToEntity(player);
            return new TeammateInfo(player.getName(), player.getHealth(), player.getMaxHealth(), distance);
        }
    }

    @Override
    public void onDisabled() {
        this.cachedTeammates.clear();
    }
}