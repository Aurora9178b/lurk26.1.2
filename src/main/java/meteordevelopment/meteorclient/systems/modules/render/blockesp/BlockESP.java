package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.GenericSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;

public class BlockESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("blocks").description("Blocks to search for.").onChanged(var1 -> {
         if (this.isActive() && Utils.canUpdate()) {
            this.onActivate();
         }
      }).build());
   private final Setting<ESPBlockData> defaultBlockConfig = this.sgGeneral
      .add(
         new GenericSetting.Builder<ESPBlockData>().name("default-block-config").description("Default block config.")
            .defaultValue(
               new ESPBlockData(ShapeMode.Lines, new SettingColor(0, 255, 200), new SettingColor(0, 255, 200, 25), true, new SettingColor(0, 255, 200, 125))
            )
            .build()
      );
   private final Setting<Map<Block, ESPBlockData>> blockConfigs = this.sgGeneral
      .add(
         new BlockDataSetting.Builder<ESPBlockData>().name("block-configs").description("Config for each block.")
            .defaultData(this.defaultBlockConfig)
            .build()
      );
   private final Setting<Boolean> tracers = this.sgGeneral
      .add(new BoolSetting.Builder().name("tracers").description("Render tracer lines.").defaultValue(false).build());
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private final Long2ObjectMap<ESPChunk> chunks = new Long2ObjectOpenHashMap<>();
   private final Set<ESPGroup> groups = new ReferenceOpenHashSet<>();
   private final ExecutorService workerThread = Executors.newSingleThreadExecutor();
   private DimensionType lastDimension;

   public BlockESP() {
      super(Categories.Render, "block-esp", "Renders specified blocks through walls.", "search");
      RainbowColors.register(this::onTickRainbow);
   }

   @Override
   public void onActivate() {
      synchronized (this.chunks) {
         this.chunks.clear();
         this.groups.clear();
      }

      for (ChunkAccess chunk : Utils.chunks()) {
         this.searchChunk(chunk);
      }

      this.lastDimension = this.mc.level.dimensionType();
   }

   @Override
   public void onDeactivate() {
      synchronized (this.chunks) {
         this.chunks.clear();
         this.groups.clear();
      }
   }

   private void onTickRainbow() {
      if (this.isActive()) {
         this.defaultBlockConfig.get().tickRainbow();

         for (ESPBlockData blockData : this.blockConfigs.get().values()) {
            blockData.tickRainbow();
         }
      }
   }

   ESPBlockData getBlockData(Block block) {
      ESPBlockData blockData = this.blockConfigs.get().get(block);
      return blockData == null ? this.defaultBlockConfig.get() : blockData;
   }

   private void updateChunk(int x, int z) {
      ESPChunk chunk = this.chunks.get(ChunkPos.pack(x, z));
      if (chunk != null) {
         chunk.update();
      }
   }

   private void updateBlock(int x, int y, int z) {
      ESPChunk chunk = this.chunks.get(ChunkPos.pack(x >> 4, z >> 4));
      if (chunk != null) {
         chunk.update(x, y, z);
      }
   }

   public ESPBlock getBlock(int x, int y, int z) {
      ESPChunk chunk = this.chunks.get(ChunkPos.pack(x >> 4, z >> 4));
      return chunk == null ? null : chunk.get(x, y, z);
   }

   public ESPGroup newGroup(Block block) {
      synchronized (this.chunks) {
         ESPGroup group = new ESPGroup(block);
         this.groups.add(group);
         return group;
      }
   }

   public void removeGroup(ESPGroup group) {
      synchronized (this.chunks) {
         this.groups.remove(group);
      }
   }

   @EventHandler
   private void onChunkData(ChunkDataEvent event) {
      this.searchChunk(event.chunk());
   }

   private void searchChunk(ChunkAccess chunk) {
      this.workerThread.submit(() -> {
         if (this.isActive()) {
            ESPChunk schunk = ESPChunk.searchChunk(chunk, this.blocks.get());
            if (schunk.size() > 0) {
               synchronized (this.chunks) {
                  this.chunks.put(chunk.getPos().pack(), schunk);
                  schunk.update();
                  this.updateChunk(chunk.getPos().x() - 1, chunk.getPos().z());
                  this.updateChunk(chunk.getPos().x() + 1, chunk.getPos().z());
                  this.updateChunk(chunk.getPos().x(), chunk.getPos().z() - 1);
                  this.updateChunk(chunk.getPos().x(), chunk.getPos().z() + 1);
               }
            }
         }
      });
   }

   @EventHandler
   private void onBlockUpdate(BlockUpdateEvent event) {
      int bx = event.pos.getX();
      int by = event.pos.getY();
      int bz = event.pos.getZ();
      int chunkX = bx >> 4;
      int chunkZ = bz >> 4;
      long key = ChunkPos.pack(chunkX, chunkZ);
      boolean added = this.blocks.get().contains(event.newState.getBlock()) && !this.blocks.get().contains(event.oldState.getBlock());
      boolean removed = !added && !this.blocks.get().contains(event.newState.getBlock()) && this.blocks.get().contains(event.oldState.getBlock());
      if (added || removed) {
         this.workerThread.submit(() -> {
            synchronized (this.chunks) {
               ESPChunk chunk = this.chunks.get(key);
               if (chunk == null) {
                  chunk = new ESPChunk(chunkX, chunkZ);
                  if (chunk.shouldBeDeleted()) {
                     return;
                  }

                  this.chunks.put(key, chunk);
               }

               this.blockPos.set(bx, by, bz);
               if (added) {
                  chunk.add(this.blockPos);
               } else {
                  chunk.remove(this.blockPos);
               }

               for (int x = -1; x < 2; x++) {
                  for (int z = -1; z < 2; z++) {
                     for (int y = -1; y < 2; y++) {
                        if (x != 0 || y != 0 || z != 0) {
                           this.updateBlock(bx + x, by + y, bz + z);
                        }
                     }
                  }
               }
            }
         });
      }
   }

   @EventHandler
   private void onPostTick(TickEvent.Post event) {
      DimensionType dimension = this.mc.level.dimensionType();
      if (this.lastDimension != dimension) {
         this.onActivate();
      }

      this.lastDimension = dimension;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      synchronized (this.chunks) {
         Iterator<ESPChunk> it = this.chunks.values().iterator();

         while (it.hasNext()) {
            ESPChunk chunk = it.next();
            if (chunk.shouldBeDeleted()) {
               this.workerThread.submit(() -> {
                  for (ESPBlock block : chunk.blocks.values()) {
                     block.group.remove(block, false);
                     block.loaded = false;
                  }
               });
               it.remove();
            } else {
               chunk.render(event);
            }
         }

         if (this.tracers.get()) {
            for (ESPGroup group : this.groups) {
               group.render(event);
            }
         }
      }
   }

   @Override
   public String getInfoString() {
      return "%s groups".formatted(this.groups.size());
   }
}
