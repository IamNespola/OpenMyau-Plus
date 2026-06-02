package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class Teams extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty scoreboard = new BooleanProperty("scoreboard", true);
    public final BooleanProperty friends = new BooleanProperty("friends", false);
    public final BooleanProperty teamColor = new BooleanProperty("team-color", true);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);

    public Teams() {
        super("Teams", true, true);
    }

    public boolean isTeam(EntityPlayer player) {
        if (!this.isEnabled() || player == null || player == mc.thePlayer) return false;
        if (this.botCheck.getValue() && TeamUtil.isBot(player)) return false;
        return this.scoreboard.getValue() && isSameScoreboardTeam(player)
                || this.friends.getValue() && TeamUtil.isFriend(player);
    }

    public boolean hasTeamColor(EntityLivingBase entity) {
        return this.isEnabled() && this.teamColor.getValue() && TeamUtil.hasTeamColor(entity);
    }

    private boolean isSameScoreboardTeam(EntityPlayer player) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return false;
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        NetworkPlayerInfo targetInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (selfInfo == null || targetInfo == null) return false;

        ScorePlayerTeam selfTeam = selfInfo.getPlayerTeam();
        ScorePlayerTeam targetTeam = targetInfo.getPlayerTeam();
        if (selfTeam == null || targetTeam == null) return false;
        if (selfTeam != targetTeam && !selfTeam.getRegisteredName().equals(targetTeam.getRegisteredName())) return false;

        String prefix = selfTeam.getColorPrefix();
        return prefix != null && prefix.length() >= 2 && !prefix.equals("§r") && !prefix.equals("§f");
    }
}
