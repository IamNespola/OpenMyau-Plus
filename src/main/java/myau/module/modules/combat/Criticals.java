package myau.module.modules.combat;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.mixin.IAccessorC03PacketPlayer;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.movement.Fly;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Criticals extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Packet", "NCPPacket", "OldBlocksMC", "OldBlocksMC2", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "CustomMotion", "Visual"});
    public final IntProperty delay = new IntProperty("delay", 0, 0, 500);
    public final IntProperty hurtTime = new IntProperty("hurt-time", 10, 0, 10);
    public final FloatProperty customMotionY = new FloatProperty("custom-y", 0.2F, 0.01F, 0.42F);
    private final TimerUtil timer = new TimerUtil();

    public Criticals() {
        super("Criticals", Category.COMBAT,false);
    }

    public void onEnabled() {
        if ((Integer) this.mode.getValue() == 4 && mc.thePlayer != null) {
            mc.thePlayer.jump();
        }

    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            if (mc.thePlayer != null && mc.theWorld != null) {
                if (event.getTarget() instanceof EntityLivingBase) {
                    EntityLivingBase target = (EntityLivingBase) event.getTarget();
                    if (mc.thePlayer.onGround) {
                        if (!mc.thePlayer.isUsingItem()) {
                            if (!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
                                if (mc.thePlayer.ridingEntity == null) {
                                    if (target.hurtTime <= (Integer) this.hurtTime.getValue()) {
                                        Fly fly = (Fly) Myau.moduleManager.modules.get(Fly.class);
                                        if (fly == null || !fly.isEnabled()) {
                                            if (this.timer.hasTimeElapsed((long) (Integer) this.delay.getValue())) {
                                                double x = mc.thePlayer.posX;
                                                double y = mc.thePlayer.posY;
                                                double z = mc.thePlayer.posZ;
                                                switch ((Integer) this.mode.getValue()) {
                                                    case 0:
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + (double) 0.0625F, z, true));
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                                                        mc.thePlayer.attackTargetEntityWithCurrentItem(target);
                                                        break;
                                                    case 1:
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.11, z, false));
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1100013579, z, false));
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 1.3579E-6, z, false));
                                                        mc.thePlayer.attackTargetEntityWithCurrentItem(target);
                                                        break;
                                                    case 2:
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.001091981, z, true));
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                                                        break;
                                                    case 3:
                                                        if (mc.thePlayer.ticksExisted % 4 == 0) {
                                                            PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0011, z, true));
                                                            PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                                                        }
                                                        break;
                                                    case 4:
                                                    default:
                                                        break;
                                                    case 5:
                                                        mc.thePlayer.motionY = 0.1;
                                                        mc.thePlayer.fallDistance = 0.1F;
                                                        mc.thePlayer.onGround = false;
                                                        break;
                                                    case 6:
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.02, z, false));
                                                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.01, z, false));
                                                        mc.thePlayer.setPosition(x, y + 0.01, z);
                                                        break;
                                                    case 7:
                                                        mc.thePlayer.motionY = 0.42;
                                                        break;
                                                    case 8:
                                                        mc.thePlayer.motionY = 0.3425;
                                                        break;
                                                    case 9:
                                                        mc.thePlayer.motionY = (double) (Float) this.customMotionY.getValue();
                                                        break;
                                                    case 10:
                                                        mc.thePlayer.attackTargetEntityWithCurrentItem(target);
                                                }

                                                this.timer.reset();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.SEND) {
                if ((Integer) this.mode.getValue() == 4 && event.getPacket() instanceof C03PacketPlayer) {
                    ((IAccessorC03PacketPlayer) event.getPacket()).setOnGround(false);
                }

            }
        }
    }

    public String[] getSuffix() {
        String[] modes = new String[]{"Packet", "NCPPacket", "OldBlocksMC", "OldBlocksMC2", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "CustomMotion", "Visual"};
        return new String[]{modes[(Integer) this.mode.getValue()]};
    }
}
