package keystrokesmod.script.packets.clientbound;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S0BPacketAnimation;

import java.util.UUID;

public class S0B extends SPacket {
    public int entityId;
    public int type;

    public S0B(S0BPacketAnimation packet) {
        super(packet);
        this.entityId = packet.getEntityID();
        this.type = packet.getAnimationType();
    }

    public S0B(int entityId, int type) {
        super(new S0BPacketAnimation(createEntityWithCustomID(entityId), type));
        this.entityId = entityId;
        this.type = type;
    }

    private static Entity createEntityWithCustomID(int id) {
        EntityOtherPlayerMP entity = new EntityOtherPlayerMP(Minecraft.getMinecraft().theWorld, new GameProfile(UUID.fromString("ae7a42d0-31b1-4926-ba4c-f28d6efad4a9"), ""));
        entity.setEntityId(id);
        return entity;
    }
}
