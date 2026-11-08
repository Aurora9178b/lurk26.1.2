package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;

public class InstantRebreak extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Integer> tickDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The delay between break attempts.").defaultValue(0).min(0).sliderMax(20).build());
   private final Setting<Boolean> pick = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-pick").description("Only tries to mine the block if you are holding a pickaxe.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Faces the block being mined server side.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders an overlay on the block being broken.").defaultValue(true).build());
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
   public final MutableBlockPos blockPos = new MutableBlockPos(0, Integer.MIN_VALUE, 0);
   private int ticks;
   private Direction direction;

   public InstantRebreak() {
      super(Categories.Player, "instant-rebreak", "Instantly re-breaks blocks in the same position.");
   }

   @Override
   public void onActivate() {
      this.ticks = 0;
      this.blockPos.set(0, -1, 0);
   }

   @EventHandler
   private void onStartBreakingBlock(StartBreakingBlockEvent event) {
      this.direction = event.direction;
      this.blockPos.set(event.blockPos);
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.ticks >= this.tickDelay.get()) {
         this.ticks = 0;
         if (this.shouldMine()) {
            if (this.rotate.get()) {
               Rotations.rotate(Rotations.getYaw(this.blockPos), Rotations.getPitch(this.blockPos), this::sendPacket);
            } else {
               this.sendPacket();
            }

            this.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }
      } else {
         this.ticks++;
      }
   }

   public void sendPacket() {
      this.mc
         .gameMode
         .startPrediction(
            this.mc.level,
            sequence -> new ServerboundPlayerActionPacket(
               Action.STOP_DESTROY_BLOCK, this.blockPos, this.direction == null ? Direction.UP : this.direction, sequence
            )
         );
   }

   public boolean shouldMine() {
      return !this.mc.level.isOutsideBuildHeight(this.blockPos) && BlockUtils.canBreak(this.blockPos)
         ? !this.pick.get() || this.mc.player.getMainHandItem().is(ItemTags.PICKAXES)
         : false;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && this.shouldMine()) {
         event.renderer.box(this.blockPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      }
   }
}
