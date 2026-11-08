package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class ESPChunk {
   private final int x;
   private final int z;
   public Long2ObjectMap<ESPBlock> blocks;

   public ESPChunk(int x, int z) {
      this.x = x;
      this.z = z;
   }

   public ESPBlock get(int x, int y, int z) {
      return this.blocks == null ? null : this.blocks.get(ESPBlock.getKey(x, y, z));
   }

   public void add(BlockPos blockPos, boolean update) {
      ESPBlock block = new ESPBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      if (this.blocks == null) {
         this.blocks = new Long2ObjectOpenHashMap<>(64);
      }

      this.blocks.put(ESPBlock.getKey(blockPos), block);
      if (update) {
         block.update();
      }
   }

   public void add(BlockPos blockPos) {
      this.add(blockPos, true);
   }

   public void remove(BlockPos blockPos) {
      if (this.blocks != null) {
         ESPBlock block = this.blocks.remove(ESPBlock.getKey(blockPos));
         if (block != null) {
            block.group.remove(block);
         }
      }
   }

   public void update() {
      if (this.blocks != null) {
         for (ESPBlock block : this.blocks.values()) {
            block.update();
         }
      }
   }

   public void update(int x, int y, int z) {
      if (this.blocks != null) {
         ESPBlock block = this.blocks.get(ESPBlock.getKey(x, y, z));
         if (block != null) {
            block.update();
         }
      }
   }

   public int size() {
      return this.blocks == null ? 0 : this.blocks.size();
   }

   public boolean shouldBeDeleted() {
      int viewDist = Utils.getRenderDistance() + 1;
      int chunkX = SectionPos.posToSectionCoord(MeteorClient.mc.player.blockPosition().getX());
      int chunkZ = SectionPos.posToSectionCoord(MeteorClient.mc.player.blockPosition().getZ());
      return this.x > chunkX + viewDist || this.x < chunkX - viewDist || this.z > chunkZ + viewDist || this.z < chunkZ - viewDist;
   }

   public void render(Render3DEvent event) {
      if (this.blocks != null) {
         for (ESPBlock block : this.blocks.values()) {
            block.render(event);
         }
      }
   }

   public static ESPChunk searchChunk(ChunkAccess chunk, List<Block> blocks) {
      ESPChunk schunk = new ESPChunk(chunk.getPos().x(), chunk.getPos().z());
      if (schunk.shouldBeDeleted()) {
         return schunk;
      }

      MutableBlockPos blockPos = new MutableBlockPos();

      for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
         for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
            int height = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE)
               .getFirstAvailable(x - chunk.getPos().getMinBlockX(), z - chunk.getPos().getMinBlockZ());

            for (int y = MeteorClient.mc.level.getMinY(); y < height; y++) {
               blockPos.set(x, y, z);
               BlockState bs = chunk.getBlockState(blockPos);
               if (blocks.contains(bs.getBlock())) {
                  schunk.add(blockPos, false);
               }
            }
         }
      }

      return schunk;
   }
}
