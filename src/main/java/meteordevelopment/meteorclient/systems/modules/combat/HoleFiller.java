package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.DirectionAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class HoleFiller extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSmart = this.settings.createGroup("Smart");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("blocks")
            .description("Which blocks can be used to fill holes.")
            .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.RESPAWN_ANCHOR, Blocks.COBWEB)
            .build()
      );
   private final Setting<Integer> searchRadius = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("search-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
      );
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
   private final Setting<Double> placeWallsRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("How far away from the player you can place a block behind walls.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Boolean> doubles = this.sgGeneral
      .add(new BoolSetting.Builder().name("doubles").description("Fills double holes.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Automatically rotates towards the holes being filled.").defaultValue(false).build());
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("place-delay").description("The ticks delay between placement.").defaultValue(1).min(0).build());
   private final Setting<Integer> blocksPerTick = this.sgGeneral
      .add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks to place in one tick.").defaultValue(3).min(1).build());
   private final Setting<Boolean> smart = this.sgSmart
      .add(new BoolSetting.Builder().name("smart").description("Take more factors into account before filling a hole.").defaultValue(true).build());
   public final Setting<Keybind> forceFill = this.sgSmart
      .add(
         new KeybindSetting.Builder()
            .name("force-fill")
            .description("Fills all holes around you regardless of target checks.")
            .defaultValue(Keybind.none())
            .visible(this.smart::get)
            .build()
      );
   private final Setting<Boolean> predictMovement = this.sgSmart
      .add(
         new BoolSetting.Builder()
            .name("predict-movement")
            .description("Predict target movement to account for ping.")
            .defaultValue(true)
            .visible(this.smart::get)
            .build()
      );
   private final Setting<Double> ticksToPredict = this.sgSmart
      .add(
         new DoubleSetting.Builder()
            .name("ticks-to-predict")
            .description("How many ticks ahead we should predict for.")
            .defaultValue(10.0)
            .min(1.0)
            .sliderMax(30.0)
            .visible(() -> this.smart.get() && this.predictMovement.get())
            .build()
      );
   private final Setting<Boolean> ignoreSafe = this.sgSmart
      .add(new BoolSetting.Builder().name("ignore-safe").description("Ignore players in safe holes.").defaultValue(true).visible(this.smart::get).build());
   private final Setting<Boolean> onlyMoving = this.sgSmart
      .add(
         new BoolSetting.Builder()
            .name("only-moving")
            .description("Ignore players if they're standing still.")
            .defaultValue(true)
            .visible(this.smart::get)
            .build()
      );
   private final Setting<Double> targetRange = this.sgSmart
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("How far away to target players.")
            .defaultValue(7.0)
            .min(0.0)
            .sliderMin(1.0)
            .sliderMax(10.0)
            .visible(this.smart::get)
            .build()
      );
   private final Setting<Double> feetRange = this.sgSmart
      .add(
         new DoubleSetting.Builder()
            .name("feet-range")
            .description("How far from a hole a player's feet must be to fill it.")
            .defaultValue(1.5)
            .min(0.0)
            .sliderMax(4.0)
            .visible(this.smart::get)
            .build()
      );
   private final Setting<Boolean> swing = this.sgRender
      .add(new BoolSetting.Builder().name("swing").description("Swing the player's hand when placing.").defaultValue(true).build());
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
   private final List<Player> targets = new ArrayList<>();
   private final List<HoleFiller.Hole> holes = new ArrayList<>();
   private int timer;

   public HoleFiller() {
      super(Categories.Combat, "hole-filler", "Fills holes with specified blocks.");
   }

   @Override
   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.smart.get()) {
         this.setTargets();
      }

      this.holes.clear();
      FindItemResult block = InvUtils.findInHotbar(itemStack -> this.blocks.get().contains(Block.byItem(itemStack.getItem())));
      if (block.found()) {
         BlockIterator.register(this.searchRadius.get(), this.searchRadius.get(), (blockPos, var2x) -> {
            if (this.validHole(blockPos)) {
               int surroundBlocks = 0;
               Direction air = null;

               for (Direction direction : Direction.values()) {
                  if (direction != Direction.UP) {
                     BlockState state = this.mc.level.getBlockState(blockPos.relative(direction));
                     if (state.getBlock().getExplosionResistance() >= 600.0F) {
                        surroundBlocks++;
                     } else {
                        if (direction == Direction.DOWN) {
                           return;
                        }

                        if (this.validHole(blockPos.relative(direction)) && air == null) {
                           for (Direction dir : Direction.values()) {
                              if (dir != direction.getOpposite() && dir != Direction.UP) {
                                 BlockState state1 = this.mc.level.getBlockState(blockPos.relative(direction).relative(dir));
                                 if (!(state1.getBlock().getExplosionResistance() >= 600.0F)) {
                                    return;
                                 }

                                 surroundBlocks++;
                              }
                           }

                           air = direction;
                        }
                     }

                     if (surroundBlocks == 5 && air == null) {
                        this.holes.add(new HoleFiller.Hole(blockPos, (byte)0));
                     } else if (surroundBlocks == 8 && this.doubles.get() && air != null) {
                        this.holes.add(new HoleFiller.Hole(blockPos, Dir.get(air)));
                     }
                  }
               }
            }
         });
         BlockIterator.after(() -> {
            if (this.timer <= 0 && !this.holes.isEmpty()) {
               int placedCount = 0;

               for (HoleFiller.Hole hole : this.holes) {
                  if (placedCount < this.blocksPerTick.get() && BlockUtils.place(hole.blockPos, block, this.rotate.get(), 10, this.swing.get(), true)) {
                     placedCount++;
                  }
               }

               this.timer = this.placeDelay.get();
            }
         });
         this.timer--;
      }
   }

   @EventHandler(priority = 100)
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.holes.isEmpty()) {
         for (int i = 0; i < this.holes.size(); i++) {
            boolean isNext = i < this.blocksPerTick.get();
            Color side = isNext ? this.nextSideColor.get() : this.sideColor.get();
            Color line = isNext ? this.nextLineColor.get() : this.lineColor.get();
            HoleFiller.Hole hole = this.holes.get(i);
            event.renderer.box(hole.blockPos, side, line, this.shapeMode.get(), hole.exclude);
         }
      }
   }

   private boolean validHole(BlockPos blockPos) {
      if (!BlockUtils.canPlace(blockPos)) {
         return false;
      } else if (!this.mc.level.getBlockState(blockPos.above()).canBeReplaced()) {
         return false;
      } else if (this.isOutOfRange(blockPos)) {
         return false;
      } else {
         return this.smart.get() && !this.forceFill.get().isPressed()
            ? this.targets.stream().anyMatch(target -> target.getY() > blockPos.getY() && this.isCloseToHolePos(target, blockPos))
            : true;
      }
   }

   private void setTargets() {
      this.targets.clear();

      for (Player player : this.mc.level.players()) {
         if (!(player.distanceToSqr(this.mc.player) > Math.pow(this.targetRange.get(), 2.0))
            && !player.isCreative()
            && player != this.mc.player
            && !player.isDeadOrDying()
            && Friends.get().shouldAttack(player)
            && (!this.ignoreSafe.get() || !this.isSurrounded(player))
            && (!this.onlyMoving.get() || player.getX() - player.xo == 0.0 && player.getY() - player.yo == 0.0 && player.getZ() - player.zo == 0.0)) {
            this.targets.add(player);
         }
      }
   }

   private boolean isSurrounded(Player target) {
      for (Direction dir : DirectionAccessor.meteor$getHorizontal()) {
         BlockPos blockPos = target.blockPosition().relative(dir);
         Block block = this.mc.level.getBlockState(blockPos).getBlock();
         if (block.getExplosionResistance() < 600.0F) {
            return false;
         }
      }

      return true;
   }

   private boolean isOutOfRange(BlockPos blockPos) {
      Vec3 pos = blockPos.getCenter().add(0.0, 0.499, 0.0);
      if (!PlayerUtils.isWithin(pos, this.placeRange.get())) {
         return true;
      }

      ClipContext clipContext = new ClipContext(
         this.mc.player.getEyePosition(), pos, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this.mc.player
      );
      BlockHitResult result = this.mc.level.clip(clipContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, this.placeWallsRange.get());
   }

   private boolean isCloseToHolePos(Player target, BlockPos blockPos) {
      Vec3 pos = target.position();
      if (this.predictMovement.get()) {
         double dx = target.getX() - target.xo;
         double dy = target.getY() - target.yo;
         double dz = target.getZ() - target.zo;
         pos = pos.add(dx * this.ticksToPredict.get(), dy * this.ticksToPredict.get(), dz * this.ticksToPredict.get());
      }

      double i = pos.x - (blockPos.getX() + 0.5);
      double j = pos.y - (blockPos.getY() + 1.0);
      double k = pos.z - (blockPos.getZ() + 0.5);
      double distance = Math.sqrt(i * i + j * j + k * k);
      return distance < this.feetRange.get();
   }

   private static class Hole {
      private final MutableBlockPos blockPos = new MutableBlockPos();
      private final byte exclude;

      public Hole(BlockPos blockPos, byte exclude) {
         this.blockPos.set(blockPos);
         this.exclude = exclude;
      }
   }
}
