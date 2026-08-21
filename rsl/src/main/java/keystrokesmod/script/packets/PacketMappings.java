package keystrokesmod.script.packets;

import java.util.LinkedHashMap;
import java.util.Map;

import keystrokesmod.script.packets.serverbound.*;
import keystrokesmod.script.packets.clientbound.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

public class PacketMappings {
    public static final Map<Class<? extends Packet<?>>, Class<? extends CPacket>> minecraftToScriptC = new LinkedHashMap<>();

    public static final Map<Class<? extends Packet<?>>, Class<? extends SPacket>> minecraftToScriptS = new LinkedHashMap<>();

    static {
        // serverbound
        minecraftToScriptC.put(C0APacketAnimation.class, C0A.class);
        minecraftToScriptC.put(C0BPacketEntityAction.class, C0B.class);
        minecraftToScriptC.put(C01PacketChatMessage.class, C01.class);
        minecraftToScriptC.put(C02PacketUseEntity.class, C02.class);
        minecraftToScriptC.put(C0FPacketConfirmTransaction.class, C0F.class);
        minecraftToScriptC.put(C0EPacketClickWindow.class, C0E.class);
        minecraftToScriptC.put(C03PacketPlayer.class, C03.class);
        minecraftToScriptC.put(C03PacketPlayer.C04PacketPlayerPosition.class, C03.class);
        minecraftToScriptC.put(C03PacketPlayer.C05PacketPlayerLook.class, C03.class);
        minecraftToScriptC.put(C03PacketPlayer.C06PacketPlayerPosLook.class, C03.class);
        minecraftToScriptC.put(C07PacketPlayerDigging.class, C07.class);
        minecraftToScriptC.put(C08PacketPlayerBlockPlacement.class, C08.class);
        minecraftToScriptC.put(C09PacketHeldItemChange.class, C09.class);
        minecraftToScriptC.put(C10PacketCreativeInventoryAction.class, C10.class);
        minecraftToScriptC.put(C13PacketPlayerAbilities.class, C13.class);
        minecraftToScriptC.put(C16PacketClientStatus.class, C16.class);
        minecraftToScriptC.put(C0DPacketCloseWindow.class, C0D.class);

        // clientbound
        minecraftToScriptS.put(S12PacketEntityVelocity.class, S12.class);
        minecraftToScriptS.put(S27PacketExplosion.class, S27.class);
        minecraftToScriptS.put(S3EPacketTeams.class, S3E.class);
        minecraftToScriptS.put(S08PacketPlayerPosLook.class, S08.class);
        minecraftToScriptS.put(S2APacketParticles.class, S2A.class);
        minecraftToScriptS.put(S25PacketBlockBreakAnim.class, S25.class);
        minecraftToScriptS.put(S06PacketUpdateHealth.class, S06.class);
        minecraftToScriptS.put(S23PacketBlockChange.class, S23.class);
        minecraftToScriptS.put(S29PacketSoundEffect.class, S29.class);
        minecraftToScriptS.put(S2FPacketSetSlot.class, S2F.class);
        minecraftToScriptS.put(S48PacketResourcePackSend.class, S48.class);
        minecraftToScriptS.put(S3APacketTabComplete.class, S3A.class);
        minecraftToScriptS.put(S02PacketChat.class, S02.class);
        minecraftToScriptS.put(S45PacketTitle.class, S45.class);
        minecraftToScriptS.put(S0BPacketAnimation.class, S0B.class);
        minecraftToScriptS.put(S14PacketEntity.class, S14.class);
        minecraftToScriptS.put(S14PacketEntity.S15PacketEntityRelMove.class, S14.class);
        minecraftToScriptS.put(S14PacketEntity.S16PacketEntityLook.class, S14.class);
        minecraftToScriptS.put(S14PacketEntity.S17PacketEntityLookMove.class, S14.class);
        minecraftToScriptS.put(S13PacketDestroyEntities.class, S13.class);
        minecraftToScriptS.put(S18PacketEntityTeleport.class, S18.class);
        minecraftToScriptS.put(S04PacketEntityEquipment.class, S04.class);
    }
}