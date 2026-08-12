package keystrokesmod.script;

import com.mojang.authlib.GameProfile;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.mixin.impl.accessor.*;
import keystrokesmod.mixin.impl.invoker.IInvokerNetworkManager;
import keystrokesmod.module.Module;
import keystrokesmod.module.Module.category;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.*;
import keystrokesmod.script.classes.*;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.script.packets.clientbound.SPacket;
import keystrokesmod.script.packets.serverbound.CPacket;
import keystrokesmod.script.packets.serverbound.PacketHandler;
import keystrokesmod.utility.*;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.event.ForgeEventFactory;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class ScriptDefaults {
    private static ExecutorService cachedExecutor;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final Bridge bridge = new Bridge();
    private static final LinkedHashMap<String, Module> modulesMap = new LinkedHashMap<>();

    public static void reloadModules() {
        modulesMap.clear();
        for (Module module : Raven.getModuleManager().getModules()) {
            modulesMap.put(module.getName(), module);
        }
        for (Module module : Raven.scriptManager.scripts.values()) {
            modulesMap.put(module.getName(), module);
        }
    }

    public static class client {
        public static boolean allowFlying() {
            return mc.thePlayer.capabilities.allowFlying;
        }

        public static void removePotionEffect(int id) {
            if (mc.thePlayer == null) {
                return;
            }
            mc.thePlayer.removePotionEffectClient(id);
        }

        public static int getUID() {
            return 4;
        }

        public static String getUser() {
            return "mic";
        }

        public static void addEnemy(String username) {
            Utils.addEnemy(username);
        }

        public static void addFriend(String username) {
            Utils.addFriend(username);
        }

        public static void async(final Runnable method) {
            if (cachedExecutor == null) {
                cachedExecutor = Executors.newCachedThreadPool();
            }
            cachedExecutor.execute(method);
        }

        public static int getFPS() {
            return Minecraft.getDebugFPS();
        }

        public static void chat(String message) {
            mc.thePlayer.sendChatMessage(message);
        }

        public static void print(String string) {
            Utils.sendRawMessage(string);
        }

        public static void print(Object object) {
            String s = String.valueOf(object);
            Utils.sendRawMessage(s);
        }

        public static void print(Message component) {
            mc.thePlayer.addChatMessage(component.component);
        }

        public static boolean isDiagonal() {
            return Utils.isDiagonal(false);
        }

        public static void setTimer(float timer) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = timer;
        }

        public static boolean isCreative() {
            return mc.thePlayer.capabilities.isCreativeMode;
        }

        public static void processPacket(SPacket packet) {
            packet.packet.processPacket(mc.getNetHandler());
        }

        public static void processPacketNoEvent(SPacket packet) {
            PacketUtils.receivePacketNoEvent(packet.packet);
        }

        public static String getTitle() {
            return ((IAccessorGuiIngame) mc.ingameGUI).getDisplayedTitle();
        }

        public static String getSubTitle() {
            return ((IAccessorGuiIngame) mc.ingameGUI).getDisplayedSubTitle();
        }

        public static String getRecordPlaying() {
            return ((IAccessorGuiIngame) mc.ingameGUI).getRecordPlaying();
        }

        public static boolean isFlying() {
            return mc.thePlayer.capabilities.isFlying;
        }

        public static void attack(Entity entity) {
            Utils.attackEntity(entity.entity, true, true);
        }

        public static boolean isSinglePlayer() {
            return mc.isSingleplayer();
        }

        public static boolean isSpectator() {
            return mc.thePlayer.isSpectator();
        }

        public static void setFlying(boolean flying) {
            mc.thePlayer.capabilities.isFlying = flying;
        }

        public static void setJump(boolean jump) {
            mc.thePlayer.movementInput.jump = jump;
        }

        public static void setJumping(boolean jump) {
            mc.thePlayer.setJumping(jump);
        }

        public static void setRenderArmPitch(float pitch) {
            mc.thePlayer.prevRenderArmPitch = pitch;
            mc.thePlayer.renderArmPitch = pitch;
        }

        public static void disconnect() {
            boolean isLocal = mc.isIntegratedServerRunning();
            boolean isRealms = mc.isConnectedToRealms();
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
            if (isLocal) {
                mc.displayGuiScreen(new GuiMainMenu());
                return;
            }
            if (isRealms) {
                new RealmsBridge().switchToRealms(new GuiMainMenu());
                return;
            }
            mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
        }

        public static float getRenderArmPitch() {
            return mc.thePlayer.renderArmPitch;
        }

        public static void setRenderArmYaw(float yaw) {
            mc.thePlayer.renderArmYaw = yaw;
        }

        public static float getRenderArmYaw() {
            return mc.thePlayer.renderArmYaw;
        }

        public static long getTotalMemory() {
            return Runtime.getRuntime().totalMemory();
        }

        public static long getFreeMemory() {
            return Runtime.getRuntime().freeMemory();
        }

        public static long getMaxMemory() {
            return Runtime.getRuntime().maxMemory();
        }

        public static List<String[]> getResourcePacks() {
            List<String[]> packs = new ArrayList<>();
            if (mc.getResourcePackRepository().getRepositoryEntries() == null || mc.getResourcePackRepository().getRepositoryEntries().isEmpty()) {
                packs.add(new String[]{mc.mcDefaultResourcePack.getPackName(), ""});
            } else {
                for (ResourcePackRepository.Entry entry : mc.getResourcePackRepository().getRepositoryEntries()) {
                    packs.add(new String[]{entry.getResourcePackName(), entry.getTexturePackDescription()});
                }
            }
            Collections.reverse(packs); // reverse it to match the correct order
            return packs;
        }

        public static void jump() {
            mc.thePlayer.jump();
        }

        public static boolean allowEditing() {
            if (mc.thePlayer == null || mc.thePlayer.capabilities == null) {
                return false;
            }
            return mc.thePlayer.capabilities.allowEdit;
        }

        public static void setItemInUseCount(int count) {
            ((IAccessorEntityPlayer) mc.thePlayer).setItemInUseCount(count);
        }

        public static int getItemInUseCount() {
            return mc.thePlayer.getItemInUseCount();
        }

        public static int getItemInUseDuration() {
            return mc.thePlayer.getItemInUseDuration();
        }

        public static void log(final Object obj) {
            Utils.log.info(obj);
        }

        public static void setSneaking(boolean sneak) {
            mc.thePlayer.setSneaking(sneak);
        }

        public static void setSneak(boolean sneak) {
            mc.thePlayer.movementInput.sneak = sneak;
        }

        public static boolean isSneak() {
            return mc.thePlayer.movementInput.sneak;
        }

        public static Entity getPlayer() {
            if (mc == null || mc.thePlayer == null) {
                return null;
            }
            return Entity.convert(mc.thePlayer);
        }

        public static void removeEnemy(String username) {
            Utils.removeEnemy(username);
        }

        public static void removeFriend(String username) {
            Utils.removeFriend(username);
        }

        public static boolean isRiding() {
            return mc.thePlayer.isRiding();
        }

        public static Vec3 getMotion() {
            return new Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
        }

        public static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }

        public static void ping() {
            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
        }

        public static void playSound(String name, float volume, float pitch) {
            mc.thePlayer.playSound(name, volume, pitch);
        }

        public static boolean isMoving() {
            return Utils.isMoving();
        }

        public static boolean isJump() {
            return mc.thePlayer.movementInput.jump;
        }

        public static float getStrafe() {
            return mc.thePlayer.movementInput.moveStrafe;
        }

        public static void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static String getTabHeader() {
            if (mc == null || mc.ingameGUI == null || mc.ingameGUI.getTabList() == null) {
                return "";
            }
            IChatComponent header = ((IAccessorGuiPlayerTabOverlay) mc.ingameGUI.getTabList()).getHeader();
            if (header != null) {
                return header.getUnformattedText();
            }
            return "";
        }

        public static String getTabFooter() {
            if (mc == null || mc.ingameGUI == null || mc.ingameGUI.getTabList() == null) {
                return "";
            }
            IChatComponent footer = ((IAccessorGuiPlayerTabOverlay) mc.ingameGUI.getTabList()).getFooter();
            if (footer != null) {
                return footer.getUnformattedText();
            }
            return "";
        }

        public static float getForward() {
            return mc.thePlayer.movementInput.moveForward;
        }

        public static void closeScreen() {
            if (mc.currentScreen instanceof ClickGui) {
                mc.displayGuiScreen(null);
                return;
            }
            mc.thePlayer.closeScreen();
        }

        public static String getScreen() {
            return mc.currentScreen == null ? "" : mc.currentScreen.getClass().getSimpleName();
        }

        public static float[] getRotationsToEntity(Entity entity) {
            return RotationUtils.getRotations(entity.entity);
        }

        public static void sendPacket(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 == null) {
                return;
            }
            mc.thePlayer.sendQueue.addToSendQueue(packet1);
        }

        public static void sendPacketNoEvent(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 == null) {
                return;
            }
            PacketUtils.sendPacketNoEvent(packet1);
        }

        public static boolean inFocus() {
            return mc.inGameHasFocus;
        }

        public static void dropItem(boolean dropStack) {
            mc.thePlayer.dropOneItem(dropStack);
        }

        public static void setMotion(double x, double y, double z) {
            mc.thePlayer.motionX = x;
            mc.thePlayer.motionY = y;
            mc.thePlayer.motionZ = z;
        }

        public static double getJumpHeight() {
            double motionY = 0.42f;
            final PotionEffect effect;
            if ((effect = mc.thePlayer.getActivePotionEffect(Potion.jump)) != null)
                motionY += (float)(effect.getAmplifier() + 1) * 0.1F;
            return motionY;
        }

        public static void setSpeed(double speed) {
            Utils.setSpeed(speed);
        }

        public static void setForward(float forward) {
            mc.thePlayer.movementInput.moveForward = forward;
        }

        public static void setStrafe(float strafe) {
            mc.thePlayer.movementInput.moveStrafe = strafe;
        }

        public static String getServerIP() {
            if (mc.getCurrentServerData() == null || mc.isSingleplayer()) {
                return "";
            }
            return mc.getCurrentServerData().serverIP;
        }

        public static int[] getDisplaySize() {
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            return new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        }

        public static float getServerDirection(PlayerState state) {
            return state.yaw;
        }

        public static Object[] raycastBlock(final double distance) {
            return raycastBlock(distance, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        public static Object[] raycastBlock(final double distance, final float yaw, final float pitch) {
            final net.minecraft.util.Vec3 eyeVec = mc.thePlayer.getPositionEyes(1.0f);
            final net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
            final net.minecraft.util.Vec3 sumVec = eyeVec.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
            final MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(eyeVec, sumVec, false, false, false);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return null;
            }
            final Vec3 pos = new Vec3(mop.getBlockPos());
            final Vec3 offset = new Vec3(mop.hitVec.xCoord - pos.x, mop.hitVec.yCoord - pos.y, mop.hitVec.zCoord - pos.z);
            return new Object[]{pos, offset, mop.sideHit.name()};
        }

        public static Object[] raycastEntity(final double distance) {
            return raycastEntity(distance, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        public static Object[] raycastEntity(final double distance, final float yaw, final float pitch) {
            net.minecraft.entity.Entity pointedEntity = null;
            MovingObjectPosition mop = mc.thePlayer.rayTrace(distance, 1.0f);
            final net.minecraft.util.Vec3 eyeVec = mc.thePlayer.getPositionEyes(1.0f);
            final net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
            final net.minecraft.util.Vec3 vec32 = eyeVec.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
            net.minecraft.util.Vec3 vec33 = null;
            final List list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), mc.getRenderViewEntity().getEntityBoundingBox().addCoord(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance).expand(1.0, 1.0, 1.0));
            double d2 = distance;
            for (int i = 0; i < list.size(); ++i) {
                final net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) list.get(i);
                if (entity instanceof EntityLivingBase && entity.canBeCollidedWith()) {
                    if (((EntityLivingBase) entity).deathTime == 0) {
                        final float cbs = entity.getCollisionBorderSize();
                        final AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand((double) cbs, (double) cbs, (double) cbs);
                        final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(eyeVec, vec32);
                        if (axisalignedbb.isVecInside(eyeVec)) {
                            if (0.0 < d2 || d2 == 0.0) {
                                pointedEntity = entity;
                                vec33 = ((movingobjectposition == null) ? eyeVec : movingobjectposition.hitVec);
                                d2 = 0.0;
                            }
                        } else if (movingobjectposition != null) {
                            final double d3 = eyeVec.distanceTo(movingobjectposition.hitVec);
                            if (d3 < d2 || d2 == 0.0) {
                                if (entity == mc.getRenderViewEntity().ridingEntity && !entity.canRiderInteract()) {
                                    if (d2 == 0.0) {
                                        pointedEntity = entity;
                                        vec33 = movingobjectposition.hitVec;
                                    }
                                } else {
                                    pointedEntity = entity;
                                    vec33 = movingobjectposition.hitVec;
                                    d2 = d3;
                                }
                            }
                        }
                    }
                }
            }
            if (pointedEntity != null && (d2 < distance || mop == null)) {
                mop = new MovingObjectPosition(pointedEntity, vec33);
                final Vec3 offset = new Vec3(mop.hitVec.xCoord - pointedEntity.posX, mop.hitVec.yCoord - pointedEntity.posY, mop.hitVec.zCoord - pointedEntity.posZ);
                return new Object[]{new Entity(mop.entityHit), offset, eyeVec.squareDistanceTo(mop.hitVec)};
            }
            return null;
        }

        public static boolean placeBlock(Vec3 targetPos, String side, Vec3 hitVec) {
            return mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), Vec3.getBlockPos(targetPos), Utils.getEnum(EnumFacing.class, side), Vec3.getVec3(hitVec));
        }

        public static float[] getRotationsToBlock(Vec3 position) {
            return RotationUtils.getRotations(new BlockPos(position.x, position.y, position.z));
        }

        public static void setSprinting(boolean sprinting) {
            mc.thePlayer.setSprinting(sprinting);
        }

        public static void swing() {
            mc.thePlayer.swingItem();
        }

        public static long time() {
            return System.currentTimeMillis();
        }

        public static boolean isFriend(String username) {
            return Utils.isFriended(username);
        }

        public static boolean isEnemy(String username) {
            return Utils.isEnemy(username);
        }
    }

    public static class world {
        public static boolean hasWorld() {
            return mc.theWorld != null;
        }

        public static void tickEntity(Entity entity) {
            final net.minecraft.entity.Entity entityIn = entity.entity;

            int i = MathHelper.floor_double(entityIn.posX);
            int j = MathHelper.floor_double(entityIn.posZ);
            boolean isForced = mc.theWorld.getPersistentChunks().containsKey(new ChunkCoordIntPair(i >> 4, j >> 4));
            int k = isForced ? 0 : 32;
            /*boolean canUpdate = mc.theWorld.isAreaLoaded(new BlockPos(i - k, 0, j - k), new BlockPos(i + k, 0, j + k), true);
            if (!canUpdate) {
                canUpdate = ForgeEventFactory.canEntityUpdate(entityIn);
            }*/

            boolean canUpdate = true;

            entityIn.lastTickPosX = entityIn.posX;
            entityIn.lastTickPosY = entityIn.posY;
            entityIn.lastTickPosZ = entityIn.posZ;
            entityIn.prevRotationYaw = entityIn.rotationYaw;
            entityIn.prevRotationPitch = entityIn.rotationPitch;
            entityIn.ticksExisted++;
            if (entityIn.ridingEntity != null) {
                entityIn.updateRidden();
            } else {
                entityIn.onUpdate();
            }

            if (Double.isNaN(entityIn.posX) || Double.isInfinite(entityIn.posX)) {
                entityIn.posX = entityIn.lastTickPosX;
            }

            if (Double.isNaN(entityIn.posY) || Double.isInfinite(entityIn.posY)) {
                entityIn.posY = entityIn.lastTickPosY;
            }

            if (Double.isNaN(entityIn.posZ) || Double.isInfinite(entityIn.posZ)) {
                entityIn.posZ = entityIn.lastTickPosZ;
            }

            if (Double.isNaN((double)entityIn.rotationPitch) || Double.isInfinite((double)entityIn.rotationPitch)) {
                entityIn.rotationPitch = entityIn.prevRotationPitch;
            }

            if (Double.isNaN((double)entityIn.rotationYaw) || Double.isInfinite((double)entityIn.rotationYaw)) {
                entityIn.rotationYaw = entityIn.prevRotationYaw;
            }
        }

        public static Block getBlockAt(int x, int y, int z) {
            IBlockState state = BlockUtils.getBlockState(new BlockPos(x, y, z));
            if (state == null) {
                return new Block(Blocks.air, new BlockPos(x, y, z));
            }
            return new Block(state, new BlockPos(x, y, z));
        }

        public static Block getBlockAt(Vec3 pos) {
            IBlockState state = BlockUtils.getBlockState(new BlockPos(pos.x, pos.y, pos.z));
            if (state == null) {
                return new Block(Blocks.air, new BlockPos(pos.x, pos.y, pos.z));
            }
            return new Block(state, new BlockPos(pos.x, pos.y, pos.z));
        }

        public static boolean isAir(int x, int y, int z) {
            return mc.theWorld.isAirBlock(new BlockPos(x, y, z));
        }

        public static boolean isAir(double x, double y, double z) {
            return mc.theWorld.isAirBlock(new BlockPos(x, y, z));
        }

        public static String getDimension() {
            if (mc.theWorld == null) {
                return "";
            }
            return mc.theWorld.provider.getDimensionName();
        }

        public static List<Entity> getEntities() {
            List<Entity> entities = new ArrayList<>();
            if (mc.theWorld == null) {
                return entities;
            }
            for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
                entities.add(Entity.convert(entity));
            }
            return entities;
        }

        public static Entity getEntityById(int entityId) {
            if (mc.theWorld == null) {
                return null;
            }
            return Entity.convert(mc.theWorld.getEntityByID(entityId));
        }

        public static List<NetworkPlayer> getNetworkPlayers() {
            List<NetworkPlayer> entities = new ArrayList<>();
            for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(false)) {
                entities.add(NetworkPlayer.convert(networkPlayerInfo));
            }
            return entities;
        }

        public static List<Entity> getPlayerEntities() {
            List<Entity> entities = new ArrayList<>();
            for (net.minecraft.entity.Entity entity : mc.theWorld.playerEntities) {
                entities.add(Entity.convert(entity));
            }
            return entities;
        }

        public static List<String> getScoreboard() {
            List<String> sidebarLines = Utils.getSidebarLines();
            if (sidebarLines.isEmpty()) {
                return null;
            }
            return sidebarLines;
        }

        public static Map<String, List<String>> getTeams() {
            Map<String, List<String>> teams = new HashMap<>();
            for (Team team : mc.theWorld.getScoreboard().getTeams()) {
                List<String> members = new ArrayList<>();
                for (String member : team.getMembershipCollection()) {
                    members.add(member);
                }
                teams.put(team.getRegisteredName(), members);
            }
            return teams;
        }

        public static List<TileEntity> getTileEntities() {
            List<TileEntity> tileEntities = new ArrayList<>();
            for (net.minecraft.tileentity.TileEntity entity : mc.theWorld.loadedTileEntityList) {
                tileEntities.add(new TileEntity(entity));
            }
            return tileEntities;
        }
    }

    public static class extra {

        public static boolean isDiagonal() {
            return Utils.isDiagonalFatVersion();
        }
    }

    public static class myau {

        public static List<String> runSilently(final String command) {
            try {
                Utils.cancelChat = true;
                mc.thePlayer.sendChatMessage(command);
                Utils.cancelChat = false;
                final List<String> msgs = new ArrayList<>(Utils.cancelledMsgs);
                Utils.cancelledMsgs.clear();
                return msgs;
            } finally {
                Utils.cancelChat = false;
            }
        }

        public static boolean isDiagonal() {
            return Utils.isDiagonalMyauCalc();
        }

        public static boolean isEnabled(final String name) {
            final List<String> result = runSilently(String.format(".%s", name));

            if (result.get(0).contains("Unknown command")) {
                return false;
            }

            final String value = result.get(0).split("\\(")[1].split("\\)")[0];

            switch (value) {
                case "ON":
                    return true;
                case "OFF":
                    return false;
                default:
                    Utils.sendMessage("Unknown value: " + value);
                    return false;
            }
        }

        public static void toggle(final String name) {
            runSilently(String.format(".t %s", name));
        }

        public static void setEnabled(final String name, final boolean state) {
            if (isEnabled(name) != state) {
                toggle(name);
            }
        }

        public static void setProperty(final String moduleName, final String propertyName, final Object value) {
            final String valueStr = String.valueOf(value);
            runSilently(String.format(".%s %s %s", moduleName, propertyName, valueStr));
        }

        public static String getProperty(final String moduleName, final String propertyName) {
            final List<String> result = runSilently(String.format(".%s", moduleName));

            if (result.get(0).contains("Unknown command")) {
                return null;
            }

            if (result.size() == 1) {
                return null;
            }

            final String searchTerm = propertyName.toLowerCase().replace("-", "");

            for (int i = 1; i < result.size(); i++) {
                if (!result.get(i).contains(":")) {
                    Utils.sendMessage("what the fuck? " + result.get(i));
                    continue;
                }

                final String[] lineTerm = result.get(i).substring(3).split(": ");

                if (lineTerm[0].equals(searchTerm)) {
                    return lineTerm[1];
                }
            }

            return null;
        }

    }

    public static class modules {
        private String superName;

        public modules(String superName) {
            this.superName = superName;
        }

        private static Module getModule(String moduleName) {
            return modulesMap.get(moduleName);
        }

        private static Module getScript(String name) {
            return modulesMap.get(name);
        }

        private static Setting getSetting(Module module, String settingName) {
            if (module == null) {
                return null;
            }
            for (Setting setting : module.getSettings()) {
                if (setting.getName().equals(settingName)) {
                    return setting;
                }
            }
            return null;
        }

        private GroupSetting getGroupForString(String group) {
            if (group.isEmpty()) {
                return null;
            }
            List<Setting> settings = getScript(this.superName).getSettings();
            for (Setting setting : settings) {
                if (!(setting instanceof GroupSetting)) {
                    continue;
                }
                if (setting.getName().equals(group)) {
                    return (GroupSetting) setting;
                }
            }
            return null;
        }

        public void enable(String moduleName) {
            if (getModule(moduleName) == null) {
                return;
            }
            getModule(moduleName).enable();
        }

        public void disable(String moduleName) {
            if (getModule(moduleName) == null) {
                return;
            }
            getModule(moduleName).disable();
        }

        public boolean isEnabled(String moduleName) {
            if (getModule(moduleName) == null) {
                return false;
            }
            return getModule(moduleName).isEnabled();
        }

        public Map<String, Object> getSettings(String name) {
            Map<String, Object> settings = new HashMap<>();
            Module module = getModule(name);
            if (module == null) {
                return settings;
            }
            for (Setting setting : module.getSettings()) {
                if (setting instanceof SliderSetting) {
                    settings.put(setting.getName(), ((SliderSetting) setting).getInput());
                } else if (setting instanceof ButtonSetting) {
                    settings.put(setting.getName(), ((ButtonSetting) setting).isToggled());
                }
            }
            return settings;
        }

        public Map<String, List<String>> getCategories() {
            Map<String, List<String>> categories = new HashMap<>();
            for (CategoryComponent categoryComponent : ClickGui.categories) {
                List<String> modules = new ArrayList<>();
                for (ModuleComponent module : categoryComponent.modules) {
                    modules.add(module.mod.getName());
                }
                categories.put(categoryComponent.category.name(), modules);
            }
            return categories;
        }

        public boolean isHidden(String moduleName) {
            Module module = getModule(moduleName);
            if (module != null) {
                return module.isHidden();
            }
            return false;
        }

        public void setCategory(category category) {
            getScript(this.superName).setCategory(category);
        }

        public void registerGroup(String name) {
            getScript(this.superName).registerSetting(new GroupSetting(name));
        }

        public void registerButton(String name, boolean defaultValue) {
            getScript(this.superName).registerSetting(new ButtonSetting(name, defaultValue));
        }

        public void registerButton(String group, String name, boolean defaultValue) {
            getScript(this.superName).registerSetting(new ButtonSetting(getGroupForString(group), name, defaultValue));
        }

        public void registerKey(String group, String name, int defaultKey) {
            getScript(this.superName).registerSetting(new KeySetting(getGroupForString(group), name, defaultKey));
        }

        public void registerKey(String name, int defaultKey) {
            getScript(this.superName).registerSetting(new KeySetting(name, defaultKey));
        }

        // main slider constructors

        public void registerSlider(String group, String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
            getScript(this.superName).registerSetting(new SliderSetting(getGroupForString(group), name, suffix, defaultValue, minimum, maximum, interval));
        }

        public void registerSlider(String group, String name, String suffix, int defaultValue, String[] stringArray) {
            getScript(this.superName).registerSetting(new SliderSetting(getGroupForString(group), name, suffix, defaultValue, stringArray));
        }

        // rest

        public void registerSlider(String name, double defaultValue, double minimum, double maximum, double interval) {
            this.registerSlider("", name, "", defaultValue, minimum, maximum, interval);
        }

        public void registerSlider(String name, int defaultValue, String[] stringArray) {
            this.registerSlider("", name, "", defaultValue, stringArray);
        }

        public void registerSlider(String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
            this.registerSlider("", name, suffix, defaultValue, minimum, maximum, interval);
        }

        public void registerSlider(String name, String suffix, int defaultValue, String[] stringArray) {
            this.registerSlider("", name, suffix, defaultValue, stringArray);
        }

        public void registerDescription(String description) {
            getScript(this.superName).registerSetting(new DescriptionSetting(description));
        }

        public boolean getButton(String moduleName, String name) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            if (setting == null) {
                return false;
            }
            boolean buttonState = setting.isToggled();
            return buttonState;
        }

        public double getSlider(String moduleName, String name) {
            SliderSetting setting = ((SliderSetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return 0;
            }
            double sliderValue = setting.getInput();
            return sliderValue;
        }

        public boolean getKeyPressed(String moduleName, String name) {
            KeySetting setting = ((KeySetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return false;
            }
            return setting.isPressed();
        }

        public void setButton(String moduleName, String name, boolean value) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            if (setting == null) {
                return;
            }
            setting.setEnabled(value);
        }

        public void setSlider(String moduleName, String name, double value) {
            SliderSetting setting = ((SliderSetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return;
            }
            setting.setValueRawWithEvent(value);
        }

        public void setKey(String moduleName, String name, int code) {
            KeySetting setting = ((KeySetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return;
            }
            setting.setKey(code);
        }
    }

    public static class gl {
        public static void alpha(boolean alpha) {
            if (alpha) {
                GlStateManager.enableAlpha();
            } else {
                GlStateManager.disableAlpha();
            }
        }

        public static void begin(int mode) {
            GL11.glBegin(mode);
        }

        public static void blend(boolean blend) {
            if (blend) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
        }

        public static void color(float r, float g, float b, float a) {
            GlStateManager.color(r, g, b, a);
        }

        public static void cull(boolean cull) {
            if (cull) {
                GlStateManager.enableCull();
            } else {
                GlStateManager.disableCull();
            }
        }

        public static void depth(boolean depth) {
            if (depth) {
                GlStateManager.enableDepth();
            } else {
                GlStateManager.disableDepth();
            }
        }

        public static void depthMask(boolean depthMask) {
            GlStateManager.depthMask(depthMask);
        }

        public static void disable(int cap) {
            GL11.glDisable(cap);
        }

        public static void disableItemLighting() {
            RenderHelper.disableStandardItemLighting();
        }

        public static void enable(int cap) {
            GL11.glEnable(cap);
        }

        public static void enableItemLighting(boolean gui) {
            if (gui) {
                RenderHelper.enableGUIStandardItemLighting();
            } else {
                RenderHelper.enableStandardItemLighting();
            }
        }

        public static void end() {
            GL11.glEnd();
        }

        public static void lighting(boolean lighting) {
            if (lighting) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }
        }

        public static void lineSmooth(boolean lineSmooth) {
            setGLEnable(GL11.GL_LINE_SMOOTH, lineSmooth);
        }

        public static void lineWidth(float width) {
            GL11.glLineWidth(width);
        }

        public static void normal(float x, float y, float z) {
            GL11.glNormal3f(x, y, z);
        }

        public static void pop() {
            GL11.glPopMatrix();
        }

        public static void push() {
            GL11.glPushMatrix();
        }

        public static void rotate(float angle, float x, float y, float z) {
            GL11.glRotatef(angle, x, y, z);
        }

        public static void scale(float x, float y, float z) {
            GL11.glScalef(x, y, z);
        }

        public static void scissor(int x, int y, int width, int height) {
            GL11.glScissor(x, y, width, height);
        }

        public static void scissor(boolean scissor) {
            setGLEnable(GL11.GL_SCISSOR_TEST, scissor);
        }

        public static void texture2d(boolean texture2d) {
            if (texture2d) {
                GlStateManager.enableTexture2D();
            } else {
                GlStateManager.disableTexture2D();
            }
        }

        public static void translate(float x, float y, float z) {
            GL11.glTranslatef(x, y, z);
        }

        public static void vertex2(float x, float y) {
            GL11.glVertex2f(x, y);
        }

        public static void vertex3(float x, float y, float z) {
            GL11.glVertex3f(x, y, z);
        }

        private static void setGLEnable(int cap, boolean enable) {
            if (enable) {
                GL11.glEnable(cap);
            } else {
                GL11.glDisable(cap);
            }
        }
    }

    public static class config {
        private static String CONFIG_DIR = mc.mcDataDir + File.separator + "keystrokes" + File.separator + "script_config.txt";
        private static String SEPARATOR = ":";
        private static String SEPARATOR_FULL = config.SEPARATOR + " ";

        private static void ensureConfigFileExists() throws IOException {
            final Path configPath = Paths.get(config.CONFIG_DIR);
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.createFile(configPath);
            }
        }

        public static boolean set(String key, final String value) {
            if (key == null || key.isEmpty()) {
                return false;
            }
            key = key.replace(config.SEPARATOR, "");
            final String entry = key + config.SEPARATOR_FULL + value;
            try {
                ensureConfigFileExists();
                final Path configPath = new File(config.CONFIG_DIR).toPath();
                final List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
                boolean keyExists = false;
                for (int i = 0; i < lines.size(); ++i) {
                    final String line = lines.get(i);
                    if (line.startsWith(key + config.SEPARATOR_FULL)) {
                        lines.set(i, entry);
                        keyExists = true;
                        break;
                    }
                }
                if (!keyExists) {
                    lines.add(entry);
                }
                Files.write(configPath, lines);
                return true;
            } catch (IOException ex) {
                return false;
            }
        }

        public static String get(final String key) {
            if (key == null || key.isEmpty()) {
                return null;
            }
            try {
                ensureConfigFileExists();
                final Path configPath = new File(config.CONFIG_DIR).toPath();
                final List<String> lines = Files.readAllLines(configPath);
                for (final String line : lines) {
                    if (line.startsWith(key + config.SEPARATOR_FULL)) {
                        return line.substring((key + config.SEPARATOR_FULL).length());
                    }
                }
            } catch (IOException ex) {
            }
            return null;
        }
    }

    public static class render {
        private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
        private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
        private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
        private static final FloatBuffer SCREEN_COORDS = GLAllocation.createDirectFloatBuffer(3);

        public static void block(Vec3 position, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(position.x, position.y, position.z), color, outline, shade);
        }

        public static void block(int x, int y, int z, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(x, y, z), color, outline, shade);
        }

        public static void entity(Entity en, int color, float partialTicks, boolean outline, boolean shade) {
            net.minecraft.entity.Entity e = en.entity;
            double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
            double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
            double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB bbox = e.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(bbox.minX - e.posX + x, bbox.minY - e.posY + y, bbox.minZ - e.posZ + z, bbox.maxX - e.posX + x, bbox.maxY - e.posY + y, bbox.maxZ - e.posZ + z);
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0f);
            float a = (color >> 24 & 0xFF) / 255.0f;
            float r = (color >> 16 & 0xFF) / 255.0f;
            float g = (color >> 8 & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            GL11.glColor4f(r, g, b, a);
            if (outline) {
                RenderGlobal.drawSelectionBoundingBox(axis);
            }
            if (shade) {
                RenderUtils.drawBoundingBox(axis, r, g, b);
            }
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static void entityGui(Entity en, int x, int y, float mouseX, float mouseY, int scale) {
            if (!en.isLiving) {
                return;
            }
            GL11.glPushMatrix();
            GuiInventory.drawEntityOnScreen(x, y, scale, mouseX, mouseY, (EntityLivingBase) en.entity);
            GL11.glPopMatrix();
        }

        public static void resetEquippedProgress() {
            mc.getItemRenderer().resetEquippedProgress();
        }

        public static void tracer(Entity entity, float lineWidth, int color, float partialTicks) {
            RenderUtils.drawTracerLine(entity.entity, color, lineWidth, partialTicks);
        }

        public static void showGui() {
            mc.displayGuiScreen(new EmptyGuiScreen());
        }

        private static class EmptyGuiScreen extends GuiScreen {
        }

        public static void item(ItemStack item, float x, float y, float scale) {
            mc.entityRenderer.setupOverlayRendering();
            GlStateManager.pushMatrix();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.disableBlend();
            GlStateManager.translate(x / scale, y / scale, 0);
            mc.getRenderItem().renderItemIntoGUI(item.itemStack, 0, 0);
            GlStateManager.enableBlend();
            RenderHelper.disableStandardItemLighting();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.popMatrix();
        }

        public static void image(Image image, float x, float y, float width, float height) {
            if (image == null || !image.isLoaded()) {
                return;
            }
            if (image.textureId == -1) {
                final DynamicTexture dynamicTexture = new DynamicTexture(image.bufferedImage);
                GL11.glTexParameteri(3553, 10240, 9728);
                dynamicTexture.updateDynamicTexture();
                image.textureId = dynamicTexture.getGlTextureId();
            }
            GlStateManager.pushMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(image.textureId);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glTexParameteri(3553, 10240, 9728);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldrenderer.pos(x, (y + height), 0.0).tex(0.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((x + width), (y + height), 0.0).tex(1.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((x + width), y, 0.0).tex(1.0, 0.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        public static Vec3 worldToScreen(double x, double y, double z, int scaleFactor, float partialTicks) {
            x -= mc.getRenderManager().viewerPosX;
            y -= mc.getRenderManager().viewerPosY;
            z -= mc.getRenderManager().viewerPosZ;
            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().renderPartialTicks, 0);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
            GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
            if (GLU.gluProject((float) x, (float) y, (float) z, render.MODELVIEW, render.PROJECTION, render.VIEWPORT, render.SCREEN_COORDS)) {
                Vec3 vec = new Vec3(render.SCREEN_COORDS.get(0) / scaleFactor, (Display.getHeight() - render.SCREEN_COORDS.get(1)) / scaleFactor, render.SCREEN_COORDS.get(2));
                mc.entityRenderer.setupOverlayRendering();
                return vec;
            }
            return null;
        }

        public static void roundedRect(float startX, float startY, float endX, float endY, float radius, int color) {
            RoundedUtils.drawRoundedRectRise(startX, startY, Math.abs(startX - endX), Math.abs(startY - endY), radius, color);
        }

        public static void gradientRect(float startX, float startY, float endX, float endY, int leftColor, int rightColor) {
            gradientRect(startX, startY, endX, endY, leftColor, leftColor, rightColor, rightColor);
        }

        public static void gradientRect(float startX, float startY, float endX, float endY, int topLeftColor, int bottomLeftColor, int topRightColor, int bottomRightColor) {
            RenderUtils.drawRoundedGradientRect(startX, startY, endX, endY, 0, topLeftColor, bottomLeftColor, topRightColor, bottomRightColor);
        }

        public static double[] getRotations() {
            return new double[]{mc.getRenderManager().playerViewY, mc.getRenderManager().playerViewX};
        }

        public static double[] getCameraRotations() {
            return new double[]{Utils.getCameraYaw(), Utils.getCameraPitch()};
        }

        public static int getFontWidth(String text) {
            return mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text);
        }

        public static int getFontHeight() {
            return mc.fontRendererObj.FONT_HEIGHT;
        }

        public static Vec3 getPosition() {
            net.minecraft.util.Vec3 position = Utils.getCameraPos(((IAccessorMinecraft) mc).getTimer().renderPartialTicks);
            return new Vec3(position);
        }

        public static void text2d(String text, float x, float y, float scale, int color, boolean shadow) {
            GlStateManager.pushMatrix();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(text, x / scale, y / scale, color, shadow);
            GlStateManager.disableBlend();
            if (scale != 1.0f) {
                GlStateManager.scale(1.0f, 1.0f, 1.0f);
            }
            GlStateManager.popMatrix();
        }

        public static void text3d(String text, Vec3 position, float scale, boolean shadow, boolean depth, boolean background, int color) {
            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().renderPartialTicks, 0);
            GlStateManager.pushMatrix();
            float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
            double px = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks;
            double py = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks;
            double pz = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks;
            GlStateManager.translate((float) position.x - px, (float) position.y - py, (float) position.z - pz);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
            float f1 = 0.02666667F;
            GlStateManager.scale(-f1, -f1, f1);
            if (depth) {
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            if (background) {
                GlStateManager.disableTexture2D();
                int width = mc.fontRendererObj.getStringWidth(text);
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldrenderer.pos(-1, -1.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
                worldrenderer.pos(-1, 8.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
                worldrenderer.pos(width + 1, 8.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
                worldrenderer.pos(width + 1, -1.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
                tessellator.draw();
                GlStateManager.enableTexture2D();
            }
            if (scale != 1) {
                GlStateManager.scale(scale, scale, scale);
            }
            mc.fontRendererObj.drawString(text, 0, 0, color, shadow);
            if (scale != 1) {
                GlStateManager.scale(1, 1, 1);
            }
            if (depth) {
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
            }
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }

        public static void rect(float startX, float startY, float endX, float endY, final int color) {
            if (startX < endX) {
                final float i = startX;
                startX = endX;
                endX = i;
            }
            if (startY < endY) {
                final float j = startY;
                startY = endY;
                endY = j;
            }
            final float f3 = (color >> 24 & 0xFF) / 255.0f;
            final float f4 = (color >> 16 & 0xFF) / 255.0f;
            final float f5 = (color >> 8 & 0xFF) / 255.0f;
            final float f6 = (color & 0xFF) / 255.0f;
            final Tessellator tessellator = Tessellator.getInstance();
            final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            GL11.glPushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(f4, f5, f6, f3);
            worldrenderer.begin(7, DefaultVertexFormats.POSITION);
            worldrenderer.pos((double) startX, (double) endY, 0.0).endVertex();
            worldrenderer.pos((double) endX, (double) endY, 0.0).endVertex();
            worldrenderer.pos((double) endX, (double) startY, 0.0).endVertex();
            worldrenderer.pos((double) startX, (double) startY, 0.0).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GL11.glPopMatrix();
        }

        public static void line2D(double startX, double startY, double endX, double endY, float lineWidth, int color) {
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            RenderUtils.glColor(color);
            GL11.glLineWidth(lineWidth);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(startX, startY);
            GL11.glVertex2d(endX, endY);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glPopMatrix();
        }

        public static void line3D(Vec3 pos1, Vec3 pos2, float lineWidth, int color) {
            line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, lineWidth, color);
        }

        public static void line3D(final double startX, final double startY, final double startZ, double endX, double endY, double endZ, final float lineWidth, final int color) {
            endX -= mc.getRenderManager().viewerPosX;
            endY -= mc.getRenderManager().viewerPosY;
            endZ -= mc.getRenderManager().viewerPosZ;
            final float a = (color >> 24 & 0xFF) / 255.0f;
            final float r = (color >> 16 & 0xFF) / 255.0f;
            final float g = (color >> 8 & 0xFF) / 255.0f;
            final float b = (color & 0xFF) / 255.0f;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glEnable(2848);
            GL11.glDisable(2929);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glLineWidth(lineWidth);
            GlStateManager.color(r, g, b, a);
            GL11.glBegin(2);
            GL11.glVertex3d(startX - mc.thePlayer.posX, startY - mc.thePlayer.posY, startZ - mc.thePlayer.posZ);
            GL11.glVertex3d(endX, endY, endZ);
            GL11.glEnd();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static boolean isInView(Entity en) {
            return RenderUtils.isInViewFrustum(en.entity);
        }



        public static class blur {
            public static void prepare() {
            }

            public static void apply(final int passes, final float radius) {
            }
        }

        public static class bloom {
            public static void prepare() {
            }

            public static void apply(final int passes, final float radius) {
            }
        }

    }

    public static class inventory {

        public static int getSlot() {
            return mc.thePlayer.inventory.currentItem;
        }

        public static void setSlot(int slot) {
            mc.thePlayer.inventory.currentItem = slot;
        }

        public static void click(int slot, int button, int mode) {
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, button, mode, mc.thePlayer);
        }

        public static List<String> getBookContents() {
            if (mc.currentScreen instanceof GuiScreenBook) {
                List<String> contents = new ArrayList<>();
                List<IChatComponent> bookContents = ((IAccessorGuiScreenBook) mc.currentScreen).getBookContents();
                if (bookContents == null) {
                    return contents;
                }
                int max = Math.min(128 / mc.fontRendererObj.FONT_HEIGHT, bookContents.size());
                for (int line = 0; line < max; ++line) {
                    IChatComponent lineStr = bookContents.get(line);
                    contents.add(lineStr.getUnformattedText());
                }
                if (!contents.isEmpty()) {
                    return contents;
                }
            }
            return null;
        }

        public static String getChest() {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) {
                    return "";
                }
                return chest.getLowerChestInventory().getDisplayName().getUnformattedText();
            }
            return "";
        }

        public static String getContainer() {
            if (mc.currentScreen instanceof GuiContainerCreative) {
                CreativeTabs creativetabs = CreativeTabs.creativeTabArray[((GuiContainerCreative) mc.currentScreen).getSelectedTabIndex()];
                if (creativetabs != null) {
                    return I18n.format(creativetabs.getTranslatedTabLabel());
                }
            } else if (mc.currentScreen != null) {
                try {
                    return ((IInventory) Reflection.containerInventoryPlayer.get(mc.currentScreen.getClass()).get(mc.currentScreen)).getDisplayName().getUnformattedText();
                } catch (Exception e) {
                }
            }
            return "";
        }

        public static int getSize() {
            return mc.thePlayer.inventory.getSizeInventory();
        }

        public static int getChestSize() {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) {
                    return -1;
                }
                return chest.getLowerChestInventory().getSizeInventory();
            }
            return -1;
        }

        public static ItemStack getStackInSlot(int slot) {
            if (mc.thePlayer.inventory.getStackInSlot(slot) == null) {
                return null;
            }
            return new ItemStack(mc.thePlayer.inventory.getStackInSlot(slot), (byte) 0);
        }

        public static ItemStack getStackInChestSlot(int slot) {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest.getLowerChestInventory().getStackInSlot(slot) == null) {
                    return null;
                }
                return new ItemStack(chest.getLowerChestInventory().getStackInSlot(slot), (byte) 0);
            }
            return null;
        }

        public static void open() {
            KeyBinding inventoryKey = mc.gameSettings.keyBindInventory;
            int originalKeyCode = inventoryKey.getKeyCode();

            if (originalKeyCode == 0) {
                inventoryKey.setKeyCode(13);
                KeyBinding.resetKeyBindingArrayAndHash();
            }

            KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), true);
            KeyBinding.onTick(inventoryKey.getKeyCode());
            KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), false);

            if (originalKeyCode == 0) {
                inventoryKey.setKeyCode(0);
                KeyBinding.resetKeyBindingArrayAndHash();
            }
        }
    }

    public static class keybinds {
        public static int[] getMousePosition() {
            return new int[]{Mouse.getX(), Mouse.getY()};
        }

        public static boolean isPressed(final String key) {
            KeyBinding keyBind = Reflection.keybinds.get(key);
            return keyBind != null && keyBind.isKeyDown();
        }

        public static void setPressed(final String key, final boolean pressed) {
            KeyBinding keyBind = Reflection.keybinds.get(key);
            if (keyBind != null) {
                KeyBinding.setKeyBindState(keyBind.getKeyCode(), pressed);
                if (pressed) {
                    KeyBinding.onTick(keyBind.getKeyCode());
                }
            }
        }

        public static int getKeyCode(final String key) {
            final KeyBinding keyBind = Reflection.keybinds.get(key);
            if (keyBind != null) {
                return keyBind.getKeyCode();
            }
            return -1;
        }

        public static int getKeyIndex(String key) {
            return Keyboard.getKeyIndex(key);
        }

        public static boolean isMouseDown(int mouseButton) {
            return Mouse.isButtonDown(mouseButton);
        }

        public static boolean isKeyDown(int key) {
            return Keyboard.isKeyDown(key);
        }

        public static void rightClick() {
            ((IAccessorMinecraft) mc).callRightClickMouse();
        }

        public static void leftClick() {
            ((IAccessorMinecraft) mc).callClickMouse();
        }

        public static int getScroll() {
            return Mouse.getDWheel();
        }
    }

    public static class util {
        public static final String colorSymbol = "§";

        public static String color(String message) {
            return Utils.formatColor(message);
        }

        public static String strip(String string) {
            return Utils.stripColor(string);
        }

        public static double round(double value, int decimals) {
            return Utils.round(value, decimals);
        }

        public static int randomInt(int min, int max) {
            return Utils.randomizeInt(min, max);
        }

        public static double randomDouble(double min, double max) {
            return Utils.randomizeDouble(min, max);
        }
    }

    public static class netty {
        public static boolean isPlayingPacket(SPacket packet) {
            return EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet.packet) != null;
        }

        public static boolean isPlayingPacket(CPacket packet) {
            return EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.SERVERBOUND, packet.packet) != null;
        }

        public static void handlePacket(SPacket packet) {
            ((IInvokerNetworkManager) mc.getNetHandler().getNetworkManager()).rvn$channelRead0(null, packet.packet);
        }
    }

    public static class entities {
        public static Entity createFakePlayer(String name) {
            return new Entity(new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.randomUUID(), name)));
        }
    }
}