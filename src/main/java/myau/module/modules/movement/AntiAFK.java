package myau.module.modules.movement;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.mixin.IAccessorKeyBinding;
import myau.module.Category;
import myau.module.Module;
import net.minecraft.client.settings.GameSettings;

public class AntiAFK extends Module {
    private int lastInput;

    public AntiAFK() {
        super("AntiAFK", Category.MOVEMENT,false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event){
        if(event.getType() == EventType.PRE && this.isEnabled()){
            GameSettings gameSettings = mc.gameSettings;
            if (gameSettings.keyBindJump.isPressed() || gameSettings.keyBindRight.isPressed() || gameSettings.keyBindForward.isPressed() || gameSettings.keyBindLeft.isPressed() || gameSettings.keyBindBack.isPressed()) {
                lastInput = 0;
            }
            lastInput++;
            if (lastInput < 20 * 10) return;
            if (mc.thePlayer.ticksExisted % 5 == 0) {
                ((IAccessorKeyBinding)mc.gameSettings.keyBindRight).setPressed(false);
                ((IAccessorKeyBinding)mc.gameSettings.keyBindLeft).setPressed(false);
                ((IAccessorKeyBinding)mc.gameSettings.keyBindJump).setPressed(false);
            }
            if (mc.thePlayer.ticksExisted % 20 == 0) {
                if (mc.thePlayer.ticksExisted % 40 == 0) {
                    ((IAccessorKeyBinding)mc.gameSettings.keyBindRight).setPressed(true);
                } else {
                    ((IAccessorKeyBinding)mc.gameSettings.keyBindLeft).setPressed(true);
                }
            }
            if (mc.thePlayer.ticksExisted % 100 == 0) {
                ((IAccessorKeyBinding)mc.gameSettings.keyBindJump).setPressed(true);
            }
        }
    }
}
