package keystrokesmod.utility;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiIngame;
import keystrokesmod.mixin.impl.accessor.IAccessorItemFood;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.*;
import net.minecraft.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

public class Utils {
    private static final Random rand = new Random();
    public static final Minecraft mc = Minecraft.getMinecraft();
    public static HashSet<String> friends = new HashSet<>();
    public static HashSet<String> enemies = new HashSet<>();
    public static final Logger log = LogManager.getLogger();
    public static boolean cancelChat = false;
    public static List<String> cancelledMsgs = Lists.newArrayList();

    private static int darkRed = new Color(189, 0, 1).getRGB();
    private static int red = new Color(253, 63, 63).getRGB();
    private static int gold = new Color(215, 162, 50).getRGB();
    private static int yellow = new Color(254, 254, 62).getRGB();
    private static int darkGreen = new Color(0, 191, 4).getRGB();
    private static int green = new Color(64, 253, 62).getRGB();
    private static int aqua = new Color(65, 255, 254).getRGB();
    private static int darkAqua = new Color(0, 190, 189).getRGB();
    private static int darkBlue = new Color(1, 1, 187).getRGB();
    private static int blue = new Color(61, 64, 255).getRGB();
    private static int lightPurple = new Color(254, 63, 255).getRGB();
    private static int darkPurple = new Color(190, 0, 190).getRGB();
    private static int gray = new Color(190, 190, 190).getRGB();
    private static int darkGray = new Color(63, 63, 63).getRGB();
    private static int black = new Color(17, 17, 17).getRGB();

    public static boolean addEnemy(String name) {
        if (enemies.add(name.toLowerCase())) {
            Utils.sendMessage("&7Added enemy&7: &b" + name);
            return true;
        }
        return false;
    }

    public static boolean removeEnemy(String name) {
        if (enemies.remove(name.toLowerCase())) {
            Utils.sendMessage("&Removed enemy&7: &b" + name);
            return true;
        }
        return false;
    }

    public static float getCameraYaw() {
        return (float) Math.toDegrees(Math.atan2(ActiveRenderInfo.getRotationZ(), ActiveRenderInfo.getRotationX()));
    }

    public static float getCameraPitch() {
        return (float) Math.toDegrees(Math.acos(ActiveRenderInfo.getRotationXZ()));
    }

    public static Vec3 getCameraPos(double renderPartialTicks) {
        if (mc.gameSettings.thirdPersonView == 0) {
            Vec3 firstPersonPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            return firstPersonPos;
        }
        float cameraDistance = 4.0F;

        Entity renderEntity = mc.getRenderViewEntity();
        float entityEyeHeight = renderEntity.getEyeHeight();

        double interpolatedX = renderEntity.prevPosX + (renderEntity.posX - renderEntity.prevPosX) * renderPartialTicks;
        double interpolatedY = renderEntity.prevPosY + (renderEntity.posY - renderEntity.prevPosY) * renderPartialTicks + entityEyeHeight;
        double interpolatedZ = renderEntity.prevPosZ + (renderEntity.posZ - renderEntity.prevPosZ) * renderPartialTicks;

        double adjustedDistance = cameraDistance;

        float cameraYaw = Utils.getCameraYaw();
        float cameraPitch = Utils.getCameraPitch();

        double offsetX = -MathHelper.sin(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.cos(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
        double offsetZ =  MathHelper.cos(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.cos(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
        double offsetY = -MathHelper.sin(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;

        for (int i = 0; i < 8; i++) {
            float cornerOffsetX = (float) ((i & 1) * 2 - 1) * 0.1F;
            float cornerOffsetY = (float) ((i >> 1 & 1) * 2 - 1) * 0.1F;
            float cornerOffsetZ = (float) ((i >> 2 & 1) * 2 - 1) * 0.1F;

            MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(new Vec3(interpolatedX + cornerOffsetX, interpolatedY + cornerOffsetY, interpolatedZ + cornerOffsetZ), new Vec3((interpolatedX - offsetX + cornerOffsetX + cornerOffsetZ), (interpolatedY - offsetY + cornerOffsetY), (interpolatedZ - offsetZ + cornerOffsetZ)));

            if (rayTraceResult != null) {
                double blockHitDistance = rayTraceResult.hitVec.distanceTo(new Vec3(interpolatedX, interpolatedY, interpolatedZ));
                if (blockHitDistance < adjustedDistance) {
                    adjustedDistance = blockHitDistance;
                }
            }
        }

        double finalCameraX = interpolatedX - offsetX * (adjustedDistance / cameraDistance);
        double finalCameraY = interpolatedY - offsetY * (adjustedDistance / cameraDistance);
        double finalCameraZ = interpolatedZ - offsetZ * (adjustedDistance / cameraDistance);

        return new Vec3(finalCameraX, finalCameraY, finalCameraZ);
    }

    public static boolean tabbedIn() {
        return mc.currentScreen == null && mc.inGameHasFocus;
    }

    public static String getHardwareIdForLoad(String url) {
        String hashedId = "";
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(((System.currentTimeMillis() / 20000L + 29062381L) + "J{LlrPhHgj8zy:uB").getBytes("UTF-8"));
            hashedId = String.format("%032x", new BigInteger(1, instance.digest()));
            instance.update((System.getenv("COMPUTERNAME") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL") + Runtime.getRuntime().availableProcessors() + url).getBytes("UTF-8"));
            return hashedId;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return hashedId;
    }

    public static boolean isConsuming(Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        return ((EntityPlayer) entity).isUsingItem() && (holdingFood((EntityPlayer) entity) || holdingPotion((EntityPlayer) entity));
    }

    public static boolean holdingFood(EntityLivingBase entity) {
        return entity.getHeldItem() != null && entity.getHeldItem().getItem() instanceof ItemFood;
    }

    public static boolean holdingPotion(EntityPlayer player) {
        ItemStack itemStack = player.getHeldItem();
        return itemStack != null && itemStack.getItem() instanceof ItemPotion;
    }


    public static int getColorFromEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)((EntityLivingBase) entity).getTeam();
            if (scoreplayerteam != null) {
                String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());
                if (s.length() >= 2) {
                    return mc.getRenderManager().getFontRenderer().getColorCode(s.charAt(1));
                }
            }
        }
        String displayName = entity.getDisplayName().getFormattedText();
        displayName = Utils.removeFormatCodes(displayName);
        if (displayName.isEmpty() || !displayName.startsWith("§") || displayName.charAt(1) == 'f') {
            return -1;
        }
        switch (displayName.charAt(1)) {
            case '0':
                return black;
            case '1':
                return darkBlue;
            case '2':
                return darkGreen;
            case '3':
                return darkAqua;
            case '4':
                return darkRed;
            case '5':
                return darkPurple;
            case '6':
                return gold;
            case '7':
                return gray;
            case '8':
                return darkGray;
            case '9':
                return blue;
            case 'a':
                return green;
            case 'b':
                return aqua;
            case 'c':
                return red;
            case 'd':
                return lightPurple;
            case 'e':
                return yellow;
        }
        return -1;
    }

    public static boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(mc.theWorld.getBlockState(new BlockPos(posX, i, posZ)).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public static net.minecraft.block.Block getBlockFromName(String name) {
        return net.minecraft.block.Block.blockRegistry.getObject(new ResourceLocation("minecraft:" + name));
    }

    public static boolean canPlayerBeSeen(EntityLivingBase player) {
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        Vec3 vecPlayer = mc.thePlayer.getPositionEyes(1.0f);
        double shoulderHeight = player.getEyeHeight() - 0.2;
        if (canSeeVec(vecPlayer, new Vec3(x + 0.3, shoulderHeight, z))) {
            return true;
        }
        if (canSeeVec(vecPlayer, new Vec3(x - 0.3, shoulderHeight, z))) {
            return true;
        }
        if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z + 0.3))) {
            return true;
        }
        if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z - 0.3))) {
            return true;
        }
        for (double d = player.getEyeHeight() + 0.2; d > 0.0; d -= 0.2) {
            Vec3 vecPoint = new Vec3(x, y + d, z);
            if (canSeeVec(vecPlayer, vecPoint)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSeeVec(Vec3 vecPlayer, Vec3 vecTarget) {
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vecPlayer, vecTarget, false, false, false);
        return mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public static List<NetworkPlayerInfo> getTablist(boolean removeSelf) {
        final ArrayList<NetworkPlayerInfo> list = new ArrayList<>(mc.getNetHandler().getPlayerInfoMap());
        removeDuplicates(list);
        if (removeSelf) {
            list.remove(mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()));
        }
        return list;
    }

    public static void removeDuplicates(final ArrayList list) {
        final HashSet set = new HashSet(list);
        list.clear();
        list.addAll(set);
    }

    public static boolean removeFriend(String name) {
        if (friends.remove(name.toLowerCase())) {
            Utils.sendMessage("&7Removed &afriend&7: &b" + name);
            return true;
        }
        return false;
    }

    public static String getCompilerDirectory() {
        String tempDirStr = System.getProperty("java.io.tmpdir") + "cmF2ZW5fc2NyaXB0cw";
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            File tempDir = new File(mc.mcDataDir + File.separator + "keystrokes" + File.separator + "scripts", "compiler_temp");
            if (!tempDir.exists()) {
                if (!tempDir.mkdirs()) {
                    return tempDirStr;
                }
            }
            return tempDir.getAbsolutePath();
        }
        return tempDirStr;
    }

    public static boolean addFriend(String name) {
        if (friends.add(name.toLowerCase())) {
            Utils.sendMessage("&7Added &afriend&7: &b" + name);
            if (enemies.contains(name.toLowerCase())) {
                enemies.remove(name.toLowerCase());
            }
            return true;
        }
        return false;
    }

    public static boolean isWholeNumber(double num) {
        return num == Math.floor(num);
    }

    public static String asWholeNum(double input) {
        return isWholeNumber(input) ? (int) input + "" : String.valueOf(input);
    }

    public static int randomizeInt(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    public static double randomizeDouble(double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    public static boolean inFov(float fov, BlockPos blockPos) {
        return inFov(fov, blockPos.getX(), blockPos.getZ());
    }

    public static boolean inFov(float fov, Entity entity) {
        return inFov(fov, entity.posX, entity.posZ);
    }

    public static boolean inFov(float fov, final double posX, final double posZ) {
        return inFov(mc.thePlayer, fov, posX, posZ);
    }

    public static boolean inFov(Entity viewPoint, float fov, final double posX, final double posZ) {
        fov *= 0.5;
        final double wrapAngleTo180_double = MathHelper.wrapAngleTo180_double((viewPoint.rotationYaw - RotationUtils.angle(posX, posZ)) % 360.0f);
        if (wrapAngleTo180_double > 0.0) {
            if (wrapAngleTo180_double < fov) {
                return true;
            }
        }
        else if (wrapAngleTo180_double > -fov) {
            return true;
        }
        return false;
    }

    public static Vec3 getLookVec(float yaw, float pitch) {
        float f = MathHelper.cos(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * ((float)Math.PI / 180F));
        float f3 = MathHelper.sin(-pitch * ((float)Math.PI / 180F));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static boolean holdingBow() {
        if (mc.thePlayer.getHeldItem() == null) {
            return false;
        }
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemBow;
    }

    public static boolean bowBackwards() {
        if (holdingBow() && mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= 0 && isMoving()) {
            return true;
        }
        return false;
    }

    public static void sendMessage(String txt) {
        if (nullCheck()) {
            String m = formatColor("&7[&dR&7]&r " + txt);
            mc.thePlayer.addChatMessage(new ChatComponentText(m));
        }
    }

    public static void sendMessage(Object object) {
        String toString = String.valueOf(object);
        sendMessage(toString);
    }

    public static void sendDebugMessage(String message) {
        if (nullCheck()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[§dR§7]§r " + message));
        }
    }

    public static void attackEntity(Entity e, boolean clientSwing, boolean silentSwing) {
        if (clientSwing) {
            mc.thePlayer.swingItem();
        }
        else if (silentSwing || (!silentSwing && !clientSwing)) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
        mc.playerController.attackEntity(mc.thePlayer, e);
    }

    public static void sendRawMessage(String txt) {
        if (nullCheck()) {
            mc.thePlayer.addChatMessage(new ChatComponentText(formatColor(txt)));
        }
    }

    public static float getCompleteHealth(EntityLivingBase entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static String getHealthStr(EntityLivingBase entity, boolean accountDead) {
        float completeHealth = getCompleteHealth(entity);
        if (accountDead && entity.isDead) {
            completeHealth = 0;
        }
        return getColorForHealth(entity.getHealth() / entity.getMaxHealth(), completeHealth);
    }

    public static int getTool(Block block) {
        float n = 1.0f;
        int n2 = -1;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            final ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
            if (getStackInSlot != null) {
                final float a = getEfficiency(getStackInSlot, block);
                if (a > n) {
                    n = a;
                    n2 = i;
                }
            }
        }
        return n2;
    }

    public static boolean onLadder(Entity entity) {
        int posX = MathHelper.floor_double(entity.posX);
        int posY = MathHelper.floor_double(entity.posY - 0.20000000298023224D);
        int posZ = MathHelper.floor_double(entity.posZ);
        BlockPos blockpos = new BlockPos(posX, posY, posZ);
        Block block1 = mc.theWorld.getBlockState(blockpos).getBlock();
        return block1 instanceof BlockLadder && !entity.onGround;
    }

    public static float getEfficiency(final ItemStack itemStack, final Block block) {
        float getStrVsBlock = itemStack.getStrVsBlock(block);
        if (getStrVsBlock > 1.0f) {
            final int getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            if (getEnchantmentLevel > 0) {
                getStrVsBlock += getEnchantmentLevel * getEnchantmentLevel + 1;
            }
        }
        return getStrVsBlock;
    }

    public static boolean isEnemy(EntityPlayer entityPlayer) {
        return !enemies.isEmpty() && enemies.contains(entityPlayer.getName().toLowerCase());
    }

    public static boolean isEnemy(String name) {
        return !enemies.isEmpty() && enemies.contains(name.toLowerCase());
    }

    public static String getColorForHealth(double n, double n2) {
        double health = round(n2, 1);
        return ((n < 0.3) ? "§c" : ((n < 0.5) ? "§6" : ((n < 0.7) ? "§e" : "§a"))) + asWholeNum(health);
    }

    public static int getColorForHealth(double health) {
        return ((health < 0.3) ? -43691 : ((health < 0.5) ? -22016 : ((health < 0.7) ? -171 : -11141291)));
    }

    public static String formatColor(String txt) {
        return txt.replaceAll("&", "§");
    }

    public static String getFirstColorCode(String input) {
        if (input == null || input.length() < 2) {
            return "";
        }
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '§') {
                char c = input.charAt(i + 1);
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                    return "§" + c;
                }
            }
        }
        return "";
    }

    public static int getBoldWidth(String string) {
        boolean bold = false;
        int additionalWidth = 0;
        for (int i = 0; i < string.length(); ++i) {
            char c0 = string.charAt(i);
            if (c0 == '§' && i + 1 < string.length()) {
                int i2 = "0123456789abcdefklmnor".indexOf(string.toLowerCase(Locale.ENGLISH).charAt(i + 1));
                if (i2 == 17) {
                    bold = true;
                }
                ++i;
            }
            else {
                if (bold) {
                    ++additionalWidth;
                }
            }
        }
        return additionalWidth;
    }

    public static void correctValue(SliderSetting c, SliderSetting d) {
        if (c.getInput() > d.getInput()) {
            double p = c.getInput();
            c.setValueWithEvent(d.getInput());
            d.setValueWithEvent(p);
        }
    }

    public static String generateRandomString(final int n) {
        final char[] array = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        final StringBuilder sb = new StringBuilder();
        IntStream.range(0, n).forEach(p2 -> sb.append(array[rand.nextInt(array.length)]));
        return sb.toString();
    }

    public static boolean isFriended(EntityPlayer entityPlayer) {
        return !friends.isEmpty() && friends.contains(entityPlayer.getName().toLowerCase());
    }

    public static boolean isFriended(String name) {
        return !friends.isEmpty() && friends.contains(name.toLowerCase());
    }

    public static double getRandomValue(SliderSetting a, SliderSetting b, Random r) {
        return a.getInput() == b.getInput() ? a.getInput() : a.getInput() + r.nextDouble() * (b.getInput() - a.getInput());
    }

    public static boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static boolean isHypixel() {
        return !mc.isSingleplayer() && mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.contains("hypixel.net");
    }

    public static String getHitsToKillStr(final EntityPlayer entityPlayer, final ItemStack itemStack) {
        final int n = (int)Math.ceil(getHitsToKill(entityPlayer, itemStack));
        return "§" + ((n <= 1) ? "c" : ((n <= 3) ? "6" : ((n <= 5) ? "e" : "a"))) + n;
    }

    public static double getHitsToKill(final EntityPlayer target, final ItemStack usedItem) {
        double heldItemDamageLevel = 1.0;
        if (usedItem != null && (usedItem.getItem() instanceof ItemSword || usedItem.getItem() instanceof ItemAxe)) {
            heldItemDamageLevel += getDamageLevel(usedItem);
        }
        double armorProtPercentage = 0.0;
        double totalEPF = 0.0;
        for (int i = 0; i < 4; ++i) {
            final ItemStack stack = target.inventory.armorItemInSlot(i);
            if (stack != null) {
                if (stack.getItem() instanceof ItemArmor) {
                    armorProtPercentage += ((ItemArmor)stack.getItem()).damageReduceAmount * 0.04;
                    final int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
                    if (protLevel != 0) {
                        final double epf = Math.floor(0.75 * (6 + protLevel * protLevel) / 3.0);
                        totalEPF += epf;
                    }
                }
            }
        }
        totalEPF = 0.04 * Math.min(Math.ceil(Math.min(totalEPF, 25.0) * 0.75), 20.0);
        final double armorReduction = armorProtPercentage + totalEPF * (1.0 - armorProtPercentage);
        final double damage = heldItemDamageLevel * (1.0 - armorReduction);
        final double hitsToKill = getCompleteHealth(target) / damage;
        return round(hitsToKill, 1);
    }

    public static float n() {
        return ae(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
    }

    public static String extractFileName(String name) {
        int firstIndex = name.indexOf("_");
        int lastIndex = name.lastIndexOf("_");

        if (firstIndex != -1 && lastIndex != -1 && lastIndex > firstIndex) {
            return name.substring(firstIndex + 1, lastIndex);
        } else {
            return name;
        }
    }

    public static int mergeAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | alpha << 24;
    }

    public static int clamp(int n) {
        if (n > 255) {
            return 255;
        }
        if (n < 4) {
            return 4;
        }
        return n;
    }

    public static boolean hasArrows(ItemStack stack) {
        final boolean flag = mc.thePlayer.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) > 0;
        return flag || mc.thePlayer.inventory.hasItem(Items.arrow);
    }

    public static int darkenColor(int color, double percent) {
        int alpha = (color >> 24) & 0xFF;
        int red   = (color >> 16) & 0xFF;
        int green = (color >> 8)  & 0xFF;
        int blue  = color & 0xFF;

        percent = (100 - percent) / 100;

        red   = (int)(red * percent);
        green = (int)(green * percent);
        blue  = (int)(blue * percent);

        red   = clamp(red);
        green = clamp(green);
        blue  = clamp(blue);

        int darkenedColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
        return darkenedColor;
    }

    public static boolean isTeamMate(Entity entity) {
        try {
            Entity teamMate = entity;
            if (mc.thePlayer.isOnSameTeam((EntityLivingBase) entity) || mc.thePlayer.getDisplayName().getUnformattedText().startsWith(teamMate.getDisplayName().getUnformattedText().substring(0, 2)) || getNetworkDisplayName().startsWith(teamMate.getDisplayName().getUnformattedText().substring(0, 2))) {
                return true;
            }
        }
        catch (Exception ignored) {}
        return false;
    }

    public static String getNetworkDisplayName() {
        try {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            return ScorePlayerTeam.formatPlayerName(playerInfo.getPlayerTeam(), playerInfo.getGameProfile().getName());
        }
        catch (Exception ignored) {}
        return "";
    }

    public static void setSpeed(double n) {
        if (n == 0.0) {
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionX = 0.0;
            return;
        }
        float n3 = n();
        mc.thePlayer.motionX = -Math.sin(n3) * n;
        mc.thePlayer.motionZ = Math.cos(n3) * n;
    }

    public static void resetTimer() {
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
    }

    public static boolean inInventory() {
        if (!Utils.nullCheck()) {
            return false;
        }
        return (mc.currentScreen != null) && (mc.thePlayer.inventoryContainer != null) && (mc.thePlayer.inventoryContainer instanceof ContainerPlayer) && (mc.currentScreen instanceof GuiInventory);
    }

    public static int getSkyWarsStatus() {
        List<String> sidebar = Utils.getSidebarLines();
        if (sidebar == null || sidebar.isEmpty()) {
            return -1;
        }
        if (Utils.stripColor(sidebar.get(0)).startsWith("SKYWARS")) {
            for (String line : sidebar) {
                line = Utils.stripColor(line);
                if (line.equals("Waiting...") || line.startsWith("Starting in ")) {
                    return 1;
                }
                else if (line.startsWith("Players left: ")) {
                    return 2;
                }
            }
            return 0;
        }
        return -1;
    }

    public static String getString(final JsonObject type, final String member) {
        try {
            return type.get(member).getAsString();
        }
        catch (Exception er) {
            return "";
        }
    }

    public static int getBedwarsStatus() {
        if (!Utils.nullCheck()) {
            return -1;
        }
        final Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return -1;
        }
        final ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null || !stripString(objective.getDisplayName()).contains("BED WARS")) {
            return -1;
        }
        for (String line : getSidebarLines()) {
            line = stripString(line);
            String[] parts = line.split("  ");
            if (parts.length > 1) {
                if (parts[1].startsWith("L")) {
                    return 0;
                }
            }
            else if (line.equals("Waiting...") || line.startsWith("Starting in")) {
                return 1;
            }
            else if (line.startsWith("R Red:") || line.startsWith("B Blue:")) {
                return 2;
            }
        }
        return -1;
    }

    public static String stripString(final String s) {
        final char[] nonValidatedString = StringUtils.stripControlCodes(s).toCharArray();
        final StringBuilder validated = new StringBuilder();
        for (final char c : nonValidatedString) {
            if (c < '' && c > '') {
                validated.append(c);
            }
        }
        return validated.toString();
    }

    public static List<String> getSidebarLines() {
        final List<String> lines = new ArrayList<>();
        if (mc.theWorld == null) {
            return lines;
        }
        final Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return lines;
        }
        final ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return lines;
        }
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        final List<Score> list = new ArrayList<>();
        for (final Score input : scores) {
            if (input != null && input.getPlayerName() != null && !input.getPlayerName().startsWith("#")) {
                list.add(input);
            }
        }
        if (list.size() > 15) {
            scores = new ArrayList<>(Lists.newArrayList(Iterables.skip(list, list.size() - 15)));
        } else {
            scores = list;
        }
        int index = 0;
        for (final Score score : scores) {
            ++index;
            final ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
            if (index == scores.size()) {
                lines.add(objective.getDisplayName());
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    public static Random getRandom() {
        return rand;
    }

    public static boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    public static void aim(Entity en, float ps, boolean pc) {
        if (en != null) {
            float[] t = getRotationsOld(en);
            if (t != null) {
                float y = t[0];
                float p = t[1] + 4.0F + ps;
                if (pc) {
                    mc.getNetHandler().addToSendQueue(new C05PacketPlayerLook(y, p, mc.thePlayer.onGround));
                }
                else {
                    mc.thePlayer.rotationYaw = y;
                    mc.thePlayer.rotationPitch = p;
                }
            }

        }
    }

    public static float[] getRotationsOld(Entity q) {
        if (q == null) {
            return null;
        }
        else {
            double diffX = q.posX - mc.thePlayer.posX;
            double diffY;
            if (q instanceof EntityLivingBase) {
                EntityLivingBase en = (EntityLivingBase) q;
                diffY = en.posY + (double) en.getEyeHeight() * 0.9D - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
            } else {
                diffY = (q.getEntityBoundingBox().minY + q.getEntityBoundingBox().maxY) / 2.0D - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
            }

            double diffZ = q.posZ - mc.thePlayer.posZ;
            double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / 3.141592653589793D) - 90.0F;
            float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / 3.141592653589793D));
            return new float[] { mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw) , mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)};
        }
    }

    public static double n(Entity en) {
        return ((double) (mc.thePlayer.rotationYaw - getYaw(en)) % 360.0D + 540.0D) % 360.0D - 180.0D;
    }

    public static float getYaw(Entity ent) {
        double x = ent.posX - mc.thePlayer.posX;
        double z = ent.posZ - mc.thePlayer.posZ;
        double yaw = Math.atan2(x, z) * 57.29577951308232;
        return (float) (yaw * -1.0D);
    }

    public static void ss(double s, boolean m) {
        if (!m || isMoving()) {
            mc.thePlayer.motionX = -Math.sin(gd()) * s;
            mc.thePlayer.motionZ = Math.cos(gd()) * s;
        }
    }

    public static boolean keysDown() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
    }

    public static boolean jumpDown() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    public static double distanceToGround(Entity entity) {
        if (entity.onGround) {
            return 0;
        }
        double fallDistance = -1;
        double y = entity.posY;
        if (entity.posY % 1 == 0) {
            y--;
        }
        for (int i = (int) Math.floor(y); i > -1; i--) {
            if (!isPlaceable(new BlockPos(entity.posX, i, entity.posZ))) {
                fallDistance = y - i;
                break;
            }
        }
        return fallDistance - 1;
    }

    public static float gd() {
        float yw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward < 0.0F) {
            yw += 180.0F;
        }

        float f;
        if (mc.thePlayer.moveForward < 0.0F) {
            f = -0.5F;
        } else if (mc.thePlayer.moveForward > 0.0F) {
            f = 0.5F;
        } else {
            f = 1.0F;
        }

        if (mc.thePlayer.moveStrafing > 0.0F) {
            yw -= 90.0F * f;
        }

        if (mc.thePlayer.moveStrafing < 0.0F) {
            yw += 90.0F * f;
        }

        yw *= 0.017453292F;
        return yw;
    }

    public static float ae(float n, float n2, float n3) {
        float n4 = 1.0f;
        if (n2 < 0.0f) {
            n += 180.0f;
            n4 = -0.5f;
        } else if (n2 > 0.0f) {
            n4 = 0.5f;
        }
        if (n3 > 0.0f) {
            n -= 90.0f * n4;
        } else if (n3 < 0.0f) {
            n += 90.0f * n4;
        }
        return n * 0.017453292f;
    }

    public static double getHorizontalSpeed() {
        return getHorizontalSpeed(mc.thePlayer);
    }

    public static double getHorizontalSpeed(Entity entity) {
        return Math.sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
    }

    public static List<String> getTopLevelLines(String fileContents) {
        List<String> topLevelLines = new ArrayList<>();
        String[] lines = fileContents.split("\\r?\\n");
        int braceLevel = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String originalLine = line;
            String processedLine = line.trim();

            if (inBlockComment) {
                if (processedLine.contains("*/")) {
                    inBlockComment = false;
                    processedLine = processedLine.substring(processedLine.indexOf("*/") + 2).trim();
                }
                else {
                    continue;
                }
            }

            if (processedLine.startsWith("//")) {
                continue;
            }

            if (processedLine.contains("/*")) {
                inBlockComment = true;
                processedLine = processedLine.substring(0, processedLine.indexOf("/*")).trim();
                if (processedLine.isEmpty()) {
                    continue;
                }
            }

            if (processedLine.contains("//")) {
                processedLine = processedLine.substring(0, processedLine.indexOf("//")).trim();
            }

            if (processedLine.contains("/*") && processedLine.contains("*/")) {
                processedLine = processedLine.substring(0, processedLine.indexOf("/*")) + processedLine.substring(processedLine.indexOf("*/") + 2);
                processedLine = processedLine.trim();
            }

            if (processedLine.isEmpty()) {
                continue;
            }

            String lineWithoutStrings = removeStringLiterals(processedLine);

            int openBraces = 0;
            int closeBraces = 0;
            for (char ch : lineWithoutStrings.toCharArray()) {
                if (ch == '{') {
                    openBraces++;
                }
                else if (ch == '}') {
                    closeBraces++;
                }
            }
            braceLevel += openBraces - closeBraces;

            if (braceLevel == 0 && !processedLine.contains("{") && !processedLine.contains("}") && !processedLine.startsWith("@")) {
                topLevelLines.add(originalLine.trim());
            }
        }

        return topLevelLines;
    }

    public static boolean holdingEdible(ItemStack stack) {
        if (stack.getItem() instanceof ItemFood && mc.thePlayer.getFoodStats().getFoodLevel() == 20) {
            ItemFood food = (ItemFood) stack.getItem();
            return ((IAccessorItemFood) food).getAlwaysEdible();
        }
        return true;
    }

    private static String removeStringLiterals(String line) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (!inString) {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public static boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public static boolean onEdge() {
        return onEdge(mc.thePlayer);
    }

    public static boolean onEdge(Entity entity) {
        return mc.theWorld.getCollidingBoundingBoxes(entity, entity.getEntityBoundingBox().offset(entity.motionX / 3.0D, -1.0D, entity.motionZ / 3.0D)).isEmpty();
    }

    public static boolean lookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null;
    }

    public static boolean isDiagonal(boolean strict) {
        float yaw = ((mc.thePlayer.rotationYaw % 360) + 360) % 360;
        yaw = yaw > 180 ? yaw - 360 : yaw;
        boolean isYawDiagonal = inBetween(-170, 170, yaw) && !inBetween(-10, 10, yaw) && !inBetween(80, 100, yaw) && !inBetween(-100, -80, yaw);
        if (strict) {
            isYawDiagonal = inBetween(-178.5, 178.5, yaw) && !inBetween(-1.5, 1.5, yaw) && !inBetween(88.5, 91.5, yaw) && !inBetween(-91.5, -88.5, yaw);
        }
        boolean isStrafing = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        return isYawDiagonal || isStrafing;
    }

    public static boolean isDiagonalMyauCalc() {
        float yaw = ((mc.thePlayer.rotationYaw % 360) + 360) % 360;
        yaw = yaw > 180 ? yaw - 360 : yaw;

        float y = Math.abs(yaw % 90.0F);
        return y > 20.0F && y < 70.0F;
    }

    public static boolean isDiagonalFatVersion() {
        float yaw = ((mc.thePlayer.rotationYaw % 360) + 360) % 360;
        yaw = yaw > 180 ? yaw - 360 : yaw;

        float y = Math.abs(yaw % 90.0F);
        return y > 15.0F && y < 70.0F;
    }

    public static double gbps(Entity en, int d) {
        double x = en.posX - en.prevPosX;
        double z = en.posZ - en.prevPosZ;
        double sp = Math.sqrt(x * x + z * z) * 20.0D;
        if (d == 0) {
            return sp;
        }
        return round(sp, d);
    }

    public static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }

    public static String removeFormatCodes(String str) {
        return str.replace("§k", "").replace("§l", "").replace("§m", "").replace("§n", "").replace("§o", "").replace("§r", "");
    }

    public static boolean isEdgeOfBlock(final double posX, final double posY, final double posZ) {
        BlockPos pos = new BlockPos(posX, posY - ((posY % 1.0 == 0.0) ? 1 : 0), posZ);
        return mc.theWorld.isAirBlock(pos);
    }

    public static boolean isEdgeOfBlock() {
        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - ((mc.thePlayer.posY % 1.0 == 0.0) ? 1 : 0), mc.thePlayer.posZ);
        return mc.theWorld.isAirBlock(pos);
    }

    public static long timeBetween(long val, long val2) {
        return Math.abs(val2 - val);
    }

    public static void sendModuleMessage(Module module, String s) {
        sendRawMessage("&3" + module.getName() + "&7: &r" + s);
    }

    public static EntityLivingBase raytrace(int range) {
        Entity entity = null;
        EntityPlayer self = mc.thePlayer;
        MovingObjectPosition rayTrace = self.rayTrace(range, 1.0f);
        final Vec3 getPositionEyes = self.getPositionEyes(1.0f);
        final float rotationYaw = self.rotationYaw;
        final float rotationPitch = self.rotationPitch;
        final float cos = MathHelper.cos(-rotationYaw * 0.017453292f - 3.1415927f);
        final float sin = MathHelper.sin(-rotationYaw * 0.017453292f - 3.1415927f);
        final float n2 = -MathHelper.cos(-rotationPitch * 0.017453292f);
        final Vec3 vec3 = new Vec3((double)(sin * n2), (double)MathHelper.sin(-rotationPitch * 0.017453292f), cos * n2);
        final Vec3 addVector = getPositionEyes.addVector(vec3.xCoord * (double)range, vec3.yCoord * (double)range, vec3.zCoord * (double)range);
        Vec3 vec4 = null;
        final List getEntitiesWithinAABBExcludingEntity = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), mc.getRenderViewEntity().getEntityBoundingBox().addCoord(vec3.xCoord * (double)range, vec3.yCoord * (double)range, vec3.zCoord * (double)range).expand(1.0, 1.0, 1.0));
        double n3 = (double)range;
        for (int i = 0; i < getEntitiesWithinAABBExcludingEntity.size(); ++i) {
            final Entity entity2 = (Entity)getEntitiesWithinAABBExcludingEntity.get(i);
            if (entity2.canBeCollidedWith()) {
                final float getCollisionBorderSize = entity2.getCollisionBorderSize();
                final AxisAlignedBB expand = entity2.getEntityBoundingBox().expand((double)getCollisionBorderSize, (double)getCollisionBorderSize, (double)getCollisionBorderSize);
                final MovingObjectPosition calculateIntercept = expand.calculateIntercept(getPositionEyes, addVector);
                if (expand.isVecInside(getPositionEyes)) {
                    if (0.0 < n3 || n3 == 0.0) {
                        entity = entity2;
                        vec4 = ((calculateIntercept == null) ? getPositionEyes : calculateIntercept.hitVec);
                        n3 = 0.0;
                    }
                }
                else if (calculateIntercept != null) {
                    final double distanceTo = getPositionEyes.distanceTo(calculateIntercept.hitVec);
                    if (distanceTo < n3 || n3 == 0.0) {
                        if (entity2 == mc.getRenderViewEntity().ridingEntity && !entity2.canRiderInteract()) {
                            if (n3 == 0.0) {
                                entity = entity2;
                                vec4 = calculateIntercept.hitVec;
                            }
                        }
                        else {
                            entity = entity2;
                            vec4 = calculateIntercept.hitVec;
                            n3 = distanceTo;
                        }
                    }
                }
            }
        }
        if (entity != null && (n3 < range || rayTrace == null)) {
            rayTrace = new MovingObjectPosition(entity, vec4);
        }
        if (rayTrace != null && rayTrace.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && rayTrace.entityHit instanceof EntityLivingBase) {
            return (EntityLivingBase)rayTrace.entityHit;
        }
        return null;
    }

    public static int getChroma(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0F / (float) speed), 1.0F, 1.0F).getRGB();
    }

    public static double round(double n, int d) {
        if (d == 0) {
            return (double) Math.round(n);
        }
        else {
            double p = Math.pow(10.0D, (double) d);
            return (double) Math.round(n * p) / p;
        }
    }

    public static String stripColor(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        final char[] array = StringUtils.stripControlCodes(s).toCharArray();
        final StringBuilder sb = new StringBuilder();
        for (final char c : array) {
            if (c < '\u007f' && c > '\u0014') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void addToClipboard(String string) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection(string);
            clipboard.setContents(stringSelection, null);
        }
        catch (Exception e) {
            Utils.sendMessage("&cFailed to copy &b" + string);
        }
    }

    public static List<String> gsl() {
        List<String> lines = new ArrayList();
        if (mc.theWorld == null) {
            return lines;
        } else {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) {
                return lines;
            } else {
                ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
                if (objective == null) {
                    return lines;
                } else {
                    Collection<Score> scores = scoreboard.getSortedScores(objective);
                    List<Score> list = new ArrayList();
                    Iterator var5 = scores.iterator();

                    Score score;
                    while (var5.hasNext()) {
                        score = (Score) var5.next();
                        if (score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                            list.add(score);
                        }
                    }

                    if (list.size() > 15) {
                        scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
                    } else {
                        scores = list;
                    }

                    var5 = scores.iterator();

                    while (var5.hasNext()) {
                        score = (Score) var5.next();
                        ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                        lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
                    }

                    return lines;
                }
            }
        }
    }

    public static void rsa() {
        EntityPlayerSP p = mc.thePlayer;
        int armSwingEnd = p.isPotionActive(Potion.digSpeed) ? 6 - (1 + p.getActivePotionEffect(Potion.digSpeed).getAmplifier()) : (p.isPotionActive(Potion.digSlowdown) ? 6 + (1 + p.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
        if (!p.isSwingInProgress || p.swingProgressInt >= armSwingEnd / 2 || p.swingProgressInt < 0) {
            p.swingProgressInt = -1;
            p.isSwingInProgress = true;
        }

    }

    public static String uf(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static boolean isPlaceable(BlockPos blockPos) {
        return BlockUtils.replaceable(blockPos) || BlockUtils.isFluid(BlockUtils.getBlock(blockPos));
    }

    public static boolean spectatorCheck() {
        return mc.thePlayer.inventory.getStackInSlot(8) != null && mc.thePlayer.inventory.getStackInSlot(8).getDisplayName().contains("Return") || Utils.stripString(((IAccessorGuiIngame) mc.ingameGUI).getDisplayedTitle()).contains("YOU DIED");
    }

    public static boolean holdingWeapon() {
        return holdingWeapon(mc.thePlayer);
    }

    public static boolean holdingWeapon(EntityLivingBase entityLivingBase) {
        if (entityLivingBase.getHeldItem() == null) {
            return false;
        }
        Item getItem = entityLivingBase.getHeldItem().getItem();
        return getItem instanceof ItemSword || (Settings.weaponAxe.isToggled() && getItem instanceof ItemAxe) || (Settings.weaponRod.isToggled() && getItem instanceof ItemFishingRod) || (Settings.weaponStick.isToggled() && getItem == Items.stick);
    }

    public static boolean holdingSword() {
        if (mc.thePlayer.getHeldItem() == null) {
            return false;
        }
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public static double getDamageLevel(ItemStack itemStack) {
        double baseDamage = 0.0;
        if (itemStack != null) {
            for (Map.Entry<String, AttributeModifier> entry : itemStack.getAttributeModifiers().entries()) {
                if (entry.getKey().equals("generic.attackDamage")) {
                    baseDamage = entry.getValue().getAmount();
                    break;
                }
            }
        }
        int sharp_level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
        int fire_level = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack);
        return baseDamage + sharp_level * 1.25 + (fire_level * 4 - 1);
    }

    public static boolean canBePlaced(ItemBlock itemBlock) {
        Block block = itemBlock.getBlock();
        if (block == null) {
            return false;
        }
        if (BlockUtils.isInteractable(block) || block instanceof BlockWeb || block instanceof BlockSapling || block instanceof BlockDaylightDetector || block instanceof BlockBeacon || block instanceof BlockBanner || block instanceof BlockEndPortalFrame || block instanceof BlockEndPortal || block instanceof BlockLever || block instanceof BlockButton || block instanceof BlockSkull || block instanceof BlockLiquid || block instanceof BlockCactus || block instanceof BlockDoublePlant || block instanceof BlockLilyPad || block instanceof BlockCarpet || block instanceof BlockTripWire || block instanceof BlockTripWireHook || block instanceof BlockTallGrass || block instanceof BlockFlower || block instanceof BlockFlowerPot || block instanceof BlockSign || block instanceof BlockLadder || block instanceof BlockTorch || block instanceof BlockRedstoneTorch || block instanceof BlockStairs || block instanceof BlockSlab || block instanceof BlockFence || block instanceof BlockPane || block instanceof BlockStainedGlassPane || block instanceof BlockGravel || block instanceof BlockClay || block instanceof BlockSand || block instanceof BlockSoulSand || block instanceof BlockRailBase) {
            return false;
        }
        return true;
    }

    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String value) {
        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(value)) {
                return enumConstant;
            }
        }
        return null;
    }

    public static int getSpeedAmplifier() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return 1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
        }
        return 0;
    }

    public static ItemStack getSpoofedItem(ItemStack original) {
        return original;
    }

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append('\n');

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static boolean isLobby() {
        if (Utils.isHypixel()) {
            List<String> sidebarLines = Utils.getSidebarLines();
            if (!sidebarLines.isEmpty()) {
                String[] parts = Utils.stripColor(sidebarLines.get(1)).split("  ");
                if (parts.length > 1 && parts[1].charAt(0) == 'L') {
                    return true;
                }
            }
        }
        return false;
    }
}
