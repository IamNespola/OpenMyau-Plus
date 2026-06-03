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
    public final BooleanProperty scoreboard = new BooleanProperty("scoreboard", true);
    public final BooleanProperty friends = new BooleanProperty("friends", false);
    public final BooleanProperty teamColor = new BooleanProperty("team-color", true);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);

    public Teams() {
        super("Teams", true, true);
    }

    public boolean isTeam(EntityPlayer player) {
        if (!this.isEnabled() || player == null) return false;
        if (this.botCheck.getValue() && TeamUtil.isBot(player)) return false;
        return this.scoreboard.getValue() && TeamUtil.isSameTeam(player)
                || this.friends.getValue() && TeamUtil.isFriend(player);
    }

    public boolean hasTeamColor(EntityLivingBase entity) {
        return this.isEnabled() && this.teamColor.getValue() && TeamUtil.hasTeamColor(entity);
    }
}

