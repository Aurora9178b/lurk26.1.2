package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AnchorAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPlace = this.settings.createGroup("Place");
   private final SettingGroup sgBreak = this.settings.createGroup("Break");
   private final SettingGroup sgPause = this.settings.createGroup("Pause");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder().name("target-range").description("Range in which to target players.").defaultValue(10.0).min(0.0).sliderMax(16.0).build()
      );
   private final Setting<SortPriority> targetPriority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the player to target."))
               .defaultValue(SortPriority.LowestHealth))
            .build()
      );
   private final Setting<Double> minDamage = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage to inflict on your target.")
            .defaultValue(7.0)
            .min(0.0)
            .sliderMax(36.0)
            .build()
      );
   private final Setting<Double> maxSelfDamage = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum damage to inflict on yourself.")
            .defaultValue(7.0)
            .min(0.0)
            .sliderMax(36.0)
            .build()
      );
   private final Setting<Boolean> antiSuicide = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break anchors if they will kill you.").defaultValue(true).build());
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(new BoolSetting.Builder().name("swap-back").description("Switches to your previous slot after using anchors.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates server-side towards the anchors being placed/broken.").defaultValue(true).build());
   private final Setting<Boolean> place = this.sgPlace
      .add(new BoolSetting.Builder().name("place").description("Allows Anchor Aura to place anchors.").defaultValue(true).build());
   private final Setting<Integer> placeDelay = this.sgPlace
      .add(
         new IntSetting.Builder()
            .name("place-delay")
            .description("The tick delay between placing anchors.")
            .defaultValue(5)
            .range(0, 10)
            .visible(this.place::get)
            .build()
      );
   private final Setting<Double> placeRange = this.sgPlace
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("The range at which anchors can be placed.")
            .defaultValue(4.0)
            .range(0.0, 6.0)
            .visible(this.place::get)
            .build()
      );
   private final Setting<Double> placeWallsRange = this.sgPlace
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to place anchors when behind blocks.")
            .defaultValue(4.0)
            .range(0.0, 6.0)
            .visible(this.place::get)
            .build()
      );
   private final Setting<Boolean> airPlace = this.sgPlace
      .add(
         new BoolSetting.Builder()
            .name("air-place")
            .description("Allows Anchor Aura to place anchors in the air.")
            .defaultValue(true)
            .visible(this.place::get)
            .build()
      );
   private final Setting<Integer> chargeDelay = this.sgBreak
      .add(new IntSetting.Builder().name("charge-delay").description("The tick delay it takes to charge anchors.").defaultValue(1).range(0, 10).build());
   private final Setting<Integer> breakDelay = this.sgBreak
      .add(new IntSetting.Builder().name("break-delay").description("The tick delay it takes to break anchors.").defaultValue(1).range(0, 10).build());
   private final Setting<Double> breakRange = this.sgBreak
      .add(new DoubleSetting.Builder().name("break-range").description("Range in which to break anchors.").defaultValue(4.5).min(0.0).sliderMax(6.0).build());
   private final Setting<Double> breakWallsRange = this.sgBreak
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to break anchors when behind blocks.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Boolean> pauseOnUse = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-use").description("Pauses while using an item.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnMine = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining blocks.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnCA = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-CA").description("Pauses while Crystal Aura is placing.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgRender
      .add(new BoolSetting.Builder().name("swing").description("Whether to swing your hand client-side.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing an anchor.").defaultValue(true).build());
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
            .description("The side color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211, 41))
            .visible(() -> this.render.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211))
            .visible(() -> this.render.get() && this.shapeMode.get().lines())
            .build()
      );
   private double bestPlaceDamage;
   private final MutableBlockPos bestPlacePos = new MutableBlockPos();
   private double bestBreakDamage;
   private final MutableBlockPos bestBreakPos = new MutableBlockPos();
   private BlockPos renderBlockPos;
   private int placeDelayLeft;
   private int chargeDelayLeft;
   private int breakDelayLeft;
   private Player target;

   public AnchorAura() {
      super(Categories.Combat, "anchor-aura", "Automatically places and breaks Respawn Anchors to harm entities.");
   }

   @Override
   public void onActivate() {
      this.renderBlockPos = null;
      this.placeDelayLeft = this.placeDelay.get();
      this.chargeDelayLeft = 0;
      this.breakDelayLeft = 0;
      this.target = null;
   }

   @Override
   public void onDeactivate() {
      this.renderBlockPos = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.level.dimension() == Level.NETHER) {
         this.error("You can't blow up respawn anchors in this dimension, disabling.");
         this.toggle();
      } else if (this.shouldPause()) {
         this.renderBlockPos = null;
      } else {
         if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
            this.renderBlockPos = null;
            this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), this.targetPriority.get());
            if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
               return;
            }
         }

         this.doAnchorAura();
      }
   }

   private void doAnchorAura() {
      this.bestPlaceDamage = 0.0;
      this.bestBreakDamage = 0.0;
      int iteratorRange = (int)Math.ceil(Math.max(this.placeRange.get(), this.breakRange.get()));
      BlockIterator.register(
         iteratorRange,
         iteratorRange,
         (blockPos, blockState) -> {
            boolean isPlacing = blockState.getBlock() != Blocks.RESPAWN_ANCHOR;
            double baseRange = isPlacing ? this.placeRange.get() : this.breakRange.get();
            double wallsRange = isPlacing ? this.placeWallsRange.get() : this.breakWallsRange.get();
            if (!this.isOutOfRange(blockPos, baseRange, wallsRange)) {
               if (isPlacing) {
                  if (!BlockUtils.canPlace(blockPos)) {
                     return;
                  }

                  if (!this.airPlace.get() && this.isAirPlace(blockPos)) {
                     return;
                  }
               }

               float bestDamage = isPlacing ? (float)this.bestPlaceDamage : (float)this.bestBreakDamage;
               float selfDamage = DamageUtils.anchorDamage(this.mc.player, blockPos.getCenter());
               float targetDamage = DamageUtils.anchorDamage(this.target, blockPos.getCenter());
               if (targetDamage >= this.minDamage.get()
                  && targetDamage > bestDamage
                  && (!this.antiSuicide.get() || selfDamage <= this.maxSelfDamage.get())
                  && (!this.antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0.0F)) {
                  if (isPlacing) {
                     this.bestPlaceDamage = targetDamage;
                     this.bestPlacePos.set(blockPos);
                  } else {
                     this.bestBreakDamage = targetDamage;
                     this.bestBreakPos.set(blockPos);
                  }
               }
            }
         }
      );
      BlockIterator.after(() -> {
         this.renderBlockPos = null;
         FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
         FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
         if (this.bestBreakDamage > 0.0) {
            this.doBreak(glowStone);
         } else if (this.bestPlaceDamage > 0.0 && this.place.get() && anchor.found() && glowStone.found()) {
            this.doPlace(anchor);
         }
      });
   }

   private void doPlace(FindItemResult anchor) {
      this.renderBlockPos = this.bestPlacePos;
      if (this.placeDelayLeft++ >= this.placeDelay.get()) {
         BlockUtils.place(this.bestPlacePos, anchor, this.rotate.get(), 50, this.swing.get(), false, this.swapBack.get());
         this.placeDelayLeft = 0;
      }
   }

   private void doBreak(FindItemResult glowStone) {
      this.renderBlockPos = this.bestBreakPos;
      if (this.rotate.get()) {
         Rotations.rotate(Rotations.getYaw(this.bestBreakPos), Rotations.getPitch(this.bestBreakPos), 40, () -> this.doInteract(glowStone));
      } else {
         this.doInteract(glowStone);
      }
   }

   private void doInteract(FindItemResult glowStone) {
      BlockState blockState = this.mc.level.getBlockState(this.bestBreakPos);
      if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR) {
         Vec3 center = this.bestBreakPos.getCenter();
         int charges = blockState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
         if (charges == 0 && this.chargeDelayLeft++ >= this.chargeDelay.get()) {
            if (!glowStone.found()) {
               return;
            }

            InvUtils.swap(glowStone.slot(), this.swapBack.get());
            BlockUtils.interact(
               new BlockHitResult(center, BlockUtils.getDirection(this.bestBreakPos), this.bestBreakPos, true), InteractionHand.MAIN_HAND, this.swing.get()
            );
            this.chargeDelayLeft = 0;
            charges++;
         }

         if (charges > 0 && this.breakDelayLeft++ >= this.breakDelay.get()) {
            FindItemResult fir = InvUtils.findInHotbar(item -> !item.getItem().equals(Items.GLOWSTONE));
            if (!fir.found()) {
               return;
            }

            InvUtils.swap(fir.slot(), this.swapBack.get());
            BlockUtils.interact(
               new BlockHitResult(center, BlockUtils.getDirection(this.bestBreakPos), this.bestBreakPos, true), InteractionHand.MAIN_HAND, this.swing.get()
            );
            this.breakDelayLeft = 0;
            this.mc.level.setBlock(this.bestBreakPos, this.mc.level.getFluidState(this.bestBreakPos).createLegacyBlock(), 0);
         }

         if (this.swapBack.get()) {
            InvUtils.swapBack();
         }
      }
   }

   private boolean isOutOfRange(BlockPos blockPos, double baseRange, double wallsRange) {
      Vec3 pos = blockPos.getCenter();
      if (!PlayerUtils.isWithin(pos, baseRange)) {
         return true;
      }

      ClipContext clipContext = new ClipContext(this.mc.player.getEyePosition(), pos, Block.COLLIDER, Fluid.NONE, this.mc.player);
      BlockHitResult result = this.mc.level.clip(clipContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, wallsRange);
   }

   private boolean isAirPlace(BlockPos blockPos) {
      for (Direction direction : Direction.values()) {
         if (!this.mc.level.getBlockState(blockPos.relative(direction)).canBeReplaced()) {
            return false;
         }
      }

      return true;
   }

   private boolean shouldPause() {
      if (this.pauseOnUse.get() && this.mc.player.isUsingItem()) {
         return true;
      }

      if (this.pauseOnMine.get() && this.mc.gameMode.isDestroying()) {
         return true;
      }

      CrystalAura CA = Modules.get().get(CrystalAura.class);
      return this.pauseOnCA.get() && CA.isActive() && CA.kaTimer > 0;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && this.renderBlockPos != null) {
         event.renderer.box(this.renderBlockPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }
}
