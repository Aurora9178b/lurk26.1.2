package meteordevelopment.meteorclient.systems.managers;

import java.util.function.Function;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.RotationAnimationEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.movement.MovementFix;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class RotationManager {
   public static final RotationManager INSTANCE = new RotationManager();
   private static final double DEFAULT_ROTATION_BACK_SPEED = 10.0;
   private final Rotation offset = new Rotation();
   public Rotation rotations;
   public Rotation lastRotations = new Rotation();
   public Rotation targetRotations;
   private float renderPitch;
   private float renderBodyYaw;
   private float renderHeadYaw;
   private float lastRenderPitch;
   private float lastRenderBodyYaw;
   private float lastRenderHeadYaw;
   private static final float RETURN_RENDER_SPEED = 25.0F;
   private boolean initialized;
   private boolean active;
   private boolean returning;
   private boolean smoothed;
   private double rotationSpeed;
   private double rotationBackSpeed = 180.0;
   private Function<Rotation, Boolean> raytrace;
   private float randomAngle;
   private boolean serverCorrection;
   private int priority;
   private Runnable callback;
   private boolean rotationRequested;

   public void init() {
      if (!this.initialized) {
         MeteorClient.EVENT_BUS.subscribe(this);
         this.initialized = true;
      }
   }

   public void setRotations(Rotation rotations, double rotationSpeed) {
      this.setRotations(rotations, rotationSpeed, 10.0, null, Priority.Medium, null);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, Priority priority) {
      this.setRotations(rotations, rotationSpeed, 10.0, null, priority, null);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, Function<Rotation, Boolean> raytrace) {
      this.setRotations(rotations, rotationSpeed, 10.0, raytrace, Priority.Medium, null);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, Function<Rotation, Boolean> raytrace, Priority priority) {
      this.setRotations(rotations, rotationSpeed, 10.0, raytrace, priority, null);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, Function<Rotation, Boolean> raytrace, Priority priority, Runnable callback) {
      this.setRotations(rotations, rotationSpeed, 10.0, raytrace, priority, callback);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, double rotationBackSpeed) {
      this.setRotations(rotations, rotationSpeed, rotationBackSpeed, null, Priority.Medium, null);
   }

   public void setRotations(Rotation rotations, double rotationSpeed, double rotationBackSpeed, Priority priority) {
      this.setRotations(rotations, rotationSpeed, rotationBackSpeed, null, priority, null);
   }

   public Rotation getRotationsWithNoise(Rotation rotations) {
      return this.getRotationsWithNoise(rotations, null);
   }

   public Rotation getRotationsWithNoise(Rotation rotations, Function<Rotation, Boolean> raytrace) {
      if (rotations == null) {
         return null;
      }

      for (int i = 0; i < 5; i++) {
         Rotation noisy = new Rotation(rotations.getYaw() + random(-0.35F, 0.35F), Mth.clamp(rotations.getPitch() + random(-0.25F, 0.25F), -90.0F, 90.0F));
         if (raytrace == null || raytrace.apply(noisy)) {
            return noisy;
         }
      }

      return rotations.copy();
   }

   public void setRotationsWithNoise(Rotation rotations, double rotationSpeed) {
      this.setRotationsWithNoise(rotations, rotationSpeed, 10.0, null, Priority.Medium);
   }

   public void setRotationsWithNoise(Rotation rotations, double rotationSpeed, double rotationBackSpeed) {
      this.setRotationsWithNoise(rotations, rotationSpeed, rotationBackSpeed, null, Priority.Medium);
   }

   public void setRotationsWithNoise(Rotation rotations, double rotationSpeed, double rotationBackSpeed, Function<Rotation, Boolean> raytrace) {
      this.setRotationsWithNoise(rotations, rotationSpeed, rotationBackSpeed, raytrace, Priority.Medium);
   }

   public void setRotationsWithNoise(
      Rotation rotations, double rotationSpeed, double rotationBackSpeed, Function<Rotation, Boolean> raytrace, Priority priority
   ) {
      this.setRotations(this.getRotationsWithNoise(rotations, raytrace), rotationSpeed, rotationBackSpeed, priority);
   }

   public void setRotationsWithNoise(LivingEntity entity, double rotationSpeed) {
      this.setRotationsWithNoise(entity, rotationSpeed, 10.0, Priority.Medium);
   }

   public void setRotationsWithNoise(LivingEntity entity, double rotationSpeed, Priority priority) {
      this.setRotationsWithNoise(entity, rotationSpeed, 10.0, priority);
   }

   public void setRotationsWithNoise(LivingEntity entity, double rotationSpeed, double rotationBackSpeed) {
      this.setRotationsWithNoise(entity, rotationSpeed, rotationBackSpeed, Priority.Medium);
   }

   public void setRotationsWithNoise(LivingEntity entity, double rotationSpeed, double rotationBackSpeed, Priority priority) {
      if (entity != null) {
         Rotation rotations = RotationUtil.getNearestRotationsWithNoise(entity.getBoundingBox());
         if (rotations == null) {
            rotations = RotationUtil.getRotationsToEntity(entity);
         }

         this.setRotations(rotations, rotationSpeed, rotationBackSpeed, priority);
      }
   }

   public void setRotations(
      Rotation rotations, double rotationSpeed, double rotationBackSpeed, Function<Rotation, Boolean> raytrace, Priority priority, Runnable callback
   ) {
      if (rotations != null && MeteorClient.mc.player != null) {
         if (!this.active || priority.priority >= this.priority) {
            if (this.serverCorrection) {
               this.resetToPlayer();
               this.serverCorrection = false;
            } else {
               this.targetRotations = rotations.copy();
               this.rotationSpeed = rotationSpeed * 18.0;
               this.rotationBackSpeed = rotationBackSpeed * 18.0;
               this.raytrace = raytrace;
               this.priority = priority.priority;
               this.callback = callback;
               this.active = true;
               this.returning = false;
               this.rotationRequested = true;
               this.smooth();
            }
         }
      }
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isSmoothed() {
      return this.smoothed;
   }

   public Rotation getRotation() {
      if (this.active && this.rotations != null) {
         return this.rotations;
      } else {
         return MeteorClient.mc.player == null ? new Rotation() : new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
      }
   }

   public Rotation getLastRotation() {
      if (this.lastRotations != null) {
         return this.lastRotations;
      } else {
         return MeteorClient.mc.player == null ? new Rotation() : new Rotation(MeteorClient.mc.player.yRotO, MeteorClient.mc.player.xRotO);
      }
   }

   public float getYaw() {
      return this.getRotation().getYaw();
   }

   public float getPitch() {
      return this.getRotation().getPitch();
   }

   public boolean isDone() {
      return this.rotations != null
         && this.targetRotations != null
         && Math.abs(Mth.wrapDegrees(this.rotations.getYaw() - this.targetRotations.getYaw())) <= 1.0F
         && Math.abs(this.rotations.getPitch() - this.targetRotations.getPitch()) <= 1.0F;
   }

   public void reset() {
      this.startReturning();
   }

   private void hardReset() {
      this.active = false;
      this.returning = false;
      this.smoothed = false;
      this.priority = 0;
      this.callback = null;
      this.raytrace = null;
      this.rotationRequested = false;
      this.serverCorrection = false;
      this.rotationBackSpeed = 180.0;
      this.offset.set(0.0F, 0.0F);
      if (MeteorClient.mc.player != null) {
         this.rotations = this.lastRotations = this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         this.resetRenderRotation();
      } else {
         this.rotations = null;
         this.lastRotations = null;
         this.targetRotations = null;
         this.renderPitch = this.renderBodyYaw = this.renderHeadYaw = 0.0F;
         this.lastRenderPitch = this.lastRenderBodyYaw = this.lastRenderHeadYaw = 0.0F;
      }
   }

   private void resetToPlayer() {
      if (MeteorClient.mc.player == null) {
         this.hardReset();
      } else {
         this.rotations = this.lastRotations = this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         this.callback = null;
         this.raytrace = null;
         this.priority = 0;
         this.rotationBackSpeed = 180.0;
         this.active = false;
         this.returning = false;
         this.smoothed = false;
         this.resetRenderRotation();
      }
   }

   private void startReturning() {
      if (MeteorClient.mc.player == null) {
         this.hardReset();
      } else {
         this.active = false;
         this.smoothed = false;
         this.priority = 0;
         this.callback = null;
         this.raytrace = null;
         this.rotationRequested = false;
         this.offset.set(0.0F, 0.0F);
         this.rotations = this.lastRotations = this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         if (!this.isRenderRotationCloseToPlayer()) {
            this.returning = true;
         } else {
            this.returning = false;
            this.resetRenderRotation();
         }
      }
   }

   private void smooth() {
      if (!this.smoothed && this.targetRotations != null) {
         if (this.lastRotations == null && MeteorClient.mc.player != null) {
            this.lastRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         }

         float targetYaw = this.targetRotations.getYaw();
         float targetPitch = this.targetRotations.getPitch();
         if (this.raytrace != null
            && this.rotations != null
            && (Math.abs(targetYaw - this.rotations.getYaw()) > 5.0F || Math.abs(targetPitch - this.rotations.getPitch()) > 5.0F)) {
            Rotation trueTargetRotations = this.targetRotations.copy();
            double speed = Math.random() * Math.random() * Math.random() * 20.0;
            this.randomAngle = this.randomAngle
               + (float)(
                  (20.0 + (float)(Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360.0))
                     * (MeteorClient.mc.player.tickCount / 10 % 2 == 0 ? -1 : 1)
               );
            this.offset
               .set(
                  (float)(this.offset.getYaw() - Mth.sin((float)Math.toRadians(this.randomAngle)) * speed),
                  (float)(this.offset.getPitch() + Mth.cos((float)Math.toRadians(this.randomAngle)) * speed)
               );
            targetYaw += this.offset.getYaw();
            targetPitch += this.offset.getPitch();
            if (!this.raytrace.apply(new Rotation(targetYaw, targetPitch))) {
               this.randomAngle = (float)Math.toDegrees(Math.atan2(trueTargetRotations.getYaw() - targetYaw, targetPitch - trueTargetRotations.getPitch()))
                  - 180.0F;
               targetYaw -= this.offset.getYaw();
               targetPitch -= this.offset.getPitch();
               this.offset
                  .set(
                     (float)(this.offset.getYaw() - Mth.sin((float)Math.toRadians(this.randomAngle)) * speed),
                     (float)(this.offset.getPitch() + Mth.cos((float)Math.toRadians(this.randomAngle)) * speed)
                  );
               targetYaw += this.offset.getYaw();
               targetPitch += this.offset.getPitch();
            }

            if (!this.raytrace.apply(new Rotation(targetYaw, targetPitch))) {
               this.offset.set(0.0F, 0.0F);
               targetYaw = (float)(this.targetRotations.getYaw() + Math.random() * 2.0);
               targetPitch = (float)(this.targetRotations.getPitch() + Math.random() * 2.0);
            }
         }

         this.rotations = RotationUtil.smooth(new Rotation(targetYaw, targetPitch), this.rotationSpeed + Math.random());
         this.smoothed = true;
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.hardReset();
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.hardReset();
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerPositionPacket || event.packet instanceof ClientboundPlayerRotationPacket) {
         this.serverCorrection = true;
      }
   }

   @EventHandler
   private void onAnimation(RotationAnimationEvent event) {
      if (this.active || this.returning) {
         switch (event.getPart()) {
            case Body:
               event.setYaw(this.renderBodyYaw);
               event.setLastYaw(this.lastRenderBodyYaw);
               break;
            case Head:
               event.setYaw(this.renderHeadYaw);
               event.setLastYaw(this.lastRenderHeadYaw);
               break;
            case Pitch:
               event.setPitch(this.renderPitch);
               event.setLastPitch(this.lastRenderPitch);
               break;
            case All:
               event.setYaw(this.renderHeadYaw);
               event.setLastYaw(this.lastRenderHeadYaw);
               event.setPitch(this.renderPitch);
               event.setLastPitch(this.lastRenderPitch);
         }
      }
   }

   @EventHandler(priority = 100)
   private void onMoveInput(MoveInputEvent event) {
      if (MeteorClient.mc.player != null && !event.isMovementFixed() && !MeteorClient.mc.player.isFallFlying()) {
         if (MovementFix.shouldFix(this)) {
            MovementFix.fixMovement(event, this.rotations.getYaw());
         }
      }
   }

   @EventHandler(priority = -200)
   private void onTick(TickEvent.Pre event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         if (this.rotations == null || this.lastRotations == null || this.targetRotations == null) {
            this.rotations = this.lastRotations = this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         }

         if (this.active) {
            if (!this.rotationRequested && this.callback == null) {
               this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
               this.rotationSpeed = this.rotationBackSpeed;
            }

            this.smooth();
         } else {
            this.rotations = this.lastRotations = this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
            this.priority = 0;
            this.raytrace = null;
            this.callback = null;
            this.offset.set(0.0F, 0.0F);
         }

         this.rotationRequested = false;
         this.smoothed = false;
      }
   }

   @EventHandler(priority = -300)
   private void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getCameraEntity() == MeteorClient.mc.player) {
         if (this.active && this.rotations != null) {
            float yaw = this.rotations.getYaw();
            float pitch = Mth.clamp(this.rotations.getPitch(), -90.0F, 90.0F);
            if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
               event.setYaw(yaw);
               event.setPitch(pitch);
            }

            if (Math.abs(Mth.wrapDegrees(this.rotations.getYaw() - MeteorClient.mc.player.getYRot())) < 1.0F
               && Math.abs(this.rotations.getPitch() - MeteorClient.mc.player.getXRot()) < 1.0F) {
               this.active = false;
               this.returning = true;
               this.priority = 0;
               this.callback = null;
               this.correctDisabledRotations();
            }

            this.lastRotations = this.rotations.copy();
         } else {
            this.lastRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         }

         if (this.returning && !this.active) {
            this.updateReturningRenderRotation();
         } else {
            this.updateRenderRotation(event.getYaw(), event.getPitch());
         }

         this.targetRotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         this.raytrace = null;
         this.smoothed = false;
         Runnable runnable = this.callback;
         this.callback = null;
         if (runnable != null) {
            runnable.run();
         }
      }
   }

   private static float random(float min, float max) {
      return min + (float)Math.random() * (max - min);
   }

   private void correctDisabledRotations() {
      if (MeteorClient.mc.player != null && this.lastRotations != null) {
         Rotation rotations = new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot());
         Rotation fixedRotations = RotationUtil.resetRotation(RotationUtil.applySensitivityPatch(rotations, this.lastRotations));
         if (fixedRotations != null) {
            MeteorClient.mc.player.setYRot(fixedRotations.getYaw());
            MeteorClient.mc.player.setXRot(fixedRotations.getPitch());
         }
      }
   }

   private void updateRenderRotation(float yaw, float pitch) {
      if (MeteorClient.mc.player != null && !Float.isNaN(yaw) && !Float.isNaN(pitch)) {
         this.lastRenderPitch = this.renderPitch;
         this.lastRenderBodyYaw = this.renderBodyYaw;
         this.lastRenderHeadYaw = this.renderHeadYaw;
         this.renderBodyYaw = this.getRenderBodyYaw(yaw, this.renderBodyYaw);
         this.renderHeadYaw = yaw;
         this.renderPitch = pitch;
      }
   }

   private void updateReturningRenderRotation() {
      if (MeteorClient.mc.player == null) {
         this.hardReset();
      } else {
         this.lastRenderPitch = this.renderPitch;
         this.lastRenderBodyYaw = this.renderBodyYaw;
         this.lastRenderHeadYaw = this.renderHeadYaw;
         this.renderHeadYaw = this.approachDegrees(this.renderHeadYaw, MeteorClient.mc.player.getYRot(), 25.0F);
         this.renderBodyYaw = this.approachDegrees(this.renderBodyYaw, MeteorClient.mc.player.yBodyRot, 25.0F);
         this.renderPitch = this.approach(this.renderPitch, MeteorClient.mc.player.getXRot(), 25.0F);
         if (this.isRenderRotationCloseToPlayer()) {
            this.returning = false;
            this.resetRenderRotation();
         }
      }
   }

   private void resetRenderRotation() {
      if (MeteorClient.mc.player != null) {
         this.renderPitch = this.lastRenderPitch = MeteorClient.mc.player.getXRot();
         this.renderBodyYaw = this.lastRenderBodyYaw = MeteorClient.mc.player.yBodyRot;
         this.renderHeadYaw = this.lastRenderHeadYaw = MeteorClient.mc.player.yHeadRot;
      }
   }

   private boolean isRenderRotationCloseToPlayer() {
      return MeteorClient.mc.player != null
         && Math.abs(Mth.wrapDegrees(this.renderHeadYaw - MeteorClient.mc.player.getYRot())) <= 0.5F
         && Math.abs(Mth.wrapDegrees(this.renderBodyYaw - MeteorClient.mc.player.yBodyRot)) <= 0.5F
         && Math.abs(this.renderPitch - MeteorClient.mc.player.getXRot()) <= 0.5F;
   }

   private float approachDegrees(float current, float target, float speed) {
      return current + Mth.clamp(Mth.wrapDegrees(target - current), -speed, speed);
   }

   private float approach(float current, float target, float speed) {
      return current + Mth.clamp(target - current, -speed, speed);
   }

   private float getRenderBodyYaw(float yaw, float previousBodyYaw) {
      if (MeteorClient.mc.player == null) {
         return yaw;
      }

      float bodyYaw = previousBodyYaw;
      double x = MeteorClient.mc.player.getX() - MeteorClient.mc.player.xo;
      double z = MeteorClient.mc.player.getZ() - MeteorClient.mc.player.zo;
      if (x * x + z * z > 0.0025000002F) {
         float movementYaw = (float)(Mth.atan2(z, x) * 180.0F / (float)Math.PI) - 90.0F;
         float wrap = Math.abs(Mth.wrapDegrees(yaw) - movementYaw);
         bodyYaw = 95.0F < wrap && wrap < 265.0F ? movementYaw - 180.0F : movementYaw;
      }

      if (MeteorClient.mc.player.attackAnim > 0.0F) {
         bodyYaw = yaw;
      }

      bodyYaw = previousBodyYaw + Mth.wrapDegrees(bodyYaw - previousBodyYaw) * 0.3F;
      float offset = Mth.wrapDegrees(yaw - bodyYaw);
      offset = Mth.clamp(offset, -75.0F, 75.0F);
      bodyYaw = yaw - offset;
      if (offset * offset > 2500.0F) {
         bodyYaw += offset * 0.2F;
      }

      return bodyYaw;
   }
}
