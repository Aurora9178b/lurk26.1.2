package meteordevelopment.meteorclient.utils.rotation;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class RaytraceUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   private RaytraceUtils() {
   }

   public static HitResult clipWithEntity(Vec3 fromPos, Vec3 toPos, boolean clipFluids, boolean useCollider, boolean useVisual, Entity entity) {
      if (mc.level == null) {
         return null;
      }

      Block blockMode = useCollider ? Block.COLLIDER : (useVisual ? Block.VISUAL : Block.OUTLINE);
      Fluid fluidMode = clipFluids ? Fluid.ANY : Fluid.NONE;
      return mc.level.clip(new ClipContext(fromPos, toPos, blockMode, fluidMode, entity));
   }

   public static boolean canSeePointFrom(Vec3 eyes, Vec3 target) {
      return mc.level != null && mc.player != null && mc.level.clip(new ClipContext(eyes, target, Block.OUTLINE, Fluid.NONE, mc.player)).getType() == Type.MISS;
   }

   public static HitResult raytrace(Rotation rotation, double range) {
      return raytrace(rotation, range, 0.0F);
   }

   public static HitResult raytrace(Rotation rotation, double range, float expand) {
      return raytrace(rotation, range, expand, mc.player);
   }

   public static HitResult rayTrace(float partialTicks, Rotation rotation) {
      return mc.getCameraEntity() != null && mc.level != null ? rayTrace(4.5, partialTicks, true, rotation) : null;
   }

   public static HitResult rayTrace(double range, float partialTicks, boolean clipFluids, Rotation rotation) {
      return mc.getCameraEntity() != null && mc.level != null ? rayTrace(range, partialTicks, clipFluids, rotation.getYaw(), rotation.getPitch()) : null;
   }

   public static HitResult rayTrace(double range, float partialTicks, boolean clipFluids, float yaw, float pitch) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePos = mc.player.getEyePosition(partialTicks);
         Vec3 viewVec = getViewVector(pitch, yaw);
         Vec3 endPos = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
         return mc.level.clip(new ClipContext(eyePos, endPos, Block.OUTLINE, clipFluids ? Fluid.ANY : Fluid.NONE, mc.player));
      } else {
         return null;
      }
   }

   public static Vec3 getViewVector(float pitch, float yaw) {
      return Vec3.directionFromRotation(pitch, yaw);
   }

   public static boolean canRayTrace(Rotation rotation, Direction direction, BlockPos blockPos, boolean checkFace) {
      if (mc.player != null && mc.level != null && rotation != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 lookDir = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
         Vec3 endPos = eyePos.add(lookDir.scale(5.0));
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePos, endPos, Block.COLLIDER, Fluid.NONE, mc.player));
         if (blockHitResult.getType() != Type.BLOCK) {
            return false;
         }

         boolean samePos = blockHitResult.getBlockPos().equals(blockPos);
         boolean sameFace = !checkFace || blockHitResult.getDirection() == direction;
         return samePos && sameFace;
      } else {
         return false;
      }
   }

   public static HitResult raytrace(Rotation rotation, double range, float expand, Entity entity) {
      if (mc.level != null && entity != null && rotation != null) {
         float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 eyePos = entity.getEyePosition(partialTicks);
         Vec3 lookVec = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
         Vec3 endVec = eyePos.add(lookVec.scale(range));
         HitResult blockHit = mc.level.clip(new ClipContext(eyePos, endVec, Block.OUTLINE, Fluid.NONE, entity));
         double closestDistance = range;
         if (blockHit.getType() != Type.MISS) {
            closestDistance = blockHit.getLocation().distanceTo(eyePos);
         }

         Entity pointedEntity = null;
         Vec3 hitVec = null;
         AABB searchBox = entity.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);

         for (Entity candidate : mc.level.getEntities(entity, searchBox, e -> !e.isSpectator() && e.isPickable())) {
            float collisionSize = candidate.getPickRadius() + expand;
            AABB entityBox = candidate.getBoundingBox().inflate(collisionSize);
            Optional<Vec3> intercept = entityBox.clip(eyePos, endVec);
            if (entityBox.contains(eyePos)) {
               if (closestDistance >= 0.0) {
                  pointedEntity = candidate;
                  hitVec = intercept.orElse(eyePos);
                  closestDistance = 0.0;
               }
            } else if (intercept.isPresent()) {
               Vec3 interceptVec = intercept.get();
               double distance = eyePos.distanceTo(interceptVec);
               if (distance < closestDistance || closestDistance == 0.0) {
                  if (candidate.getRootVehicle() == entity.getRootVehicle()) {
                     if (closestDistance == 0.0) {
                        pointedEntity = candidate;
                        hitVec = interceptVec;
                     }
                  } else {
                     pointedEntity = candidate;
                     hitVec = interceptVec;
                     closestDistance = distance;
                  }
               }
            }
         }

         return pointedEntity == null || !(closestDistance < range) && blockHit.getType() != Type.MISS ? blockHit : new EntityHitResult(pointedEntity, hitVec);
      } else {
         return null;
      }
   }

   public static boolean overBlock(Rotation rotation, Direction direction, BlockPos pos, boolean strict) {
      if (mc.player != null && mc.level != null && rotation != null) {
         Vec3 lookVec = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 endVec = eyePos.add(lookVec.scale(4.5));
         BlockHitResult result = mc.level.clip(new ClipContext(eyePos, endVec, Block.OUTLINE, Fluid.NONE, mc.player));
         return result.getType() != Type.MISS && result.getBlockPos().equals(pos) && (!strict || result.getDirection() == direction);
      } else {
         return false;
      }
   }

   public static boolean overBlock(Rotation rotation, BlockPos pos, boolean strict) {
      return overBlock(rotation, Direction.UP, pos, strict);
   }

   public static boolean overBlock(Rotation rotation, BlockPos pos) {
      return overBlock(rotation, Direction.UP, pos, false);
   }

   public static boolean overBlock(Rotation rotation, BlockPos pos, Direction direction) {
      return overBlock(rotation, direction, pos, true);
   }
}
