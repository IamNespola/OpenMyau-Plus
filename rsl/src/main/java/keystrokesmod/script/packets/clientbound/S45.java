package keystrokesmod.script.packets.clientbound;

import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.ChatComponentText;

public class S45 extends SPacket {
    public String type;
    public String message;
    private int fadeInTime;
    private int displayTime;
    private int fadeOutTime;

    public S45(S45PacketTitle packet) {
        super(packet);
        this.type = packet.getType().name();
        this.message = packet.getMessage().getUnformattedText();
        this.fadeInTime = packet.getFadeInTime();
        this.displayTime = packet.getDisplayTime();
        this.fadeOutTime = packet.getFadeOutTime();
    }

    public S45(String type, String message, int fadeInTime, int displayTime, int fadeOutTime) {
        super(new S45PacketTitle(Utils.getEnum(S45PacketTitle.Type.class, type), new ChatComponentText(message), fadeInTime, displayTime, fadeOutTime));
        this.type = type;
        this.message = message;
        this.fadeInTime = fadeInTime;
        this.displayTime = displayTime;
        this.fadeOutTime = fadeOutTime;
    }
}
