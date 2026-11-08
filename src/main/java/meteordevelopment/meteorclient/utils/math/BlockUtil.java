package meteordevelopment.meteorclient.utils.math;

import java.util.LinkedHashMap;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockUtil {
   private static final Map<BlockPos, BlockState> bufferMap = new LinkedHashMap<>();

   public static boolean isSafeBlock(BlockPos pos) {
      return MeteorClient.mc.level.getBlockState(pos).canBeReplaced()
         && MeteorClient.mc.level.getFluidState(pos).isEmpty()
         && !MeteorClient.mc.level.getBlockState(pos).is(Blocks.POWDER_SNOW);
   }

   private BlockUtil() {
   }

   public static Map<BlockPos, BlockState> getBufferMap() {
      return bufferMap;
   }

   public static BlockPos findVclipHole(BlockPos vec, double vclip) {
      if (vclip > 0.0) {
         for (int i = (int)vclip + vec.getY(); i >= vec.getY(); i--) {
            BlockPos vec1 = new BlockPos(vec.getX(), i, vec.getZ());
            BlockPos vecUp = vec1.above(1);
            if (isSafeBlock(vec1) && isSafeBlock(vecUp)) {
               return vec1;
            }
         }
      }

      if (vclip < 0.0) {
         for (int i = (int)vclip + vec.getY(); i <= vec.getY(); i++) {
            BlockPos vec2 = new BlockPos(vec.getX(), i, vec.getZ());
            BlockPos vecUp = vec2.above(1);
            if (isSafeBlock(vec2) && isSafeBlock(vecUp)) {
               return vec2;
            }
         }
      }

      return vec;
   }

   public static boolean checkCanMove(Vec3 fromVec, Vec3 toVec) {
      return isSafeBlock(BlockPos.containing(fromVec)) && isSafeBlock(BlockPos.containing(toVec));
   }

   public static boolean checkNoPosCollide(BlockPos pos) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         AABB playerBox = MeteorClient.mc.player.getBoundingBox();
         AABB testBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
         BlockState state = MeteorClient.mc.level.getBlockState(pos);
         if (!state.canBeReplaced() && !state.isAir()) {
            return !testBox.intersects(playerBox);
         }

         BlockPos up = pos.above();
         BlockState upState = MeteorClient.mc.level.getBlockState(up);
         return !upState.canBeReplaced() && !upState.isAir() ? !testBox.intersects(playerBox) : true;
      } else {
         return false;
      }
   }

   public static boolean isBlinkVec(Vec3 vec) {
      if (vec == null) {
         return false;
      }

      if (MeteorClient.mc.level == null) {
         return false;
      }

      BlockPos pos = BlockPos.containing(vec);
      return checkNoPosCollide(pos);
   }
}
