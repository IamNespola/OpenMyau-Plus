package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty matrixBot = new BooleanProperty("MatrixBot", false);
    public final BooleanProperty tab = new BooleanProperty("Tab", true);
    public final ModeProperty tabMode = new ModeProperty("TabMode", 1, new String[]{"Equals", "Contains"}, tab::getValue);
    public final BooleanProperty entityID = new BooleanProperty("EntityID", true);
    public final BooleanProperty color = new BooleanProperty("Color", false);
    public final BooleanProperty livingTime = new BooleanProperty("LivingTime", false);
    public final IntProperty livingTimeTicks = new IntProperty("LivingTimeTicks", 40, 1, 200, livingTime::getValue);
    public final BooleanProperty ground = new BooleanProperty("Ground", true);
    public final BooleanProperty air = new BooleanProperty("Air", false);
    public final BooleanProperty invalidGround = new BooleanProperty("InvalidGround", true);
    public final BooleanProperty swing = new BooleanProperty("Swing", false);
    public final BooleanProperty health = new BooleanProperty("Health", false);
    public final FloatProperty minHealth = new FloatProperty("MinHealth", 0.0F, 0.0F, 100.0F, health::getValue);
    public final FloatProperty maxHealth = new FloatProperty("MaxHealth", 20.0F, 0.0F, 100.0F, health::getValue);
    public final BooleanProperty derp = new BooleanProperty("Derp", true);
    public final BooleanProperty wasInvisible = new BooleanProperty("WasInvisible", false);
    public final BooleanProperty armor = new BooleanProperty("Armor", false);
    public final BooleanProperty ping = new BooleanProperty("Ping", false);
    public final BooleanProperty needHit = new BooleanProperty("NeedHit", false);
    public final BooleanProperty spawnInCombat = new BooleanProperty("SpawnInCombat", false);
    public final BooleanProperty duplicateInWorld = new BooleanProperty("DuplicateInWorld", false);
    public final BooleanProperty duplicateInTab = new BooleanProperty("DuplicateInTab", false);
    public final ModeProperty duplicateCompareMode = new ModeProperty(
            "DuplicateCompareMode",
            0,
            new String[]{"OnTime", "WhenSpawn"},
            () -> duplicateInTab.getValue() || duplicateInWorld.getValue()
    );
    public final BooleanProperty experimentalNPCDetection = new BooleanProperty("ExperimentalNPCDetection", false);
    public final BooleanProperty illegalName = new BooleanProperty("IllegalName", false);
    public final BooleanProperty removeFromWorld = new BooleanProperty("RemoveFromWorld", false);
    public final IntProperty removeInterval = new IntProperty("Remove-Interval", 20, 1, 100, removeFromWorld::getValue);
    public final BooleanProperty debug = new BooleanProperty("Debug", false);

    private final Set<Integer> touchedGround = new HashSet<>();
    private final Set<Integer> touchedAir = new HashSet<>();
    private final Map<Integer, Integer> invalidGroundVL = new HashMap<>();
    private final Set<Integer> swung = new HashSet<>();
    private final Set<Integer> invisible = new HashSet<>();
    private final Set<Integer> hasRemovedEntities = new HashSet<>();
    private final Set<Integer> spawnedInCombat = new HashSet<>();
    private final Set<Integer> hit = new HashSet<>();
    private final Set<UUID> duplicate = new HashSet<>();
    private final List<EntityPlayer> matrixPlayerList = new ArrayList<>();
    private final List<EntityPlayer> matrixNotAlwaysInRadius = new ArrayList<>();
    private final Map<EntityPlayer, Double> matrixX = new HashMap<>();
    private final Map<EntityPlayer, Double> matrixZ = new HashMap<>();
    private boolean matrixNext;

    public AntiBot() {
        super("AntiBot", true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        handleMatrixBot();

        if (!removeFromWorld.getValue() || mc.thePlayer.ticksExisted <= 0 || mc.thePlayer.ticksExisted % removeInterval.getValue() != 0) return;

        List<EntityPlayer> bots = new ArrayList<>();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer && isBot(player)) bots.add(player);
        }
        if (bots.isEmpty()) return;

        for (EntityPlayer bot : bots) {
            mc.theWorld.removeEntity(bot);
            if (debug.getValue()) {
                ChatUtil.sendFormatted(String.format("%sAntiBot: &fRemoved &r%s &fdue to it being a bot.", Myau.clientName, bot.getName()));
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getType() != EventType.RECEIVE || mc.thePlayer == null || mc.theWorld == null) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof S14PacketEntity) {
            handleEntityPacket((S14PacketEntity) packet);
        } else if (packet instanceof S0BPacketAnimation) {
            handleAnimationPacket((S0BPacketAnimation) packet);
        } else if (packet instanceof S38PacketPlayerListItem) {
            handlePlayerListPacket((S38PacketPlayerListItem) packet);
        } else if (packet instanceof S0CPacketSpawnPlayer) {
            handleSpawnPlayerPacket((S0CPacketSpawnPlayer) packet);
        } else if (packet instanceof S13PacketDestroyEntities) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                hasRemovedEntities.add(id);
            }
        }
    }

    private void handleMatrixBot() {
        if (!matrixBot.getValue()) return;

        if (matrixNotAlwaysInRadius.size() > 1000) matrixNotAlwaysInRadius.clear();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer
                    && (mc.thePlayer.getDistanceToEntity(entity) > 10.0F || !within(entity.posY, mc.thePlayer.posY - 1.5, mc.thePlayer.posY + 1.5))
                    && !matrixNotAlwaysInRadius.contains(entity)) {
                matrixNotAlwaysInRadius.add((EntityPlayer) entity);
            }
        }

        if (!matrixNext) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityPlayer) || matrixNotAlwaysInRadius.contains(entity)) continue;

                EntityPlayer player = (EntityPlayer) entity;
                matrixPlayerList.add(player);
                matrixX.put(player, player.posX);
                matrixZ.put(player, player.posZ);
            }
        } else {
            for (EntityPlayer player : matrixPlayerList) {
                Double lastX = matrixX.get(player);
                Double lastZ = matrixZ.get(player);
                if (lastX == null || lastZ == null) continue;

                double xDiff = player.posX - lastX;
                double zDiff = player.posZ - lastZ;
                double speed = Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 10.0;

                if (isMatrixBot(player, speed)) {
                    mc.theWorld.removeEntity(player);
                    if (debug.getValue()) {
                        ChatUtil.sendFormatted(String.format("%sAntiBot/MatrixBot: &fRemoved &r%s&f.", Myau.clientName, player.getName()));
                    }
                }
            }

            matrixPlayerList.clear();
            matrixX.clear();
            matrixZ.clear();
        }

        matrixNext = !matrixNext;
    }

    private boolean isMatrixBot(EntityPlayer player, double speed) {
        return player != mc.thePlayer
                && speed > 6.75
                && speed < 27.5
                && mc.thePlayer.getDistanceToEntity(player) <= 5.5F
                && within(player.posY, mc.thePlayer.posY - 1.5, mc.thePlayer.posY + 1.5);
    }

    private boolean within(double value, double min, double max) {
        return value >= min && value <= max;
    }

    private void handleEntityPacket(S14PacketEntity packet) {
        Entity entity = packet.getEntity(mc.theWorld);
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        int id = player.getEntityId();
        if (packet.func_149060_h()) touchedGround.add(id);
        else touchedAir.add(id);

        if (packet.func_149060_h()) {
            if (player.prevPosY != player.posY) invalidGroundVL.put(id, invalidGroundVL.getOrDefault(id, 0) + 1);
        } else {
            int vl = invalidGroundVL.getOrDefault(id, 0) / 2;
            if (vl <= 0) invalidGroundVL.remove(id);
            else invalidGroundVL.put(id, vl);
        }

        if (player.isInvisible()) invisible.add(id);
    }

    private void handleAnimationPacket(S0BPacketAnimation packet) {
        Entity entity = mc.theWorld.getEntityByID(packet.getEntityID());
        if (entity instanceof EntityLivingBase && packet.getAnimationType() == 0) {
            swung.add(entity.getEntityId());
        }
    }

    private void handlePlayerListPacket(S38PacketPlayerListItem packet) {
        if (duplicateCompareMode.getValue() != 1 || packet.getAction() != S38PacketPlayerListItem.Action.ADD_PLAYER) return;

        for (S38PacketPlayerListItem.AddPlayerData entry : packet.getEntries()) {
            if (entry.getProfile() == null) continue;
            String name = entry.getProfile().getName();
            boolean duplicateWorld = duplicateInWorld.getValue() && mc.theWorld.playerEntities.stream().anyMatch(player -> player.getName().equals(name));
            boolean duplicateTab = duplicateInTab.getValue() && mc.getNetHandler().getPlayerInfoMap().stream().anyMatch(info -> info.getGameProfile() != null && info.getGameProfile().getName().equals(name));
            if (duplicateWorld || duplicateTab) duplicate.add(entry.getProfile().getId());
        }
    }

    private void handleSpawnPlayerPacket(S0CPacketSpawnPlayer packet) {
        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.target != null && !hasRemovedEntities.contains(packet.getEntityID())) {
            spawnedInCombat.add(packet.getEntityID());
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity target = event.getTarget();
        if (target instanceof EntityLivingBase) hit.add(target.getEntityId());
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clearAll();
    }

    private void clearAll() {
        hit.clear();
        swung.clear();
        touchedGround.clear();
        touchedAir.clear();
        invalidGroundVL.clear();
        invisible.clear();
        hasRemovedEntities.clear();
        spawnedInCombat.clear();
        duplicate.clear();
        matrixPlayerList.clear();
        matrixNotAlwaysInRadius.clear();
        matrixX.clear();
        matrixZ.clear();
        matrixNext = false;
    }

    @Override
    public void onDisabled() {
        clearAll();
    }

    public boolean isBotPlayer(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) return false;
        if (!isEnabled()) return false;

        EntityPlayer player = (EntityPlayer) entity;
        int id = player.getEntityId();

        if (experimentalNPCDetection.getValue()) {
            String display = strip(player.getDisplayName().getUnformattedText()).toLowerCase(Locale.ROOT);
            if (display.contains("npc") || display.contains("cit-")) return true;
        }
        if (illegalName.getValue() && (player.getName().contains(" ") || player.getDisplayName().getUnformattedText().contains(" "))) return true;
        if (color.getValue() && !player.getDisplayName().getFormattedText().replace("§r", "").contains("§")) return true;
        if (livingTime.getValue() && player.ticksExisted < livingTimeTicks.getValue()) return true;
        if (ground.getValue() && !touchedGround.contains(id)) return true;
        if (air.getValue() && !touchedAir.contains(id)) return true;
        if (spawnInCombat.getValue() && spawnedInCombat.contains(id)) return true;
        if (swing.getValue() && !swung.contains(id)) return true;
        if (health.getValue() && (player.getHealth() > maxHealth.getValue() || player.getHealth() < minHealth.getValue())) return true;
        if (entityID.getValue() && (id >= 1000000000 || id <= -1)) return true;
        if (derp.getValue() && (player.rotationPitch > 90.0F || player.rotationPitch < -90.0F)) return true;
        if (wasInvisible.getValue() && invisible.contains(id)) return true;
        if (armor.getValue() && hasNoArmor(player)) return true;
        if (ping.getValue()) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null && info.getResponseTime() == 0) return true;
        }
        if (needHit.getValue() && !hit.contains(id)) return true;
        if (invalidGround.getValue() && invalidGroundVL.getOrDefault(id, 0) >= 10) return true;
        if (tab.getValue() && !isInTab(player)) return true;
        if (duplicateCompareMode.getValue() == 1 && duplicate.contains(player.getGameProfile().getId())) return true;
        if (duplicateInWorld.getValue() && duplicateCompareMode.getValue() == 0 && hasDuplicateInWorld(player)) return true;
        if (duplicateInTab.getValue() && duplicateCompareMode.getValue() == 0 && hasDuplicateInTab(player)) return true;

        return player.getName().isEmpty() || player.getName().equals(mc.thePlayer.getName());
    }

    private boolean hasNoArmor(EntityPlayer player) {
        return player.inventory.armorInventory[0] == null
                && player.inventory.armorInventory[1] == null
                && player.inventory.armorInventory[2] == null
                && player.inventory.armorInventory[3] == null;
    }

    private boolean isInTab(EntityPlayer player) {
        if (mc.getNetHandler() == null) return false;
        boolean equals = tabMode.getValue() == 0;
        String targetName = strip(player.getDisplayName().getFormattedText());
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            String networkName = strip(getNetworkName(info));
            if (equals ? targetName.equalsIgnoreCase(networkName) : targetName.contains(networkName)) return true;
        }
        return false;
    }

    private String getNetworkName(NetworkPlayerInfo info) {
        if (info == null) return "";
        if (info.getDisplayName() != null) return info.getDisplayName().getFormattedText();
        return info.getGameProfile() == null ? "" : info.getGameProfile().getName();
    }

    private boolean hasDuplicateInWorld(EntityPlayer player) {
        String name = player.getName();
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer && name.equals(((EntityPlayer) entity).getName()))
                .count() > 1;
    }

    private boolean hasDuplicateInTab(EntityPlayer player) {
        if (mc.getNetHandler() == null) return false;
        String name = player.getName();
        return mc.getNetHandler().getPlayerInfoMap().stream()
                .filter(info -> info.getGameProfile() != null && name.equals(info.getGameProfile().getName()))
                .count() > 1;
    }

    private static String strip(String text) {
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        return stripped == null ? "" : stripped;
    }

    public static boolean isBot(EntityLivingBase entity) {
        AntiBot antiBot = (AntiBot) Myau.moduleManager.getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && antiBot.isBotPlayer(entity);
    }
}
