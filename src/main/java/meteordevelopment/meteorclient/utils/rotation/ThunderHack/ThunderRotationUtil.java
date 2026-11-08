package meteordevelopment.meteorclient.utils.rotation.ThunderHack;

import java.util.List;
import java.util.Optional;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ThunderRotationUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   private ThunderRotationUtil() {
   }

   public static ThunderRotation from(Rotation rotation) {
      return rotation == null ? new ThunderRotation() : new ThunderRotation(rotation.getYaw(), rotation.getPitch());
   }

   public static Rotation toMeteor(ThunderRotation rotation) {
      return rotation == null ? new Rotation() : new Rotation(rotation.yaw(), rotation.pitch());
   }

   public static ThunderRotation calcAngle(Vec3 to) {
      return mc.player != null && to != null ? calcAngle(eyePos(), to) : new ThunderRotation();
   }

   public static ThunderRotation calcAngle(Vec3 from, Vec3 to) {
      if (from != null && to != null) {
         double difX = to.x - from.x;
         double difY = (to.y - from.y) * -1.0;
         double difZ = to.z - from.z;
         double dist = Math.sqrt(difX * difX + difZ * difZ);
         float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
         float pitch = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)));
         return new ThunderRotation(yaw, pitch);
      } else {
         return new ThunderRotation();
      }
   }

   public static ThunderRotation calcAngle(Entity entity) {
      return entity == null ? new ThunderRotation() : calcAngle(entity.getEyePosition());
   }

   public static Vec3 getRotationVector(float yaw, float pitch) {
      return Vec3.directionFromRotation(pitch, yaw);
   }

   public static Vec3 eyePos() {
      return mc.player == null ? Vec3.ZERO : new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
   }

   public static double gcd() {
      if (mc.options == null) {
         return 0.15;
      }

      double sensitivity = mc.options.sensitivity().get();
      return Math.pow(sensitivity * 0.6 + 0.2, 3.0) * 1.2;
   }

   public static ThunderRotation applyGcd(ThunderRotation current, ThunderRotation target) {
      if (current != null && target != null) {
         double gcd = gcd();
         float yaw = (float)(target.yaw() - (target.yaw() - current.yaw()) % gcd);
         float pitch = (float)(target.pitch() - (target.pitch() - current.pitch()) % gcd);
         return new ThunderRotation(yaw, pitch);
      } else {
         return target == null ? new ThunderRotation() : target.copy();
      }
   }

   public static ThunderRotation step(ThunderRotation current, ThunderRotation target, float yawStep, float pitchStep) {
      if (current != null && target != null) {
         float deltaYaw = Mth.wrapDegrees(target.yaw() - current.yaw());
         float deltaPitch = target.pitch() - current.pitch();
         float clampedYaw = Mth.clamp(Math.abs(deltaYaw), 0.0F, Math.abs(yawStep));
         float clampedPitch = Mth.clamp(deltaPitch, -Math.abs(pitchStep), Math.abs(pitchStep));
         float newYaw = current.yaw() + (deltaYaw > 0.0F ? clampedYaw : -clampedYaw);
         float newPitch = Mth.clamp(current.pitch() + clampedPitch, -90.0F, 90.0F);
         return applyGcd(current, new ThunderRotation(newYaw, newPitch));
      } else {
         return target == null ? new ThunderRotation() : target.copy();
      }
   }

   public static ThunderRotation stepToPoint(ThunderRotation current, Vec3 point, float yawStep, float pitchStep) {
      return step(current, calcAngle(point), yawStep, pitchStep);
   }

   public static HitResult rayTrace(double distance, float yaw, float pitch) {
      if (mc.player != null && mc.level != null) {
         Vec3 start = eyePos();
         Vec3 direction = getRotationVector(yaw, pitch);
         Vec3 end = start.add(direction.scale(distance));
         return mc.level.clip(new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, mc.player));
      } else {
         return null;
      }
   }

   public static HitResult rayTrace(double distance, ThunderRotation rotation) {
      return rotation == null ? null : rayTrace(distance, rotation.yaw(), rotation.pitch());
   }

   public static EntityHitResult getRtxTarget(float yaw, float pitch, float distance, boolean ignoreWalls) {
      if (mc.player != null && mc.level != null) {
         HitResult blockResult = ignoreWalls ? null : rayTrace(distance, yaw, pitch);
         Vec3 start = eyePos();
         double maxDistanceSq = distance * distance;
         if (blockResult != null && blockResult.getType() != Type.MISS) {
            maxDistanceSq = start.distanceToSqr(blockResult.getLocation());
         }

         Vec3 direction = getRotationVector(yaw, pitch);
         Vec3 end = start.add(direction.scale(distance));
         AABB box = mc.player.getBoundingBox().expandTowards(direction.scale(distance)).inflate(1.0, 1.0, 1.0);
         List<Entity> entities = mc.level.getEntities(mc.player, box, entityx -> !entityx.isSpectator() && entityx.isPickable());
         Entity pointedEntity = null;
         Vec3 hitVec = null;
         double closest = maxDistanceSq;

         for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = entityBox.clip(start, end);
            if (entityBox.contains(start)) {
               if (closest >= 0.0) {
                  pointedEntity = entity;
                  hitVec = hit.orElse(start);
                  closest = 0.0;
               }
            } else if (hit.isPresent()) {
               Vec3 hitPos = hit.get();
               double distanceSq = start.distanceToSqr(hitPos);
               if (distanceSq < closest || closest == 0.0) {
                  pointedEntity = entity;
                  hitVec = hitPos;
                  closest = distanceSq;
               }
            }
         }

         return pointedEntity == null ? null : new EntityHitResult(pointedEntity, hitVec);
      } else {
         return null;
      }
   }

   public static Vec3 getRtxPoint(float yaw, float pitch, float distance) {
      EntityHitResult result = getRtxTarget(yaw, pitch, distance, true);
      return result == null ? null : result.getLocation();
   }

   public static boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity target) {
      return target == null ? false : checkRtx(yaw, pitch, distance, wallDistance, ThunderRayTraceMode.OnlyTarget, target);
   }

   public static boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, ThunderRayTraceMode mode, Entity target) {
      if (mode == ThunderRayTraceMode.Off) {
         return true;
      }

      if (mc.player != null && mc.level != null) {
         HitResult blockResult = rayTrace(distance, yaw, pitch);
         Vec3 start = eyePos();
         double distanceSq = distance * distance;
         if (blockResult != null && blockResult.getType() != Type.MISS) {
            distanceSq = start.distanceToSqr(blockResult.getLocation());
         }

         Vec3 direction = getRotationVector(yaw, pitch);
         Vec3 end = start.add(direction.scale(distance));
         AABB area = mc.player.getBoundingBox().expandTowards(direction.scale(distance)).inflate(1.0, 1.0, 1.0);
         List<Entity> entities = mc.level.getEntities(mc.player, area, entityx -> !entityx.isSpectator() && entityx.isPickable());
         double maxDistanceSq = Math.max(distanceSq, wallDistance * wallDistance);
         EntityHitResult best = null;
         double bestDistanceSq = maxDistanceSq;

         for (Entity entity : entities) {
            if (mode != ThunderRayTraceMode.OnlyTarget || target == null || entity == target) {
               AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
               Optional<Vec3> hit = entityBox.clip(start, end);
               if (!hit.isEmpty()) {
                  double hitDistanceSq = start.distanceToSqr(hit.get());
                  if (hitDistanceSq <= bestDistanceSq) {
                     best = new EntityHitResult(entity, hit.get());
                     bestDistanceSq = hitDistanceSq;
                  }
               }
            }
         }

         if (best == null) {
            return false;
         }

         double entityDistanceSq = start.distanceToSqr(best.getLocation());
         boolean allowedWallDistance = entityDistanceSq <= wallDistance * wallDistance;
         boolean wallMissing = blockResult == null || blockResult.getType() == Type.MISS;
         boolean wallBehindEntity = entityDistanceSq < distanceSq;
         boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;
         boolean correctTarget = target == null || best.getEntity() == target || mode == ThunderRayTraceMode.AllEntities;
         return allowWallHit && entityDistanceSq <= distance * distance && correctTarget;
      } else {
         return false;
      }
   }

   public static boolean isLookingAtBlock(float yaw, float pitch, BlockPos blockPos, double distance) {
      if (mc.player != null && mc.level != null && blockPos != null) {
         Vec3 start = eyePos();
         Vec3 end = start.add(getRotationVector(yaw, pitch).scale(distance));
         BlockHitResult result = mc.level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, mc.player));
         return result.getType() == Type.BLOCK && result.getBlockPos().equals(blockPos);
      } else {
         return false;
      }
   }

   public static void fixMovement(MoveInputEvent event, float yaw) {
      if (event != null && mc.player != null) {
         float forward = event.getForward();
         float strafe = event.getStrafe();
         float directionFactor = Math.max(Math.abs(forward), Math.abs(strafe));
         double angleDifference = Mth.wrapDegrees(getMovementDirection(forward, strafe) - yaw);
         double angleDistance = Math.abs(angleDifference);
         forward = 0.0F;
         strafe = 0.0F;
         if (angleDistance <= 67.5) {
            forward++;
         } else if (angleDistance >= 112.5) {
            forward--;
         }

         if (angleDifference >= 22.5 && angleDifference <= 157.5) {
            strafe--;
         } else if (angleDifference <= -22.5 && angleDifference >= -157.5) {
            strafe++;
         }

         event.setForward(forward * directionFactor);
         event.setStrafe(strafe * directionFactor);
      }
   }

   public static Vec3 fixVelocity(float yaw, Vec3 movementInput, float speed) {
      if (movementInput == null) {
         return Vec3.ZERO;
      }

      double lengthSq = movementInput.lengthSqr();
      if (lengthSq < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 vector = lengthSq > 1.0 ? movementInput.normalize().scale(speed) : movementInput.scale(speed);
      float radians = (float)Math.toRadians(yaw);
      float sin = Mth.sin(radians);
      float cos = Mth.cos(radians);
      return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.z * cos + vector.x * sin);
   }

   private static float getMovementDirection(float forward, float strafe) {
      if (mc.player == null) {
         return 0.0F;
      }

      float direction = mc.player.getYRot();
      boolean movingForward = forward > 0.0F;
      boolean movingBack = forward < 0.0F;
      boolean movingLeft = strafe > 0.0F;
      boolean movingRight = strafe < 0.0F;
      boolean movingSideways = movingLeft || movingRight;
      boolean movingStraight = movingForward || movingBack;
      if (forward != 0.0F || strafe != 0.0F) {
         if (movingBack && !movingSideways) {
            return direction + 180.0F;
         }

         if (movingForward && movingLeft) {
            return direction + 45.0F;
         }

         if (movingForward && movingRight) {
            return direction - 45.0F;
         }

         if (!movingStraight && movingLeft) {
            return direction + 90.0F;
         }

         if (!movingStraight && movingRight) {
            return direction - 90.0F;
         }

         if (movingBack && movingLeft) {
            return direction + 135.0F;
         }

         if (movingBack) {
            return direction - 135.0F;
         }
      }

      return direction;
   }
}
