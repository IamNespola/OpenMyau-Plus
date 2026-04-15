package myau.module.modules.combat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.UpdateEvent;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.player.Scaffold;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MSTimer;
import myau.util.PacketUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class BackTrack extends Module {

   public final BooleanProperty legit =
           new BooleanProperty("Legit", false);

   public final BooleanProperty releaseOnHit =
           new BooleanProperty("ReleaseOnHit", true, () -> this.legit.getValue());

   public final IntProperty delay =
           new IntProperty("Delay", 400, 0, 1000);

   public final FloatProperty hitRange =
           new FloatProperty("Range", 3.0f, 3.0f, 10.0f);

   public final BooleanProperty onlyIfNeeded =
           new BooleanProperty("OnlyIfNeeded", true);

   public final BooleanProperty esp =
           new BooleanProperty("ESP", true);

   public final ModeProperty espMode =
           new ModeProperty("ESPMODE", 0, new String[]{"Hitbox", "None"});

   private final Queue<Packet> incomingPackets = new LinkedList<>();
   private final Queue<Packet> outgoingPackets = new LinkedList<>();

   private final Map<Integer, Vec3> realPositions = new HashMap<>();

   private final MSTimer timer = new MSTimer();

   private EntityLivingBase target;
   private Vec3 lastRealPos;

   public BackTrack() {
      super("BackTrack", Category.COMBAT,false);
   }

   @Override
   public void onEnabled() {
      incomingPackets.clear();
      outgoingPackets.clear();
      realPositions.clear();
      lastRealPos = null;
      timer.reset();
   }

   @Override
   public void onDisabled() {
      releaseAll();
      incomingPackets.clear();
      outgoingPackets.clear();
      realPositions.clear();
      lastRealPos = null;
   }

   @EventTarget
   public void onPacket(PacketEvent event) {
      if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
         return;

      Module scaffold = Myau.moduleManager.getModule(Scaffold.class);
      if (scaffold != null && scaffold.isEnabled()) {
         releaseAll();
         incomingPackets.clear();
         outgoingPackets.clear();
         return;
      }

      if (event.getType() == EventType.RECEIVE) {
         handleIncoming(event);
      } else if (event.getType() == EventType.SEND) {
         handleOutgoing(event);
      }
   }

   private void handleIncoming(PacketEvent event) {
      Packet<?> packet = event.getPacket();

      if (packet instanceof S14PacketEntity) {
         S14PacketEntity p = (S14PacketEntity) packet;
         Entity e = p.getEntity(mc.theWorld);
         if (e == null) return;

         int id = e.getEntityId();
         Vec3 pos = realPositions.getOrDefault(id, new Vec3(0, 0, 0));

         realPositions.put(
                 id,
                 pos.addVector(
                         p.func_149062_c() / 32.0,
                         p.func_149061_d() / 32.0,
                         p.func_149064_e() / 32.0
                 )
         );
      }

      if (packet instanceof S18PacketEntityTeleport) {
         S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
         realPositions.put(
                 p.getEntityId(),
                 new Vec3(p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0)
         );
      }

      if (shouldQueue()) {
         if (blockIncoming(packet)) {
            incomingPackets.add(packet);
            event.setCancelled(true);
         }
      } else {
         releaseIncoming();
      }
   }

   private void handleOutgoing(PacketEvent event) {
      Packet<?> packet = event.getPacket();

      if (!legit.getValue())
         return;

      if (shouldQueue()) {
         if (blockOutgoing(packet)) {
            outgoingPackets.add(packet);
            event.setCancelled(true);
         }
      } else {
         releaseOutgoing();
      }
   }

   @EventTarget
   public void onUpdate(UpdateEvent e) {
      if (!isEnabled() || mc.thePlayer == null) return;

      EntityLivingBase newTarget = getClosestEntity();
      if (newTarget != target) {
         releaseAll();
         lastRealPos = null;
      }
      target = newTarget;

      if (target == null)
         return;

      Vec3 real = realPositions.get(target.getEntityId());
      if (real == null)
         return;

      double distReal = mc.thePlayer.getDistance(real.xCoord, real.yCoord, real.zCoord);
      double distCurrent = mc.thePlayer.getDistanceToEntity(target);

      if (mc.thePlayer.maxHurtTime > 0 && mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
         releaseAll();
      }

      if (distReal > hitRange.getValue() || timer.hasTimePassed(delay.getValue())) {
         releaseAll();
      }

      if (onlyIfNeeded.getValue()) {
         if (distCurrent <= distReal) {
            releaseAll();
         }

         if (lastRealPos != null) {
            double lastDist = mc.thePlayer.getDistance(
                    lastRealPos.xCoord,
                    lastRealPos.yCoord,
                    lastRealPos.zCoord
            );

            if (distReal < lastDist) {
               releaseAll();
            }
         }
      }

      if (legit.getValue() && releaseOnHit.getValue() && target.hurtTime == 1) {
         releaseAll();
      }

      lastRealPos = real;
   }

   @EventTarget
   public void onRender3D(Render3DEvent e) {
      if (!esp.getValue())
         return;

      if (espMode.getValue() != 0)
         return;

      if (target == null)
         return;

      Vec3 real = realPositions.get(target.getEntityId());
      if (real == null)
         return;

      double x = real.xCoord - mc.getRenderManager().viewerPosX;
      double y = real.yCoord - mc.getRenderManager().viewerPosY;
      double z = real.zCoord - mc.getRenderManager().viewerPosZ;

      AxisAlignedBB box = new AxisAlignedBB(
              x - target.width / 2,
              y,
              z - target.width / 2,
              x + target.width / 2,
              y + target.height,
              z + target.width / 2
      );

      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);

      GlStateManager.color(1F, 0F, 0F, 0.4F);

      RenderGlobal.drawOutlinedBoundingBox(box, 255, 0, 0, 153);

      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GlStateManager.popMatrix();
   }

   private boolean shouldQueue() {
      if (target == null)
         return false;

      Vec3 real = realPositions.get(target.getEntityId());
      if (real == null)
         return false;

      if (!onlyIfNeeded.getValue()) {
         double distReal = mc.thePlayer.getDistance(
                 real.xCoord, real.yCoord, real.zCoord);
         double distCurrent = mc.thePlayer.getDistanceToEntity(target);

         return distReal + 0.15 < distCurrent
                 && !timer.hasTimePassed(delay.getValue());
      }

      double distReal = mc.thePlayer.getDistance(
              real.xCoord, real.yCoord, real.zCoord
      );
      double distCurrent = mc.thePlayer.getDistanceToEntity(target);

      return distReal < distCurrent;
   }

   private void releaseIncoming() {
      if (mc.getNetHandler() == null)
         return;

      while (!incomingPackets.isEmpty()) {
         incomingPackets.poll().processPacket(mc.getNetHandler());
      }
      timer.reset();
   }

   private void releaseOutgoing() {
      while (!outgoingPackets.isEmpty()) {
         PacketUtil.sendPacketNoEvent(outgoingPackets.poll());
      }
      timer.reset();
   }

   private void releaseAll() {
      releaseIncoming();
      releaseOutgoing();
   }

   private boolean blockIncoming(Packet<?> p) {
      if (!onlyIfNeeded.getValue()) {

         if (p instanceof S12PacketEntityVelocity
                 || p instanceof S27PacketExplosion) {
            return false;
         }

         return p instanceof S14PacketEntity
                 || p instanceof S18PacketEntityTeleport
                 || p instanceof S19PacketEntityHeadLook
                 || p instanceof S0FPacketSpawnMob;
      }

      return p instanceof S12PacketEntityVelocity
              || p instanceof S27PacketExplosion
              || p instanceof S14PacketEntity
              || p instanceof S18PacketEntityTeleport
              || p instanceof S19PacketEntityHeadLook
              || p instanceof S0FPacketSpawnMob;

   }

   private boolean blockOutgoing(Packet<?> p) {
      return p instanceof C03PacketPlayer
              || p instanceof C02PacketUseEntity
              || p instanceof C0APacketAnimation
              || p instanceof C0BPacketEntityAction
              || p instanceof C08PacketPlayerBlockPlacement
              || p instanceof C07PacketPlayerDigging
              || p instanceof C09PacketHeldItemChange
              || p instanceof C00PacketKeepAlive
              || p instanceof C01PacketPing;
   }

   private EntityLivingBase getClosestEntity() {
      EntityLivingBase closest = null;
      double closestDist = Double.MAX_VALUE;
      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
            double dist = mc.thePlayer.getDistanceToEntity(entity);
            if (dist < closestDist && dist <= hitRange.getValue()) {
               closestDist = dist;
               closest = (EntityLivingBase) entity;
            }
         }
      }
      return closest;
   }
}


