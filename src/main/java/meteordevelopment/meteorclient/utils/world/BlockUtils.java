package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockUtils {
   public static boolean breaking;
   private static boolean breakingThisTick;
   private static final ThreadLocal<MutableBlockPos> EXPOSED_POS = ThreadLocal.withInitial(MutableBlockPos::new);

   public static boolean isSafeBlock(BlockPos pos) {
      if (MeteorClient.mc.level == null) {
         return false;
      }

      BlockState state = MeteorClient.mc.level.getBlockState(pos);
      if (state.isAir()) {
         return false;
      }

      if (state.getBlock() instanceof LiquidBlock) {
         return false;
      }

      VoxelShape shape = state.getCollisionShape(MeteorClient.mc.level, pos);
      return !shape.isEmpty();
   }

   private BlockUtils() {
   }

   @PreInit
   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(BlockUtils.class);
   }

   public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority) {
      return place(blockPos, findItemResult, rotationPriority, true);
   }

   public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority) {
      return place(blockPos, findItemResult, rotate, rotationPriority, true);
   }

   public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean checkEntities) {
      return place(blockPos, findItemResult, rotate, rotationPriority, true, checkEntities);
   }

   public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority, boolean checkEntities) {
      return place(blockPos, findItemResult, true, rotationPriority, true, checkEntities);
   }

   public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities) {
      return place(blockPos, findItemResult, rotate, rotationPriority, swingHand, checkEntities, true);
   }

   public static boolean place(
      BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack
   ) {
      if (findItemResult.isOffhand()) {
         return place(
            blockPos,
            InteractionHand.OFF_HAND,
            MeteorClient.mc.player.getInventory().getSelectedSlot(),
            rotate,
            rotationPriority,
            swingHand,
            checkEntities,
            swapBack
         );
      } else {
         return findItemResult.isHotbar()
            ? place(blockPos, InteractionHand.MAIN_HAND, findItemResult.slot(), rotate, rotationPriority, swingHand, checkEntities, swapBack)
            : false;
      }
   }

   public static boolean place(
      BlockPos blockPos, InteractionHand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack
   ) {
      if (slot >= 0 && slot <= 8) {
         Block toPlace = Blocks.OBSIDIAN;
         ItemStack i = hand == InteractionHand.MAIN_HAND
            ? MeteorClient.mc.player.getInventory().getItem(slot)
            : MeteorClient.mc.player.getInventory().getItem(40);
         if (i.getItem() instanceof BlockItem blockItem) {
            toPlace = blockItem.getBlock();
         }

         if (!canPlaceBlock(blockPos, checkEntities, toPlace)) {
            return false;
         }

         Vec3 hitPos = Vec3.atCenterOf(blockPos);
         Direction side = getPlaceSide(blockPos);
         BlockPos neighbour;
         if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
         } else {
            neighbour = blockPos.relative(side);
            hitPos = hitPos.add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
         }

         BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
         if (rotate) {
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
               InvUtils.swap(slot, swapBack);
               interact(bhr, hand, swingHand);
               if (swapBack) {
                  InvUtils.swapBack();
               }
            });
         } else {
            InvUtils.swap(slot, swapBack);
            interact(bhr, hand, swingHand);
            if (swapBack) {
               InvUtils.swapBack();
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void interact(BlockHitResult blockHitResult, InteractionHand hand, boolean swing) {
      boolean wasSneaking = MeteorClient.mc.player.isShiftKeyDown();
      MeteorClient.mc.player.setShiftKeyDown(false);
      InteractionResult result = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, hand, blockHitResult);
      if (result.consumesAction()) {
         if (swing) {
            MeteorClient.mc.player.swing(hand);
         } else {
            MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(hand));
         }
      }

      MeteorClient.mc.player.setShiftKeyDown(wasSneaking);
   }

   public static boolean canPlaceBlock(BlockPos blockPos, boolean checkEntities, Block block) {
      if (blockPos == null) {
         return false;
      } else if (!Level.isInSpawnableBounds(blockPos)) {
         return false;
      } else {
         return !MeteorClient.mc.level.getBlockState(blockPos).canBeReplaced()
            ? false
            : !checkEntities || MeteorClient.mc.level.isUnobstructed(block.defaultBlockState(), blockPos, CollisionContext.empty());
      }
   }

   public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
      return canPlaceBlock(blockPos, checkEntities, Blocks.OBSIDIAN);
   }

   public static boolean canPlace(BlockPos blockPos) {
      return canPlace(blockPos, true);
   }

   public static Direction getPlaceSide(BlockPos blockPos) {
      Vec3 lookVec = blockPos.getCenter().subtract(MeteorClient.mc.player.getEyePosition());
      double bestRelevancy = -Double.MAX_VALUE;
      Direction bestSide = null;

      for (Direction side : Direction.values()) {
         BlockPos neighbor = blockPos.relative(side);
         BlockState state = MeteorClient.mc.level.getBlockState(neighbor);
         if (!state.isAir() && !isClickable(state.getBlock()) && state.getFluidState().isEmpty()) {
            double relevancy = side.getAxis().choose(lookVec.x(), lookVec.y(), lookVec.z()) * side.getAxisDirection().getStep();
            if (relevancy > bestRelevancy) {
               bestRelevancy = relevancy;
               bestSide = side;
            }
         }
      }

      return bestSide;
   }

   public static Direction getClosestPlaceSide(BlockPos blockPos) {
      return getClosestPlaceSide(blockPos, MeteorClient.mc.player.getEyePosition());
   }

   public static Direction getClosestPlaceSide(BlockPos blockPos, Vec3 pos) {
      Direction closestSide = null;
      double closestDistance = Double.MAX_VALUE;

      for (Direction side : Direction.values()) {
         BlockPos neighbor = blockPos.relative(side);
         BlockState state = MeteorClient.mc.level.getBlockState(neighbor);
         if (!state.isAir() && !isClickable(state.getBlock()) && state.getFluidState().isEmpty()) {
            double distance = pos.distanceToSqr(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            if (distance < closestDistance) {
               closestDistance = distance;
               closestSide = side;
            }
         }
      }

      return closestSide;
   }

   @EventHandler(priority = 300)
   private static void onTickPre(TickEvent.Pre event) {
      breakingThisTick = false;
   }

   @EventHandler(priority = -300)
   private static void onTickPost(TickEvent.Post event) {
      if (!breakingThisTick && breaking) {
         breaking = false;
         if (MeteorClient.mc.gameMode != null) {
            MeteorClient.mc.gameMode.stopDestroyBlock();
         }
      }
   }

   public static boolean breakBlock(BlockPos blockPos, boolean swing) {
      if (!canBreak(blockPos, MeteorClient.mc.level.getBlockState(blockPos))) {
         return false;
      }

      BlockPos pos = blockPos instanceof MutableBlockPos ? new BlockPos(blockPos) : blockPos;
      InstantRebreak ir = Modules.get().get(InstantRebreak.class);
      if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
         ir.sendPacket();
         return true;
      }

      if (MeteorClient.mc.gameMode.isDestroying()) {
         MeteorClient.mc.gameMode.continueDestroyBlock(pos, getDirection(blockPos));
      } else {
         MeteorClient.mc.gameMode.startDestroyBlock(pos, getDirection(blockPos));
      }

      if (swing) {
         MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
      } else {
         MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }

      breaking = true;
      breakingThisTick = true;
      return true;
   }

   public static boolean canBreak(BlockPos blockPos, BlockState state) {
      return !MeteorClient.mc.player.isCreative() && state.getDestroySpeed(MeteorClient.mc.level, blockPos) < 0.0F
         ? false
         : state.getShape(MeteorClient.mc.level, blockPos) != Shapes.empty();
   }

   public static boolean canBreak(BlockPos blockPos) {
      return canBreak(blockPos, MeteorClient.mc.level.getBlockState(blockPos));
   }

   public static boolean canInstaBreak(BlockPos blockPos, float breakSpeed) {
      return MeteorClient.mc.player.isCreative() || calcBlockBreakingDelta2(blockPos, breakSpeed) >= 1.0F;
   }

   public static boolean canInstaBreak(BlockPos blockPos) {
      BlockState state = MeteorClient.mc.level.getBlockState(blockPos);
      return canInstaBreak(blockPos, MeteorClient.mc.player.getDestroySpeed(state));
   }

   public static float calcBlockBreakingDelta2(BlockPos blockPos, float breakSpeed) {
      BlockState state = MeteorClient.mc.level.getBlockState(blockPos);
      float f = state.getDestroySpeed(MeteorClient.mc.level, blockPos);
      if (f == -1.0F) {
         return 0.0F;
      }

      int i = MeteorClient.mc.player.hasCorrectToolForDrops(state) ? 30 : 100;
      return breakSpeed / f / i;
   }

   public static boolean isClickable(Block block) {
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

   public static BlockUtils.MobSpawn isValidMobSpawn(BlockPos blockPos, BlockState blockState, int spawnLightLimit) {
      boolean snow = blockState.getBlock() instanceof SnowLayerBlock && blockState.getValue(SnowLayerBlock.LAYERS) == 1;
      if (!blockState.isAir() && !snow) {
         return BlockUtils.MobSpawn.Never;
      } else if (!isValidSpawnBlock(MeteorClient.mc.level.getBlockState(blockPos.below()))) {
         return BlockUtils.MobSpawn.Never;
      } else if (MeteorClient.mc.level.getBrightness(LightLayer.BLOCK, blockPos) > spawnLightLimit) {
         return BlockUtils.MobSpawn.Never;
      } else {
         return MeteorClient.mc.level.getBrightness(LightLayer.SKY, blockPos) > spawnLightLimit ? BlockUtils.MobSpawn.Potential : BlockUtils.MobSpawn.Always;
      }
   }

   public static boolean isValidSpawnBlock(BlockState blockState) {
      Block block = blockState.getBlock();
      if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block instanceof TransparentBlock || block instanceof ScaffoldingBlock) {
         return false;
      }

      if (block != Blocks.SOUL_SAND && block != Blocks.MUD) {
         if (block instanceof SlabBlock && blockState.getValue(SlabBlock.TYPE) == SlabType.TOP) {
            return true;
         } else {
            return block instanceof StairBlock && blockState.getValue(StairBlock.HALF) == Half.TOP ? true : blockState.isSolidRender();
         }
      } else {
         return true;
      }
   }

   public static Direction getDirection(BlockPos pos) {
      double eyePos = MeteorClient.mc.player.getY() + MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose());
      VoxelShape outline = MeteorClient.mc.level.getBlockState(pos).getCollisionShape(MeteorClient.mc.level, pos);
      if (eyePos > pos.getY() + outline.max(Axis.Y) && MeteorClient.mc.level.getBlockState(pos.above()).canBeReplaced()) {
         return Direction.UP;
      } else if (eyePos < pos.getY() + outline.min(Axis.Y) && MeteorClient.mc.level.getBlockState(pos.below()).canBeReplaced()) {
         return Direction.DOWN;
      } else {
         BlockPos difference = pos.subtract(MeteorClient.mc.player.blockPosition());
         if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
            return difference.getX() > 0 ? Direction.WEST : Direction.EAST;
         } else {
            return difference.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
         }
      }
   }

   public static boolean isExposed(BlockPos blockPos) {
      for (Direction direction : Direction.values()) {
         if (!MeteorClient.mc.level.getBlockState(EXPOSED_POS.get().setWithOffset(blockPos, direction)).isSolidRender()) {
            return true;
         }
      }

      return false;
   }

   public static double getBreakDelta(int slot, BlockState state) {
      float hardness = state.getDestroySpeed(null, null);
      return hardness == -1.0F
         ? 0.0
         : getDestroySpeed(slot, state)
            / hardness
            / (
               state.requiresCorrectToolForDrops() && !MeteorClient.mc.player.getInventory().getNonEquipmentItems().get(slot).isCorrectToolForDrops(state)
                  ? 100
                  : 30
            );
   }

   private static double getDestroySpeed(int slot, BlockState block) {
      double speed = MeteorClient.mc.player.getInventory().getNonEquipmentItems().get(slot).getDestroySpeed(block);
      if (speed > 1.0) {
         ItemStack tool = MeteorClient.mc.player.getInventory().getItem(slot);
         int efficiency = Utils.getEnchantmentLevel(tool, Enchantments.EFFICIENCY);
         if (efficiency > 0 && !tool.isEmpty()) {
            speed += efficiency * efficiency + 1;
         }
      }

      if (MobEffectUtil.hasDigSpeed(MeteorClient.mc.player)) {
         speed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(MeteorClient.mc.player) + 1) * 0.2F;
      }

      if (MeteorClient.mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
         float k = switch (MeteorClient.mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         speed *= k;
      }

      if (MeteorClient.mc.player.isEyeInFluid(FluidTags.WATER)) {
         speed *= MeteorClient.mc.player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
      }

      if (!MeteorClient.mc.player.onGround()) {
         speed /= 5.0;
      }

      return speed;
   }

   public static MutableBlockPos mutateAround(MutableBlockPos mutable, BlockPos origin, int xOffset, int yOffset, int zOffset) {
      return mutable.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
   }

   public enum MobSpawn {
      Never,
      Potential,
      Always;
   }
}
