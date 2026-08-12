package keystrokesmod.mixin.impl.entity;

import com.mojang.authlib.GameProfile;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    @Unique private double rsl$prevX;
    @Unique private double rsl$prevY;
    @Unique private double rsl$prevZ;
    @Unique private float rsl$prevYaw;
    @Unique private float rsl$prevPitch;
    @Unique private boolean rsl$prevGround;
    @Unique private boolean rsl$prevSprinting;
    @Unique private boolean rsl$prevSneaking;

    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onUpdatePre(CallbackInfo c) {
        if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PreUpdateEvent());
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void onUpdatePost(CallbackInfo c) {
        if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PostUpdateEvent());
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    public void onUpdateWalkingPlayer$head(CallbackInfo ci) {
        PreMotionEvent.setRenderYaw(false);
        PreMotionEvent preMotionEvent = new PreMotionEvent(
                this.posX,
                this.getEntityBoundingBox().minY,
                this.posZ,
                this.rotationYaw,
                this.rotationPitch,
                this.onGround,
                this.isSprinting(),
                this.isSneaking()
        );

        MinecraftForge.EVENT_BUS.post(preMotionEvent);

        RotationUtils.serverRotations = new float[] { preMotionEvent.getYaw(), preMotionEvent.getPitch() };


        rsl$prevX = this.posX;
        rsl$prevY = this.getEntityBoundingBox().minY;
        rsl$prevZ = this.posZ;
        rsl$prevYaw = this.rotationYaw;
        rsl$prevPitch = this.rotationPitch;
        rsl$prevGround = this.onGround;
        rsl$prevSprinting = this.isSprinting();
        rsl$prevSneaking = this.isSneaking();

        this.setPosition(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ());
        this.rotationYaw = preMotionEvent.getYaw();
        this.rotationPitch = preMotionEvent.getPitch();
        this.onGround = preMotionEvent.isOnGround();
        this.setSprinting(preMotionEvent.isSprinting());
        this.setSneaking(preMotionEvent.isSneaking());
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("TAIL"))
    public void onUpdateWalkingPlayer$tail(CallbackInfo ci) {
        this.setPosition(rsl$prevX, rsl$prevY, rsl$prevZ);
        this.rotationYaw = rsl$prevYaw;
        this.rotationPitch = rsl$prevPitch;
        this.onGround = rsl$prevGround;
        this.setSprinting(rsl$prevSprinting);
        this.setSneaking(rsl$prevSneaking);
        MinecraftForge.EVENT_BUS.post(new PostMotionEvent());
    }

}