package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class PlayerUtils {
   private static final double diagonal = 1.0 / Math.sqrt(2.0);
   private static final Vec3 horizontalVelocity = new Vec3(0.0, 0.0, 0.0);
   private static final Color color = new Color();

   private PlayerUtils() {
   }

   public static Color getPlayerColor(Player entity, Color defaultColor) {
      if (Friends.get().isFriend(entity)) {
         return color.set(Config.get().friendColor.get()).a(defaultColor.a);
      } else {
         return Config.get().useTeamColor.get() && !color.set(TextUtils.getMostPopularColor(entity.getDisplayName())).equals(Utils.WHITE)
            ? color.a(defaultColor.a)
            : defaultColor;
      }
   }

   public static Vec3 getHorizontalVelocity(double bps) {
      float yaw = MeteorClient.mc.player.getYRot();
      if (PathManagers.get().isPathing()) {
         yaw = PathManagers.get().getTargetYaw();
      }

      Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
      Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
      double velX = 0.0;
      double velZ = 0.0;
      boolean a = false;
      if (MeteorClient.mc.player.input.keyPresses.forward()) {
         velX += forward.x / 20.0 * bps;
         velZ += forward.z / 20.0 * bps;
         a = true;
      }

      if (MeteorClient.mc.player.input.keyPresses.backward()) {
         velX -= forward.x / 20.0 * bps;
         velZ -= forward.z / 20.0 * bps;
         a = true;
      }

      boolean b = false;
      if (MeteorClient.mc.player.input.keyPresses.right()) {
         velX += right.x / 20.0 * bps;
         velZ += right.z / 20.0 * bps;
         b = true;
      }

      if (MeteorClient.mc.player.input.keyPresses.left()) {
         velX -= right.x / 20.0 * bps;
         velZ -= right.z / 20.0 * bps;
         b = true;
      }

      if (a && b) {
         velX *= diagonal;
         velZ *= diagonal;
      }

      ((IVec3)horizontalVelocity).meteor$setXZ(velX, velZ);
      return horizontalVelocity;
   }

   public static void centerPlayer() {
      double x = Mth.floor(MeteorClient.mc.player.getX()) + 0.5;
      double z = Mth.floor(MeteorClient.mc.player.getZ()) + 0.5;
      MeteorClient.mc.player.setPos(x, MeteorClient.mc.player.getY(), z);
      MeteorClient.mc
         .player
         .connection
         .send(
            new Pos(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               MeteorClient.mc.player.onGround(),
               MeteorClient.mc.player.horizontalCollision
            )
         );
   }

   public static boolean canSeeEntity(Entity entity) {
      Vec3 vec1 = new Vec3(0.0, 0.0, 0.0);
      Vec3 vec2 = new Vec3(0.0, 0.0, 0.0);
      ((IVec3)vec1)
         .meteor$set(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY() + MeteorClient.mc.player.getEyeHeight(), MeteorClient.mc.player.getZ());
      ((IVec3)vec2).meteor$set(entity.getX(), entity.getY(), entity.getZ());
      boolean canSeeFeet = MeteorClient.mc.level.clip(new ClipContext(vec1, vec2, Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player)).getType() == Type.MISS;
      ((IVec3)vec2).meteor$set(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
      boolean canSeeEyes = MeteorClient.mc.level.clip(new ClipContext(vec1, vec2, Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player)).getType() == Type.MISS;
      return canSeeFeet || canSeeEyes;
   }

   public static float[] calculateAngle(Vec3 target) {
      Vec3 eyesPos = new Vec3(
         MeteorClient.mc.player.getX(),
         MeteorClient.mc.player.getY() + MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()),
         MeteorClient.mc.player.getZ()
      );
      double dX = target.x - eyesPos.x;
      double dY = (target.y - eyesPos.y) * -1.0;
      double dZ = target.z - eyesPos.z;
      double dist = Math.sqrt(dX * dX + dZ * dZ);
      return new float[]{(float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0), (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dY, dist)))};
   }

   public static boolean shouldPause(boolean ifBreaking, boolean ifEating, boolean ifDrinking) {
      if (ifBreaking && MeteorClient.mc.gameMode.isDestroying()) {
         return true;
      } else {
         return !ifEating
               || !MeteorClient.mc.player.isUsingItem()
               || !MeteorClient.mc.player.getMainHandItem().getItem().components().has(DataComponents.FOOD)
                  && !MeteorClient.mc.player.getOffhandItem().getItem().components().has(DataComponents.FOOD)
            ? ifDrinking
               && MeteorClient.mc.player.isUsingItem()
               && (
                  MeteorClient.mc.player.getMainHandItem().getItem() instanceof PotionItem
                     || MeteorClient.mc.player.getOffhandItem().getItem() instanceof PotionItem
               )
            : true;
      }
   }

   public static boolean isMoving() {
      return MeteorClient.mc.player.zza != 0.0F || MeteorClient.mc.player.xxa != 0.0F;
   }

   public static boolean isSprinting() {
      return MeteorClient.mc.player.isSprinting() && (MeteorClient.mc.player.zza != 0.0F || MeteorClient.mc.player.xxa != 0.0F);
   }

   public static boolean isInHole(boolean doubles) {
      if (!Utils.canUpdate()) {
         return false;
      }

      BlockPos blockPos = MeteorClient.mc.player.blockPosition();
      int air = 0;

      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP) {
            BlockState state = MeteorClient.mc.level.getBlockState(blockPos.relative(direction));
            if (state.getBlock().getExplosionResistance() < 600.0F) {
               if (!doubles || direction == Direction.DOWN) {
                  return false;
               }

               air++;

               for (Direction dir : Direction.values()) {
                  if (dir != direction.getOpposite() && dir != Direction.UP) {
                     BlockState blockState1 = MeteorClient.mc.level.getBlockState(blockPos.relative(direction).relative(dir));
                     if (blockState1.getBlock().getExplosionResistance() < 600.0F) {
                        return false;
                     }
                  }
               }
            }
         }
      }

      return air < 2;
   }

   public static float possibleHealthReductions() {
      return possibleHealthReductions(true, true);
   }

   public static float possibleHealthReductions(boolean entities, boolean fall) {
      float damageTaken = 0.0F;
      if (entities) {
         for (Entity entity : MeteorClient.mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal) {
               float crystalDamage = DamageUtils.crystalDamage(MeteorClient.mc.player, entity.position());
               if (crystalDamage > damageTaken) {
                  damageTaken = crystalDamage;
               }
            } else if (entity instanceof Player player && !Friends.get().isFriend(player) && isWithin(entity, 5.0)) {
               float attackDamage = DamageUtils.getAttackDamage(player, MeteorClient.mc.player);
               if (attackDamage > damageTaken) {
                  damageTaken = attackDamage;
               }
            }
         }

         if (MeteorClient.mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE).explodes()) {
            for (BlockEntity blockEntity : Utils.blockEntities()) {
               BlockPos bp = blockEntity.getBlockPos();
               Vec3 pos = new Vec3(bp.getX(), bp.getY(), bp.getZ());
               if (blockEntity instanceof BedBlockEntity) {
                  float explosionDamage = DamageUtils.bedDamage(MeteorClient.mc.player, pos);
                  if (explosionDamage > damageTaken) {
                     damageTaken = explosionDamage;
                  }
               }
            }
         }
      }

      if (fall && !Modules.get().isActive(NoFall.class) && MeteorClient.mc.player.fallDistance > 3.0) {
         float damage = DamageUtils.fallDamage(MeteorClient.mc.player);
         if (damage > damageTaken && !EntityUtils.isAboveWater(MeteorClient.mc.player)) {
            damageTaken = damage;
         }
      }

      return damageTaken;
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
   }

   public static double distanceTo(Entity entity) {
      return distanceTo(entity.getX(), entity.getY(), entity.getZ());
   }

   public static double distanceTo(BlockPos blockPos) {
      return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   public static double distanceTo(Vec3 vec3d) {
      return distanceTo(vec3d.x(), vec3d.y(), vec3d.z());
   }

   public static double distanceTo(double x, double y, double z) {
      return Math.sqrt(squaredDistanceTo(x, y, z));
   }

   public static double squaredDistanceTo(Entity entity) {
      return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
   }

   public static double squaredDistanceTo(BlockPos blockPos) {
      return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   public static double squaredDistanceTo(double x, double y, double z) {
      return squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), x, y, z);
   }

   public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double f = x1 - x2;
      double g = y1 - y2;
      double h = z1 - z2;
      return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
   }

   public static boolean isWithin(Entity entity, double r) {
      return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ()) <= r * r;
   }

   public static boolean isWithin(Vec3 vec3d, double r) {
      return squaredDistanceTo(vec3d.x(), vec3d.y(), vec3d.z()) <= r * r;
   }

   public static boolean isWithin(BlockPos blockPos, double r) {
      return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ()) <= r * r;
   }

   public static boolean isWithin(double x, double y, double z, double r) {
      return squaredDistanceTo(x, y, z) <= r * r;
   }

   public static double distanceToCamera(double x, double y, double z) {
      return Math.sqrt(squaredDistanceToCamera(x, y, z));
   }

   public static double distanceToCamera(Entity entity) {
      return distanceToCamera(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
   }

   public static double squaredDistanceToCamera(double x, double y, double z) {
      Vec3 cameraPos = MeteorClient.mc.gameRenderer.getMainCamera().position();
      return squaredDistance(cameraPos.x, cameraPos.y, cameraPos.z, x, y, z);
   }

   public static double squaredDistanceToCamera(Entity entity) {
      return squaredDistanceToCamera(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
   }

   public static boolean isWithinCamera(Entity entity, double r) {
      return squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) <= r * r;
   }

   public static boolean isWithinCamera(Vec3 vec3d, double r) {
      return squaredDistanceToCamera(vec3d.x(), vec3d.y(), vec3d.z()) <= r * r;
   }

   public static boolean isWithinCamera(BlockPos blockPos, double r) {
      return squaredDistanceToCamera(blockPos.getX(), blockPos.getY(), blockPos.getZ()) <= r * r;
   }

   public static boolean isWithinCamera(double x, double y, double z, double r) {
      return squaredDistanceToCamera(x, y, z) <= r * r;
   }

   public static boolean isWithinReach(Entity entity) {
      return isWithinReach(entity.getX(), entity.getY(), entity.getZ());
   }

   public static boolean isWithinReach(Vec3 vec3d) {
      return isWithinReach(vec3d.x(), vec3d.y(), vec3d.z());
   }

   public static boolean isWithinReach(BlockPos blockPos) {
      return isWithinReach(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   public static boolean isWithinReach(double x, double y, double z) {
      return squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getEyeY(), MeteorClient.mc.player.getZ(), x, y, z)
         <= MeteorClient.mc.player.blockInteractionRange() * MeteorClient.mc.player.blockInteractionRange();
   }

   public static Dimension getDimension() {
      if (MeteorClient.mc.level == null) {
         return Dimension.Overworld;
      }

      return switch (MeteorClient.mc.level.dimension().identifier().getPath()) {
         case "the_nether" -> Dimension.Nether;
         case "the_end" -> Dimension.End;
         default -> Dimension.Overworld;
      };
   }

   public static GameType getGameMode() {
      if (MeteorClient.mc.player == null) {
         return null;
      }

      PlayerInfo playerListEntry = MeteorClient.mc.getConnection().getPlayerInfo(MeteorClient.mc.player.getUUID());
      return playerListEntry == null ? null : playerListEntry.getGameMode();
   }

   public static float getTotalHealth() {
      return MeteorClient.mc.player.getHealth() + MeteorClient.mc.player.getAbsorptionAmount();
   }

   public static boolean isAlive() {
      return MeteorClient.mc.player.isAlive() && !MeteorClient.mc.player.isDeadOrDying();
   }

   public static int getPing() {
      if (MeteorClient.mc.getConnection() == null) {
         return 0;
      }

      PlayerInfo playerListEntry = MeteorClient.mc.getConnection().getPlayerInfo(MeteorClient.mc.player.getUUID());
      return playerListEntry == null ? 0 : playerListEntry.getLatency();
   }
}
