package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ========== 原有字段（保持兼容，供其他模块调用） ==========
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false);

    // ========== Rise 新增字段（所有功能完整保留） ==========
    // PercentProperty 接受 int，所以传入 60 表示 60%，对应原 0.6
    public final PercentProperty slowDownVelocity = new PercentProperty("Hit Slow Down During Velocity", 60);
    public final PercentProperty slowDownNormal = new PercentProperty("Hit Slow Down Normal", 60);
    public final PercentProperty bufferDecrease = new PercentProperty("Buffer Decrease", 100);   // 1.0 → 100%
    public final PercentProperty maxBuffer = new PercentProperty("Max Buffer", 500);            // 5.0 → 500%

    public final BooleanProperty sprintSlowDownVelocity = new BooleanProperty("Velocity Hit Sprint", false);
    public final BooleanProperty sprintSlowDownNormal = new BooleanProperty("Normal Hit Sprint", false);
    public final BooleanProperty bufferAbuse = new BooleanProperty("Buffer Abuse", false);
    public final BooleanProperty onlyInAir = new BooleanProperty("Only In Air", false);

    // ========== 原 Rise 的状态变量 ==========
    public boolean hitFlag;      // 对应 By
    public double bufferCount;   // 对应 Bz

    public KeepSprint() {
        super("KeepSprint", false);
    }

    @Override
    public boolean shouldKeepSprint() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        // 原有 groundOnly 逻辑
        if (groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        }

        // 原有 reachOnly 逻辑
        if (reachOnly.getValue()) {
            if (mc.objectMouseOver == null || mc.objectMouseOver.hitVec == null) {
                return false;
            }
            double distance = mc.objectMouseOver.hitVec.distanceTo(
                    mc.getRenderViewEntity().getPositionEyes(1.0F)
            );
            return distance > 3.0;
        }

        return true;
    }

    /**
     * Rise 完整功能：每帧执行（需在你的客户端主循环中调用）
     */
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // —— 当 玩家在地上 且 onlyInAir 为 true 时，直接返回 ——
        if (mc.thePlayer.onGround && onlyInAir.getValue()) {
            return;
        }

        // —— 缓冲（Buffer Abuse）逻辑 ——
        if (bufferAbuse.getValue()) {
            if (bufferCount < maxBuffer.getValue() && !hitFlag) {
                bufferCount++;
            } else {
                if (bufferCount > 0) {
                    bufferCount = Math.max(0, bufferCount - bufferDecrease.getValue());
                    hitFlag = true;
                    return;
                }
                hitFlag = false;
            }
        } else {
            bufferCount = 0;
            hitFlag = false;
        }

        // —— 受伤 / 正常状态下的减速与疾跑控制 ——
        // 将百分比转为小数（60 → 0.6）
        double velocityFactor = slowDownVelocity.getValue().doubleValue() / 100.0;
        double normalFactor = slowDownNormal.getValue().doubleValue() / 100.0;

        if (mc.thePlayer.hurtTime > 0) {
            mc.thePlayer.motionX *= velocityFactor;
            mc.thePlayer.motionZ *= velocityFactor;
            mc.thePlayer.setSprinting(sprintSlowDownVelocity.getValue());
        } else {
            mc.thePlayer.motionX *= normalFactor;
            mc.thePlayer.motionZ *= normalFactor;
            mc.thePlayer.setSprinting(sprintSlowDownNormal.getValue());
        }
    }

    /**
     * 获取当前应该使用的减速值（供外部调用，如 PlayerUtil 等）
     * 兼容原有 slowDown 字段，返回百分比值
     */
    public double getCurrentSlowdown() {
        if (mc.thePlayer == null) return slowdown.getValue().doubleValue();
        if (mc.thePlayer.hurtTime > 0) {
            return slowDownVelocity.getValue().doubleValue();
        } else {
            return slowDownNormal.getValue().doubleValue();
        }
    }
                }
