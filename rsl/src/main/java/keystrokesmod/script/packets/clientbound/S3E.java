package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3EPacketTeams;

import java.util.Collection;

public class S3E extends SPacket {
    public String name;
    public String displayName;
    public String prefix;
    public String suffix;
    public String nametagVisibility;
    public Collection<String> playerList;
    public int action;
    public int friendlyFlags;
    public int color;

    public S3E(S3EPacketTeams packet) {
        super(packet);
        this.name = packet.getName();
        this.displayName = packet.getDisplayName();
        this.prefix = packet.getPrefix();
        this.suffix = packet.getSuffix();
        this.nametagVisibility = packet.getNameTagVisibility();
        this.playerList = packet.getPlayers();
        this.action = packet.getAction();
        this.friendlyFlags = packet.getFriendlyFlags();
        this.color = packet.getColor();
    }

    public S3E(Packet packet, String name, String displayName, String prefix, String suffix, String nametagVisibility, Collection<String> playerList, int action, int friendlyFlags, int color) {
        super(packet);
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.nametagVisibility = nametagVisibility;
        this.playerList = playerList;
        this.action = action;
        this.friendlyFlags = friendlyFlags;
        this.color = color;
    }
}
