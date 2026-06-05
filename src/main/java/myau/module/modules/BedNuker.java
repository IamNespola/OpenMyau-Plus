package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.ChatColors;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.management.RotationState;
import myau.mixin.IAccessorC03PacketPlayer;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BedNuker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long WHITELIST_SCAN_DELAY_MS = 1000L;

    private final TimerUtil timer = new TimerUtil();
    private final ArrayList<BlockPos> bedWhitelist = new ArrayList<BlockPos>();
    private final Map<BlockPos, Float> breakProgressMap = new HashMap<>();
    private final Color colorRed = new Color(ChatColors.RED.toAwtColor());
    private final Color colorYellow = new Color(ChatColors.YELLOW.toAwtColor());
    private final Color colorGreen = new Color(ChatColors.GREEN.toAwtColor());

    private BlockPos targetBed = null;
    private BlockPos[] bedPos = null;
    private BlockPos nearestBlock = null;
    private int savedSlot = -1;
    private int packetSlot = -1;
    private int ticksAfterBreak = 0;
    private float breakProgress = 0.0F;
    private float lastProgress = 0.0F;
    private boolean isBed = false;
    private boolean readyToBreak = false;
    private boolean breaking = false;
    private boolean rotate = false;
    private boolean waitingForStart = false;
    private boolean delayStart = false;
    private long whitelistScanAt = -1L;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL", "INSTANT", "SWAP"});
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final PercentProperty speed = new PercentProperty("speed", 0);
    public final BooleanProperty groundSpeed = new BooleanProperty("ground-spoof", false);
    public final ModeProperty ignoreVelocity = new ModeProperty("ignore-velocity", 0, new String[]{"NONE", "CANCEL", "DELAY"});
    public final BooleanProperty surroundings = new BooleanProperty("surroundings", true);
    public final BooleanProperty toolCheck = new BooleanProperty("tool-check", true);
    public final BooleanProperty whiteList = new BooleanProperty("whitelist", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeProperty showProgress = new ModeProperty("show-progress", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public BedNuker() {
        super("BedNuker", false);
    }

    private void resetBreaking() {
        if (this.targetBed != null && mc.theWorld != null && mc.thePlayer != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.targetBed, -1);
        }
        this.targetBed = null;
        this.bedPos = null;
        this.nearestBlock = null;
        this.breakProgress = 0.0F;
        this.lastProgress = 0.0F;
        this.isBed = false;
        this.readyToBreak = false;
        this.breaking = false;
        this.rotate = false;
        this.ticksAfterBreak = 0;
        this.delayStart = false;
        this.breakProgressMap.clear();
    }

    private void scheduleWhitelistScan() {
        this.whitelistScanAt = System.currentTimeMillis() + WHITELIST_SCAN_DELAY_MS;
    }

    private void runPendingWhitelistScan() {
        if (this.whitelistScanAt == -1L || System.currentTimeMillis() < this.whitelistScanAt) {
            return;
        }
        this.whitelistScanAt = -1L;
        this.bedWhitelist.clear();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        int sX = MathHelper.floor_double(mc.thePlayer.posX);
        int sY = MathHelper.floor_double(mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        int sZ = MathHelper.floor_double(mc.thePlayer.posZ);
        for (int i = sX - 25; i <= sX + 25; i++) {
            for (int j = sY - 25; j <= sY + 25; j++) {
                for (int k = sZ - 25; k <= sZ + 25; k++) {
                    BlockPos blockPos = new BlockPos(i, j, k);
                    Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                    if (block instanceof BlockBed) {
                        this.bedWhitelist.add(blockPos);
                    }
                }
            }
        }
    }

    private float calcProgress() {
        return Math.min(1.0F, this.breakProgress);
    }

    private void restoreSlot() {
        if (this.savedSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.savedSlot;
            this.syncHeldItem();
            this.savedSlot = -1;
        }
        this.packetSlot = -1;
    }

    private void syncHeldItem() {
        int currentPlayerItem = ((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem();
        if (mc.thePlayer.inventory.currentItem != currentPlayerItem) {
            mc.thePlayer.stopUsingItem();
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    private void setSlot(int slot) {
        if (slot == -1 || slot == mc.thePlayer.inventory.currentItem) {
            return;
        }
        if (this.savedSlot == -1) {
            this.savedSlot = mc.thePlayer.inventory.currentItem;
        }
        mc.thePlayer.inventory.currentItem = slot;
        this.syncHeldItem();
    }

    private void setPacketSlot(int slot) {
        if (slot == -1 || slot == this.packetSlot) {
            return;
        }
        PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
        this.packetSlot = slot;
    }

    private boolean hasProperTool(Block block) {
        Material material = block.getMaterial();
        if (material != Material.iron && material != Material.anvil && material != Material.rock) {
            return true;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null) {
                    Item item = stack.getItem();
                    if (item instanceof ItemPickaxe) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private EnumFacing getHitFacing(BlockPos blockPos) {
        double x = (double) blockPos.getX() + 0.5 - mc.thePlayer.posX;
        double y = (double) blockPos.getY() + 0.25 - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
        double z = (double) blockPos.getZ() + 0.5 - mc.thePlayer.posZ;
        float[] rotations = RotationUtil.getRotationsTo(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], 8.0, 1.0F);
        return mop == null ? EnumFacing.UP : mop.sideHit;
    }

    private float getDigSpeed(IBlockState iBlockState, int slot, boolean onGround) {
        ItemStack item = mc.thePlayer.inventory.getStackInSlot(slot);
        float digSpeed = item == null ? 1.0F : item.getItem().getDigSpeed(item, iBlockState);
        if (digSpeed > 1.0F) {
            int enchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, item);
            if (enchantmentLevel > 0) {
                digSpeed += (float) (enchantmentLevel * enchantmentLevel + 1);
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            digSpeed *= 1.0F + (float) (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    digSpeed *= 0.3F;
                    break;
                case 1:
                    digSpeed *= 0.09F;
                    break;
                case 2:
                    digSpeed *= 0.0027F;
                    break;
                default:
                    digSpeed *= 8.1E-4F;
            }
        }
        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            digSpeed /= 5.0F;
        }
        if (!onGround) {
            digSpeed /= 5.0F;
        }
        return digSpeed;
    }

    boolean canHarvest(Block block, int slot) {
        if (block.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            return stack != null && stack.canHarvestBlock(block);
        }
    }

    private float getBreakDelta(IBlockState iBlockState, BlockPos blockPos, int slot, boolean onGround) {
        Block block = iBlockState.getBlock();
        float hardness = block.getBlockHardness(mc.theWorld, blockPos);
        float boost = this.canHarvest(block, slot) ? 30.0F : 100.0F;
        float multiplier = 1.0F + 0.7F * ((float) this.speed.getValue().intValue() / 100.0F);
        return hardness < 0.0F ? 0.0F : this.getDigSpeed(iBlockState, slot, onGround) / hardness / boost * multiplier;
    }

    private float calcBlockStrength(BlockPos blockPos) {
        IBlockState blockState = mc.theWorld.getBlockState(blockPos);
        int slot = ItemUtil.findInventorySlot(mc.thePlayer.inventory.currentItem, blockState.getBlock());
        return this.getBreakDelta(blockState, blockPos, slot, mc.thePlayer.onGround || this.groundSpeed.getValue());
    }

    private BlockPos[] getBedPos() {
        int scanRange = Math.round(this.range.getValue());
        List<BlockPos> targets = new ArrayList<>();
        BlockPos player = mc.thePlayer.getPosition();
        for (int x = player.getX() - scanRange; x <= player.getX() + scanRange; x++) {
            for (int y = player.getY() - scanRange; y <= player.getY() + scanRange; y++) {
                for (int z = player.getZ() - scanRange; z <= player.getZ() + scanRange; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (this.whiteList.getValue() && this.bedWhitelist.contains(pos)) {
                        continue;
                    }
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (state.getBlock() == Blocks.bed && state.getValue((IProperty<?>) BlockBed.PART) == EnumPartType.FOOT
                            && PlayerUtil.canReach(pos, this.range.getValue().doubleValue())) {
                        targets.add(pos);
                    }
                }
            }
        }
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(pos -> pos.distanceSqToCenter(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ)));
        BlockPos foot = targets.get(0);
        IBlockState state = mc.theWorld.getBlockState(foot);
        return new BlockPos[]{foot, foot.offset((EnumFacing) state.getValue((IProperty<?>) BlockBed.FACING))};
    }

    private boolean isCovered(BlockPos blockPos) {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos offset = blockPos.offset(facing);
            Block block = mc.theWorld.getBlockState(offset).getBlock();
            if (BlockUtil.isReplaceable(block) || !block.isFullBlock()) {
                return false;
            }
        }
        return true;
    }

    private BlockPos getBestBlock(BlockPos[] positions, boolean getSurrounding) {
        if (positions == null) {
            return null;
        }
        double maxRangeSquared = this.range.getValue() * this.range.getValue();
        float bestEfficiency = 0.0F;
        BlockPos closestBlock = null;
        for (BlockPos pos : positions) {
            if (pos == null) {
                continue;
            }
            if (getSurrounding) {
                for (EnumFacing facing : EnumFacing.values()) {
                    if (facing == EnumFacing.DOWN) {
                        continue;
                    }
                    BlockPos offset = pos.offset(facing);
                    if (Arrays.asList(positions).contains(offset) || !PlayerUtil.canReach(offset, this.range.getValue().doubleValue())) {
                        continue;
                    }
                    Block block = mc.theWorld.getBlockState(offset).getBlock();
                    if (BlockUtil.isReplaceable(block) || block instanceof BlockBed) {
                        continue;
                    }
                    if (this.toolCheck.getValue() && !this.hasProperTool(block)) {
                        continue;
                    }
                    float efficiency = this.breakProgressMap.containsKey(offset) ? this.breakProgressMap.get(offset) : this.calcBlockStrength(offset);
                    double distance = mc.thePlayer.getDistanceSqToCenter(offset);
                    if (this.betterBlock(distance, efficiency, maxRangeSquared, bestEfficiency)) {
                        maxRangeSquared = distance;
                        bestEfficiency = efficiency;
                        closestBlock = offset;
                    }
                }
            } else if (PlayerUtil.canReach(pos, this.range.getValue().doubleValue())) {
                float efficiency = this.breakProgressMap.containsKey(pos) ? this.breakProgressMap.get(pos) : this.calcBlockStrength(pos);
                double distance = mc.thePlayer.getDistanceSqToCenter(pos);
                if (this.betterBlock(distance, efficiency, maxRangeSquared, bestEfficiency)) {
                    maxRangeSquared = distance;
                    bestEfficiency = efficiency;
                    closestBlock = pos;
                }
            }
        }
        return closestBlock;
    }

    private boolean betterBlock(double distance, float efficiency, double maxRangeSquared, float bestEfficiency) {
        return distance < maxRangeSquared || efficiency > bestEfficiency;
    }

    private void doSwing() {
        if (this.swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    private void startBreak(BlockPos blockPos) {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, blockPos, this.getHitFacing(blockPos)));
    }

    private void stopBreak(BlockPos blockPos) {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, blockPos, this.getHitFacing(blockPos)));
    }

    private void breakBlock(BlockPos blockPos) {
        if (blockPos == null || !PlayerUtil.canReach(blockPos, this.range.getValue().doubleValue()) || mc.theWorld.isAirBlock(blockPos)) {
            return;
        }
        this.targetBed = blockPos;
        this.isBed = mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockBed;
        this.readyToBreak = true;
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        int slot = ItemUtil.findInventorySlot(mc.thePlayer.inventory.currentItem, block);
        if (this.mode.getValue() == 1) {
            this.rotate = true;
            this.doSwing();
            this.startBreak(blockPos);
            this.setSlot(slot);
            this.stopBreak(blockPos);
            this.restoreSlot();
            this.timer.reset();
            this.resetBreaking();
            return;
        }
        if (this.breakProgress == 0.0F) {
            this.restoreSlot();
            this.rotate = true;
            this.breaking = true;
            if (this.mode.getValue() == 0) {
                this.setSlot(slot);
            }
            this.doSwing();
            this.startBreak(blockPos);
        } else if (this.breakProgress >= 1.0F) {
            if (this.mode.getValue() == 2) {
                this.setPacketSlot(slot);
            }
            this.stopBreak(blockPos);
            this.doSwing();
            IBlockState blockState = mc.theWorld.getBlockState(blockPos);
            if (blockState.getBlock().getMaterial() != Material.air) {
                mc.theWorld.playAuxSFX(2001, blockPos, Block.getStateId(blockState));
                mc.theWorld.setBlockToAir(blockPos);
            }
            this.delayStart = true;
            this.breakProgressMap.remove(blockPos);
            this.timer.reset();
            this.resetBreaking();
            return;
        } else if (this.mode.getValue() == 0) {
            this.rotate = true;
        }
        float progress = this.getBreakDelta(mc.theWorld.getBlockState(blockPos), blockPos,
                this.mode.getValue() == 2 && slot != -1 ? slot : mc.thePlayer.inventory.currentItem,
                mc.thePlayer.onGround || this.groundSpeed.getValue());
        if (this.lastProgress != 0.0F && this.breakProgress >= this.lastProgress) {
            this.breaking = true;
        }
        this.breakProgress += progress;
        this.breakProgressMap.put(blockPos, this.breakProgress);
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int) (this.breakProgress * 10.0F) - 1);
        this.lastProgress = 0.0F;
        while (this.lastProgress + progress < 1.0F) {
            this.lastProgress += progress;
        }
    }

    private Color getProgressColor(int mode) {
        switch (mode) {
            case 1:
                float progress = this.calcProgress();
                if (progress <= 0.5F) {
                    return ColorUtil.interpolate(progress / 0.5F, this.colorRed, this.colorYellow);
                }
                return ColorUtil.interpolate((progress - 0.5F) / 0.5F, this.colorYellow, this.colorGreen);
            case 2:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    public boolean isReady() {
        return this.targetBed != null && this.readyToBreak;
    }

    public boolean isBreaking() {
        return this.targetBed != null && this.breaking;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        this.runPendingWhitelistScan();
        if (!this.isEnabled()) {
            return;
        }
        AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn.isEnabled()) {
            return;
        }
        if (!mc.thePlayer.capabilities.allowEdit || mc.thePlayer.isSpectator()) {
            this.restoreSlot();
            this.resetBreaking();
            return;
        }
        if (this.delayStart) {
            if (this.ticksAfterBreak++ <= 0) {
                return;
            }
            this.restoreSlot();
            this.delayStart = false;
            this.ticksAfterBreak = 0;
        }
        if (this.bedPos == null) {
            if (this.timer.hasTimeElapsed(200)) {
                this.bedPos = this.getBedPos();
                this.timer.reset();
            }
            if (this.bedPos == null) {
                this.restoreSlot();
                this.resetBreaking();
                Myau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
                return;
            }
        } else if (!(mc.theWorld.getBlockState(this.bedPos[0]).getBlock() instanceof BlockBed)
                || this.targetBed != null && mc.theWorld.isAirBlock(this.targetBed)) {
            this.restoreSlot();
            this.resetBreaking();
            return;
        }
        if (this.surroundings.getValue() && this.isCovered(this.bedPos[0]) && this.isCovered(this.bedPos[1])) {
            if (this.nearestBlock == null) {
                this.nearestBlock = this.getBestBlock(this.bedPos, true);
            }
            this.breakBlock(this.nearestBlock);
        } else {
            this.nearestBlock = null;
            this.restoreSlot();
            BlockPos bestBlock = this.getBestBlock(this.bedPos, false);
            this.breakBlock(bestBlock != null ? bestBlock : this.bedPos[0]);
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled()) {
                return;
            }
            if ((this.rotate || this.breakProgress >= 1.0F || this.breakProgress == 0.0F) && this.targetBed != null) {
                double x = (double) this.targetBed.getX() + 0.5 - mc.thePlayer.posX;
                double y = (double) this.targetBed.getY() + 0.5 - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                double z = (double) this.targetBed.getZ() + 0.5 - mc.thePlayer.posZ;
                float[] rotations = RotationUtil.getRotationsTo(x, y, z, event.getYaw(), event.getPitch());
                event.setRotation(rotations[0], rotations[1], 5);
                event.setPervRotation(this.moveFix.getValue() != 0 ? rotations[0] : mc.thePlayer.rotationYaw, 5);
                this.rotate = false;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 5.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled() && !event.isCancelled() && !(event.getY() <= 0.0)) {
            if (this.ignoreVelocity.getValue() == 1 && this.targetBed != null) {
                event.setCancelled(true);
                event.setX(mc.thePlayer.motionX);
                event.setY(mc.thePlayer.motionY);
                event.setZ(mc.thePlayer.motionZ);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.targetBed != null && (!this.isBed || !this.surroundings.getValue())) {
            if (this.showProgress.getValue() != 0) {
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                float scale = hud.scale.getValue();
                String text = String.format("%d%%", (int) (this.calcProgress() * 100.0F));
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 0.0F);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                int width = mc.fontRendererObj.getStringWidth(text);
                mc.fontRendererObj.drawString(
                        text,
                        (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / scale - (float) width / 2.0F,
                        (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 2.0F / scale,
                        this.getProgressColor(this.showProgress.getValue()).getRGB() & 16777215 | -1090519040,
                        hud.shadow.getValue()
                );
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.targetBed != null && !mc.theWorld.isAirBlock(this.targetBed)) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.targetBed, (int) (this.calcProgress() * 10.0F) - 1);
            if (this.showTarget.getValue() != 0) {
                BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
                Color color = this.getProgressColor(this.showTarget.getValue());
                RenderUtil.enableRenderState();
                double newHeight = this.isBed ? bedESP.getHeight() : 1.0;
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                RenderUtil.drawBlockBox(this.targetBed, newHeight, r, b, g);
                RenderUtil.disableRenderState();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waitingForStart = false;
        this.whitelistScanAt = -1L;
        this.bedWhitelist.clear();
        this.restoreSlot();
        this.resetBreaking();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            if (event.getPacket() instanceof S02PacketChat) {
                String text = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
                if (text.contains("§e§lProtect your bed and destroy the enemy bed") || text.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                    this.waitingForStart = true;
                }
            }
            if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForStart) {
                this.waitingForStart = false;
                this.bedWhitelist.clear();
                this.scheduleWhitelistScan();
            }
            if (this.isEnabled() && this.targetBed != null && this.groundSpeed.getValue() && !mc.thePlayer.isInWater()
                    && event.getPacket() instanceof C03PacketPlayer) {
                ((IAccessorC03PacketPlayer) event.getPacket()).setOnGround(true);
            }
            if (this.isEnabled() && this.targetBed != null && this.ignoreVelocity.getValue() == 2 && Myau.delayManager.getDelayModule() != DelayModules.BED_NUKER) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                    if (packet.getEntityID() == mc.thePlayer.getEntityId() && packet.getMotionY() > 0) {
                        Myau.delayManager.delay(DelayModules.BED_NUKER);
                        Myau.delayManager.delayedPacket.offer(packet);
                        event.setCancelled(true);
                    }
                }
                if (event.getPacket() instanceof S27PacketExplosion) {
                    S27PacketExplosion explosion = (S27PacketExplosion) event.getPacket();
                    if (explosion.func_149149_c() != 0.0F || explosion.func_149144_d() != 0.0F || explosion.func_149147_e() != 0.0F) {
                        Myau.delayManager.delay(DelayModules.BED_NUKER);
                        Myau.delayManager.delayedPacket.offer(explosion);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            if (this.isReady() || this.targetBed != null && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isReady()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            if (this.isReady() || this.targetBed != null && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled() && this.savedSlot != -1) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisabled() {
        this.restoreSlot();
        this.resetBreaking();
        this.waitingForStart = false;
        this.whitelistScanAt = -1L;
        this.bedWhitelist.clear();
        Myau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
