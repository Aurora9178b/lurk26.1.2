package meteordevelopment.meteorclient.utils.player;

import java.util.ArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PathFinder {
   private static final int PATH_AHEAD = 3;
   private static final int QUAD_1 = 1;
   private static final int QUAD_2 = 2;
   private static final int SOUTH = 0;
   private static final int NORTH = 180;
   private final ArrayList<PathFinder.PathBlock> path = new ArrayList<>(3);
   private Entity target;
   private PathFinder.PathBlock currentPathBlock;

   public PathFinder.PathBlock getNextPathBlock() {
      PathFinder.PathBlock nextBlock = new PathFinder.PathBlock(BlockPos.containing(this.getNextStraightPos()));
      if (this.isSolidFloor(nextBlock.blockPos) && this.isAirAbove(nextBlock.blockPos)) {
         return nextBlock;
      }

      if (!this.isSolidFloor(nextBlock.blockPos) && this.isAirAbove(nextBlock.blockPos)) {
         int drop = this.getDrop(nextBlock.blockPos);
         if (this.getDrop(nextBlock.blockPos) < 3) {
            nextBlock = new PathFinder.PathBlock(new BlockPos(nextBlock.blockPos.getX(), nextBlock.blockPos.getY() - drop, nextBlock.blockPos.getZ()));
         }
      }

      return nextBlock;
   }

   public int getDrop(BlockPos pos) {
      int drop = 0;

      while (!this.isSolidFloor(pos) && drop < 3) {
         drop++;
         pos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
      }

      return drop;
   }

   public boolean isAirAbove(BlockPos blockPos) {
      return !this.getBlockStateAtPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()).isAir()
         ? false
         : this.getBlockStateAtPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()).isAir();
   }

   public Vec3 getNextStraightPos() {
      Vec3 nextPos = new Vec3(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ());

      for (double multiplier = 1.0; nextPos == MeteorClient.mc.player.position(); multiplier += 0.1) {
         nextPos = new Vec3(
            (int)(MeteorClient.mc.player.getX() + multiplier * Math.cos(Math.toRadians(MeteorClient.mc.player.getYRot()))),
            (int)MeteorClient.mc.player.getY(),
            (int)(MeteorClient.mc.player.getZ() + multiplier * Math.sin(Math.toRadians(MeteorClient.mc.player.getYRot())))
         );
      }

      return nextPos;
   }

   public int getYawToTarget() {
      if (this.target != null && MeteorClient.mc.player != null) {
         Vec3 tPos = this.target.position();
         Vec3 pPos = MeteorClient.mc.player.position();
         int direction = this.getDirection();
         double tan = (tPos.z - pPos.z) / (tPos.x - pPos.x);
         int yaw;
         switch (direction) {
            case 1:
               yaw = (int)((Math.PI / 2) - Math.atan(tan));
               break;
            case 2:
               yaw = (int)((-Math.PI / 2) - Math.atan(tan));
               break;
            default:
               return direction;
         }

         return yaw;
      } else {
         return Integer.MAX_VALUE;
      }
   }

   public int getDirection() {
      if (this.target != null && MeteorClient.mc.player != null) {
         Vec3 targetPos = this.target.position();
         Vec3 playerPos = MeteorClient.mc.player.position();
         if (targetPos.x == playerPos.x && targetPos.z > playerPos.z) {
            return 0;
         } else if (targetPos.x == playerPos.x && targetPos.z < playerPos.z) {
            return 180;
         } else if (targetPos.x < playerPos.x) {
            return 1;
         } else {
            return targetPos.x > playerPos.x ? 2 : 0;
         }
      } else {
         return 0;
      }
   }

   public BlockState getBlockStateAtPos(BlockPos pos) {
      return MeteorClient.mc.level != null ? MeteorClient.mc.level.getBlockState(pos) : null;
   }

   public BlockState getBlockStateAtPos(int x, int y, int z) {
      return MeteorClient.mc.level != null ? MeteorClient.mc.level.getBlockState(new BlockPos(x, y, z)) : null;
   }

   public Block getBlockAtPos(BlockPos pos) {
      return MeteorClient.mc.level != null ? MeteorClient.mc.level.getBlockState(pos).getBlock() : null;
   }

   public boolean isSolidFloor(BlockPos blockPos) {
      return this.isAir(this.getBlockAtPos(blockPos));
   }

   public boolean isAir(Block block) {
      return block == Blocks.AIR;
   }

   public boolean isWater(Block block) {
      return block == Blocks.WATER;
   }

   public void lookAtDestination(PathFinder.PathBlock pathBlock) {
      if (MeteorClient.mc.player != null) {
         MeteorClient.mc
            .player
            .lookAt(
               Anchor.EYES, new Vec3(pathBlock.blockPos.getX(), pathBlock.blockPos.getY() + MeteorClient.mc.player.getEyeHeight(), pathBlock.blockPos.getZ())
            );
      }
   }

   @EventHandler
   private void moveEventListener(PlayerMoveEvent event) {
      if (this.target != null && MeteorClient.mc.player != null) {
         if (!PlayerUtils.isWithin(this.target, 3.0)) {
            if (this.currentPathBlock == null) {
               this.currentPathBlock = this.getNextPathBlock();
            }

            if (MeteorClient.mc
                  .player
                  .position()
                  .distanceToSqr(new Vec3(this.currentPathBlock.blockPos.getX(), this.currentPathBlock.blockPos.getY(), this.currentPathBlock.blockPos.getZ()))
               < 0.01) {
               this.currentPathBlock = this.getNextPathBlock();
            }

            this.lookAtDestination(this.currentPathBlock);
            if (!MeteorClient.mc.options.keyUp.isDown()) {
               MeteorClient.mc.options.keyUp.setDown(true);
            }
         } else {
            if (MeteorClient.mc.options.keyUp.isDown()) {
               MeteorClient.mc.options.keyUp.setDown(false);
            }

            this.path.clear();
            this.currentPathBlock = null;
         }
      }
   }

   public void initiate(Entity entity) {
      this.target = entity;
      if (this.target != null) {
         this.currentPathBlock = this.getNextPathBlock();
      }

      MeteorClient.EVENT_BUS.subscribe(this);
   }

   public void disable() {
      this.target = null;
      this.path.clear();
      if (MeteorClient.mc.options.keyUp.isDown()) {
         MeteorClient.mc.options.keyUp.setDown(false);
      }

      MeteorClient.EVENT_BUS.unsubscribe(this);
   }

   public class PathBlock {
      public final Block block;
      public final BlockPos blockPos;
      public final BlockState blockState;
      public double yaw;

      public PathBlock(Block b, BlockPos pos, BlockState state) {
         this.block = b;
         this.blockPos = pos;
         this.blockState = state;
      }

      public PathBlock(Block b, BlockPos pos) {
         this.block = b;
         this.blockPos = pos;
         this.blockState = PathFinder.this.getBlockStateAtPos(this.blockPos);
      }

      public PathBlock(BlockPos pos) {
         this.blockPos = pos;
         this.block = PathFinder.this.getBlockAtPos(pos);
         this.blockState = PathFinder.this.getBlockStateAtPos(this.blockPos);
      }
   }
}
