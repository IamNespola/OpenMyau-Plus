package keystrokesmod.script.packets.clientbound;

import keystrokesmod.mixin.impl.accessor.IAccessorS14PacketEntity;
import net.minecraft.network.play.server.S14PacketEntity;

public class S14 extends SPacket {
    public int entityId;
    public byte posX;
    public byte posY;
    public byte posZ;
    public byte yaw;
    public byte pitch;
    public boolean onGround;
    public boolean rotating;

    public S14(S14PacketEntity e) {
        super(e);
        this.entityId = ((IAccessorS14PacketEntity) e).getEntityId();
        this.posX = e.func_149062_c();
        this.posY = e.func_149061_d();
        this.posZ = e.func_149064_e();
        this.yaw = e.func_149066_f();
        this.pitch = e.func_149063_g();
        this.rotating = e.func_149060_h();
    }

    public S14(S14PacketEntity.S15PacketEntityRelMove e) {
        super(e);
        this.entityId = ((IAccessorS14PacketEntity) e).getEntityId();
        this.posX = e.func_149062_c();
        this.posY = e.func_149061_d();
        this.posZ = e.func_149064_e();
        this.yaw = e.func_149066_f();
        this.pitch = e.func_149063_g();
        this.rotating = e.func_149060_h();
    }

    public S14(S14PacketEntity.S16PacketEntityLook e) {
        super(e);
        this.entityId = ((IAccessorS14PacketEntity) e).getEntityId();
        this.posX = e.func_149062_c();
        this.posY = e.func_149061_d();
        this.posZ = e.func_149064_e();
        this.yaw = e.func_149066_f();
        this.pitch = e.func_149063_g();
        this.rotating = e.func_149060_h();
    }

    public S14(S14PacketEntity.S17PacketEntityLookMove e) {
        super(e);
        this.entityId = ((IAccessorS14PacketEntity) e).getEntityId();
        this.posX = e.func_149062_c();
        this.posY = e.func_149061_d();
        this.posZ = e.func_149064_e();
        this.yaw = e.func_149066_f();
        this.pitch = e.func_149063_g();
        this.rotating = e.func_149060_h();
    }

    public S14(int entityId, byte posX, byte posY, byte posZ, byte yaw, byte pitch, boolean onGround) {
        super(new S14PacketEntity.S17PacketEntityLookMove(entityId, posX, posY, posZ, yaw, pitch, onGround));
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public S14(int entityId, byte posX, byte posY, byte posZ, boolean onGround) {
        super(new S14PacketEntity.S15PacketEntityRelMove(entityId, posX, posY, posZ, onGround));
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.onGround = onGround;
    }

    public S14(int entityId, byte yaw, byte pitch, boolean onGround) {
        super(new S14PacketEntity.S16PacketEntityLook(entityId, yaw, pitch, onGround));
        this.entityId = entityId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }
}
