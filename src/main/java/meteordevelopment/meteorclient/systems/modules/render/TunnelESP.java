package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class TunnelESP extends Module {
   private static final MutableBlockPos BP = new MutableBlockPos();
   private static final Direction[] DIRECTIONS = new Direction[]{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST};
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> height = this.sgGeneral
      .add(new DoubleSetting.Builder().name("height").description("Height of the rendered box.").defaultValue(0.1).sliderMax(2.0).build());
   private final Setting<Boolean> connected = this.sgGeneral
      .add(new BoolSetting.Builder().name("connected").description("If neighbouring holes should be connected.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 175, 25, 50)).build());
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 175, 25, 255)).build());
   private final Long2ObjectMap<TunnelESP.TChunk> chunks = new Long2ObjectOpenHashMap<>();

   public TunnelESP() {
      super(Categories.Render, "tunnel-esp", "Highlights tunnels.");
   }

   @Override
   public void onDeactivate() {
      this.chunks.clear();
   }

   private static int pack(int x, int y, int z) {
      return (x & 0xFF) << 24 | (y & 65535) << 8 | z & 0xFF;
   }

   private static byte getPackedX(int p) {
      return (byte)(p >> 24 & 0xFF);
   }

   private static short getPackedY(int p) {
      return (short)(p >> 8 & 65535);
   }

   private static byte getPackedZ(int p) {
      return (byte)(p & 0xFF);
   }

   private void searchChunk(ChunkAccess chunk, TunnelESP.TChunk tChunk) {
      TunnelESP.Context ctx = new TunnelESP.Context();
      IntSet set = new IntOpenHashSet();
      int startX = chunk.getPos().getMinBlockX();
      int startZ = chunk.getPos().getMinBlockZ();
      int endX = chunk.getPos().getMaxBlockX();
      int endZ = chunk.getPos().getMaxBlockZ();

      for (int x = startX; x <= endX; x++) {
         for (int z = startZ; z <= endZ; z++) {
            int height = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE).getFirstAvailable(x - startX, z - startZ);

            for (short y = (short)this.mc.level.getMinY(); y < height; y++) {
               if (this.isTunnel(ctx, x, y, z)) {
                  set.add(pack(x - startX, y, z - startZ));
               }
            }
         }
      }

      IntSet positions = new IntOpenHashSet();
      IntIterator it = set.iterator();

      while (it.hasNext()) {
         int packed = it.nextInt();
         byte x = getPackedX(packed);
         short y = getPackedY(packed);
         byte z = getPackedZ(packed);
         if (x != 0 && x != 15 && z != 0 && z != 15) {
            boolean has = false;

            for (Direction dir : DIRECTIONS) {
               if (set.contains(pack(x + dir.getStepX(), y, z + dir.getStepZ()))) {
                  has = true;
                  break;
               }
            }

            if (has) {
               positions.add(packed);
            }
         } else {
            positions.add(packed);
         }
      }

      tChunk.positions = positions;
   }

   private boolean isTunnel(TunnelESP.Context ctx, int x, int y, int z) {
      if (!this.canWalkIn(ctx, x, y, z)) {
         return false;
      }

      TunnelESP.TunnelSide s1 = this.getTunnelSide(ctx, x + 1, y, z);
      if (s1 == TunnelESP.TunnelSide.PartiallyBlocked) {
         return false;
      }

      TunnelESP.TunnelSide s2 = this.getTunnelSide(ctx, x - 1, y, z);
      if (s2 == TunnelESP.TunnelSide.PartiallyBlocked) {
         return false;
      }

      TunnelESP.TunnelSide s3 = this.getTunnelSide(ctx, x, y, z + 1);
      if (s3 == TunnelESP.TunnelSide.PartiallyBlocked) {
         return false;
      }

      TunnelESP.TunnelSide s4 = this.getTunnelSide(ctx, x, y, z - 1);
      return s4 == TunnelESP.TunnelSide.PartiallyBlocked
         ? false
         : s1 == TunnelESP.TunnelSide.Walkable
               && s2 == TunnelESP.TunnelSide.Walkable
               && s3 == TunnelESP.TunnelSide.FullyBlocked
               && s4 == TunnelESP.TunnelSide.FullyBlocked
            || s1 == TunnelESP.TunnelSide.FullyBlocked
               && s2 == TunnelESP.TunnelSide.FullyBlocked
               && s3 == TunnelESP.TunnelSide.Walkable
               && s4 == TunnelESP.TunnelSide.Walkable;
   }

   private TunnelESP.TunnelSide getTunnelSide(TunnelESP.Context ctx, int x, int y, int z) {
      if (this.canWalkIn(ctx, x, y, z)) {
         return TunnelESP.TunnelSide.Walkable;
      } else {
         return !this.canWalkThrough(ctx, x, y, z) && !this.canWalkThrough(ctx, x, y + 1, z)
            ? TunnelESP.TunnelSide.FullyBlocked
            : TunnelESP.TunnelSide.PartiallyBlocked;
      }
   }

   private boolean canWalkOn(TunnelESP.Context ctx, int x, int y, int z) {
      BlockState state = ctx.get(x, y, z);
      if (state.isAir()) {
         return false;
      } else {
         return !state.getFluidState().isEmpty() ? false : !state.getCollisionShape(this.mc.level, BP.set(x, y, z)).isEmpty();
      }
   }

   private boolean canWalkThrough(TunnelESP.Context ctx, int x, int y, int z) {
      BlockState state = ctx.get(x, y, z);
      if (state.isAir()) {
         return true;
      } else {
         return !state.getFluidState().isEmpty() ? false : state.getCollisionShape(this.mc.level, BP.set(x, y, z)).isEmpty();
      }
   }

   private boolean canWalkIn(TunnelESP.Context ctx, int x, int y, int z) {
      if (!this.canWalkOn(ctx, x, y - 1, z)) {
         return false;
      } else if (!this.canWalkThrough(ctx, x, y, z)) {
         return false;
      } else {
         return this.canWalkThrough(ctx, x, y + 2, z) ? false : this.canWalkThrough(ctx, x, y + 1, z);
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      synchronized (this.chunks) {
         for (TunnelESP.TChunk tChunk : this.chunks.values()) {
            tChunk.marked = false;
         }

         int added = 0;

         for (ChunkAccess chunk : Utils.chunks(true)) {
            long key = ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z());
            if (this.chunks.containsKey(key)) {
               this.chunks.get(key).marked = true;
            } else if (added < 48) {
               TunnelESP.TChunk tChunk = new TunnelESP.TChunk(chunk.getPos().x(), chunk.getPos().z());
               this.chunks.put(tChunk.getKey(), tChunk);
               MeteorExecutor.execute(() -> this.searchChunk(chunk, tChunk));
               added++;
            }
         }

         this.chunks.values().removeIf(tChunkx -> !tChunkx.marked);
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      synchronized (this.chunks) {
         for (TunnelESP.TChunk chunk : this.chunks.values()) {
            chunk.render(event.renderer);
         }
      }
   }

   private boolean chunkContains(TunnelESP.TChunk chunk, int x, int y, int z) {
      int key;
      if (x == -1) {
         chunk = this.chunks.get(ChunkPos.pack(chunk.x - 1, chunk.z));
         key = pack(15, y, z);
      } else if (x == 16) {
         chunk = this.chunks.get(ChunkPos.pack(chunk.x + 1, chunk.z));
         key = pack(0, y, z);
      } else if (z == -1) {
         chunk = this.chunks.get(ChunkPos.pack(chunk.x, chunk.z - 1));
         key = pack(x, y, 15);
      } else if (z == 16) {
         chunk = this.chunks.get(ChunkPos.pack(chunk.x, chunk.z + 1));
         key = pack(x, y, 0);
      } else {
         key = pack(x, y, z);
      }

      return chunk != null && chunk.positions != null && chunk.positions.contains(key);
   }

   private static class Context {
      private final Level world = MeteorClient.mc.level;
      private ChunkAccess lastChunk;

      public Context() {
      }

      public BlockState get(int x, int y, int z) {
         if (this.world.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
         }

         int cx = x >> 4;
         int cz = z >> 4;
         ChunkAccess chunk;
         if (this.lastChunk != null && this.lastChunk.getPos().x() == cx && this.lastChunk.getPos().z() == cz) {
            chunk = this.lastChunk;
         } else {
            chunk = this.world.getChunk(cx, cz, ChunkStatus.FULL, false);
         }

         if (chunk == null) {
            return Blocks.VOID_AIR.defaultBlockState();
         }

         LevelChunkSection section = chunk.getSections()[chunk.getSectionIndex(y)];
         if (section == null) {
            return Blocks.VOID_AIR.defaultBlockState();
         }

         this.lastChunk = chunk;
         return section.getBlockState(x & 15, y & 15, z & 15);
      }
   }

   private class TChunk {
      private final int x;
      private final int z;
      public IntSet positions;
      public boolean marked;

      public TChunk(int x, int z) {
         this.x = x;
         this.z = z;
         this.marked = true;
      }

      public void render(Renderer3D renderer) {
         if (this.positions != null) {
            IntIterator it = this.positions.iterator();

            while (it.hasNext()) {
               int pos = it.nextInt();
               int x = TunnelESP.getPackedX(pos);
               int y = TunnelESP.getPackedY(pos);
               int z = TunnelESP.getPackedZ(pos);
               int excludeDir = 0;
               if (TunnelESP.this.connected.get()) {
                  for (Direction dir : TunnelESP.DIRECTIONS) {
                     if (TunnelESP.this.chunkContains(this, x + dir.getStepX(), y, z + dir.getStepZ())) {
                        excludeDir |= Dir.get(dir);
                     }
                  }
               }

               x += this.x * 16;
               z += this.z * 16;
               renderer.box(
                  x,
                  y,
                  z,
                  x + 1,
                  y + TunnelESP.this.height.get(),
                  z + 1,
                  TunnelESP.this.sideColor.get(),
                  TunnelESP.this.lineColor.get(),
                  TunnelESP.this.shapeMode.get(),
                  excludeDir
               );
            }
         }
      }

      public long getKey() {
         return ChunkPos.pack(this.x, this.z);
      }
   }

   private enum TunnelSide {
      Walkable,
      PartiallyBlocked,
      FullyBlocked;
   }
}
