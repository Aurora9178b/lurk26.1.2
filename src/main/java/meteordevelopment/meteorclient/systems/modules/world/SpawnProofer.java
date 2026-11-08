package meteordevelopment.meteorclient.systems.modules.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnProofer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("place-delay").description("The tick delay between placing blocks.").defaultValue(1).range(0, 10).build());
   private final Setting<Double> placeRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("How far away from the player you can place a block.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> wallsRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("How far away from the player you can place a block behind walls.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Integer> blocksPerTick = this.sgGeneral
      .add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks to place in one tick.").defaultValue(1).min(1).build());
   private final Setting<Integer> lightLevel = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("light-level")
            .description("Light levels to spawn proof. Old spawning light: 7.")
            .defaultValue(0)
            .min(0)
            .sliderMax(15)
            .build()
      );
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("blocks")
            .description("Block to use for spawn proofing.")
            .defaultValue(Blocks.TORCH, Blocks.STONE_BUTTON, Blocks.STONE_SLAB)
            .filter(this::filterBlocks)
            .build()
      );
   private final Setting<SpawnProofer.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("Which spawn types should be spawn proofed."))
               .defaultValue(SpawnProofer.Mode.Both))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates towards the blocks being placed.").defaultValue(true).build());
   private final Pool<MutableBlockPos> spawnPool = new Pool<>(MutableBlockPos::new);
   private final List<MutableBlockPos> spawns = new ArrayList<>();
   private int timer;

   public SpawnProofer() {
      super(Categories.World, "spawn-proofer", "Automatically spawnproofs unlit areas.");
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      if (this.timer >= this.placeDelay.get()) {
         boolean foundBlock = InvUtils.testInHotbar(itemStack -> this.blocks.get().contains(Block.byItem(itemStack.getItem())));
         if (!foundBlock) {
            this.error("Found none of the chosen blocks in hotbar.");
            this.toggle();
         } else {
            this.spawnPool.freeAll(this.spawns);
            this.spawns.clear();
            BlockIterator.register(
               (int)Math.ceil(this.placeRange.get()),
               (int)Math.ceil(this.placeRange.get()),
               (blockPos, blockState) -> {
                  BlockUtils.MobSpawn spawn = BlockUtils.isValidMobSpawn(blockPos, blockState, this.lightLevel.get());
                  if (spawn == BlockUtils.MobSpawn.Always && (this.mode.get() == SpawnProofer.Mode.Always || this.mode.get() == SpawnProofer.Mode.Both)
                     || spawn == BlockUtils.MobSpawn.Potential && (this.mode.get() == SpawnProofer.Mode.Potential || this.mode.get() == SpawnProofer.Mode.Both)
                     )
                   {
                     if (!BlockUtils.canPlace(blockPos)) {
                        return;
                     }

                     if (this.isOutOfRange(blockPos)) {
                        return;
                     }

                     this.spawns.add(this.spawnPool.get().set(blockPos));
                  }
               }
            );
         }
      }
   }

   @EventHandler
   private void onTickPost(TickEvent.Post event) {
      if (this.timer++ >= this.placeDelay.get()) {
         if (!this.spawns.isEmpty()) {
            FindItemResult block = InvUtils.findInHotbar(itemStack -> this.blocks.get().contains(Block.byItem(itemStack.getItem())));
            if (!block.found()) {
               this.error("Found none of the chosen blocks in hotbar.");
               this.toggle();
            } else {
               int placedCount = 0;
               if (this.isLightSource(Block.byItem(this.mc.player.getInventory().getItem(block.slot()).getItem()))) {
                  this.spawns.sort(Comparator.comparingInt(blockPosx -> this.mc.level.getMaxLocalRawBrightness(blockPosx)));
                  placedCount = this.blocksPerTick.get() - 1;
               }

               for (BlockPos blockPos : this.spawns) {
                  if (placedCount < this.blocksPerTick.get() && BlockUtils.place(blockPos, block, this.rotate.get(), -50, false)) {
                     placedCount++;
                  }
               }

               this.timer = 0;
            }
         }
      }
   }

   private boolean isOutOfRange(BlockPos blockPos) {
      Vec3 pos = blockPos.getCenter();
      if (!PlayerUtils.isWithin(pos, this.placeRange.get())) {
         return true;
      }

      ClipContext raycastContext = new ClipContext(
         this.mc.player.getEyePosition(), pos, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this.mc.player
      );
      BlockHitResult result = this.mc.level.clip(raycastContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, this.wallsRange.get());
   }

   private boolean filterBlocks(Block block) {
      return this.isNonOpaqueBlock(block) || this.isLightSource(block);
   }

   private boolean isNonOpaqueBlock(Block block) {
      return block instanceof ButtonBlock
         || block instanceof SlabBlock
         || block instanceof BasePressurePlateBlock
         || block instanceof TransparentBlock
         || block instanceof TripWireBlock
         || block instanceof CarpetBlock
         || block instanceof LeverBlock
         || block instanceof DiodeBlock
         || block instanceof BaseRailBlock;
   }

   private boolean isLightSource(Block block) {
      return block.defaultBlockState().getLightEmission() > 0;
   }

   public enum Mode {
      Always,
      Potential,
      Both;
   }
}
