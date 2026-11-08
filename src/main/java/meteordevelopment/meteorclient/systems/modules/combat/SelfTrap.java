package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SelfTrap extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("whitelist").description("Which blocks to use.").defaultValue(Blocks.OBSIDIAN, Blocks.NETHERITE_BLOCK).build());
   private final Setting<SelfTrap.TopMode> topPlacement = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("top-mode"))
                  .description("Which positions to place on your top half."))
               .defaultValue(SelfTrap.TopMode.Top))
            .build()
      );
   private final Setting<SelfTrap.BottomMode> bottomPlacement = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("bottom-mode"))
                  .description("Which positions to place on your bottom half."))
               .defaultValue(SelfTrap.BottomMode.None))
            .build()
      );
   private final Setting<Integer> delaySetting = this.sgGeneral
      .add(new IntSetting.Builder().name("place-delay").description("How many ticks between block placements.").defaultValue(1).build());
   private final Setting<Boolean> center = this.sgGeneral
      .add(new BoolSetting.Builder().name("center").description("Centers you on the block you are standing on before placing.").defaultValue(true).build());
   private final Setting<Boolean> turnOff = this.sgGeneral
      .add(new BoolSetting.Builder().name("turn-off").description("Turns off after placing.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the blocks will be placed.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
      );
   private final List<BlockPos> placePositions = new ArrayList<>();
   private boolean placed;
   private int delay;

   public SelfTrap() {
      super(Categories.Combat, "self-trap", "Places blocks above your head.");
   }

   @Override
   public void onActivate() {
      if (!this.placePositions.isEmpty()) {
         this.placePositions.clear();
      }

      this.delay = 0;
      this.placed = false;
      if (this.center.get()) {
         PlayerUtils.centerPlayer();
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      for (Block currentBlock : this.blocks.get()) {
         FindItemResult itemResult = InvUtils.findInHotbar(currentBlock.asItem());
         if (!this.turnOff.get() || (!this.placed || !this.placePositions.isEmpty()) && itemResult.found()) {
            if (itemResult.found()) {
               this.findPlacePos(currentBlock);
               if (this.delay >= this.delaySetting.get() && !this.placePositions.isEmpty()) {
                  BlockPos blockPos = this.placePositions.getLast();
                  if (BlockUtils.place(blockPos, itemResult, this.rotate.get(), 50)) {
                     this.placePositions.remove(blockPos);
                     this.placed = true;
                  }

                  this.delay = 0;
               } else {
                  this.delay++;
               }

               return;
            }

            this.placePositions.clear();
         } else {
            this.toggle();
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.placePositions.isEmpty()) {
         for (BlockPos pos : this.placePositions) {
            event.renderer.box(pos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
         }
      }
   }

   private void findPlacePos(Block block) {
      this.placePositions.clear();
      BlockPos pos = this.mc.player.blockPosition();
      switch ((SelfTrap.TopMode)this.topPlacement.get()) {
         case AntiFacePlace:
            this.add(pos.offset(1, 1, 0), block);
            this.add(pos.offset(-1, 1, 0), block);
            this.add(pos.offset(0, 1, 1), block);
            this.add(pos.offset(0, 1, -1), block);
            break;
         case Full:
            this.add(pos.offset(0, 2, 0), block);
            this.add(pos.offset(1, 1, 0), block);
            this.add(pos.offset(-1, 1, 0), block);
            this.add(pos.offset(0, 1, 1), block);
            this.add(pos.offset(0, 1, -1), block);
            break;
         case Top:
            this.add(pos.offset(0, 2, 0), block);
      }

      if (this.bottomPlacement.get() == SelfTrap.BottomMode.Single) {
         this.add(pos.offset(0, -1, 0), block);
      }
   }

   private void add(BlockPos blockPos, Block block) {
      if (!this.placePositions.contains(blockPos)
         && this.mc.level.getBlockState(blockPos).canBeReplaced()
         && this.mc.level.isUnobstructed(block.defaultBlockState(), blockPos, CollisionContext.empty())) {
         this.placePositions.add(blockPos);
      }
   }

   public enum BottomMode {
      Single,
      None;
   }

   public enum TopMode {
      AntiFacePlace,
      Full,
      Top,
      None;
   }
}
