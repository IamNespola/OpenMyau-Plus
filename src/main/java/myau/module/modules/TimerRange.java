package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.module.modules.BackTrack;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.client.ChatUtil;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.reflect.Field;

public class TimerRange extends Module {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public final ModeProperty timerBoostMode = new ModeProperty("mode", 2, new String[]{"NORMAL", "SMART", "MODERN"});
   public final IntProperty ticksValue = new IntProperty("ticks", 10, 1, 20);

   public final FloatProperty timerBoostValue = new FloatProperty("timer-boost", 1.5F, 0.01F, 35.0F);
   public final FloatProperty boostDelayMin = new FloatProperty("boost-delay-min", 0.5F, 0.1F, 1.0F);
   public final FloatProperty boostDelayMax = new FloatProperty("boost-delay-max", 0.55F, 0.1F, 1.0F);

   public final FloatProperty timerChargedValue = new FloatProperty("timer-charged", 0.45F, 0.05F, 5.0F);
   public final FloatProperty chargedDelayMin = new FloatProperty("charged-delay-min", 0.75F, 0.1F, 1.0F);
   public final FloatProperty chargedDelayMax = new FloatProperty("charged-delay-max", 0.9F, 0.1F, 1.0F);

   public final FloatProperty rangeValue = new FloatProperty("range", 3.5F, 1.0F, 5.0F, () -> timerBoostMode.getValue() == 0);
   public final IntProperty cooldownTickValue = new IntProperty("cooldown-tick", 10, 1, 50, () -> timerBoostMode.getValue() == 0);

   public final FloatProperty minRange = new FloatProperty("min-range", 2.5F, 2.0F, 8.0F, () -> timerBoostMode.getValue() != 0);
   public final FloatProperty maxRange = new FloatProperty("max-range", 3.0F, 2.0F, 8.0F, () -> timerBoostMode.getValue() != 0);
   public final FloatProperty scanRange = new FloatProperty("scan-range", 8.0F, 2.0F, 12.0F, () -> timerBoostMode.getValue() != 0);

   public final IntProperty minTickDelay = new IntProperty("min-tick-delay", 30, 1, 200, () -> timerBoostMode.getValue() != 0);
   public final IntProperty maxTickDelay = new IntProperty("max-tick-delay", 60, 1, 200, () -> timerBoostMode.getValue() != 0);

   public final BooleanProperty blink = new BooleanProperty("blink", false);
   public final IntProperty predictClientMovement = new IntProperty("predict-client-movement", 2, 0, 5);
   public final FloatProperty predictEnemyPosition = new FloatProperty("predict-enemy-position", 1.5F, -1.0F, 2.0F);
   public final FloatProperty maxAngleDifference = new FloatProperty("max-angle-difference", 5.0F, 5.0F, 90.0F, () -> timerBoostMode.getValue() == 2);

   public final ModeProperty markMode = new ModeProperty("mark", 0, new String[]{"OFF", "BOX", "PLATFORM"}, () -> timerBoostMode.getValue() == 2);
   public final BooleanProperty outline = new BooleanProperty("outline", false, () -> timerBoostMode.getValue() == 2 && markMode.getValue() == 1);

   public final BooleanProperty onWeb = new BooleanProperty("on-web", false);
   public final BooleanProperty onLiquid = new BooleanProperty("on-liquid", false);
   public final BooleanProperty onForwardOnly = new BooleanProperty("on-forward-only", true);
   public final BooleanProperty resetOnLagBack = new BooleanProperty("reset-on-lagback", false);
   public final BooleanProperty resetOnKnockback = new BooleanProperty("reset-on-knockback", false);
   public final BooleanProperty chatDebug = new BooleanProperty("chat-debug", true, () -> resetOnLagBack.getValue() || resetOnKnockback.getValue());

   private int playerTicks = 0;
   private int smartTick = 0;
   private int cooldownTick = 0;
   private float randomRange = 0f;

   private final Queue<Packet<?>> packetsReceived = new ConcurrentLinkedQueue<>();
   private boolean blinked = false;
   private boolean shouldReset = false;
   private boolean confirmTick = false;
   private boolean confirmStop = false;
   private boolean confirmAttack = false;

   public TimerRange() {
      super("TimerRange", false);
   }

   @Override
   public void onDisabled() {
      shouldResetTimer();
      unblink();
      smartTick = 0;
      cooldownTick = 0;
      playerTicks = 0;
      shouldReset = false;
      blinked = false;
      confirmTick = false;
      confirmStop = false;
      confirmAttack = false;
   }

   @EventTarget
   public void onLoadWorld(LoadWorldEvent event) {
      if (blink.getValue()) {
         packetsReceived.clear();
      }
   }

   @EventTarget
   public void onAttack(AttackEvent event) {
      if (mc.thePlayer == null) return;

      if (!(event.getTarget() instanceof EntityLivingBase) && playerTicks >= 1) {
         shouldResetTimer();
         return;
      } else {
         confirmAttack = true;
      }

      if (!(event.getTarget() instanceof EntityLivingBase)) return;
      EntityLivingBase targetEntity = (EntityLivingBase) event.getTarget();

      float entityDistance = mc.thePlayer.getDistanceToEntity(targetEntity);
      int randomTickDelay = RandomUtil.nextInt(minTickDelay.getValue(), maxTickDelay.getValue());

      boolean shouldReturn = BackTrack.runWithNearestTrackedDistance(targetEntity, () -> !updateDistance(targetEntity));

      if (shouldReturn || (mc.thePlayer.isInWater() && !onLiquid.getValue()) || (mc.thePlayer.isInLava() && !onLiquid.getValue())) {
         return;
      }

      smartTick++;
      cooldownTick++;

      boolean shouldSlowed = false;
      if (timerBoostMode.getValue() == 0) {
         shouldSlowed = cooldownTick >= cooldownTickValue.getValue() && entityDistance <= rangeValue.getValue();
      } else if (timerBoostMode.getValue() == 1) {
         shouldSlowed = smartTick >= randomTickDelay && entityDistance <= randomRange;
      }

      if (shouldSlowed && confirmAttack) {
         if (updateDistance(targetEntity)) {
            confirmAttack = false;
            playerTicks = ticksValue.getValue();
            cooldownTick = 0;
            smartTick = 0;
         }
      } else {
         shouldResetTimer();
      }
   }

   @EventTarget
   public void onTick(TickEvent event) {
      if (event.getType() == EventType.PRE) {
         float timerBoost = RandomUtil.nextFloat(boostDelayMin.getValue(), boostDelayMax.getValue());
         float charged = RandomUtil.nextFloat(chargedDelayMin.getValue(), chargedDelayMax.getValue());

         if (mc.thePlayer != null && mc.theWorld != null) {
            randomRange = RandomUtil.nextFloat(minRange.getValue(), maxRange.getValue());
         }

         if (playerTicks <= 0 || confirmStop) {
            shouldResetTimer();
            if (blink.getValue() && blinked) {
               unblink();
               blinked = false;
            }
         } else {
            double tickProgress = (double) playerTicks / (double) ticksValue.getValue();
            float playerSpeed = 1f;
            if (tickProgress < timerBoost) {
               playerSpeed = timerBoostValue.getValue();
            } else if (tickProgress < charged) {
               playerSpeed = timerChargedValue.getValue();
            }

            float speedAdjustment = playerSpeed >= 0 ? playerSpeed : 1f + ticksValue.getValue() - playerTicks;

            setTimerSpeed(Math.max(speedAdjustment, 0f));
            playerTicks--;
         }

         if (timerBoostMode.getValue() != 2) return;
         EntityLivingBase nearbyEntity = getNearestEntityInRange();
         if (nearbyEntity == null) return;

         int randomTickDelay = RandomUtil.nextInt(minTickDelay.getValue(), maxTickDelay.getValue());

         boolean shouldReturn2 = BackTrack.runWithNearestTrackedDistance(nearbyEntity, () -> !updateDistance(nearbyEntity));

         if (shouldReturn2 || (mc.thePlayer.isInWater() && !onLiquid.getValue()) || (mc.thePlayer.isInLava() && !onLiquid.getValue())) {
            return;
         }

         if (isPlayerMoving()) {
            smartTick++;
            if (smartTick >= randomTickDelay) {
               confirmTick = true;
               smartTick = 0;
            }
         } else {
            smartTick = 0;
         }

         if (isPlayerMoving() && !confirmStop) {
            if (isLookingOnEntities(nearbyEntity, maxAngleDifference.getValue())) {
               float entityDistance = mc.thePlayer.getDistanceToEntity(nearbyEntity);
               if (confirmTick && entityDistance >= randomRange && entityDistance <= maxRange.getValue()) {
                  if (updateDistance(nearbyEntity)) {
                     playerTicks = ticksValue.getValue();
                     confirmTick = false;
                  }
               }
            } else {
               shouldResetTimer();
            }
         } else {
            shouldResetTimer();
         }
      } else if (event.getType() == EventType.POST && blink.getValue()) {
         if (!packetsReceived.isEmpty()) {
            for (Packet<?> p : packetsReceived) {
               try {
                  PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p);
               } catch (Exception ignored) {}
            }
            packetsReceived.clear();
         }
      }
   }

   @EventTarget
   public void onPacket(PacketEvent event) {
      if (mc.thePlayer == null || mc.thePlayer.isDead) return;
      Packet<?> packet = event.getPacket();

      if (blink.getValue()) {
         if (playerTicks > 0 && !blinked) {
            blinked = true;
         }

         if (blinked && event.getType() == EventType.RECEIVE) {
            if (packet instanceof S08PacketPlayerPosLook || packet instanceof S27PacketExplosion || packet instanceof S06PacketUpdateHealth) {
               if (packet instanceof S27PacketExplosion) {
                  S27PacketExplosion expl = (S27PacketExplosion) packet;
                  if (expl.func_149149_c() != 0f || expl.func_149144_d() != 0f || expl.func_149147_e() != 0f) {
                     unblink();
                     return;
                  }
               } else if (packet instanceof S06PacketUpdateHealth) {
                  if (((S06PacketUpdateHealth) packet).getHealth() < mc.thePlayer.getHealth()) {
                     unblink();
                     return;
                  }
               } else {
                  unblink();
                  return;
               }
            }

            event.setCancelled(true);
            packetsReceived.add(packet);
         }

         if (blinked && event.getType() == EventType.SEND) {
            if (packet instanceof C07PacketPlayerDigging || packet instanceof C12PacketUpdateSign || packet instanceof C19PacketResourcePackStatus) {
               unblink();
            }
         }
      }

      if (event.getType() == EventType.RECEIVE) {
         if (resetOnLagBack.getValue() && packet instanceof S08PacketPlayerPosLook) {
            shouldResetTimer();
            if (shouldReset) {
               if (chatDebug.getValue()) ChatUtil.sendFormatted("§c[TimerRange] §fLagback Received | AnimationTimer Reset");
               shouldReset = false;
            }
         }

         if (resetOnKnockback.getValue() && packet instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
               shouldResetTimer();
               if (shouldReset) {
                  if (chatDebug.getValue()) ChatUtil.sendFormatted("§c[TimerRange] §fKnockback Received | AnimationTimer Reset");
                  shouldReset = false;
               }
            }
         }
      }
   }

   @EventTarget
   public void onRender3D(Render3DEvent event) {
      if (mc.thePlayer == null || mc.getRenderManager() == null || timerBoostMode.getValue() != 2 || markMode.getValue() == 0) return;

      EntityLivingBase nearbyEntity = getNearestEntityInRange();
      if (nearbyEntity != null && mc.thePlayer.getDistanceToEntity(nearbyEntity) <= scanRange.getValue()) {
         Color color = isLookingOnEntities(nearbyEntity, maxAngleDifference.getValue())
                 ? new Color(37, 126, 255, 70)
                 : new Color(210, 60, 60, 70);

         float partialTicks = event.getPartialTicks();
         double x = nearbyEntity.lastTickPosX + (nearbyEntity.posX - nearbyEntity.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
         double y = nearbyEntity.lastTickPosY + (nearbyEntity.posY - nearbyEntity.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
         double z = nearbyEntity.lastTickPosZ + (nearbyEntity.posZ - nearbyEntity.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

         float width = nearbyEntity.width / 2.0F;
         float height = nearbyEntity.height;

         if (markMode.getValue() == 1) {
            AxisAlignedBB aabb = new AxisAlignedBB(x - width, y, z - width, x + width, y + height, z + width);
            RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.8F);
         } else if (markMode.getValue() == 2) {
            AxisAlignedBB aabb = new AxisAlignedBB(x - width, y, z - width, x + width, y + 0.1D, z + width);
            RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.8F);
         }
      }
   }

   private void unblink() {
      if (!packetsReceived.isEmpty()) {
         for (Packet<?> p : packetsReceived) {
            try {
               PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) p);
            } catch (Exception ignored) {}
         }
         packetsReceived.clear();
      }
      blinked = false;
   }

   private boolean isPlayerMoving() {
      if (!onForwardOnly.getValue()) {
         return mc.thePlayer.moveForward != 0f || mc.thePlayer.moveStrafing != 0f;
      } else {
         return mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing == 0f;
      }
   }

   private EntityLivingBase getNearestEntityInRange() {
      if (mc.theWorld == null || mc.thePlayer == null) return null;
      EntityLivingBase nearest = null;
      double minDst = Double.MAX_VALUE;

      for (Entity e : mc.theWorld.loadedEntityList) {
         if (e instanceof EntityPlayer && e != mc.thePlayer && !e.isDead) {
            double dst = mc.thePlayer.getDistanceToEntity(e);
            double maxCheck = timerBoostMode.getValue() == 0 ? rangeValue.getValue() : (scanRange.getValue() + randomRange);
            if (dst <= maxCheck && dst < minDst) {
               nearest = (EntityLivingBase) e;
               minDst = dst;
            }
         }
      }
      return nearest;
   }

   private void shouldResetTimer() {
      EntityLivingBase nearest = getNearestEntityInRange();
      if (nearest == null || nearest.isDead) {
         if (!shouldReset) {
            setTimerSpeed(1f);
            shouldReset = true;
         }
      } else {
         if (!shouldReset && getTimerSpeed() != 1f) {
            setTimerSpeed(1f);
            shouldReset = true;
         } else {
            shouldReset = false;
         }
      }
   }

   private boolean updateDistance(Entity entity) {
      if (mc.thePlayer == null) return false;

      double predX = (entity.posX - entity.prevPosX) * (2 + predictEnemyPosition.getValue());
      double predY = (entity.posY - entity.prevPosY) * (2 + predictEnemyPosition.getValue());
      double predZ = (entity.posZ - entity.prevPosZ) * (2 + predictEnemyPosition.getValue());

      AxisAlignedBB boundingBox = entity.getEntityBoundingBox().offset(predX, predY, predZ);

      double clientX = mc.thePlayer.posX + (mc.thePlayer.motionX * predictClientMovement.getValue());
      double clientY = mc.thePlayer.posY + (mc.thePlayer.motionY * predictClientMovement.getValue());
      double clientZ = mc.thePlayer.posZ + (mc.thePlayer.motionZ * predictClientMovement.getValue());

      double dstX = Math.max(boundingBox.minX - clientX, Math.max(0, clientX - boundingBox.maxX));
      double dstY = Math.max(boundingBox.minY - clientY, Math.max(0, clientY - boundingBox.maxY));
      double dstZ = Math.max(boundingBox.minZ - clientZ, Math.max(0, clientZ - boundingBox.maxZ));

      double distance = Math.sqrt(dstX * dstX + dstY * dstY + dstZ * dstZ);
      double maxCheck = timerBoostMode.getValue() == 0 ? rangeValue.getValue() : randomRange;

      return distance <= maxCheck;
   }

   private boolean isLookingOnEntities(Entity entity, float maxAngleDiff) {
      if (entity == null || mc.thePlayer == null) return false;
      double diffX = entity.posX - mc.thePlayer.posX;
      double diffZ = entity.posZ - mc.thePlayer.posZ;
      float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
      float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw));
      return yawDiff <= maxAngleDiff;
   }


   private void setTimerSpeed(float speed) {
      try {
         Field timerField = null;
         try { timerField = Minecraft.class.getDeclaredField("timer"); }
         catch (NoSuchFieldException e) { timerField = Minecraft.class.getDeclaredField("field_71428_T"); }
         timerField.setAccessible(true);
         Object timerObj = timerField.get(mc);

         Field speedField = null;
         try { speedField = timerObj.getClass().getDeclaredField("timerSpeed"); }
         catch (NoSuchFieldException e) { speedField = timerObj.getClass().getDeclaredField("field_74278_d"); }
         speedField.setAccessible(true);
         speedField.setFloat(timerObj, speed);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private float getTimerSpeed() {
      try {
         Field timerField = null;
         try { timerField = Minecraft.class.getDeclaredField("timer"); }
         catch (NoSuchFieldException e) { timerField = Minecraft.class.getDeclaredField("field_71428_T"); }
         timerField.setAccessible(true);
         Object timerObj = timerField.get(mc);

         Field speedField = null;
         try { speedField = timerObj.getClass().getDeclaredField("timerSpeed"); }
         catch (NoSuchFieldException e) { speedField = timerObj.getClass().getDeclaredField("field_74278_d"); }
         speedField.setAccessible(true);
         return speedField.getFloat(timerObj);
      } catch (Exception e) {
         return 1.0f;
      }
   }
}