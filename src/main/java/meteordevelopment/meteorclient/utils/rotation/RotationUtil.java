package meteordevelopment.meteorclient.utils.rotation;

import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RotationUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   private RotationUtil() {
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction bestSide = findBestDirection(pos, true);
      if (bestSide != null) {
         return bestSide;
      }

      bestSide = findBestDirection(pos, false);
      return bestSide != null ? bestSide : Direction.UP;
   }

   private static Direction findBestDirection(BlockPos pos, boolean useVisibilityCheck) {
      if (mc.player == null) {
         return null;
      }

      Direction bestSide = null;
      double minDistance = Double.MAX_VALUE;
      Vec3 eyePos = mc.player.getEyePosition();

      for (Direction side : Direction.values()) {
         if (useVisibilityCheck ? canSee(pos, side) : isGrimDirection(pos, side)) {
            double distance = eyePos.distanceToSqr(Vec3.atCenterOf(pos.relative(side)));
            if (distance < minDistance) {
               minDistance = distance;
               bestSide = side;
            }
         }
      }

      return bestSide;
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      if (mc.player != null && mc.level != null) {
         Vec3 testVec = pos.getCenter().add(side.getUnitVec3i().getX() * 0.5, side.getUnitVec3i().getY() * 0.5, side.getUnitVec3i().getZ() * 0.5);
         HitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), testVec, Block.COLLIDER, Fluid.NONE, mc.player));
         return result.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   private static boolean isIntersected(AABB box, AABB other) {
      return other.maxX - 1.0E-7 > box.minX
         && other.minX + 1.0E-7 < box.maxX
         && other.maxY - 1.0E-7 > box.minY
         && other.minY + 1.0E-7 < box.maxY
         && other.maxZ - 1.0E-7 > box.minZ
         && other.minZ + 1.0E-7 < box.maxZ;
   }

   private static AABB getCombinedBox(BlockPos pos, Level level) {
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).move(pos);
      AABB combined = new AABB(pos);

      for (AABB box : shape.toAabbs()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   public static boolean isGrimDirection(BlockPos pos, Direction direction) {
      if (mc.player != null && mc.level != null) {
         AABB combined = getCombinedBox(pos, mc.level);
         LocalPlayer player = mc.player;
         AABB eyePositions = new AABB(player.getX(), player.getY() + 0.4, player.getZ(), player.getX(), player.getY() + 1.62, player.getZ()).inflate(2.0E-4);
         if (isIntersected(eyePositions, combined)) {
            return true;
         }

         switch (direction) {
            case NORTH:
               if (!(eyePositions.minZ > combined.minZ)) {
                  return true;
               }
               break;
            case SOUTH:
               if (!(eyePositions.maxZ < combined.maxZ)) {
                  return true;
               }
               break;
            case EAST:
               if (!(eyePositions.maxX < combined.maxX)) {
                  return true;
               }
               break;
            case WEST:
               if (!(eyePositions.minX > combined.minX)) {
                  return true;
               }
               break;
            case UP:
               if (!(eyePositions.maxY < combined.maxY)) {
                  return true;
               }
               break;
            case DOWN:
               if (!(eyePositions.minY > combined.minY)) {
                  return true;
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return false;
      } else {
         return false;
      }
   }

   public static Direction getDirection(BlockPos blockPos) {
      if (mc.player != null && mc.level != null) {
         double eyePos = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
         VoxelShape outline = mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos);
         if (eyePos > blockPos.getY() + outline.max(Axis.Y) && mc.level.getBlockState(blockPos.above()).canBeReplaced()) {
            return Direction.UP;
         } else if (eyePos < blockPos.getY() + outline.min(Axis.Y) && mc.level.getBlockState(blockPos.below()).canBeReplaced()) {
            return Direction.DOWN;
         } else {
            BlockPos difference = blockPos.subtract(mc.player.blockPosition());
            if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
               return difference.getX() > 0 ? Direction.WEST : Direction.EAST;
            } else {
               return difference.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
            }
         }
      } else {
         return Direction.UP;
      }
   }

   public static boolean isInFov(LivingEntity entity, float fov) {
      if (mc.player == null) {
         return false;
      }

      if (fov >= 360.0F) {
         return true;
      }

      float yawDiff = Math.abs(Mth.wrapDegrees(getRotationsToEntity(entity).getYaw() - mc.player.getYRot()));
      return yawDiff <= fov / 2.0F;
   }

   public static Rotation getNearestRotations(AABB box) {
      return box == null ? null : rotationFromVec(getNearestPointBB(box));
   }

   public static Rotation getNearestRotationsWithNoise(AABB box) {
      return box == null ? null : rotationFromVecWithNoise(getNearestPointBB(box));
   }

   public static Vec3 getNearestPointBB(AABB box) {
      if (mc.player == null) {
         return null;
      }

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      Vec3 nearestPoint = new Vec3(Mth.clamp(eyePos.x, box.minX, box.maxX), Mth.clamp(eyePos.y, box.minY, box.maxY), Mth.clamp(eyePos.z, box.minZ, box.maxZ));
      if (isVisible(nearestPoint)) {
         return nearestPoint;
      }

      AABB scanBox = box.inflate(-0.002);
      double stepX = Math.max(scanBox.getXsize() / 2.0, 0.001);
      double stepY = Math.max(scanBox.getYsize() / 2.0, 0.001);
      double stepZ = Math.max(scanBox.getZsize() / 2.0, 0.001);
      Vec3 bestPoint = null;
      double minDistanceSq = Double.MAX_VALUE;

      for (double x = scanBox.minX; x <= scanBox.maxX; x += stepX) {
         for (double y = scanBox.minY; y <= scanBox.maxY; y += stepY) {
            for (double z = scanBox.minZ; z <= scanBox.maxZ; z += stepZ) {
               Vec3 point = new Vec3(x, y, z);
               if (isVisible(point)) {
                  double distSq = eyePos.distanceToSqr(point);
                  if (distSq < minDistanceSq) {
                     minDistanceSq = distSq;
                     bestPoint = point;
                  }
               }
            }
         }
      }

      return bestPoint != null ? bestPoint : nearestPoint;
   }

   public static boolean isVisible(Vec3 targetVec) {
      if (mc.player != null && mc.level != null && targetVec != null) {
         Vec3 eyePos = mc.player.getEyePosition();
         if (eyePos.distanceToSqr(targetVec) > 4096.0) {
            return false;
         }

         Vec3 lookVec = mc.player.getViewVector(1.0F);
         Vec3 toTarget = targetVec.subtract(eyePos).normalize();
         if (lookVec.dot(toTarget) < 0.0) {
            return false;
         }

         ClipContext context = new ClipContext(eyePos, targetVec, Block.COLLIDER, Fluid.NONE, mc.player);
         return mc.level.clip(context).getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static Rotation getRotationsToEntity(LivingEntity entity) {
      if (mc.player == null) {
         return new Rotation();
      }

      Vec3 eyePos = mc.player.getEyePosition();
      Vec3 targetPos = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0);
      double dx = targetPos.x - eyePos.x;
      double dy = targetPos.y - eyePos.y;
      double dz = targetPos.z - eyePos.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(-Math.atan2(dx, dz));
      float pitch = (float)Math.toDegrees(-Math.atan2(dy, dist));
      return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
   }

   public static double getEyeDistanceToEntity(LivingEntity entity) {
      if (mc.player == null) {
         return Double.MAX_VALUE;
      }

      Vec3 eyePos = mc.player.getEyePosition();
      AABB box = entity.getBoundingBox();
      double dx = Math.max(box.minX - eyePos.x, Math.max(0.0, eyePos.x - box.maxX));
      double dy = Math.max(box.minY - eyePos.y, Math.max(0.0, eyePos.y - box.maxY));
      double dz = Math.max(box.minZ - eyePos.z, Math.max(0.0, eyePos.z - box.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static Rotation calculate(Vec3 from, Vec3 to) {
      Vec3 diff = to.subtract(from);
      double distance = Math.hypot(diff.x, diff.z);
      float yaw = (float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, distance)));
      return new Rotation(yaw, pitch);
   }

   public static Rotation getRotationsToBlockWithNoise(BlockPos blockPos, float partialTicks) {
      if (mc.player == null) {
         return new Rotation();
      }

      Vec3 predictedPos = new Vec3(
         mc.player.getX() + mc.player.getDeltaMovement().x * partialTicks,
         mc.player.getY() + mc.player.getEyeHeight() + mc.player.getDeltaMovement().y * partialTicks,
         mc.player.getZ() + mc.player.getDeltaMovement().z * partialTicks
      );
      return rotationFromDeltas(
         addNoise(blockPos.getX() - predictedPos.x + 0.5), addNoise(blockPos.getY() - predictedPos.y + 0.5), addNoise(blockPos.getZ() - predictedPos.z + 0.5)
      );
   }

   public static Rotation getRotationsToBlock(BlockPos blockPos, float partialTicks) {
      if (mc.player == null) {
         return new Rotation();
      }

      Vec3 predictedPos = new Vec3(
         mc.player.getX() + mc.player.getDeltaMovement().x * partialTicks,
         mc.player.getY() + mc.player.getEyeHeight() + mc.player.getDeltaMovement().y * partialTicks,
         mc.player.getZ() + mc.player.getDeltaMovement().z * partialTicks
      );
      return rotationFromDeltas(blockPos.getX() - predictedPos.x + 0.5, blockPos.getY() - predictedPos.y + 0.5, blockPos.getZ() - predictedPos.z + 0.5);
   }

   private static double addNoise(double value) {
      double amount = ThreadLocalRandom.current().nextDouble(0.05, 0.08);
      return value + amount * (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0);
   }

   public static Rotation rotationFromDeltas(double dx, double dy, double dz) {
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontalDist)));
      return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
   }

   public static Rotation rotationFromVecWithNoise(Vec3 target) {
      return rotationFromCoordsWithNoise(target.x, target.y, target.z);
   }

   public static Rotation rotationFromVec(Vec3 target) {
      return rotationFromCoords(target.x, target.y, target.z);
   }

   public static Rotation rotationFromCoordsWithNoise(double x, double y, double z) {
      return mc.player == null
         ? new Rotation()
         : rotationFromPointsWithNoise(x, y, z, mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
   }

   public static Rotation rotationFromCoords(double x, double y, double z) {
      return mc.player == null ? new Rotation() : rotationFromPoints(x, y, z, mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
   }

   private static double normalizeAngle(double angle) {
      return ((angle + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
   }

   public static double angleDiffDouble(float angleA, float angleB) {
      return normalizeAngle(angleA - angleB);
   }

   public static Rotation rotationFromPointsWithNoise(double targetX, double targetY, double targetZ, double fromX, double fromY, double fromZ) {
      return rotationFromDeltas(addNoise(targetX - fromX), addNoise(targetY - fromY), addNoise(targetZ - fromZ));
   }

   public static Rotation rotationFromPoints(double targetX, double targetY, double targetZ, double fromX, double fromY, double fromZ) {
      return rotationFromDeltas(targetX - fromX, targetY - fromY, targetZ - fromZ);
   }

   public static float moveTowards(float maxStep, float current, float target) {
      return rotateTowards(current, target, maxStep);
   }

   public static float rotateTowards(float current, float target, float maxStep) {
      float diff = Mth.wrapDegrees(target - current);
      diff = Mth.clamp(diff, -maxStep, maxStep);
      return current + diff;
   }

   public static Rotation calculate(Entity entity) {
      return mc.player == null
         ? new Rotation()
         : calculate(
            entity.position()
               .add(0.0, Mth.clamp(mc.player.getY() - entity.getY() + mc.player.getEyeHeight(), 0.0, entity.getBoundingBox().getYsize() * 0.9), 0.0)
         );
   }

   public static Rotation calculate(Entity entity, boolean adaptive, double range) {
      Rotation normalRotations = calculate(entity);
      HitResult result = RaytraceUtils.raytrace(normalRotations, range, 0.0F);
      if (adaptive && (result == null || result.getType() != Type.ENTITY)) {
         AABB box = entity.getBoundingBox();
         Vec3 basePos = entity.position();

         for (double yPercent = 1.0; yPercent >= 0.0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1.0; xPercent >= -0.5; xPercent -= 0.5) {
               for (double zPercent = 1.0; zPercent >= -0.5; zPercent -= 0.5) {
                  Vec3 targetPoint = basePos.add(box.getXsize() * xPercent, box.getYsize() * yPercent, box.getZsize() * zPercent);
                  Rotation adaptiveRotations = calculate(targetPoint);
                  HitResult rayCastResult = RaytraceUtils.raytrace(adaptiveRotations, range, 0.0F);
                  if (rayCastResult != null && rayCastResult.getType() == Type.ENTITY) {
                     return adaptiveRotations;
                  }
               }
            }
         }

         return normalRotations;
      } else {
         return normalRotations;
      }
   }

   public static Rotation calculate(BlockPos to) {
      return mc.player == null ? new Rotation() : calculate(mc.player.getEyePosition(), to.getCenter());
   }

   public static Rotation calculate(Vec3 to) {
      return mc.player == null ? new Rotation() : calculate(mc.player.getEyePosition(), to);
   }

   public static Rotation calculate(Vec3 position, Direction direction) {
      double x = position.x + 0.5 + direction.getStepX() * 0.5;
      double y = position.y + 0.5 + direction.getStepY() * 0.5;
      double z = position.z + 0.5 + direction.getStepZ() * 0.5;
      return calculate(new Vec3(x, y, z));
   }

   public static Rotation calculate(BlockPos position, Direction direction) {
      double x = position.getX() + 0.5 + direction.getStepX() * 0.5;
      double y = position.getY() + 0.5 + direction.getStepY() * 0.5;
      double z = position.getZ() + 0.5 + direction.getStepZ() * 0.5;
      return calculate(new Vec3(x, y, z));
   }

   public static Rotation applySensitivityPatch(Rotation rotation) {
      return applySensitivityPatch(rotation, RotationManager.INSTANCE.getLastRotation());
   }

   public static Rotation applySensitivityPatch(Rotation rotation, Rotation previousRotation) {
      float mouseSensitivity = (float)(mc.options.sensitivity().get() * (1.0 + Math.random() / 1.0E7) * 0.6F + 0.2F);
      double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15;
      float yaw = previousRotation.getYaw() + (float)(Math.round((rotation.getYaw() - previousRotation.getYaw()) / multiplier) * multiplier);
      float pitch = previousRotation.getPitch() + (float)(Math.round((rotation.getPitch() - previousRotation.getPitch()) / multiplier) * multiplier);
      return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
   }

   public static Rotation relateToPlayerRotation(Rotation rotation) {
      Rotation previousRotation = RotationManager.INSTANCE.getLastRotation();
      float yaw = previousRotation.getYaw() + Mth.wrapDegrees(rotation.getYaw() - previousRotation.getYaw());
      float pitch = Mth.clamp(rotation.getPitch(), -90.0F, 90.0F);
      return new Rotation(yaw, pitch);
   }

   public static Rotation resetRotation(Rotation rotation) {
      if (rotation != null && mc.player != null) {
         float yaw = rotation.getYaw() + Mth.wrapDegrees(mc.player.getYRot() - rotation.getYaw());
         float pitch = mc.player.getXRot();
         return new Rotation(yaw, pitch);
      } else {
         return null;
      }
   }

   public static Rotation move(Rotation targetRotation, double speed) {
      return move(RotationManager.INSTANCE.lastRotations, targetRotation, speed);
   }

   public static Rotation move(Rotation lastRotation, Rotation targetRotation, double speed) {
      if (lastRotation != null && targetRotation != null && speed != 0.0) {
         double deltaYaw = Mth.wrapDegrees(targetRotation.getYaw() - lastRotation.getYaw());
         double deltaPitch = targetRotation.getPitch() - lastRotation.getPitch();
         double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
         if (distance < 1.0E-4) {
            return new Rotation();
         }

         double distributionYaw = Math.abs(deltaYaw / distance);
         double distributionPitch = Math.abs(deltaPitch / distance);
         double maxYaw = speed * distributionYaw;
         double maxPitch = speed * distributionPitch;
         float moveYaw = (float)Mth.clamp(deltaYaw, -maxYaw, maxYaw);
         float movePitch = (float)Mth.clamp(deltaPitch, -maxPitch, maxPitch);
         return new Rotation(moveYaw, movePitch);
      } else {
         return new Rotation();
      }
   }

   public static Rotation smooth(Rotation targetRotation, double speed) {
      return smooth(RotationManager.INSTANCE.lastRotations, targetRotation, speed);
   }

   public static Rotation smooth(Rotation lastRotation, Rotation targetRotation, double speed) {
      if (lastRotation != null && targetRotation != null) {
         float yaw = targetRotation.getYaw();
         float pitch = targetRotation.getPitch();
         if (speed != 0.0) {
            Rotation move = move(lastRotation, targetRotation, speed);
            yaw = lastRotation.getYaw() + move.getYaw();
            pitch = lastRotation.getPitch() + move.getPitch();
            int iterations = Math.max(1, (int)(mc.getFps() / 20.0F + Math.random() * 10.0));

            for (int i = 1; i <= iterations; i++) {
               if (Math.abs(move.getYaw()) + Math.abs(move.getPitch()) > 1.0E-4) {
                  yaw += (float)((Math.random() - 0.5) / 1000.0);
                  pitch -= (float)(Math.random() / 200.0);
               }

               Rotation fixedRotations = applySensitivityPatch(new Rotation(yaw, pitch));
               yaw = fixedRotations.getYaw();
               pitch = fixedRotations.getPitch();
            }
         }

         return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
      } else {
         return targetRotation == null ? new Rotation() : targetRotation.copy();
      }
   }
}
