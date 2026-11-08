package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoTrap extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("whitelist").description("Which blocks to use.").defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN).build());
   private final Setting<Double> placeRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("The range at which blocks can be placed.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> placeWallsRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to place when behind blocks.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the player to target."))
               .defaultValue(SortPriority.LowestHealth))
            .build()
      );
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The maximum distance to target players.")
            .defaultValue(3.0)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("place-delay").description("How many ticks between block placements.").defaultValue(1).build());
   private final Setting<Integer> blocksPerTick = this.sgGeneral
      .add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks to place in one tick.").defaultValue(1).min(1).build());
   private final Setting<AutoTrap.TopMode> topPlacement = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("top-blocks"))
                  .description("Which blocks to place on the top half of the target."))
               .defaultValue(AutoTrap.TopMode.Full))
            .build()
      );
   private final Setting<AutoTrap.BottomMode> bottomPlacement = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("bottom-blocks"))
                  .description("Which blocks to place on the bottom half of the target."))
               .defaultValue(AutoTrap.BottomMode.Platform))
            .build()
      );
   private final Setting<Boolean> selfToggle = this.sgGeneral
      .add(new BoolSetting.Builder().name("self-toggle").description("Turns off after placing all blocks.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates towards blocks when placing.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How the shapes are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .visible(() -> this.render.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .visible(() -> this.render.get() && this.shapeMode.get().lines())
            .build()
      );
   private final Setting<SettingColor> nextSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("next-side-color")
            .description("The side color of the next block to be placed.")
            .defaultValue(new SettingColor(227, 196, 245, 10))
            .visible(() -> this.render.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> nextLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("next-line-color")
            .description("The line color of the next block to be placed.")
            .defaultValue(new SettingColor(5, 139, 221))
            .visible(() -> this.render.get() && this.shapeMode.get().lines())
            .build()
      );
   private final List<BlockPos> placePositions = new ArrayList<>();
   private Player target;
   private boolean placed;
   private int timer;

   public AutoTrap() {
      super(Categories.Combat, "auto-trap", "Traps people in a box to prevent them from moving.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.placePositions.clear();
      this.timer = 0;
      this.placed = false;
   }

   @Override
   public void onDeactivate() {
      this.placePositions.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.selfToggle.get() && this.placed && this.placePositions.isEmpty()) {
         this.placed = false;
         this.toggle();
      } else {
         FindItemResult block = InvUtils.findInHotbar(itemStack -> this.blocks.get().contains(Block.byItem(itemStack.getItem())));
         if (block.found()) {
            if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
               this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), this.priority.get());
               if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
                  return;
               }
            }

            this.fillPlaceArray(this.target);
            if (this.timer >= this.delay.get() && !this.placePositions.isEmpty()) {
               int placedCount = 0;

               for (BlockPos placePosition : this.placePositions) {
                  if (placedCount < this.blocksPerTick.get() && BlockUtils.place(placePosition, block, this.rotate.get(), 50, true)) {
                     this.placed = true;
                     placedCount++;
                  }
               }

               this.timer = 0;
            } else {
               this.timer++;
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.placePositions.isEmpty()) {
         for (int i = 0; i < this.placePositions.size(); i++) {
            boolean isNext = i < this.blocksPerTick.get();
            Color side = isNext ? this.nextSideColor.get() : this.sideColor.get();
            Color line = isNext ? this.nextLineColor.get() : this.lineColor.get();
            event.renderer.box(this.placePositions.get(i), side, line, this.shapeMode.get(), 0);
         }
      }
   }

   private void fillPlaceArray(Player target) {
      this.placePositions.clear();
      double epsilon = 1.0E-5;
      AABB box = target.getBoundingBox();
      List<BlockPos> corners = new ArrayList<>();
      corners.add(BlockPos.containing(box.minX, box.minY, box.minZ));
      corners.add(BlockPos.containing(box.minX, box.minY, box.maxZ - epsilon));
      corners.add(BlockPos.containing(box.maxX - epsilon, box.minY, box.minZ));
      corners.add(BlockPos.containing(box.maxX - epsilon, box.minY, box.maxZ - epsilon));

      for (BlockPos targetPos : new LinkedHashSet<>(corners)) {
         switch ((AutoTrap.TopMode)this.topPlacement.get()) {
            case Full:
               this.add(targetPos.offset(0, 2, 0));
               this.add(targetPos.offset(1, 1, 0));
               this.add(targetPos.offset(-1, 1, 0));
               this.add(targetPos.offset(0, 1, 1));
               this.add(targetPos.offset(0, 1, -1));
               break;
            case Top:
               this.add(targetPos.offset(0, 2, 0));
               break;
            case Face:
               this.add(targetPos.offset(1, 1, 0));
               this.add(targetPos.offset(-1, 1, 0));
               this.add(targetPos.offset(0, 1, 1));
               this.add(targetPos.offset(0, 1, -1));
         }

         switch ((AutoTrap.BottomMode)this.bottomPlacement.get()) {
            case Single:
               this.add(targetPos.offset(0, -1, 0));
               break;
            case Platform:
               this.add(targetPos.offset(0, -1, 0));
               this.add(targetPos.offset(1, -1, 0));
               this.add(targetPos.offset(-1, -1, 0));
               this.add(targetPos.offset(0, -1, 1));
               this.add(targetPos.offset(0, -1, -1));
               break;
            case Full:
               this.add(targetPos.offset(0, -1, 0));
               this.add(targetPos.offset(1, 0, 0));
               this.add(targetPos.offset(-1, 0, 0));
               this.add(targetPos.offset(0, 0, -1));
               this.add(targetPos.offset(0, 0, 1));
         }
      }

      double pX = this.mc.player.getX();
      double pY = this.mc.player.getY();
      double pZ = this.mc.player.getZ();
      this.placePositions
         .sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * -1.0));
   }

   private void add(BlockPos blockPos) {
      if (!this.placePositions.contains(blockPos)) {
         if (BlockUtils.canPlace(blockPos)) {
            if (!this.isOutOfRange(blockPos)) {
               this.placePositions.add(blockPos);
            }
         }
      }
   }

   private boolean isOutOfRange(BlockPos blockPos) {
      Vec3 pos = blockPos.getCenter();
      if (!PlayerUtils.isWithin(pos, this.placeRange.get())) {
         return true;
      }

      ClipContext clipContext = new ClipContext(
         this.mc.player.getEyePosition(), pos, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this.mc.player
      );
      BlockHitResult result = this.mc.level.clip(clipContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, this.placeWallsRange.get());
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }

   public enum BottomMode {
      Single,
      Platform,
      Full,
      None;
   }

   public enum TopMode {
      Full,
      Top,
      Face,
      None;
   }
}
