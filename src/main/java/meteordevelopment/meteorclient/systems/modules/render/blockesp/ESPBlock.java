package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ESPBlock {
   private static final MutableBlockPos blockPos = new MutableBlockPos();
   private static final BlockESP blockEsp = Modules.get().get(BlockESP.class);
   public static final int FO = 2;
   public static final int FO_RI = 4;
   public static final int RI = 8;
   public static final int BA_RI = 16;
   public static final int BA = 32;
   public static final int BA_LE = 64;
   public static final int LE = 128;
   public static final int FO_LE = 256;
   public static final int TO = 512;
   public static final int TO_FO = 1024;
   public static final int TO_BA = 2048;
   public static final int TO_RI = 4096;
   public static final int TO_LE = 8192;
   public static final int BO = 16384;
   public static final int BO_FO = 32768;
   public static final int BO_BA = 65536;
   public static final int BO_RI = 131072;
   public static final int BO_LE = 262144;
   public static final int[] SIDES = new int[]{2, 32, 128, 8, 512, 16384};
   public final int x;
   public final int y;
   public final int z;
   private BlockState state;
   public int neighbours;
   public ESPGroup group;
   public boolean loaded = true;

   public ESPBlock(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public ESPBlock getSideBlock(int side) {
      return switch (side) {
         case 2 -> blockEsp.getBlock(this.x, this.y, this.z + 1);
         case 8 -> blockEsp.getBlock(this.x + 1, this.y, this.z);
         case 32 -> blockEsp.getBlock(this.x, this.y, this.z - 1);
         case 128 -> blockEsp.getBlock(this.x - 1, this.y, this.z);
         case 512 -> blockEsp.getBlock(this.x, this.y + 1, this.z);
         case 16384 -> blockEsp.getBlock(this.x, this.y - 1, this.z);
         default -> null;
      };
   }

   private void assignGroup() {
      ESPGroup firstGroup = null;

      for (int side : SIDES) {
         if ((this.neighbours & side) == side) {
            ESPBlock neighbour = this.getSideBlock(side);
            if (neighbour != null && neighbour.group != null) {
               if (firstGroup == null) {
                  firstGroup = neighbour.group;
               } else if (firstGroup != neighbour.group) {
                  firstGroup.merge(neighbour.group);
               }
            }
         }
      }

      if (firstGroup == null) {
         firstGroup = blockEsp.newGroup(this.state.getBlock());
      }

      firstGroup.add(this);
   }

   public void update() {
      this.state = MeteorClient.mc.level.getBlockState(blockPos.set(this.x, this.y, this.z));
      this.neighbours = 0;
      if (this.isNeighbour(Direction.SOUTH)) {
         this.neighbours |= 2;
      }

      if (this.isNeighbourDiagonal(1.0, 0.0, 1.0)) {
         this.neighbours |= 4;
      }

      if (this.isNeighbour(Direction.EAST)) {
         this.neighbours |= 8;
      }

      if (this.isNeighbourDiagonal(1.0, 0.0, -1.0)) {
         this.neighbours |= 16;
      }

      if (this.isNeighbour(Direction.NORTH)) {
         this.neighbours |= 32;
      }

      if (this.isNeighbourDiagonal(-1.0, 0.0, -1.0)) {
         this.neighbours |= 64;
      }

      if (this.isNeighbour(Direction.WEST)) {
         this.neighbours |= 128;
      }

      if (this.isNeighbourDiagonal(-1.0, 0.0, 1.0)) {
         this.neighbours |= 256;
      }

      if (this.isNeighbour(Direction.UP)) {
         this.neighbours |= 512;
      }

      if (this.isNeighbourDiagonal(0.0, 1.0, 1.0)) {
         this.neighbours |= 1024;
      }

      if (this.isNeighbourDiagonal(0.0, 1.0, -1.0)) {
         this.neighbours |= 2048;
      }

      if (this.isNeighbourDiagonal(1.0, 1.0, 0.0)) {
         this.neighbours |= 4096;
      }

      if (this.isNeighbourDiagonal(-1.0, 1.0, 0.0)) {
         this.neighbours |= 8192;
      }

      if (this.isNeighbour(Direction.DOWN)) {
         this.neighbours |= 16384;
      }

      if (this.isNeighbourDiagonal(0.0, -1.0, 1.0)) {
         this.neighbours |= 32768;
      }

      if (this.isNeighbourDiagonal(0.0, -1.0, -1.0)) {
         this.neighbours |= 65536;
      }

      if (this.isNeighbourDiagonal(1.0, -1.0, 0.0)) {
         this.neighbours |= 131072;
      }

      if (this.isNeighbourDiagonal(-1.0, -1.0, 0.0)) {
         this.neighbours |= 262144;
      }

      if (this.group == null) {
         this.assignGroup();
      }
   }

   private boolean isNeighbour(Direction dir) {
      blockPos.set(this.x + dir.getStepX(), this.y + dir.getStepY(), this.z + dir.getStepZ());
      BlockState neighbourState = MeteorClient.mc.level.getBlockState(blockPos);
      if (neighbourState.getBlock() != this.state.getBlock()) {
         return false;
      }

      VoxelShape cube = Shapes.block();
      VoxelShape shape = this.state.getShape(MeteorClient.mc.level, blockPos);
      VoxelShape neighbourShape = neighbourState.getShape(MeteorClient.mc.level, blockPos);
      if (shape.isEmpty()) {
         shape = cube;
      }

      if (neighbourShape.isEmpty()) {
         neighbourShape = cube;
      }

      switch (dir) {
         case SOUTH:
            if (shape.max(Axis.Z) == 1.0 && neighbourShape.min(Axis.Z) == 0.0) {
               return true;
            }
            break;
         case NORTH:
            if (shape.min(Axis.Z) == 0.0 && neighbourShape.max(Axis.Z) == 1.0) {
               return true;
            }
            break;
         case EAST:
            if (shape.max(Axis.X) == 1.0 && neighbourShape.min(Axis.X) == 0.0) {
               return true;
            }
            break;
         case WEST:
            if (shape.min(Axis.X) == 0.0 && neighbourShape.max(Axis.X) == 1.0) {
               return true;
            }
            break;
         case UP:
            if (shape.max(Axis.Y) == 1.0 && neighbourShape.min(Axis.Y) == 0.0) {
               return true;
            }
            break;
         case DOWN:
            if (shape.min(Axis.Y) == 0.0 && neighbourShape.max(Axis.Y) == 1.0) {
               return true;
            }
      }

      return false;
   }

   private boolean isNeighbourDiagonal(double x, double y, double z) {
      blockPos.set(this.x + x, this.y + y, this.z + z);
      return this.state.getBlock() == MeteorClient.mc.level.getBlockState(blockPos).getBlock();
   }

   public void render(Render3DEvent event) {
      double x1 = this.x;
      double y1 = this.y;
      double z1 = this.z;
      double x2 = this.x + 1;
      double y2 = this.y + 1;
      double z2 = this.z + 1;
      VoxelShape shape = this.state.getShape(MeteorClient.mc.level, blockPos);
      if (!shape.isEmpty()) {
         x1 = this.x + shape.min(Axis.X);
         y1 = this.y + shape.min(Axis.Y);
         z1 = this.z + shape.min(Axis.Z);
         x2 = this.x + shape.max(Axis.X);
         y2 = this.y + shape.max(Axis.Y);
         z2 = this.z + shape.max(Axis.Z);
      }

      ESPBlockData blockData = blockEsp.getBlockData(this.state.getBlock());
      ShapeMode shapeMode = blockData.shapeMode;
      Color lineColor = blockData.lineColor;
      Color sideColor = blockData.sideColor;
      if (this.neighbours == 0) {
         event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, 0);
      } else {
         if (shapeMode.lines()) {
            if ((this.neighbours & 128) != 128 && (this.neighbours & 32) != 32
               || (this.neighbours & 128) == 128 && (this.neighbours & 32) == 32 && (this.neighbours & 64) != 64) {
               event.renderer.line(x1, y1, z1, x1, y2, z1, lineColor);
            }

            if ((this.neighbours & 128) != 128 && (this.neighbours & 2) != 2
               || (this.neighbours & 128) == 128 && (this.neighbours & 2) == 2 && (this.neighbours & 256) != 256) {
               event.renderer.line(x1, y1, z2, x1, y2, z2, lineColor);
            }

            if ((this.neighbours & 8) != 8 && (this.neighbours & 32) != 32
               || (this.neighbours & 8) == 8 && (this.neighbours & 32) == 32 && (this.neighbours & 16) != 16) {
               event.renderer.line(x2, y1, z1, x2, y2, z1, lineColor);
            }

            if ((this.neighbours & 8) != 8 && (this.neighbours & 2) != 2
               || (this.neighbours & 8) == 8 && (this.neighbours & 2) == 2 && (this.neighbours & 4) != 4) {
               event.renderer.line(x2, y1, z2, x2, y2, z2, lineColor);
            }

            if ((this.neighbours & 32) != 32 && (this.neighbours & 16384) != 16384 || (this.neighbours & 32) != 32 && (this.neighbours & 65536) == 65536) {
               event.renderer.line(x1, y1, z1, x2, y1, z1, lineColor);
            }

            if ((this.neighbours & 2) != 2 && (this.neighbours & 16384) != 16384 || (this.neighbours & 2) != 2 && (this.neighbours & 32768) == 32768) {
               event.renderer.line(x1, y1, z2, x2, y1, z2, lineColor);
            }

            if ((this.neighbours & 32) != 32 && (this.neighbours & 512) != 512 || (this.neighbours & 32) != 32 && (this.neighbours & 2048) == 2048) {
               event.renderer.line(x1, y2, z1, x2, y2, z1, lineColor);
            }

            if ((this.neighbours & 2) != 2 && (this.neighbours & 512) != 512 || (this.neighbours & 2) != 2 && (this.neighbours & 1024) == 1024) {
               event.renderer.line(x1, y2, z2, x2, y2, z2, lineColor);
            }

            if ((this.neighbours & 128) != 128 && (this.neighbours & 16384) != 16384 || (this.neighbours & 128) != 128 && (this.neighbours & 262144) == 262144) {
               event.renderer.line(x1, y1, z1, x1, y1, z2, lineColor);
            }

            if ((this.neighbours & 8) != 8 && (this.neighbours & 16384) != 16384 || (this.neighbours & 8) != 8 && (this.neighbours & 131072) == 131072) {
               event.renderer.line(x2, y1, z1, x2, y1, z2, lineColor);
            }

            if ((this.neighbours & 128) != 128 && (this.neighbours & 512) != 512 || (this.neighbours & 128) != 128 && (this.neighbours & 8192) == 8192) {
               event.renderer.line(x1, y2, z1, x1, y2, z2, lineColor);
            }

            if ((this.neighbours & 8) != 8 && (this.neighbours & 512) != 512 || (this.neighbours & 8) != 8 && (this.neighbours & 4096) == 4096) {
               event.renderer.line(x2, y2, z1, x2, y2, z2, lineColor);
            }
         }

         if (shapeMode.sides()) {
            if ((this.neighbours & 16384) != 16384) {
               event.renderer.quadHorizontal(x1, y1, z1, x2, z2, sideColor);
            }

            if ((this.neighbours & 512) != 512) {
               event.renderer.quadHorizontal(x1, y2, z1, x2, z2, sideColor);
            }

            if ((this.neighbours & 2) != 2) {
               event.renderer.quadVertical(x1, y1, z2, x2, y2, z2, sideColor);
            }

            if ((this.neighbours & 32) != 32) {
               event.renderer.quadVertical(x1, y1, z1, x2, y2, z1, sideColor);
            }

            if ((this.neighbours & 8) != 8) {
               event.renderer.quadVertical(x2, y1, z1, x2, y2, z2, sideColor);
            }

            if ((this.neighbours & 128) != 128) {
               event.renderer.quadVertical(x1, y1, z1, x1, y2, z2, sideColor);
            }
         }
      }
   }

   public static long getKey(int x, int y, int z) {
      return (long)y << 16 | (long)(z & 15) << 8 | x & 15;
   }

   public static long getKey(BlockPos blockPos) {
      return getKey(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }
}
