package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoWeb extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> placeRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("The range at which webs can be placed.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> placeWallsRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to place webs when behind blocks.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to filter targets within range."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The maximum distance to target players.")
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(30.0)
            .build()
      );
   private final Setting<Boolean> predictMovement = this.sgGeneral
      .add(new BoolSetting.Builder().name("predict-movement").description("Predict target movement to account for ping.").defaultValue(true).build());
   private final Setting<Double> ticksToPredict = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("ticks-to-predict")
            .description("How many ticks ahead we should predict for.")
            .defaultValue(10.0)
            .min(1.0)
            .sliderMax(30.0)
            .visible(this.predictMovement::get)
            .build()
      );
   private final Setting<Boolean> doubles = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("doubles")
            .description("Places webs in the target's upper hitbox as well as the lower hitbox.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates towards the webs when placing.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders an overlay where webs are placed.").defaultValue(true).build());
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
            .description("The side color of the placed web rendering.")
            .defaultValue(new SettingColor(239, 231, 244, 31))
            .visible(() -> this.render.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the placed web rendering.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> this.render.get() && this.shapeMode.get().lines())
            .build()
      );
   private final List<BlockPos> placePositions = new ArrayList<>();
   private Player target = null;

   public AutoWeb() {
      super(Categories.Combat, "auto-web", "Automatically places webs on other players.");
   }

   @Override
   public void onActivate() {
      this.target = null;
   }

   @Override
   public void onDeactivate() {
      this.placePositions.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.placePositions.clear();
      if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
         this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), this.priority.get());
         if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
            return;
         }
      }

      FindItemResult webs = InvUtils.findInHotbar(Items.COBWEB);
      if (webs.found()) {
         Vec3 pos = this.target.position();
         if (this.predictMovement.get()) {
            double dx = this.target.getX() - this.target.xo;
            double dy = this.target.getY() - this.target.yo;
            double dz = this.target.getZ() - this.target.zo;
            pos = pos.add(dx * this.ticksToPredict.get(), dy * this.ticksToPredict.get(), dz * this.ticksToPredict.get());
         }

         BlockPos blockPos = BlockPos.containing(pos);
         if (this.canPlaceWebAt(blockPos)) {
            BlockUtils.place(blockPos, webs, this.rotate.get(), 0, false);
            this.placePositions.add(blockPos);
         }

         if (this.doubles.get() && this.canPlaceWebAt(blockPos.above())) {
            BlockUtils.place(blockPos.above(), webs, this.rotate.get(), 0, false);
            this.placePositions.add(blockPos.above());
         }
      }
   }

   private boolean canPlaceWebAt(BlockPos blockPos) {
      return !this.mc.level.getBlockState(blockPos).canBeReplaced() ? false : !this.isOutOfRange(blockPos);
   }

   private boolean isOutOfRange(BlockPos blockPos) {
      Vec3 pos = blockPos.getCenter();
      if (!PlayerUtils.isWithin(pos, this.placeRange.get())) {
         return true;
      }

      ClipContext clipContext = new ClipContext(this.mc.player.getEyePosition(), pos, Block.COLLIDER, Fluid.NONE, this.mc.player);
      BlockHitResult result = this.mc.level.clip(clipContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, this.placeWallsRange.get());
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.placePositions.isEmpty()) {
         for (BlockPos placePosition : this.placePositions) {
            event.renderer.box(placePosition, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
         }
      }
   }
}
