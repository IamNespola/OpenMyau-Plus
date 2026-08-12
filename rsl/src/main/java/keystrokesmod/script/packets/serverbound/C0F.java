package keystrokesmod.script.packets.serverbound;

import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

public class C0F extends CPacket {
    public int windowId;
    public short uid;

    public C0F(int windowId, short uid) {
        super(null);
        this.windowId = windowId;
        this.uid = uid;
    }

    public C0F(C0FPacketConfirmTransaction packet) {
        super(packet);
        this.windowId = packet.getWindowId();
        this.uid = packet.getUid();
    }

    @Override
    public C0FPacketConfirmTransaction convert() {
        return new C0FPacketConfirmTransaction(this.windowId, this.uid, true);
    }
}
