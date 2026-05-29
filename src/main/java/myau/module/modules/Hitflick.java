package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

public class Hitflick extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty direction = new ModeProperty("Direction", 0, new String[]{"Left", "Right", "Back", "Custom"});
    public final FloatProperty customAngle = new FloatProperty("Custom-Angle", 90F, 1F, 180F, () -> this.direction.getValue() == 3);
    public final IntProperty cooldown = new IntProperty("Cooldown", 1, 1, 40);
    public final BooleanProperty blink = new BooleanProperty("Blink", false);

    private long sinceLastFlick;
    private float originalYaw;
    private float flickYaw;
    private FlickState state = FlickState.IDLE;

    private enum FlickState {
        IDLE, FLICKING_AWAY, RESTORING
    }

    public Hitflick() {
        super("Hitflick", false, true, "Flick away on hit then restore");
    }

    @Override
    public void onEnabled() {
        sinceLastFlick = 0;
        state = FlickState.IDLE;
    }

    @Override
    public void onDisabled() {
        state = FlickState.IDLE;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() == null || event.getTarget() == mc.thePlayer) return;
        if (state != FlickState.IDLE || sinceLastFlick < cooldown.getValue()) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        originalYaw = mc.thePlayer.rotationYaw;
        flickYaw = originalYaw + getFlickAngle();
        state = FlickState.FLICKING_AWAY;

        if (blink.getValue()) {
            Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
            Myau.blinkManager.setBlinkState(true, BlinkModules.HITFLICK);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null) return;

        if (state == FlickState.IDLE) {
            sinceLastFlick++;
            return;
        }

        if (state == FlickState.FLICKING_AWAY) {
            event.setRotation(flickYaw, mc.thePlayer.rotationPitch, 0);
            state = FlickState.RESTORING;
        } else if (state == FlickState.RESTORING) {
            event.setRotation(originalYaw, mc.thePlayer.rotationPitch, 0);
            state = FlickState.IDLE;
            sinceLastFlick = 0;
            if (blink.getValue()) {
                Myau.blinkManager.setBlinkState(false, BlinkModules.HITFLICK);
            }
        }
    }

    private float getFlickAngle() {
        switch (direction.getValue()) {
            case 0: return -90F;
            case 1: return 90F;
            case 2: return 180F;
            case 3: return customAngle.getValue();
            default: return 90F;
        }
    }
}
