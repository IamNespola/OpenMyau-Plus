package myau.module.modules.combat;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.google.common.base.CaseFormat;

import lombok.Getter;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.CancelUseEvent;
import myau.events.HitBlockEvent;
import myau.events.LeftClickMouseEvent;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.RightClickMouseEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.mixin.IAccessorRenderManager;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.misc.BedNuker;
import myau.module.modules.movement.NoSlow;
import myau.module.modules.player.AutoHeal;
import myau.module.modules.player.Scaffold;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.util.rotation.Rotation;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings.GameType;

public class KillAura extends Module {
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final long ROTATION_UPDATE_INTERVAL_MS = 50;
    public final ModeProperty cpsMode;
    public final ModeProperty mode;
    public final ModeProperty sort;
    public final ModeProperty autoBlock;
    public final BooleanProperty autoBlockRequirePress;
    public final FloatProperty autoBlockCPS;
    public final FloatProperty autoBlockRange;
    public final FloatProperty swingRange;
    public final FloatProperty attackRange;
    public final IntProperty fov;
    public final IntProperty minCPS;
    public final IntProperty maxCPS;
    public final IntProperty switchDelay;
    public final ModeProperty rotations;
    public final FloatProperty deadZoneSize;
    public final FloatProperty maxTurnSpeed;
    public final FloatProperty minTurnSpeed;
    public final FloatProperty acceleration;
    public final FloatProperty deceleration;
    public final BooleanProperty useOvershoot;
    public final FloatProperty overshootStrength;
    public final FloatProperty overshootRecovery;
    public final FloatProperty noiseStrength;
    public final BooleanProperty visualizeAim;
    public final BooleanProperty smoothBack;
    public final ModeProperty moveFix;
    public final PercentProperty smoothing;
    public final IntProperty angleStep;
    public final BooleanProperty throughWalls;
    public final BooleanProperty requirePress;
    public final BooleanProperty allowMining;
    public final BooleanProperty weaponsOnly;
    public final BooleanProperty allowTools;
    public final BooleanProperty inventoryCheck;
    public final BooleanProperty botCheck;
    public final BooleanProperty players;
    public final BooleanProperty bosses;
    public final BooleanProperty mobs;
    public final BooleanProperty animals;
    public final BooleanProperty golems;
    public final BooleanProperty silverfish;
    public final BooleanProperty teams;
    public final ModeProperty showTarget;
    public final ModeProperty debugLog;
    public final BooleanProperty randomize;
    public final FloatProperty randomizeRange;
    public final FloatProperty yRandomizeStrength;
    public final FloatProperty liquidBounceHorizontalSpeed;
    public final FloatProperty liquidBounceVerticalSpeed;
    public final FloatProperty liquidBounceSmoothFactor;
    public final BooleanProperty liquidBouncePredict;
    public final FloatProperty liquidBouncePredictSize;
    public final BooleanProperty liquidBounceRandomize;
    public final FloatProperty liquidBounceRandomizeRange;
    public final FloatProperty liquidBounceHorizontalSearch;
    public final FloatProperty liquidBounceBodyPointMin;
    public final FloatProperty liquidBounceBodyPointMax;
    private final int[] clickPattern = {16, 22, 14, 46, 18, 8, 8, 63, 25, 25, 12, 39, 26, 18, 6, 62, 26, 18, 21, 40, 26, 8, 16, 46, 26, 20, 15, 50, 25, 10, 11, 43, 25, 11, 37, 39, 25, 12, 18, 54, 25, 25, 15, 41, 27, 9, 1, 66, 26, 17, 21, 48, 27, 8, 6, 62, 28, 19, 13, 47, 26, 7, 14, 53, 27, 16, 29, 38, 27, 8, 6, 60, 27, 22, 19, 45, 26, 10, 10, 62, 25, 20, 28, 22, 26, 19, 11, 57, 26, 16, 32, 36, 26, 9, 9, 66, 27, 19, 27, 38, 26, 9, 10, 61, 26, 25, 15, 34, 26, 20, 10, 52, 26, 22, 28, 29, 27, 8, 3, 63, 26, 21, 27, 38, 26, 10, 11, 38, 27, 15, 31, 39, 25, 13, 10, 45, 27, 14, 27, 40, 26, 10, 6, 51, 26, 18, 31, 27, 27, 11, 14, 47, 26, 23, 21, 35, 26, 12, 13, 41, 26, 15, 31, 36, 27, 16, 9, 44, 27, 14, 30, 39, 25, 14, 10, 46, 28, 10, 24, 45, 26, 7, 5, 46, 26, 20, 6, 50, 26, 8, 6, 51, 26, 17, 20, 40, 27, 25, 1, 32, 26, 20, 9, 46, 25, 15, 12, 30, 26, 11, 25, 46, 27, 13, 10, 36, 27, 20, 15, 41, 26, 8, 6, 41, 26, 12, 29, 44, 26, 13, 11, 44, 26, 12, 27, 36, 26, 23, 4, 39, 26, 24, 12, 47, 26, 9, 2, 65, 26, 16, 27, 34, 26, 25, 0, 53, 26, 16, 3, 47, 27, 16, 10, 41, 26, 18, 25, 38, 26, 11, 10, 50, 27, 20, 20, 29, 26, 11, 7, 66, 26, 20, 18, 31, 26, 21, 21, 28, 26, 21, 29, 25, 27, 15, 12, 43, 28, 11, 31, 32, 27, 23, 0, 49, 27, 20, 30, 30, 25, 32, 0, 50, 26, 12, 25, 34, 27, 11, 11, 44, 27, 23, 26, 25, 27, 16, 11, 46, 26, 13, 32, 35, 28, 9, 5, 48, 26, 21, 29, 37, 26, 10, 7, 48, 27, 20, 21, 41, 24, 7, 18, 46, 25, 22, 22, 33, 25, 10, 5, 59, 26, 21, 19, 29, 26, 11, 10, 46, 25, 22, 29, 31, 25, 11, 12, 50, 24, 20, 28, 40, 25, 10, 4, 56, 25, 16, 36, 30, 24, 10, 9, 63, 25, 22, 22, 32, 25, 9, 8, 58, 27, 10, 43, 30, 26, 8, 3, 60, 26, 24, 14, 42, 26, 12, 9, 49, 25, 11, 32, 38, 27, 8, 8, 50, 26, 20, 26, 32, 25, 10, 4, 66, 25, 18, 28, 24, 26, 10, 8, 54, 25, 16, 32, 34, 24, 9, 12, 54, 25, 18, 18, 41, 28, 9, 16, 50, 28, 15, 21, 46, 27, 9, 8, 49, 26, 21, 18, 36, 26, 15, 10, 54, 27, 22, 27, 32, 25, 9, 15, 48, 28, 19, 26, 35, 27, 9, 13, 48, 25, 21, 23, 33, 27, 8, 3, 65, 26, 19, 23, 39, 25, 9, 13, 44, 26, 25, 19, 35, 26, 14, 6, 63, 27, 15, 23, 32, 28, 8, 2, 65, 26, 19, 24, 34, 27, 12, 0, 49, 26, 21, 34, 34, 26, 8, 9, 60, 26, 23, 19, 34, 26, 10, 5, 59, 26, 12, 36, 39, 26, 11, 11, 44, 26, 25, 5, 47, 25, 9, 10, 49, 27, 19, 24, 31, 26, 10, 4, 60, 27, 25, 9, 41, 26, 20, 7, 54, 24, 11, 35, 35, 26, 9, 5, 67, 26, 17, 19, 43, 26, 24, 17, 39, 25, 16, 11, 45, 25, 9, 3, 60, 25, 25, 16, 37, 28, 9, 5, 55, 26, 15, 12, 49, 25, 17, 8, 39, 25, 15, 16, 48, 25, 12, 9, 37, 25, 17, 31, 38, 27, 8, 8, 62, 26, 23, 14, 38, 27, 16, 10, 45, 26, 13, 25, 42, 25, 9, 8, 57, 27, 12, 36, 38, 27, 13, 11, 30, 27, 21, 24, 47, 25, 10, 6, 54, 26, 13, 28, 42, 25, 10, 5, 47, 26, 21, 22, 44, 26, 10, 8, 50, 28, 17, 26, 33, 26, 10, 14, 55, 27, 14, 30, 29, 25, 13, 1, 70, 26, 14, 30, 26, 27, 12, 14, 67, 25, 21, 4, 33, 25, 11, 5, 48, 26, 21, 21, 39, 25, 11, 1, 55, 26, 11, 29, 32, 26, 12, 10, 50, 27, 16, 26, 36, 27, 23, 3, 57, 27, 11, 23, 37, 26, 9, 16, 37, 26, 16, 38, 37, 26, 9, 2, 60, 27, 22, 16, 38, 27, 9, 5, 53, 26, 14, 33, 30, 25, 13, 11, 46, 25, 23, 22, 43, 24, 10, 13, 51, 25, 21, 25, 35, 27, 8, 16, 48, 25, 21, 19, 42, 25, 12, 12, 49, 26, 21, 18, 42, 25, 12, 13, 51, 27, 16, 25, 37, 26, 11, 12, 47, 27, 21, 13, 39, 27, 5, 9, 61, 25, 24, 11, 39, 26, 10, 9, 52, 26, 15, 33, 28, 38, 0, 9, 55, 26, 14, 39, 24, 25, 10, 9, 52, 27, 13, 29, 36, 25, 12, 9, 49, 25, 22, 30, 26, 26, 10, 2, 66, 27, 17, 30, 31, 26, 14, 7, 64, 28, 16, 31, 28, 24, 13, 14, 54, 25, 12, 29, 35, 27, 10, 8, 49, 27, 18, 26, 38, 25, 8, 14, 46, 26, 23, 15, 36, 26, 11, 5, 61, 27, 23, 8, 42, 25, 9, 10, 57, 26, 11, 29, 37, 25, 11, 9, 56, 27, 11, 32, 35, 26, 12, 6, 62, 27, 20, 33, 27, 27, 10, 14, 50, 27, 17, 28, 40, 25, 9, 8, 46, 26, 23, 16, 44, 26, 11, 13, 47, 28, 19, 19, 36, 26, 8, 7, 55, 26, 15, 24, 39, 26, 12, 9, 56, 26, 15, 28, 36, 25, 10, 10, 51, 25, 17, 32, 36, 25, 9, 7, 58, 26, 11, 31, 32, 26, 7, 14, 57, 26, 13, 22, 25, 24, 9, 14, 42, 26, 12, 27, 31, 25, 9, 2, 62, 27, 23, 12, 33, 26, 8, 18, 46, 25, 24, 14, 33, 24, 10, 14, 50, 25, 20, 21, 38, 26, 9, 1, 61, 25, 11, 30, 35, 26, 10, 10, 53, 25, 18, 22, 35, 25, 8, 4, 44, 25, 25, 21, 37, 24, 13, 6, 35, 27, 11, 34, 32, 25, 9, 10, 51, 26, 17, 18, 31, 24, 11, 8, 53, 26, 16, 30, 35, 26, 8, 10, 60, 25, 11, 32, 29, 25, 22, 2, 53, 26, 16, 30, 33, 27, 9, 11, 57, 25, 13, 32, 30, 25, 14, 10, 67, 24, 21, 29, 35, 27, 8, 12, 70, 26, 14, 19, 42, 27, 22, 0, 57, 27, 12, 31, 33, 25, 9, 12, 62, 27, 23, 14, 43, 25, 11, 2, 71, 28, 12, 33, 31, 27, 8, 12, 71, 26, 15, 23, 42, 28, 9, 8, 63, 26, 22, 22, 37, 27, 7, 4, 78, 27, 20, 26, 34, 25, 9, 15, 64, 27, 21, 23, 32, 26, 12, 11, 77, 25, 11, 32, 29, 26, 9, 15, 63, 27, 19, 23, 38, 26, 10, 15, 57, 26, 14, 37, 14, 26, 18, 6, 67, 26, 13, 31, 33, 26, 19, 1, 60, 27, 25, 22, 24, 27, 22, 2, 55, 26, 13, 25, 34, 26, 24, 0, 68, 25, 20, 22, 31, 25, 11, 4, 80, 24, 22, 22, 29, 26, 16, 8, 81, 25, 11, 22, 38, 27, 10, 11, 50, 27, 18, 35, 32, 26, 10, 5, 76, 26, 23, 22, 30, 24, 21, 8, 67, 27, 24, 16, 42, 27, 8, 3};
    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();
    public AttackData target = null;
    private Rotation serverRotation = new Rotation(0, 0);
    private boolean isSmoothBacking = false;
    private boolean wantsToDisable = false;
    private Vec3 currentAimVec = null;
    private int patternIndex = 0;
    private int switchTick = 0;
    private boolean hitRegistered = false;
    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private boolean blinkReset = false;
    private long attackDelayMS = 0L;
    private int blockTick = 0;
    private int lastTickProcessed;
    private long lastRotationUpdateTime = 0;

    public KillAura() {
        super("KillAura", Category.COMBAT, false, false);
        this.lastTickProcessed = 0;

        // 新增CPS模式属性
        this.cpsMode = new ModeProperty("CPS Mode", 0, new String[]{"Normal", "Record"});

        this.mode = new ModeProperty("Mode", 1, new String[]{"Single", "Switch"});
        this.sort = new ModeProperty("Sort", 1, new String[]{"Distance", "Health", "HurtTime", "FOV"});
        this.autoBlock = new ModeProperty("auto-block", 3, new String[]{"NONE", "VANILLA", "SPOOF", "HYPIXEL", "BLINK", "INTERACT", "SWAP", "LEGIT", "FAKE"});
        this.autoBlockCPS = new FloatProperty("AutoBlockCPS", 8.0F, 1.0F, 10.0F);
        this.autoBlockRequirePress = new BooleanProperty("AutoBlockRequirePress", false);
        this.autoBlockRange = new FloatProperty("AutoBlockRange", 6.0F, 3.0F, 8.0F);
        this.swingRange = new FloatProperty("SwingRange", 3.5F, 3.0F, 6.0F);
        this.attackRange = new FloatProperty("AttackRange", 3.0F, 3.0F, 6.0F);
        this.fov = new IntProperty("FOV", 360, 30, 360);
        this.minCPS = new IntProperty("MinCPS", 14, 1, 20);
        this.maxCPS = new IntProperty("MaxCPS", 14, 1, 20);
        this.switchDelay = new IntProperty("SwitchDelay", 150, 0, 1000);
        this.rotations = new ModeProperty("Rotations", 2, new String[]{"NONE", "Legit", "Silent", "LockView", "LiquidBounce"});
        this.deadZoneSize = new FloatProperty("DeadZone", 0.5F, 0.0F, 2.0F, () -> rotations.getValue() == 4);
        this.maxTurnSpeed = new FloatProperty("MaxSpeed", 25.0F, 5.0F, 180.0F, () -> rotations.getValue() == 4);
        this.minTurnSpeed = new FloatProperty("MinSpeed", 5.0F, 1.0F, 90.0F, () -> rotations.getValue() == 4);
        this.acceleration = new FloatProperty("Acceleration", 2.5F, 0.1F, 10.0F, () -> rotations.getValue() == 4);
        this.deceleration = new FloatProperty("Deceleration", 1.5F, 0.1F, 10.0F, () -> rotations.getValue() == 4);
        this.useOvershoot = new BooleanProperty("Overshoot", true, () -> rotations.getValue() == 4);
        this.overshootStrength = new FloatProperty("OverStr", 5.0F, 0.0F, 20.0F, () -> rotations.getValue() == 4 && useOvershoot.getValue());
        this.overshootRecovery = new FloatProperty("OverRecov", 0.2F, 0.01F, 1.0F, () -> rotations.getValue() == 4 && useOvershoot.getValue());
        this.noiseStrength = new FloatProperty("Noise", 0.2F, 0.0F, 2.0F, () -> rotations.getValue() == 4);
        this.randomize = new BooleanProperty("Randomize", true, () -> rotations.getValue() == 4);
        this.randomizeRange = new FloatProperty("RandomRange", 0.4F, 0.0F, 1.0F, () -> rotations.getValue() == 4 && randomize.getValue());
        this.yRandomizeStrength = new FloatProperty("YRandomize", 0.3F, 0.0F, 1.0F, () -> rotations.getValue() == 4 && randomize.getValue());
        this.visualizeAim = new BooleanProperty("VisualizeAim", true, () -> rotations.getValue() == 4);
        this.smoothBack = new BooleanProperty("SmoothBack", true, () -> rotations.getValue() == 4);
        this.moveFix = new ModeProperty("MoveFix", 1, new String[]{"NONE", "Silent", "Strict"});
        this.smoothing = new PercentProperty("Smoothing", 0);
        this.angleStep = new IntProperty("AngleStep", 90, 30, 180);
        this.throughWalls = new BooleanProperty("ThroughWalls", true);
        this.requirePress = new BooleanProperty("RequirePress", false);
        this.allowMining = new BooleanProperty("AllowMining", true);
        this.weaponsOnly = new BooleanProperty("WeaponsOnly", true);
        this.allowTools = new BooleanProperty("AllowTools", false, this.weaponsOnly::getValue);
        this.inventoryCheck = new BooleanProperty("InventoryCheck", true);
        this.botCheck = new BooleanProperty("BotCheck", true);
        this.players = new BooleanProperty("Players", true);
        this.bosses = new BooleanProperty("Bosses", false);
        this.mobs = new BooleanProperty("Mobs", false);
        this.animals = new BooleanProperty("Animals", false);
        this.golems = new BooleanProperty("Golems", false);
        this.silverfish = new BooleanProperty("Silverfish", false);
        this.teams = new BooleanProperty("Teams", true);
        this.showTarget = new ModeProperty("ShowTarget", 0, new String[]{"NONE", "Default"});
        this.debugLog = new ModeProperty("Debug", 0, new String[]{"NONE", "Health"});

        this.liquidBounceHorizontalSpeed = new FloatProperty("LB-HSpeed", 180.0F, 1.0F, 180.0F, () -> rotations.getValue() == 4);
        this.liquidBounceVerticalSpeed = new FloatProperty("LB-VSpeed", 180.0F, 1.0F, 180.0F, () -> rotations.getValue() == 4);
        this.liquidBounceSmoothFactor = new FloatProperty("LB-Smooth", 0.5F, 0.1F, 1.0F, () -> rotations.getValue() == 4);
        this.liquidBouncePredict = new BooleanProperty("LB-Predict", true, () -> rotations.getValue() == 4);
        this.liquidBouncePredictSize = new FloatProperty("LB-PredictSize", 1.0F, 0.0F, 3.0F, () -> rotations.getValue() == 4 && liquidBouncePredict.getValue());
        this.liquidBounceRandomize = new BooleanProperty("LB-Randomize", true, () -> rotations.getValue() == 4);
        this.liquidBounceRandomizeRange = new FloatProperty("LB-RandomRange", 0.5F, 0.0F, 1.0F, () -> rotations.getValue() == 4 && liquidBounceRandomize.getValue());
        this.liquidBounceHorizontalSearch = new FloatProperty("LB-HSearch", 0.5F, 0.0F, 1.0F, () -> rotations.getValue() == 4);
        this.liquidBounceBodyPointMin = new FloatProperty("LB-BodyMin", 0.1F, 0.0F, 1.0F, () -> rotations.getValue() == 4);
        this.liquidBounceBodyPointMax = new FloatProperty("LB-BodyMax", 0.9F, 0.0F, 1.0F, () -> rotations.getValue() == 4);
    }

    private long getAttackDelay() {
        if (this.isBlocking) {
            return (long) (1000.0F / this.autoBlockCPS.getValue());
        } else {
            if (this.cpsMode.getValue() == 1) {
                if (patternIndex >= clickPattern.length) {
                    patternIndex = 0;
                }
                return clickPattern[patternIndex];
            } else {
                return 1000L / RandomUtil.nextLong(this.minCPS.getValue(), this.maxCPS.getValue());
            }
        }
    }

    private boolean performAttack(float yaw, float pitch) {
        if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
            if (this.isPlayerBlocking() && this.autoBlock.getValue() != 1) {
                return false;
            } else if (this.attackDelayMS > 0L) {
                return false;
            } else {
                if (this.rotations.getValue() == 4) {
                    MovingObjectPosition intercept = RotationUtil.rayTrace(
                            this.target.getBox(),
                            yaw,
                            pitch,
                            this.attackRange.getValue()
                    );
                    if (intercept == null) {
                        return false;
                    }
                }

                this.attackDelayMS = this.getAttackDelay();
                mc.thePlayer.swingItem();

                if (this.cpsMode.getValue() == 1) {
                    patternIndex = (patternIndex + 1) % clickPattern.length;
                }

                if (this.rotations.getValue() != 4 && this.rotations.getValue() != 0) {
                    if (!this.isBoxInAttackRange(this.target.getBox())
                            && RotationUtil.rayTrace(this.target.getBox(), yaw, pitch, this.attackRange.getValue()) == null) {
                        return false;
                    }
                }
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.ATTACK));
                if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
                    PlayerUtil.attackEntity(this.target.getEntity());
                }
                this.hitRegistered = true;
                return true;
            }
        } else {
            return false;
        }
    }

    private void sendUseItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        this.startBlock(mc.thePlayer.getHeldItem());
    }

    private void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        this.blockingState = true;
    }

    private void stopBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.blockingState = false;
    }

    private void interactAttack(float yaw, float pitch) {
        if (this.target != null) {
            MovingObjectPosition mop = RotationUtil.rayTrace(this.target.getBox(), yaw, pitch, 8.0);
            if (mop != null) {
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(
                        new C02PacketUseEntity(
                                this.target.getEntity(),
                                new Vec3(mop.hitVec.xCoord - this.target.getX(), mop.hitVec.yCoord - this.target.getY(), mop.hitVec.zCoord - this.target.getZ())
                        )
                );
                PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.INTERACT));
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                this.blockingState = true;
            }
        }
    }

    private boolean canAttack() {
        if (this.inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        } else if (!(Boolean) this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
                return false;
            } else if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
                return false;
            } else {
                AutoHeal autoHeal = (AutoHeal) Myau.moduleManager.modules.get(AutoHeal.class);
                if (autoHeal.isEnabled() && autoHeal.isSwitching()) {
                    return false;
                } else {
                    BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
                    if (bedNuker.isEnabled() && bedNuker.isReady()) {
                        return false;
                    } else if (Myau.moduleManager.modules.get(Scaffold.class).isEnabled()) {
                        return false;
                    } else if (this.requirePress.getValue()) {
                        return PlayerUtil.isAttacking();
                    } else {
                        return !this.allowMining.getValue() || !mc.objectMouseOver.typeOfHit.equals(MovingObjectType.BLOCK) || !PlayerUtil.isAttacking();
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean canAutoBlock() {
        if (!ItemUtil.isHoldingSword()) {
            return false;
        } else {
            return !this.autoBlockRequirePress.getValue() || PlayerUtil.isUsingItem();
        }
    }

    private boolean hasValidTarget() {
        return mc.theWorld
                .loadedEntityList
                .stream()
                .anyMatch(
                        entity -> entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                                && this.isInBlockRange((EntityLivingBase) entity)
                );
    }

    private boolean isValidTarget(EntityLivingBase entityLivingBase) {
        if (!mc.theWorld.loadedEntityList.contains(entityLivingBase)) {
            return false;
        } else if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.thePlayer.ridingEntity) {
            if (entityLivingBase == mc.getRenderViewEntity() || entityLivingBase == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityLivingBase.deathTime > 0) {
                return false;
            } else if (RotationUtil.angleToEntity(entityLivingBase) > this.fov.getValue().floatValue()) {
                return false;
            } else if (!this.throughWalls.getValue() && RotationUtil.rayTrace(entityLivingBase) != null) {
                return false;
            } else if (entityLivingBase instanceof EntityOtherPlayerMP) {
                if (!this.players.getValue()) {
                    return false;
                } else if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                    return false;
                } else {
                    return (!this.teams.getValue() || !TeamUtil.isSameTeam((EntityPlayer) entityLivingBase)) && (!this.botCheck.getValue() || !TeamUtil.isBot((EntityPlayer) entityLivingBase));
                }
            } else if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
                return this.bosses.getValue();
            } else if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
                if (entityLivingBase instanceof EntityAnimal
                        || entityLivingBase instanceof EntityBat
                        || entityLivingBase instanceof EntitySquid
                        || entityLivingBase instanceof EntityVillager) {
                    return this.animals.getValue();
                } else if (!(entityLivingBase instanceof EntityIronGolem)) {
                    return false;
                } else {
                    return this.golems.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
                }
            } else if (!(entityLivingBase instanceof EntitySilverfish)) {
                return this.mobs.getValue();
            } else {
                return this.silverfish.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
            }
        } else {
            return false;
        }
    }

    private boolean isInRange(EntityLivingBase entityLivingBase) {
        return this.isInBlockRange(entityLivingBase) || this.isInSwingRange(entityLivingBase) || this.isInAttackRange(entityLivingBase);
    }

    private boolean isInBlockRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.autoBlockRange.getValue();
    }

    private boolean isInSwingRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.swingRange.getValue();
    }

    private boolean isBoxInSwingRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.swingRange.getValue();
    }

    private boolean isInAttackRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.attackRange.getValue();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.attackRange.getValue();
    }

    private boolean isPlayerTarget(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer) entityLivingBase);
    }

    private int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && !stack.hasDisplayName()) {
                    return i;
                }
            }
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    private int findSwordSlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
                if (item != null && item.getItem() instanceof ItemSword) {
                    return i;
                }
            }
        }
        return -1;
    }

    public EntityLivingBase getTarget() {
        return this.target != null ? this.target.getEntity() : null;
    }

    public boolean isAttackAllowed() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (!this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            return !this.requirePress.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        } else {
            return false;
        }
    }

    public boolean shouldAutoBlock() {
        if (this.isPlayerBlocking() && this.isBlocking) {
            return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && (this.autoBlock.getValue() == 3
                    || this.autoBlock.getValue() == 4
                    || this.autoBlock.getValue() == 5
                    || this.autoBlock.getValue() == 6
                    || this.autoBlock.getValue() == 7);
        } else {
            return false;
        }
    }

    public boolean isBlocking() {
        return this.fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || this.blockingState) && ItemUtil.isHoldingSword();
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST && this.blinkReset) {
            this.blinkReset = false;
            Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            Myau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
        }
        if (this.isEnabled() || this.wantsToDisable) {
            if (event.getType() == EventType.PRE) {
                if (this.wantsToDisable) {
                    if (this.smoothBack.getValue()) {
                        Rotation currentRot = this.serverRotation;
                        Rotation playerRot = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                            Rotation nextRot = getSmoothBackRotation(currentRot, playerRot);
                            this.serverRotation = nextRot;
                            event.setRotation(nextRot.yaw, nextRot.pitch, 1);
                            if (this.moveFix.getValue() != 0) {
                                event.setPervRotation(nextRot.yaw, 1);
                            }
                            mc.thePlayer.rotationYawHead = nextRot.yaw;
                            mc.thePlayer.renderYawOffset = nextRot.yaw;
                            return;
                        } else {
                            this.wantsToDisable = false;
                            this.setEnabled(false);
                            return;
                        }
                    } else {
                        this.wantsToDisable = false;
                        this.setEnabled(false);
                        return;
                    }
                }
                if (this.attackDelayMS > 0L) {
                    this.attackDelayMS -= 50L;
                }
                boolean attack = this.target != null && this.canAttack();
                boolean block = attack && this.canAutoBlock();
                if (!block) {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                    this.blockTick = 0;
                }
                if (attack) {
                    boolean swap = false;
                    boolean blocked = false;
                    if (block) {
                        switch (this.autoBlock.getValue()) {
                            case 0:
                                if (PlayerUtil.isUsingItem()) {
                                    this.isBlocking = true;
                                    if (!this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        swap = true;
                                    }
                                } else {
                                    this.isBlocking = false;
                                    if (this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        this.stopBlock();
                                    }
                                }
                                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.fakeBlockState = false;
                                break;
                            case 1:
                                if (this.hasValidTarget()) {
                                    if (!this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        swap = true;
                                    }
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 2:
                                if (this.hasValidTarget()) {
                                    int item = ((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem();
                                    if (Myau.playerStateManager.digging || Myau.playerStateManager.placing || mc.thePlayer.inventory.currentItem != item || this.isPlayerBlocking() && this.blockTick != 0 || this.attackDelayMS > 0L && this.attackDelayMS <= 50L) {
                                        this.blockTick = 0;
                                    } else {
                                        int slot = this.findEmptySlot(item);
                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(item));
                                        swap = true;
                                        this.blockTick = 1;
                                    }
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 3:
                                if (this.hasValidTarget()) {
                                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                blocked = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    if (Myau.moduleManager.modules.get(NoSlow.class).isEnabled()) {
                                                        int randomSlot = new Random().nextInt(9);
                                                        while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                                            randomSlot = new Random().nextInt(9);
                                                        }
                                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                                    }
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 4:
                                if (this.hasValidTarget()) {
                                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blinkReset = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 5:
                                if (this.hasValidTarget()) {
                                    int item = ((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem();
                                    if (mc.thePlayer.inventory.currentItem == item && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blinkReset = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    int slot = this.findEmptySlot(item);
                                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
                                                    ((IAccessorPlayerControllerMP) mc.playerController).setCurrentPlayerItem(slot);
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 6:
                                if (this.hasValidTarget()) {
                                    int item = ((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem();
                                    if (mc.thePlayer.inventory.currentItem == item && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        switch (this.blockTick) {
                                            case 0:
                                                int slot = this.findSwordSlot(item);
                                                if (slot != -1) {
                                                    if (!this.isPlayerBlocking()) {
                                                        swap = true;
                                                    }
                                                    this.blockTick = 1;
                                                }
                                                break;
                                            case 1:
                                                int swordsSlot = this.findSwordSlot(item);
                                                if (swordsSlot == -1) {
                                                    this.blockTick = 0;
                                                } else if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                } else if (this.attackDelayMS <= 50L) {
                                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(swordsSlot));
                                                    ((IAccessorPlayerControllerMP) mc.playerController).setCurrentPlayerItem(swordsSlot);
                                                    this.startBlock(mc.thePlayer.inventory.getStackInSlot(swordsSlot));
                                                    attack = false;
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                        Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                        this.isBlocking = true;
                                        this.fakeBlockState = true;
                                        break;
                                    }
                                }
                                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.isBlocking = false;
                                this.fakeBlockState = false;
                                break;
                            case 7:
                                if (this.hasValidTarget()) {
                                    if (!Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 8:
                                Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.isBlocking = false;
                                this.fakeBlockState = this.hasValidTarget();
                                if (PlayerUtil.isUsingItem() && !this.isPlayerBlocking() && !Myau.playerStateManager.digging && !Myau.playerStateManager.placing) {
                                    swap = true;
                                }
                        }
                    }
                    boolean attacked = false;

                 if (this.rotations.getValue() == 4 && this.smoothBack.getValue() && (this.target == null || !this.isBoxInSwingRange(this.target.getBox()))) {
                     Rotation currentRot = this.serverRotation;
                     Rotation playerRot = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                     
                     if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                         this.isSmoothBacking = true;
                         Rotation nextRot = getSmoothBackRotation(currentRot, playerRot);
                         
                         float[] patched = RotationUtil.gcd(new float[]{nextRot.yaw, nextRot.pitch}, new float[]{currentRot.yaw, currentRot.pitch});
                         nextRot = new Rotation(patched[0], patched[1]);

                         this.serverRotation = nextRot;
                         event.setRotation(nextRot.yaw, nextRot.pitch, 1);
                         
                         if (this.moveFix.getValue() != 0) {
                             event.setPervRotation(nextRot.yaw, 1);
                         }
                     } else {
                         this.isSmoothBacking = false;
                         this.serverRotation = playerRot;
                     }
                 }

                 if (this.target != null && this.isBoxInSwingRange(this.target.getBox())) {
                     
                     if (this.rotations.getValue() == 4) {
                         Rotation currentRot = this.serverRotation;
                         if (Float.isNaN(currentRot.yaw) || Float.isNaN(currentRot.pitch)) {
                             currentRot = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                         }

                         Rotation nextRot = updateLiquidBounceRotation(currentRot);
                         
                         float[] patched = RotationUtil.gcd(new float[]{nextRot.yaw, nextRot.pitch}, new float[]{currentRot.yaw, currentRot.pitch});
                         nextRot = new Rotation(patched[0], patched[1]);

                         this.serverRotation = nextRot;
                         updateRenderAimPosition(nextRot);
                         event.setRotation(nextRot.yaw, nextRot.pitch, 1);
                         
                         if (this.moveFix.getValue() != 0) {
                             event.setPervRotation(nextRot.yaw, 1);
                         }
                         
                         mc.thePlayer.rotationYawHead = nextRot.yaw;
                         mc.thePlayer.renderYawOffset = nextRot.yaw;
                         
                         if (attack) {
                             attacked = this.performAttack(nextRot.yaw, nextRot.pitch);
                         }
                         
                     } 
                     else if (this.rotations.getValue() >= 1) {
                         float randomYaw = RandomUtil.nextFloat(-2.5F, 2.5F);
                         float randomPitch = RandomUtil.nextFloat(-1.5F, 1.5F);
                         
                         float[] rotations = RotationUtil.getRotationsToBox(
                                 this.target.getBox(),
                                 event.getYaw(),
                                 event.getPitch(),
                                 (float) this.angleStep.getValue() + RandomUtil.nextFloat(-5.0F, 5.0F),
                                 (float) this.smoothing.getValue() / 100.0F
                         );
                         
                         float finalYaw = rotations[0] + randomYaw;
                         float finalPitch = rotations[1] + randomPitch;
                         
                         float[] patched = RotationUtil.gcd(new float[]{finalYaw, finalPitch}, new float[]{event.getYaw(), event.getPitch()});
                         finalYaw = patched[0];
                         finalPitch = MathHelper.clamp_float(patched[1], -90.0F, 90.0F);

                         event.setRotation(finalYaw, finalPitch, 1);
                         
                         if (this.rotations.getValue() == 3) {
                             Myau.rotationManager.setRotation(finalYaw, finalPitch, 1, true);
                         } else {
                             mc.thePlayer.rotationYawHead = finalYaw;
                             mc.thePlayer.renderYawOffset = finalYaw;
                         }
                         
                         if (this.moveFix.getValue() != 0 || this.rotations.getValue() == 3) {
                             event.setPervRotation(finalYaw, 1);
                         }
                         
                         if (attack) {
                             attacked = this.performAttack(event.getNewYaw(), event.getNewPitch());
                         }
                     } 
                     else {
                         if (attack) {
                             attacked = this.performAttack(event.getNewYaw(), event.getNewPitch());
                         }
                     }
                 }

                 if (swap) {
                     if (attacked) {
                         this.interactAttack(event.getNewYaw(), event.getNewPitch());
                     } else {
                         this.sendUseItem();
                     }
                 }

                 if (blocked) {
                     Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                     Myau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
                 }
                }
            }
        }
    }

    private Rotation updateLiquidBounceRotation(Rotation current) {
        if (this.target == null || System.currentTimeMillis() - this.lastRotationUpdateTime < ROTATION_UPDATE_INTERVAL_MS) {
            return current;
        }

        this.lastRotationUpdateTime = System.currentTimeMillis();
        EntityLivingBase targetEntity = this.target.getEntity();

        AxisAlignedBB bb = targetEntity.getEntityBoundingBox().expand(targetEntity.getCollisionBorderSize(), targetEntity.getCollisionBorderSize(), targetEntity.getCollisionBorderSize());
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);

        Vec3 targetPoint = searchCenterPoint(bb, eyes);

        if (targetPoint == null) {
            targetPoint = new Vec3(
                    (bb.minX + bb.maxX) / 2.0,
                    (bb.minY + bb.maxY) / 2.0,
                    (bb.minZ + bb.maxZ) / 2.0
            );
        }

        if (this.liquidBouncePredict.getValue()) {
            targetPoint = applyPrediction(targetPoint, targetEntity);
        }

        double diffX = targetPoint.xCoord - eyes.xCoord;
        double diffY = targetPoint.yCoord - eyes.yCoord;
        double diffZ = targetPoint.zCoord - eyes.zCoord;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / Math.PI));

        targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);
        targetPitch = MathHelper.wrapAngleTo180_float(targetPitch);

        return limitAngleChange(current, new Rotation(targetYaw, targetPitch));
    }

    private Vec3 searchCenterPoint(AxisAlignedBB bb, Vec3 eyes) {
        double scanRange = Math.max(this.attackRange.getValue(), this.swingRange.getValue());
        double attackRange = this.attackRange.getValue();
        double throughWallsRange = this.throughWalls.getValue() ? attackRange : 0;

        double minBody = this.liquidBounceBodyPointMin.getValue();
        double maxBody = this.liquidBounceBodyPointMax.getValue();
        double hMin = 0.0;
        double hMax = this.liquidBounceHorizontalSearch.getValue();

        Vec3 bestPoint = null;
        double bestScore = Double.MAX_VALUE;

        for (double x = hMin; x <= hMax; x += 0.25) {
            for (double y = minBody; y <= maxBody; y += 0.25) {
                for (double z = hMin; z <= hMax; z += 0.25) {
                    Vec3 point = new Vec3(
                            bb.minX + (bb.maxX - bb.minX) * x,
                            bb.minY + (bb.maxY - bb.minY) * y,
                            bb.minZ + (bb.maxZ - bb.minZ) * z
                    );

                    double distance = eyes.distanceTo(point);

                    if (distance > scanRange) {
                        continue;
                    }

                    boolean visible = isVisible(eyes, point);
                    if (!visible && distance > throughWallsRange) {
                        continue;
                    }

                    double score = distance;
                    if (distance > attackRange) {
                        score += 10.0;
                    }
                    if (!visible) {
                        score += 5.0;
                    }

                    if (this.liquidBounceRandomize.getValue()) {
                        score += random.nextDouble() * this.liquidBounceRandomizeRange.getValue() * 5.0;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        bestPoint = point;
                    }
                }
            }
        }

        return bestPoint;
    }

    private boolean isVisible(Vec3 from, Vec3 to) {
        if (this.throughWalls.getValue()) {
            return true;
        }
        return mc.theWorld.rayTraceBlocks(from, to, false, true, false) == null;
    }

    private Vec3 applyPrediction(Vec3 point, EntityLivingBase entity) {
        double predictX = entity.posX + entity.motionX * this.liquidBouncePredictSize.getValue();
        double predictY = entity.posY + entity.motionY * this.liquidBouncePredictSize.getValue() * 0.5;
        double predictZ = entity.posZ + entity.motionZ * this.liquidBouncePredictSize.getValue();

        double offsetX = point.xCoord - entity.posX;
        double offsetY = point.yCoord - entity.posY;
        double offsetZ = point.zCoord - entity.posZ;

        return new Vec3(predictX + offsetX, predictY + offsetY, predictZ + offsetZ);
    }

    private Rotation limitAngleChange(Rotation current, Rotation target) {
        float maxHorizontalChange = this.liquidBounceHorizontalSpeed.getValue();
        float maxVerticalChange = this.liquidBounceVerticalSpeed.getValue();
        float smoothFactor = this.liquidBounceSmoothFactor.getValue();

        float yawDiff = MathHelper.wrapAngleTo180_float(target.yaw - current.yaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(target.pitch - current.pitch);

        yawDiff = MathHelper.clamp_float(yawDiff, -maxHorizontalChange, maxHorizontalChange);
        pitchDiff = MathHelper.clamp_float(pitchDiff, -maxVerticalChange, maxVerticalChange);

        yawDiff *= smoothFactor;
        pitchDiff *= smoothFactor;

        return new Rotation(
                current.yaw + yawDiff,
                MathHelper.clamp_float(current.pitch + pitchDiff, -90.0f, 90.0f)
        );
    }

    private void updateRenderAimPosition(Rotation rotation) {
        if (rotation == null || mc.thePlayer == null) {
            this.currentAimVec = null;
            return;
        }
        float yawRad = rotation.yaw * (float) Math.PI / 180.0F;
        float pitchRad = rotation.pitch * (float) Math.PI / 180.0F;
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        double renderDistance = Math.max(0.1, mc.thePlayer.getDistanceToEntity(this.target.getEntity()) * 0.5);
        renderDistance = Math.min(renderDistance, 4.0);
        this.currentAimVec = new Vec3(
                eyePos.xCoord + lookX * renderDistance,
                eyePos.yCoord + lookY * renderDistance,
                eyePos.zCoord + lookZ * renderDistance
        );
    }

    private Rotation getSmoothBackRotation(Rotation current, Rotation target) {
        float yawDiff = MathHelper.wrapAngleTo180_float(target.yaw - current.yaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(target.pitch - current.pitch);
        float speed = Math.max(5.0f, Math.abs(yawDiff) * 0.3f);
        float yawStep = MathHelper.clamp_float(yawDiff, -speed, speed);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -speed, speed);
        return new Rotation(current.yaw + yawStep, current.pitch + pitchStep);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() || this.wantsToDisable) {
            switch (event.getType()) {
                case PRE:
                    if (this.target == null && !this.isSmoothBacking && !this.wantsToDisable) {
                        this.serverRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        this.currentAimVec = null;
                    }
                    ArrayList<EntityLivingBase> validTargets = new ArrayList<>();
                    boolean needsNewTarget = false;
                    if (this.target == null
                            || !this.isValidTarget(this.target.getEntity())
                            || !this.isBoxInAttackRange(this.target.getBox())
                            || !this.isBoxInSwingRange(this.target.getBox())
                            || this.timer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {
                        needsNewTarget = true;
                        this.timer.reset();
                    }
                    for (Entity entity : mc.theWorld.loadedEntityList) {
                        if (entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                                && this.isInRange((EntityLivingBase) entity)) {
                            validTargets.add((EntityLivingBase) entity);
                        }
                    }
                    if (validTargets.isEmpty()) {
                        this.target = null;
                        this.currentAimVec = null;
                    } else {
                        if (validTargets.stream().anyMatch(this::isInSwingRange)) {
                            validTargets.removeIf(entityLivingBase -> !this.isInSwingRange(entityLivingBase));
                        }
                        if (validTargets.stream().anyMatch(this::isInAttackRange)) {
                            validTargets.removeIf(entityLivingBase -> !this.isInAttackRange(entityLivingBase));
                        }
                        if (validTargets.stream().anyMatch(this::isPlayerTarget)) {
                            validTargets.removeIf(entityLivingBase -> !this.isPlayerTarget(entityLivingBase));
                        }
                        validTargets.sort(
                                (entityLivingBase1, entityLivingBase2) -> {
                                    int sortBase = 0;
                                    switch (this.sort.getValue()) {
                                        case 1:
                                            sortBase = Float.compare(TeamUtil.getHealthScore(entityLivingBase1), TeamUtil.getHealthScore(entityLivingBase2));
                                            break;
                                        case 2:
                                            sortBase = Integer.compare(entityLivingBase1.hurtResistantTime, entityLivingBase2.hurtResistantTime);
                                            break;
                                        case 3:
                                            sortBase = Float.compare(
                                                    RotationUtil.angleToEntity(entityLivingBase1),
                                                    RotationUtil.angleToEntity(entityLivingBase2)
                                            );
                                    }
                                    return sortBase != 0
                                            ? sortBase
                                            : Double.compare(RotationUtil.distanceToEntity(entityLivingBase1), RotationUtil.distanceToEntity(entityLivingBase2));
                                }
                        );
                        if (needsNewTarget) {
                            if (this.mode.getValue() == 1 && this.hitRegistered) {
                                this.hitRegistered = false;
                                this.switchTick++;
                            }
                            if (this.mode.getValue() == 0 || this.switchTick >= validTargets.size()) {
                                this.switchTick = 0;
                            }
                            EntityLivingBase newTarget = validTargets.get(this.switchTick);
                            boolean targetChanged = this.target == null || this.target.getEntity() != newTarget;
                            if (targetChanged) {
                                this.target = new AttackData(newTarget, this);
                            }
                        }
                    }
                    if (this.target != null) {
                        this.target.update();
                    }
                    break;
                case POST:
                    if (this.isPlayerBlocking() && !mc.thePlayer.isBlocking()) {
                        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                    }
			default:
				break;
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
                if (packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    this.blockingState = false;
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                this.blockingState = false;
                if (this.isBlocking) {
                    mc.thePlayer.stopUsingItem();
                }
            }
            if (this.debugLog.getValue() == 1 && this.isAttackAllowed()) {
                if (event.getPacket() instanceof S06PacketUpdateHealth) {
                    float packet = ((S06PacketUpdateHealth) event.getPacket()).getHealth() - mc.thePlayer.getHealth();
                    if (packet != 0.0F && this.lastTickProcessed != mc.thePlayer.ticksExisted) {
                        this.lastTickProcessed = mc.thePlayer.ticksExisted;
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                                        Myau.clientName,
                                        packet > 0.0F ? "&a" : "&c",
                                        df.format(packet),
                                        mc.thePlayer.ticksExisted
                                )
                        );
                    }
                }
                if (event.getPacket() instanceof S1CPacketEntityMetadata) {
                    S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) event.getPacket();
                    if (packet.getEntityId() == mc.thePlayer.getEntityId()) {
                        for (WatchableObject watchableObject : packet.func_149376_c()) {
                            if (watchableObject.getDataValueId() == 6) {
                                float diff = (Float) watchableObject.getObject() - mc.thePlayer.getHealth();
                                if (diff != 0.0F && this.lastTickProcessed != mc.thePlayer.ticksExisted) {
                                    this.lastTickProcessed = mc.thePlayer.ticksExisted;
                                    ChatUtil.sendFormatted(
                                            String.format(
                                                    "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                                                    Myau.clientName,
                                                    diff > 0.0F ? "&a" : "&c",
                                                    df.format(diff),
                                                    mc.thePlayer.ticksExisted
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() || this.wantsToDisable) {
            boolean isSilent = this.rotations.getValue() == 2;
            boolean isLiquidBounce = this.rotations.getValue() == 4;
            if (this.moveFix.getValue() != 0 && (isSilent || isLiquidBounce)) {
                if (RotationState.isActived() && RotationState.getPriority() == 1.0F && MoveUtil.isForwardPressed()) {
                    MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                }
            }
            if (this.shouldAutoBlock()) {
                mc.thePlayer.movementInput.jump = false;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        try {
            if (mc.getRenderManager() == null || mc.getRenderViewEntity() == null) return;
            IAccessorRenderManager renderManagerAccessor = (IAccessorRenderManager) mc.getRenderManager();
            double viewerX = renderManagerAccessor.getRenderPosX();
            double viewerY = renderManagerAccessor.getRenderPosY();
            double viewerZ = renderManagerAccessor.getRenderPosZ();
            if (this.isEnabled() && target != null) {
                if (this.showTarget.getValue() != 0
                        && TeamUtil.isEntityLoaded(this.target.getEntity())
                        && this.isAttackAllowed()) {
                    Color color = new Color(-1);
                    if (this.showTarget.getValue() == 1) {
                        if (this.target.getEntity().hurtTime > 0) {
                            color = new Color(16733525);
                        } else {
                            color = new Color(5635925);
                        }
                    }
                    RenderUtil.enableRenderState();
                    RenderUtil.drawEntityBox(this.target.getEntity(), color.getRed(), color.getGreen(), color.getBlue());
                    RenderUtil.disableRenderState();
                }
            }
            if ((this.isEnabled() || this.wantsToDisable) && this.visualizeAim.getValue() && this.currentAimVec != null) {
                double x = this.currentAimVec.xCoord;
                double y = this.currentAimVec.yCoord;
                double z = this.currentAimVec.zCoord;
                x -= viewerX;
                y -= viewerY;
                z -= viewerZ;
                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.color(0.0F, 1.0F, 1.0F, 0.8F);
                double size = 0.08;
                AxisAlignedBB bb = new AxisAlignedBB(x - size, y - size, z - size, x + size, y + size, z + size);
                RenderGlobal.drawSelectionBoundingBox(bb);
                GlStateManager.color(0.0F, 1.0F, 0.0F, 0.5F);
                GL11.glLineWidth(2.0F);
                RenderGlobal.drawOutlinedBoundingBox(bb, 0, 255, 0, 128);
                GlStateManager.depthMask(true);
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        if (!this.wantsToDisable) {
            this.target = null;
            this.switchTick = 0;
            this.hitRegistered = false;
            this.attackDelayMS = 0L;
            this.blockTick = 0;
            this.serverRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            this.currentAimVec = null;
            this.isSmoothBacking = false;
            this.lastRotationUpdateTime = 0;
            this.patternIndex = 0;
        }
    }

    @Override
    public void onDisabled() {
        if (this.rotations.getValue() == 4 && this.smoothBack.getValue()) {
            Rotation currentRot = this.serverRotation;
            Rotation playerRot = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                this.wantsToDisable = true;
                this.setEnabled(true);
                return;
            }
        }
        Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
        this.wantsToDisable = false;
        this.currentAimVec = null;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    @Getter
    public static class AttackData {
        private final EntityLivingBase entity;
        private final KillAura killAura;
        private AxisAlignedBB box;
        private double x;
        private double y;
        private double z;

        public AttackData(EntityLivingBase entityLivingBase, KillAura killAura) {
            this.entity = entityLivingBase;
            this.killAura = killAura;
            update();
        }

        public void update() {
            double collisionBorderSize = entity.getCollisionBorderSize();
            this.box = entity.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            this.x = entity.posX;
            this.y = entity.posY;
            this.z = entity.posZ;
        }
    }
}