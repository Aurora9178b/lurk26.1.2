package meteordevelopment.meteorclient.utils.leaves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeavesBlockUtil {
   public static final CopyOnWriteArrayList<BlockPos> placeList = new CopyOnWriteArrayList<>();
   public static final List<Block> shiftBlocks = Arrays.asList(
      Blocks.ENDER_CHEST,
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.CRAFTING_TABLE,
      Blocks.ANVIL,
      Blocks.BREWING_STAND,
      Blocks.HOPPER,
      Blocks.DROPPER,
      Blocks.DISPENSER,
      Blocks.ENCHANTING_TABLE,
      Blocks.WHITE_SHULKER_BOX,
      Blocks.ORANGE_SHULKER_BOX,
      Blocks.MAGENTA_SHULKER_BOX,
      Blocks.LIGHT_BLUE_SHULKER_BOX,
      Blocks.YELLOW_SHULKER_BOX,
      Blocks.LIME_SHULKER_BOX,
      Blocks.PINK_SHULKER_BOX,
      Blocks.GRAY_SHULKER_BOX,
      Blocks.CYAN_SHULKER_BOX,
      Blocks.PURPLE_SHULKER_BOX,
      Blocks.BLUE_SHULKER_BOX,
      Blocks.BROWN_SHULKER_BOX,
      Blocks.GREEN_SHULKER_BOX,
      Blocks.RED_SHULKER_BOX,
      Blocks.BLACK_SHULKER_BOX
   );

   private LeavesBlockUtil() {
   }

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction side = null;
      double minDistance = Double.MAX_VALUE;
      if (MeteorClient.mc.player == null) {
         return null;
      }

      for (Direction direction : Direction.values()) {
         if (isGrimDirection(pos, direction)) {
            double distance = MeteorClient.mc.player.getEyePosition().distanceToSqr(pos.relative(direction).getCenter());
            if (!(distance > minDistance)) {
               side = direction;
               minDistance = distance;
            }
         }
      }

      return side;
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignorePlayer) {
      for (Entity entity : getEntities(new AABB(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof ThrownExperienceBottle)
            && !(entity instanceof Arrow)
            && (!ignoreCrystal || !(entity instanceof EndCrystal))
            && (!ignorePlayer || !(entity instanceof Player))) {
            return false;
         }
      }

      return true;
   }

   public static boolean canClick(BlockPos pos) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         BlockState state = MeteorClient.mc.level.getBlockState(pos);
         return !state.getCollisionShape(MeteorClient.mc.level, pos).isEmpty() && (!isClickable(getBlock(pos)) || MeteorClient.mc.player.isShiftKeyDown());
      } else {
         return false;
      }
   }

   public static boolean canClick(BlockPos pos, boolean ignoreSneak) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         BlockState state = MeteorClient.mc.level.getBlockState(pos);
         return !state.getCollisionShape(MeteorClient.mc.level, pos).isEmpty()
            && (!isClickable(getBlock(pos)) || MeteorClient.mc.player.isShiftKeyDown() || ignoreSneak);
      } else {
         return false;
      }
   }

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, null);
   }

   public static boolean canPlace(BlockPos pos, Predicate<Direction> directionPredicate) {
      if (MeteorClient.mc.level == null || pos == null) {
         return false;
      } else if (getPlaceSide(pos, directionPredicate) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, false);
      }
   }

   public static boolean canReplace(BlockPos pos) {
      return MeteorClient.mc.level != null && pos.getY() < MeteorClient.mc.level.getHeight() ? MeteorClient.mc.level.getBlockState(pos).canBeReplaced() : false;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new AABB(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof ThrownExperienceBottle)
            && !(entity instanceof Arrow)
            && (!ignoreCrystal || !(entity instanceof EndCrystal))) {
            return true;
         }
      }

      return false;
   }

   public static List<Entity> getEntities(AABB box) {
      List<Entity> list = new ArrayList<>();
      if (MeteorClient.mc.level == null) {
         return list;
      }

      for (Entity entity : MeteorClient.mc.level.entitiesForRendering()) {
         if (entity != null && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static ArrayList<BlockPos> getSphere(double range) {
      return MeteorClient.mc.player == null ? new ArrayList<>() : getSphere(range, MeteorClient.mc.player.getEyePosition());
   }

   public static ArrayList<BlockPos> getSphere(double range, Vec3 pos) {
      ArrayList<BlockPos> list = new ArrayList<>();

      for (double x = pos.x - range; x < pos.x + range; x++) {
         for (double z = pos.z - range; z < pos.z + range; z++) {
            for (double y = pos.y - range; y < pos.y + range; y++) {
               BlockPos curPos = new LeavesBlockPos(x, y, z);
               if (!(curPos.getCenter().distanceTo(pos) > range) && !list.contains(curPos)) {
                  list.add(curPos);
               }
            }
         }
      }

      return list;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for (Entity entity : getEndCrystals(new AABB(pos))) {
         if (entity.isAlive()) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystalPlace(BlockPos pos) {
      for (EndCrystal crystal : getEndCrystals(new AABB(pos))) {
         if (crystal.isAlive() && crystal.blockPosition().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public static List<EndCrystal> getEndCrystals(AABB box) {
      List<EndCrystal> list = new ArrayList<>();
      if (MeteorClient.mc.level == null) {
         return list;
      }

      for (Entity entity : MeteorClient.mc.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal crystal && crystal.getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> directionPredicate) {
      if (pos != null && MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         double distance = Double.MAX_VALUE;
         Direction side = null;

         for (Direction direction : Direction.values()) {
            if (directionPredicate == null || directionPredicate.test(direction)) {
               BlockPos neighbor = pos.relative(direction);
               BlockState neighborState = MeteorClient.mc.level.getBlockState(neighbor);
               if (canClick(neighbor) && !neighborState.canBeReplaced() && isGrimDirection(neighbor, direction.getOpposite())) {
                  double vecDis = MeteorClient.mc
                     .player
                     .getEyePosition()
                     .distanceToSqr(pos.getCenter().add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5));
                  if (side == null || vecDis < distance) {
                     side = direction;
                     distance = vecDis;
                  }
               }
            }
         }

         return side;
      } else {
         return null;
      }
   }

   public static ArrayList<Direction> getPlaceSides(BlockPos pos, Predicate<Direction> directionPredicate) {
      ArrayList<Direction> sides = new ArrayList<>();
      if (pos != null && MeteorClient.mc.level != null) {
         for (Direction direction : Direction.values()) {
            if (directionPredicate == null || directionPredicate.test(direction)) {
               BlockPos neighbor = pos.relative(direction);
               BlockState neighborState = MeteorClient.mc.level.getBlockState(neighbor);
               if (canClick(neighbor) && !neighborState.canBeReplaced() && isGrimDirection(neighbor, direction.getOpposite())) {
                  sides.add(direction);
               }
            }
         }

         return sides;
      } else {
         return sides;
      }
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         Vec3 testVec = pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
         HitResult result = MeteorClient.mc
            .level
            .clip(
               new ClipContext(
                  MeteorClient.mc.player.getEyePosition(), testVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player
               )
            );
         return result == null || result.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static Direction getClickSide(BlockPos pos) {
      if (MeteorClient.mc.player == null) {
         return Direction.UP;
      }

      Direction side = null;
      double range = Double.MAX_VALUE;

      for (Direction direction : Direction.values()) {
         if (canSee(pos, direction)) {
            double distance = MeteorClient.mc.player.getEyePosition().distanceToSqr(pos.relative(direction).getCenter());
            if (!(distance > range)) {
               side = direction;
               range = distance;
            }
         }
      }

      if (side != null) {
         return side;
      }

      side = Direction.UP;

      for (Direction direction : Direction.values()) {
         if (isGrimDirection(pos, direction)) {
            double distance = MeteorClient.mc.player.getEyePosition().distanceToSqr(pos.relative(direction).getCenter());
            if (!(distance > range)) {
               side = direction;
               range = distance;
            }
         }
      }

      return side;
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

   private static boolean isIntersected(AABB box, AABB other) {
      return other.maxX - 1.0E-7 > box.minX
         && other.minX + 1.0E-7 < box.maxX
         && other.maxY - 1.0E-7 > box.minY
         && other.minY + 1.0E-7 < box.maxY
         && other.maxZ - 1.0E-7 > box.minZ
         && other.minZ + 1.0E-7 < box.maxZ;
   }

   public static boolean isGrimDirection(BlockPos pos, Direction direction) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         AABB combined = getCombinedBox(pos, MeteorClient.mc.level);
         LocalPlayer player = MeteorClient.mc.player;
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

   public static Block getBlock(BlockPos pos) {
      if (MeteorClient.mc.level == null) {
         return Blocks.AIR;
      } else {
         return LeavesCombatUtil.modifyPos != null && LeavesCombatUtil.modifyPos.equals(pos)
            ? LeavesCombatUtil.modifyBlockState.getBlock()
            : MeteorClient.mc.level.getBlockState(pos).getBlock();
      }
   }

   public static void placeBlock(BlockPos pos, Direction side, boolean rotate) {
      clickBlock(pos.relative(side), side.getOpposite(), rotate);
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && MeteorClient.mc.level != null) {
         Vec3 directionVec = pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
         if (rotate) {
            LeavesRotation.snapAt(directionVec);
         }

         LeavesEntityUtil.placeSwingHand();
         BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
         InteractionResult interaction = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, InteractionHand.MAIN_HAND, result);
         if (!interaction.consumesAction() && MeteorClient.mc.getConnection() != null) {
            MeteorClient.mc
               .gameMode
               .startPrediction(MeteorClient.mc.level, sequence -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, sequence));
         }

         placeList.add(pos);
         if (rotate) {
            LeavesRotation.snapBack();
         }
      }
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.level.isEmptyBlock(pos)) {
         BlockPos obsPos = pos.below();
         BlockPos boost = obsPos.above();
         return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
            && getClickSideStrict(obsPos) != null
            && MeteorClient.mc.level.isEmptyBlock(boost)
            && !hasEntityBlockCrystal(boost, false)
            && !hasEntityBlockCrystal(boost.above(), false);
      } else {
         return false;
      }
   }

   public static boolean hasEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new AABB(pos))) {
         if (entity.isAlive() && (!ignoreCrystal || !(entity instanceof EndCrystal))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isClickable(Block block) {
      return block instanceof CraftingTableBlock
         || block instanceof AnvilBlock
         || block instanceof LoomBlock
         || block instanceof CartographyTableBlock
         || block instanceof GrindstoneBlock
         || block instanceof StonecutterBlock
         || block instanceof ButtonBlock
         || block instanceof BasePressurePlateBlock
         || block instanceof BaseEntityBlock
         || block instanceof BedBlock
         || block instanceof FenceGateBlock
         || block instanceof DoorBlock
         || block instanceof NoteBlock
         || block instanceof TrapDoorBlock;
   }
}
