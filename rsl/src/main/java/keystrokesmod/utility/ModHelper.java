package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.item.ItemFireball;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.Iterator;
import java.util.Map;

public class ModHelper {
    private final Minecraft mc;
    public static int inAirTicks;
    public static int groundTicks;
    private int unTargetTicks;
    public static boolean threwFireball;
    public static boolean threwFireballLow;
    public static long MAX_EXPLOSION_DIST_SQ = 10;
    private long FIREBALL_TIMEOUT = 500L;
    private long fireballTime = 0;

    public ModHelper(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (inAirTicks <= 20) {
            inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        } else {
            inAirTicks = 19;
        }
        groundTicks = !mc.thePlayer.onGround ? 0 : ++groundTicks;
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball) {
            if (Mouse.isButtonDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
                if (mc.thePlayer.rotationPitch > 50F) {
                    threwFireballLow = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT / 3) {
            threwFireballLow = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = threwFireballLow = false;
            fireballTime = 0;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || e.isCanceled()) {
            return;
        }
        if (e.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion s27 = (S27PacketExplosion) e.getPacket();
            if (threwFireball) {
                if ((mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) <= MAX_EXPLOSION_DIST_SQ)) {
                    threwFireball = false;
                    e.setCanceled(false);
                }
            }
        }
    }
}
