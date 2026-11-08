package meteordevelopment.meteorclient.systems.modules.world;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

public class StashFinder extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private static final List<Block> DEFAULT_SUPPORT_BLOCK_BLACKLIST = List.of(
      Blocks.OXIDIZED_COPPER,
      Blocks.OXIDIZED_CUT_COPPER,
      Blocks.TUFF_BRICKS,
      Blocks.WAXED_COPPER_BLOCK,
      Blocks.WAXED_OXIDIZED_COPPER,
      Blocks.WAXED_OXIDIZED_CUT_COPPER,
      Blocks.BARREL,
      Blocks.WAXED_COPPER_BULB
   );
   private final Setting<List<BlockEntityType<?>>> storageBlocks = this.sgGeneral
      .add(
         new StorageBlockListSetting.Builder()
            .name("storage-blocks")
            .description("Select the storage blocks to search for.")
            .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
            .build()
      );
   private final Setting<Integer> minimumStorageCount = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("minimum-storage-count")
            .description("The minimum amount of storage blocks in a chunk to record the chunk.")
            .defaultValue(4)
            .min(1)
            .sliderMin(1)
            .build()
      );
   private final Setting<List<Block>> blacklistedBlocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("blacklisted-support-blocks")
            .description("Blocks that prevent counting a storage block entity when it sits on them.")
            .defaultValue(DEFAULT_SUPPORT_BLOCK_BLACKLIST)
            .build()
      );
   private final Setting<Integer> minimumDistance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("minimum-distance")
            .description("The minimum distance you must be from spawn to record a certain chunk.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10000)
            .build()
      );
   private final Setting<Boolean> sendNotifications = this.sgGeneral
      .add(new BoolSetting.Builder().name("notifications").description("Sends Minecraft notifications when new stashes are found.").defaultValue(true).build());
   private final Setting<StashFinder.Mode> notificationMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("notification-mode"))
                     .description("The mode to use for notifications."))
                  .defaultValue(StashFinder.Mode.Both))
               .visible(this.sendNotifications::get))
            .build()
      );
   private final Setting<Boolean> renderTracer = this.sgRender
      .add(new BoolSetting.Builder().name("render-tracer").description("Renders a tracer to the last found stash.").defaultValue(true).build());
   private final Setting<SettingColor> traceColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tracer-color")
            .description("Color of the stash tracer.")
            .defaultValue(new SettingColor(255, 215, 0, 255))
            .visible(this.renderTracer::get)
            .build()
      );
   private final Setting<Integer> traceArrivalDistance = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("tracer-hide-at-distance")
            .description("Hide the trace when you are this close to the stash.")
            .defaultValue(16)
            .min(1)
            .sliderMin(1)
            .sliderMax(50)
            .visible(this.renderTracer::get)
            .build()
      );
   private final Setting<Integer> traceMaxDistance = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("tracer-max-distance")
            .description("Hide the trace when you are farther than this distance from the stash.")
            .defaultValue(2000)
            .min(10)
            .sliderMin(50)
            .sliderMax(10000)
            .visible(this.renderTracer::get)
            .build()
      );
   private final Setting<Boolean> renderChunkColumn = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-chunk-column")
            .description("Renders a vertical column at the center of traced chunks.")
            .defaultValue(false)
            .build()
      );
   private final Setting<SettingColor> traceColumnColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("chunk-column-color")
            .description("Color of the stash tracer column.")
            .defaultValue(new SettingColor(255, 215, 0, 100))
            .visible(this.renderChunkColumn::get)
            .build()
      );
   private final Setting<Keybind> clearTracesBind = this.sgRender
      .add(new KeybindSetting.Builder().name("clear-traces-bind").description("Keybind to clear all stash traces.").defaultValue(Keybind.none()).build());
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final Map<ChunkPos, Vec3> tracerPositions = new HashMap<>();
   public List<StashFinder.Chunk> chunks = new ArrayList<>();

   public StashFinder() {
      super(Categories.World, "stash-finder", "Searches loaded chunks for storage blocks. Saves to <your minecraft folder>/meteor-client");
   }

   @Override
   public void onActivate() {
      this.load();
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.clearTracesBind.get().isPressed()) {
         this.tracerPositions.clear();
      }
   }

   @EventHandler
   private void onChunkData(ChunkDataEvent event) {
      double chunkXAbs = Math.abs(event.chunk().getPos().x() * 16);
      double chunkZAbs = Math.abs(event.chunk().getPos().z() * 16);
      if (!(Math.sqrt(chunkXAbs * chunkXAbs + chunkZAbs * chunkZAbs) < this.minimumDistance.get().intValue())) {
         StashFinder.Chunk chunk = new StashFinder.Chunk(event.chunk().getPos());
         List<Block> blockBlacklist = this.blacklistedBlocks.get();

         for (BlockEntity blockEntity : event.chunk().getBlockEntities().values()) {
            if (this.storageBlocks.get().contains(blockEntity.getType())) {
               if (!blockBlacklist.isEmpty()) {
                  BlockPos below = blockEntity.getBlockPos().below();
                  if (blockBlacklist.contains(event.chunk().getBlockState(below).getBlock())) {
                     continue;
                  }
               }

               switch (blockEntity) {
                  case ChestBlockEntity var18:
                     chunk.chests++;
                     break;
                  case BarrelBlockEntity var19:
                     chunk.barrels++;
                     break;
                  case ShulkerBoxBlockEntity var20:
                     chunk.shulkers++;
                     break;
                  case EnderChestBlockEntity var21:
                     chunk.enderChests++;
                     break;
                  case AbstractFurnaceBlockEntity var22:
                     chunk.furnaces++;
                     break;
                  case DispenserBlockEntity var23:
                     chunk.dispensersDroppers++;
                     break;
                  case HopperBlockEntity var24:
                     chunk.hoppers++;
                     break;
                  default:
               }
            }
         }

         if (chunk.getTotal() >= this.minimumStorageCount.get()) {
            StashFinder.Chunk prevChunk = null;
            int i = this.chunks.indexOf(chunk);
            if (i < 0) {
               this.chunks.add(chunk);
            } else {
               prevChunk = this.chunks.set(i, chunk);
            }

            if (this.renderTracer.get()) {
               double y = this.mc.player != null ? this.mc.player.getEyeY() : 0.0;
               this.tracerPositions.put(chunk.chunkPos, new Vec3(chunk.x, y, chunk.z));
            }

            this.saveJson();
            this.saveCsv();
            if (this.sendNotifications.get() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
               switch ((StashFinder.Mode)this.notificationMode.get()) {
                  case Chat:
                     this.sendChatNotification(chunk);
                     break;
                  case Toast: {
                     MeteorToast toast = new MeteorToast.Builder(this.title).icon(Items.CHEST).text("Found Stash!").build();
                     this.mc.getToastManager().addToast(toast);
                     break;
                  }
                  case Both: {
                     this.sendChatNotification(chunk);
                     MeteorToast toast = new MeteorToast.Builder(this.title).icon(Items.CHEST).text("Found Stash!").build();
                     this.mc.getToastManager().addToast(toast);
                  }
               }
            }
         }
      }
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      this.chunks.sort(Comparator.comparingInt(value -> -value.getTotal()));
      WVerticalList list = theme.verticalList();
      WHorizontalList hl = theme.horizontalList();
      WButton clear = hl.add(theme.button("Clear Chunks")).widget();
      WButton resetTracers = hl.add(theme.button("Reset Tracers")).widget();
      list.add(hl);
      WTable table = new WTable();
      if (!this.chunks.isEmpty()) {
         list.add(table);
      }

      clear.action = () -> {
         this.chunks.clear();
         table.clear();
         this.tracerPositions.clear();
      };
      resetTracers.action = () -> {
         table.clear();
         this.tracerPositions.clear();
         this.fillTable(theme, table);
      };
      this.fillTable(theme, table);
      return list;
   }

   private void fillTable(GuiTheme theme, WTable table) {
      for (StashFinder.Chunk chunk : this.chunks) {
         table.add(theme.label("Pos: " + chunk.x + ", " + chunk.z)).padRight(10.0);
         table.add(theme.label("Total: " + chunk.getTotal())).padRight(10.0);
         WCheckbox visible = table.add(theme.checkbox(this.tracerPositions.containsKey(chunk.chunkPos))).widget();
         visible.action = () -> {
            if (visible.checked) {
               double y = this.mc.player != null ? this.mc.player.getEyeY() : 0.0;
               this.tracerPositions.put(chunk.chunkPos, new Vec3(chunk.x, y, chunk.z));
            } else {
               this.tracerPositions.remove(chunk.chunkPos);
            }
         };
         WButton open = table.add(theme.button("Open")).widget();
         open.action = () -> this.mc.setScreen(new StashFinder.ChunkScreen(theme, chunk));
         WButton gotoBtn = table.add(theme.button("Goto")).widget();
         gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(chunk.x, 0, chunk.z), true);
         WMinus delete = table.add(theme.minus()).widget();
         delete.action = () -> {
            if (this.chunks.remove(chunk)) {
               this.tracerPositions.remove(chunk.chunkPos);
               table.clear();
               this.fillTable(theme, table);
               this.saveJson();
               this.saveCsv();
            }
         };
         table.row();
      }
   }

   private void load() {
      boolean loaded = false;
      File file = this.getJsonFile();
      if (file.exists()) {
         try {
            FileReader reader = new FileReader(file);
            this.chunks = GSON.fromJson(reader, (new TypeToken<List<StashFinder.Chunk>>() {}).getType());
            reader.close();

            for (StashFinder.Chunk chunk : this.chunks) {
               chunk.calculatePos();
            }

            loaded = true;
         } catch (Exception var8) {
            if (this.chunks == null) {
               this.chunks = new ArrayList<>();
            }
         }
      }

      file = this.getCsvFile();
      if (!loaded && file.exists()) {
         try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
               String[] values = line.split(" ");
               StashFinder.Chunk chunk = new StashFinder.Chunk(new ChunkPos(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
               chunk.chests = Integer.parseInt(values[2]);
               chunk.shulkers = Integer.parseInt(values[3]);
               chunk.enderChests = Integer.parseInt(values[4]);
               chunk.furnaces = Integer.parseInt(values[5]);
               chunk.dispensersDroppers = Integer.parseInt(values[6]);
               chunk.hoppers = Integer.parseInt(values[7]);
               this.chunks.add(chunk);
            }

            reader.close();
         } catch (Exception var7) {
            if (this.chunks == null) {
               this.chunks = new ArrayList<>();
            }
         }
      }
   }

   private void saveCsv() {
      try {
         File file = this.getCsvFile();
         file.getParentFile().mkdirs();
         Writer writer = new FileWriter(file);
         writer.write("X,Z,Chests,Barrels,Shulkers,EnderChests,Furnaces,DispensersDroppers,Hoppers\n");

         for (StashFinder.Chunk chunk : this.chunks) {
            chunk.write(writer);
         }

         writer.close();
      } catch (IOException e) {
         MeteorClient.LOG.error("Error while writing the stash list to csv", e);
      }
   }

   private void saveJson() {
      try {
         File file = this.getJsonFile();
         file.getParentFile().mkdirs();
         Writer writer = new FileWriter(file);
         GSON.toJson(this.chunks, writer);
         writer.close();
      } catch (IOException e) {
         MeteorClient.LOG.error("Error while writing the stash list to json", e);
      }
   }

   private File getJsonFile() {
      return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getFileWorldName()), "stashes.json");
   }

   private File getCsvFile() {
      return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getFileWorldName()), "stashes.csv");
   }

   @Override
   public String getInfoString() {
      return String.valueOf(this.chunks.size());
   }

   private void sendChatNotification(StashFinder.Chunk chunk) {
      MutableComponent coords = Component.literal(chunk.x + ", " + chunk.z)
         .setStyle(
            Style.EMPTY
               .withColor(ChatFormatting.WHITE)
               .applyFormat(ChatFormatting.UNDERLINE)
               .withHoverEvent(new ShowText(Component.literal("Path to stash")))
               .withClickEvent(new RunnableClickEvent(() -> PathManagers.get().moveTo(new BlockPos(chunk.x, 0, chunk.z), true)))
         );
      MutableComponent message = Component.literal("Found stash at ")
         .withStyle(ChatFormatting.GRAY)
         .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
         .append(coords)
         .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
         .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
      ChatUtils.sendMsg(message);
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (!this.tracerPositions.isEmpty() && this.mc.player != null) {
         double playerX = this.mc.player.getX();
         double playerZ = this.mc.player.getZ();
         this.tracerPositions.entrySet().removeIf(entry -> {
            Vec3 posx = entry.getValue();
            double horizontalDistx = Math.hypot(posx.x - playerX, posx.z - playerZ);
            return horizontalDistx <= this.traceArrivalDistance.get().intValue();
         });
         if (this.renderTracer.get() || this.renderChunkColumn.get()) {
            for (Vec3 pos : this.tracerPositions.values()) {
               double horizontalDist = Math.hypot(pos.x - playerX, pos.z - playerZ);
               if (!(horizontalDist > this.traceMaxDistance.get().intValue())) {
                  if (this.renderTracer.get()) {
                     event.renderer
                        .line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, pos.x, this.mc.player.getEyeY(), pos.z, this.traceColor.get());
                  }

                  if (this.renderChunkColumn.get()) {
                     double x1 = pos.x - 0.5;
                     double x2 = pos.x + 0.5;
                     double z1 = pos.z - 0.5;
                     double z2 = pos.z + 0.5;
                     int bottomY = this.mc.level.getMinY();
                     int topY = bottomY + this.mc.level.dimensionType().height();
                     event.renderer.line(x1, bottomY, z1, x1, topY, z1, this.traceColumnColor.get());
                     event.renderer.line(x1, bottomY, z2, x1, topY, z2, this.traceColumnColor.get());
                     event.renderer.line(x2, bottomY, z1, x2, topY, z1, this.traceColumnColor.get());
                     event.renderer.line(x2, bottomY, z2, x2, topY, z2, this.traceColumnColor.get());
                  }
               }
            }
         }
      }
   }

   public static class Chunk {
      private static final StringBuilder sb = new StringBuilder();
      public ChunkPos chunkPos;
      public transient int x;
      public transient int z;
      public int chests;
      public int barrels;
      public int shulkers;
      public int enderChests;
      public int furnaces;
      public int dispensersDroppers;
      public int hoppers;

      public Chunk(ChunkPos chunkPos) {
         this.chunkPos = chunkPos;
         this.calculatePos();
      }

      public void calculatePos() {
         this.x = this.chunkPos.x() * 16 + 8;
         this.z = this.chunkPos.z() * 16 + 8;
      }

      public int getTotal() {
         return this.chests + this.barrels + this.shulkers + this.enderChests + this.furnaces + this.dispensersDroppers + this.hoppers;
      }

      public void write(Writer writer) throws IOException {
         sb.setLength(0);
         sb.append(this.x).append(',').append(this.z).append(',');
         sb.append(this.chests)
            .append(',')
            .append(this.barrels)
            .append(',')
            .append(this.shulkers)
            .append(',')
            .append(this.enderChests)
            .append(',')
            .append(this.furnaces)
            .append(',')
            .append(this.dispensersDroppers)
            .append(',')
            .append(this.hoppers)
            .append('\n');
         writer.write(sb.toString());
      }

      public boolean countsEqual(StashFinder.Chunk c) {
         return c == null
            ? false
            : this.chests != c.chests
               || this.barrels != c.barrels
               || this.shulkers != c.shulkers
               || this.enderChests != c.enderChests
               || this.furnaces != c.furnaces
               || this.dispensersDroppers != c.dispensersDroppers
               || this.hoppers != c.hoppers;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            StashFinder.Chunk chunk = (StashFinder.Chunk)o;
            return Objects.equals(this.chunkPos, chunk.chunkPos);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.chunkPos);
      }
   }

   private static class ChunkScreen extends WindowScreen {
      private final StashFinder.Chunk chunk;

      public ChunkScreen(GuiTheme theme, StashFinder.Chunk chunk) {
         super(theme, "Chunk at " + chunk.x + ", " + chunk.z);
         this.chunk = chunk;
      }

      @Override
      public void initWidgets() {
         WTable t = this.add(this.theme.table()).expandX().widget();
         t.add(this.theme.label("Total:"));
         t.add(this.theme.label(this.chunk.getTotal() + ""));
         t.row();
         t.add(this.theme.horizontalSeparator()).expandX();
         t.row();
         t.add(this.theme.label("Chests:"));
         t.add(this.theme.label(this.chunk.chests + ""));
         t.row();
         t.add(this.theme.label("Barrels:"));
         t.add(this.theme.label(this.chunk.barrels + ""));
         t.row();
         t.add(this.theme.label("Shulkers:"));
         t.add(this.theme.label(this.chunk.shulkers + ""));
         t.row();
         t.add(this.theme.label("Ender Chests:"));
         t.add(this.theme.label(this.chunk.enderChests + ""));
         t.row();
         t.add(this.theme.label("Furnaces:"));
         t.add(this.theme.label(this.chunk.furnaces + ""));
         t.row();
         t.add(this.theme.label("Dispensers and droppers:"));
         t.add(this.theme.label(this.chunk.dispensersDroppers + ""));
         t.row();
         t.add(this.theme.label("Hoppers:"));
         t.add(this.theme.label(this.chunk.hoppers + ""));
      }
   }

   public enum Mode {
      Chat,
      Toast,
      Both;
   }
}
