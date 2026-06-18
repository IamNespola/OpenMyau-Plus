package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isWorldRenderPacket(Packet<?> packet) {
        return packet instanceof S21PacketChunkData
                || packet instanceof S22PacketMultiBlockChange
                || packet instanceof S23PacketBlockChange
                || packet instanceof S24PacketBlockAction
                || packet instanceof S25PacketBlockBreakAnim
                || packet instanceof S26PacketMapChunkBulk
                || packet instanceof S28PacketEffect;
    }

    public static void sendPacket(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacket(packet, null);
    }

    public static void handlePacket(Packet<INetHandlerPlayClient> packet) {
        if (packet == null) return;

        if (packet instanceof S00PacketKeepAlive) {
            mc.getNetHandler().handleKeepAlive((S00PacketKeepAlive) packet);
        } else if (packet instanceof S01PacketJoinGame) {
            mc.getNetHandler().handleJoinGame((S01PacketJoinGame) packet);
        } else if (packet instanceof S02PacketChat) {
            mc.getNetHandler().handleChat((S02PacketChat) packet);
        } else if (packet instanceof S03PacketTimeUpdate) {
            mc.getNetHandler().handleTimeUpdate((S03PacketTimeUpdate) packet);
        } else if (packet instanceof S04PacketEntityEquipment) {
            mc.getNetHandler().handleEntityEquipment((S04PacketEntityEquipment) packet);
        } else if (packet instanceof S05PacketSpawnPosition) {
            mc.getNetHandler().handleSpawnPosition((S05PacketSpawnPosition) packet);
        } else if (packet instanceof S06PacketUpdateHealth) {
            mc.getNetHandler().handleUpdateHealth((S06PacketUpdateHealth) packet);
        } else if (packet instanceof S07PacketRespawn) {
            mc.getNetHandler().handleRespawn((S07PacketRespawn) packet);
        } else if (packet instanceof S08PacketPlayerPosLook) {
            mc.getNetHandler().handlePlayerPosLook((S08PacketPlayerPosLook) packet);
        } else if (packet instanceof S09PacketHeldItemChange) {
            mc.getNetHandler().handleHeldItemChange((S09PacketHeldItemChange) packet);
        } else if (packet instanceof S10PacketSpawnPainting) {
            mc.getNetHandler().handleSpawnPainting((S10PacketSpawnPainting) packet);
        } else if (packet instanceof S0APacketUseBed) {
            mc.getNetHandler().handleUseBed((S0APacketUseBed) packet);
        } else if (packet instanceof S0BPacketAnimation) {
            mc.getNetHandler().handleAnimation((S0BPacketAnimation) packet);
        } else if (packet instanceof S0CPacketSpawnPlayer) {
            mc.getNetHandler().handleSpawnPlayer((S0CPacketSpawnPlayer) packet);
        } else if (packet instanceof S0DPacketCollectItem) {
            mc.getNetHandler().handleCollectItem((S0DPacketCollectItem) packet);
        } else if (packet instanceof S0EPacketSpawnObject) {
            mc.getNetHandler().handleSpawnObject((S0EPacketSpawnObject) packet);
        } else if (packet instanceof S0FPacketSpawnMob) {
            mc.getNetHandler().handleSpawnMob((S0FPacketSpawnMob) packet);
        } else if (packet instanceof S11PacketSpawnExperienceOrb) {
            mc.getNetHandler().handleSpawnExperienceOrb((S11PacketSpawnExperienceOrb) packet);
        } else if (packet instanceof S12PacketEntityVelocity) {
            mc.getNetHandler().handleEntityVelocity((S12PacketEntityVelocity) packet);
        } else if (packet instanceof S13PacketDestroyEntities) {
            mc.getNetHandler().handleDestroyEntities((S13PacketDestroyEntities) packet);
        } else if (packet instanceof S14PacketEntity) {
            mc.getNetHandler().handleEntityMovement((S14PacketEntity) packet);
        } else if (packet instanceof S18PacketEntityTeleport) {
            mc.getNetHandler().handleEntityTeleport((S18PacketEntityTeleport) packet);
        } else if (packet instanceof S19PacketEntityStatus) {
            mc.getNetHandler().handleEntityStatus((S19PacketEntityStatus) packet);
        } else if (packet instanceof S19PacketEntityHeadLook) {
            mc.getNetHandler().handleEntityHeadLook((S19PacketEntityHeadLook) packet);
        } else if (packet instanceof S1BPacketEntityAttach) {
            mc.getNetHandler().handleEntityAttach((S1BPacketEntityAttach) packet);
        } else if (packet instanceof S1CPacketEntityMetadata) {
            mc.getNetHandler().handleEntityMetadata((S1CPacketEntityMetadata) packet);
        } else if (packet instanceof S1DPacketEntityEffect) {
            mc.getNetHandler().handleEntityEffect((S1DPacketEntityEffect) packet);
        } else if (packet instanceof S1EPacketRemoveEntityEffect) {
            mc.getNetHandler().handleRemoveEntityEffect((S1EPacketRemoveEntityEffect) packet);
        } else if (packet instanceof S1FPacketSetExperience) {
            mc.getNetHandler().handleSetExperience((S1FPacketSetExperience) packet);
        } else if (packet instanceof S20PacketEntityProperties) {
            mc.getNetHandler().handleEntityProperties((S20PacketEntityProperties) packet);
        } else if (packet instanceof S21PacketChunkData) {
            mc.getNetHandler().handleChunkData((S21PacketChunkData) packet);
        } else if (packet instanceof S22PacketMultiBlockChange) {
            mc.getNetHandler().handleMultiBlockChange((S22PacketMultiBlockChange) packet);
        } else if (packet instanceof S23PacketBlockChange) {
            mc.getNetHandler().handleBlockChange((S23PacketBlockChange) packet);
        } else if (packet instanceof S24PacketBlockAction) {
            mc.getNetHandler().handleBlockAction((S24PacketBlockAction) packet);
        } else if (packet instanceof S25PacketBlockBreakAnim) {
            mc.getNetHandler().handleBlockBreakAnim((S25PacketBlockBreakAnim) packet);
        } else if (packet instanceof S26PacketMapChunkBulk) {
            mc.getNetHandler().handleMapChunkBulk((S26PacketMapChunkBulk) packet);
        } else if (packet instanceof S27PacketExplosion) {
            mc.getNetHandler().handleExplosion((S27PacketExplosion) packet);
        } else if (packet instanceof S28PacketEffect) {
            mc.getNetHandler().handleEffect((S28PacketEffect) packet);
        } else if (packet instanceof S29PacketSoundEffect) {
            mc.getNetHandler().handleSoundEffect((S29PacketSoundEffect) packet);
        } else if (packet instanceof S2APacketParticles) {
            mc.getNetHandler().handleParticles((S2APacketParticles) packet);
        } else if (packet instanceof S2BPacketChangeGameState) {
            mc.getNetHandler().handleChangeGameState((S2BPacketChangeGameState) packet);
        } else if (packet instanceof S2CPacketSpawnGlobalEntity) {
            mc.getNetHandler().handleSpawnGlobalEntity((S2CPacketSpawnGlobalEntity) packet);
        } else if (packet instanceof S2DPacketOpenWindow) {
            mc.getNetHandler().handleOpenWindow((S2DPacketOpenWindow) packet);
        } else if (packet instanceof S2EPacketCloseWindow) {
            mc.getNetHandler().handleCloseWindow((S2EPacketCloseWindow) packet);
        } else if (packet instanceof S2FPacketSetSlot) {
            mc.getNetHandler().handleSetSlot((S2FPacketSetSlot) packet);
        } else if (packet instanceof S30PacketWindowItems) {
            mc.getNetHandler().handleWindowItems((S30PacketWindowItems) packet);
        } else if (packet instanceof S31PacketWindowProperty) {
            mc.getNetHandler().handleWindowProperty((S31PacketWindowProperty) packet);
        } else if (packet instanceof S32PacketConfirmTransaction) {
            mc.getNetHandler().handleConfirmTransaction((S32PacketConfirmTransaction) packet);
        } else if (packet instanceof S33PacketUpdateSign) {
            mc.getNetHandler().handleUpdateSign((S33PacketUpdateSign) packet);
        } else if (packet instanceof S34PacketMaps) {
            mc.getNetHandler().handleMaps((S34PacketMaps) packet);
        } else if (packet instanceof S35PacketUpdateTileEntity) {
            mc.getNetHandler().handleUpdateTileEntity((S35PacketUpdateTileEntity) packet);
        } else if (packet instanceof S36PacketSignEditorOpen) {
            mc.getNetHandler().handleSignEditorOpen((S36PacketSignEditorOpen) packet);
        } else if (packet instanceof S37PacketStatistics) {
            mc.getNetHandler().handleStatistics((S37PacketStatistics) packet);
        } else if (packet instanceof S38PacketPlayerListItem) {
            mc.getNetHandler().handlePlayerListItem((S38PacketPlayerListItem) packet);
        } else if (packet instanceof S39PacketPlayerAbilities) {
            mc.getNetHandler().handlePlayerAbilities((S39PacketPlayerAbilities) packet);
        } else if (packet instanceof S3APacketTabComplete) {
            mc.getNetHandler().handleTabComplete((S3APacketTabComplete) packet);
        } else if (packet instanceof S3BPacketScoreboardObjective) {
            mc.getNetHandler().handleScoreboardObjective((S3BPacketScoreboardObjective) packet);
        } else if (packet instanceof S3CPacketUpdateScore) {
            mc.getNetHandler().handleUpdateScore((S3CPacketUpdateScore) packet);
        } else if (packet instanceof S3DPacketDisplayScoreboard) {
            mc.getNetHandler().handleDisplayScoreboard((S3DPacketDisplayScoreboard) packet);
        } else if (packet instanceof S3EPacketTeams) {
            mc.getNetHandler().handleTeams((S3EPacketTeams) packet);
        } else if (packet instanceof S3FPacketCustomPayload) {
            mc.getNetHandler().handleCustomPayload((S3FPacketCustomPayload) packet);
        } else if (packet instanceof S40PacketDisconnect) {
            mc.getNetHandler().handleDisconnect((S40PacketDisconnect) packet);
        } else if (packet instanceof S41PacketServerDifficulty) {
            mc.getNetHandler().handleServerDifficulty((S41PacketServerDifficulty) packet);
        } else if (packet instanceof S42PacketCombatEvent) {
            mc.getNetHandler().handleCombatEvent((S42PacketCombatEvent) packet);
        } else if (packet instanceof S43PacketCamera) {
            mc.getNetHandler().handleCamera((S43PacketCamera) packet);
        } else if (packet instanceof S44PacketWorldBorder) {
            mc.getNetHandler().handleWorldBorder((S44PacketWorldBorder) packet);
        } else if (packet instanceof S45PacketTitle) {
            mc.getNetHandler().handleTitle((S45PacketTitle) packet);
        } else if (packet instanceof S46PacketSetCompressionLevel) {
            mc.getNetHandler().handleSetCompressionLevel((S46PacketSetCompressionLevel) packet);
        } else if (packet instanceof S47PacketPlayerListHeaderFooter) {
            mc.getNetHandler().handlePlayerListHeaderFooter((S47PacketPlayerListHeaderFooter) packet);
        } else if (packet instanceof S48PacketResourcePackSend) {
            mc.getNetHandler().handleResourcePack((S48PacketResourcePackSend) packet);
        } else if (packet instanceof S49PacketUpdateEntityNBT) {
            mc.getNetHandler().handleEntityNBT((S49PacketUpdateEntityNBT) packet);
        } else {
            throw new IllegalArgumentException("Unable to match packet type to handle: " + packet.getClass());
        }
    }
}
