package meteordevelopment.meteorclient.utils.rotation.ThunderHack;

import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ThunderRotationSystem {
   private static final Minecraft mc = Minecraft.getInstance();
   private ThunderRotation rotation = new ThunderRotation();
   private Vec3 rotationPoint = Vec3.ZERO;
   private Vec3 rotationMotion = Vec3.ZERO;
   private float pitchAcceleration = 1.0F;
   private float fixRotation = Float.NaN;

   public ThunderRotation rotation() {
      return this.rotation;
   }

   public Rotation meteorRotation() {
      return ThunderRotationUtil.toMeteor(this.rotation);
   }

   public float fixRotation() {
      return this.fixRotation;
   }

   public void reset() {
      this.rotationPoint = Vec3.ZERO;
      this.rotationMotion = Vec3.ZERO;
      this.pitchAcceleration = 1.0F;
      this.fixRotation = Float.NaN;
      if (mc.player != null) {
         this.rotation = new ThunderRotation(mc.player.getYRot(), mc.player.getXRot());
      } else {
         this.rotation = new ThunderRotation();
      }
   }

   public ThunderRotation update(Entity target, ThunderRotationSystem.Settings settings) {
      if (mc.player != null && target != null) {
         if (!this.rotation.valid()) {
            this.rotation = new ThunderRotation(mc.player.getYRot(), mc.player.getXRot());
         }

         Vec3 targetPoint = this.getLegitLook(target, settings);
         this.pitchAcceleration = ThunderRotationUtil.checkRtx(
               this.rotation.yaw(),
               this.rotation.pitch(),
               settings.range + settings.aimRange,
               settings.range + settings.aimRange,
               settings.rayTraceMode,
               target
            )
            ? settings.aimedPitchStep
            : (this.pitchAcceleration < settings.maxPitchStep ? this.pitchAcceleration * settings.pitchAccelerate : settings.maxPitchStep);
         float yawStep = settings.track ? random(settings.minYawStep, settings.maxYawStep) : 360.0F;
         float pitchStep = settings.track ? this.pitchAcceleration + random(-1.0F, 1.0F) : 180.0F;
         if (settings.ready) {
            switch (settings.accelerateOnHit) {
               case Off:
               default:
                  break;
               case Yaw:
                  yawStep = 180.0F;
                  break;
               case Pitch:
                  pitchStep = 90.0F;
                  break;
               case Both:
                  yawStep = 180.0F;
                  pitchStep = 90.0F;
            }
         }

         if (settings.trackTicks <= 0 && !settings.track) {
            this.rotation = new ThunderRotation(mc.player.getYRot(), mc.player.getXRot());
         } else {
            this.rotation = ThunderRotationUtil.stepToPoint(this.rotation, targetPoint, yawStep, pitchStep);
         }

         this.fixRotation = this.rotation.yaw();
         return this.rotation;
      } else {
         this.reset();
         return this.rotation;
      }
   }

   public boolean lookingAt(Entity target, ThunderRotationSystem.Settings settings) {
      return ThunderRotationUtil.checkRtx(this.rotation.yaw(), this.rotation.pitch(), settings.range, settings.wallRange, settings.rayTraceMode, target);
   }

   public boolean inRange(Entity target, ThunderRotationSystem.Settings settings) {
      if (mc.player != null && target != null) {
         if (ThunderRotationUtil.eyePos().distanceToSqr(target.getEyePosition()) > settings.squaredRotateDistance() + 4.0F) {
            return false;
         }

         double halfBox = target.getBoundingBox().getXsize() / 2.0;

         for (double x = -halfBox; x <= halfBox; x += 0.15) {
            for (double z = -halfBox; z <= halfBox; z += 0.15) {
               for (double y = 0.05; y <= target.getBoundingBox().getYsize(); y += 0.25) {
                  Vec3 point = new Vec3(target.getX() + x, target.getY() + y, target.getZ() + z);
                  if (!(ThunderRotationUtil.eyePos().distanceToSqr(point) > settings.squaredRotateDistance())) {
                     ThunderRotation rot = ThunderRotationUtil.calcAngle(point);
                     if (ThunderRotationUtil.checkRtx(
                        rot.yaw(), rot.pitch(), (float)Math.sqrt(settings.squaredRotateDistance()), settings.wallRange, settings.rayTraceMode, target
                     )) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public Vec3 getLegitLook(Entity target, ThunderRotationSystem.Settings settings) {
      if (target == null) {
         return Vec3.ZERO;
      }

      double lengthX = target.getBoundingBox().getXsize();
      double lengthY = target.getBoundingBox().getYsize();
      double lengthZ = target.getBoundingBox().getZsize();
      if (this.rotationMotion.equals(Vec3.ZERO)) {
         this.rotationMotion = new Vec3(random(-0.05F, 0.05F), random(-0.05F, 0.05F), random(-0.05F, 0.05F));
      }

      this.rotationPoint = this.rotationPoint.add(this.rotationMotion);
      if (this.rotationPoint.x >= (lengthX - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(-random(0.003F, 0.03F), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y >= lengthY) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, -random(0.001F, 0.03F), this.rotationMotion.z);
      }

      if (this.rotationPoint.z >= (lengthZ - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, -random(0.003F, 0.03F));
      }

      if (this.rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(random(0.003F, 0.03F), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y <= 0.05) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, random(0.001F, 0.03F), this.rotationMotion.z);
      }

      if (this.rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, random(0.003F, 0.03F));
      }

      this.rotationPoint = this.rotationPoint.add(random(-0.03F, 0.03F), 0.0, random(-0.03F, 0.03F));
      if (!mc.player.hasLineOfSight(target) && settings.wallBypass == ThunderRotationSystem.WallBypass.V1) {
         return target.position().add(random(-0.15F, 0.15F), lengthY, random(-0.15F, 0.15F));
      }

      if (!ThunderRotationUtil.checkRtx(this.rotation.yaw(), this.rotation.pitch(), settings.range, settings.wallRange, settings.rayTraceMode, target)) {
         Vec3 center = target.position().add(0.0, target.getEyeHeight(target.getPose()) / 2.0F, 0.0);
         ThunderRotation centerRotation = ThunderRotationUtil.calcAngle(center);
         if (ThunderRotationUtil.eyePos().distanceToSqr(center) <= settings.attackRange * settings.attackRange
            && ThunderRotationUtil.checkRtx(centerRotation.yaw(), centerRotation.pitch(), settings.range, 0.0F, settings.rayTraceMode, target)) {
            this.rotationPoint = new Vec3(random(-0.1F, 0.1F), target.getEyeHeight(target.getPose()) / random(1.8F, 2.5F), random(-0.1F, 0.1F));
         } else {
            this.scanVisiblePoint(target, settings, lengthX);
         }
      }

      return target.position().add(this.rotationPoint);
   }

   public void fixMovement(MoveInputEvent event, ThunderMoveFixMode mode) {
      if (mode == ThunderMoveFixMode.Free && !Float.isNaN(this.fixRotation)) {
         ThunderRotationUtil.fixMovement(event, this.fixRotation);
      }
   }

   private void scanVisiblePoint(Entity target, ThunderRotationSystem.Settings settings, double lengthX) {
      double halfBox = lengthX / 2.0;

      for (double x = -halfBox; x <= halfBox; x += 0.05) {
         for (double z = -halfBox; z <= halfBox; z += 0.05) {
            for (double y = 0.05; y <= target.getBoundingBox().getYsize(); y += 0.15) {
               Vec3 point = new Vec3(target.getX() + x, target.getY() + y, target.getZ() + z);
               if (!(ThunderRotationUtil.eyePos().distanceToSqr(point) > settings.attackRange * settings.attackRange)) {
                  ThunderRotation rot = ThunderRotationUtil.calcAngle(point);
                  if (ThunderRotationUtil.checkRtx(rot.yaw(), rot.pitch(), settings.range, 0.0F, settings.rayTraceMode, target)) {
                     this.rotationPoint = new Vec3(x, y, z);
                     return;
                  }
               }
            }
         }
      }
   }

   private static float random(float min, float max) {
      return min + ThreadLocalRandom.current().nextFloat() * (max - min);
   }

   public enum AccelerateOnHit {
      Off,
      Yaw,
      Pitch,
      Both;
   }

   public static class Settings {
      public boolean track = true;
      public boolean ready;
      public int trackTicks;
      public float range = 3.0F;
      public float wallRange = 0.0F;
      public float aimRange = 1.0F;
      public float attackRange = 3.0F;
      public float minYawStep = 45.0F;
      public float maxYawStep = 90.0F;
      public float aimedPitchStep = 6.0F;
      public float maxPitchStep = 45.0F;
      public float pitchAccelerate = 1.65F;
      public ThunderRayTraceMode rayTraceMode = ThunderRayTraceMode.OnlyTarget;
      public ThunderRotationSystem.AccelerateOnHit accelerateOnHit = ThunderRotationSystem.AccelerateOnHit.Off;
      public ThunderRotationSystem.WallBypass wallBypass = ThunderRotationSystem.WallBypass.Off;

      public float squaredRotateDistance() {
         float distance = this.range + this.aimRange;
         return distance * distance;
      }
   }

   public enum WallBypass {
      Off,
      V1,
      V2;
   }
}
